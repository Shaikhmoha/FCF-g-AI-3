package com.fcf.videoconverter.media_engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Crop
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.*
import com.fcf.videoconverter.core.system.MemoryMonitor
import com.fcf.videoconverter.core.system.StorageManager
import com.fcf.videoconverter.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import kotlin.math.roundToInt

/**
 * Primary Apache-2.0 hardware-accelerated media processor powered by AndroidX Media3 Transformer.
 * Never overwrites original user files; writes to app temp directory first, then commits to MediaStore.
 */
@UnstableApi
class Media3VideoProcessor(
    private val context: Context,
    private val storageManager: StorageManager,
    private val memoryMonitor: MemoryMonitor
) {

    fun convertVideo(config: ConversionConfig): Flow<ProcessingState> = callbackFlow {
        if (!config.container.media3Supported) {
            trySend(
                ProcessingState.Failed(
                    message = "${config.container.name} container muxing requires optional native FFmpeg JNI plugin; Media3 Transformer natively supports MP4.",
                    recoverable = false
                )
            )
            close()
            return@callbackFlow
        }

        if (config.resolution == ResolutionPreset.P2160 && !memoryMonitor.supports4kHardware()) {
            trySend(
                ProcessingState.Failed(
                    message = "4K export disabled on this device due to 6 GB RAM headroom threshold. Select 1080p or 720p.",
                    recoverable = true
                )
            )
            close()
            return@callbackFlow
        }

        val tempOut = File(storageManager.tempDir, "conv_${System.currentTimeMillis()}.mp4")
        val mediaItem = MediaItem.fromUri(Uri.parse(config.sourceUri))
        val effects = buildList<Effect> {
            add(Presentation.createForHeight(config.resolution.height))
        }
        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effects))
            .build()

        runTransformer(
            editedItems = listOf(editedItem),
            tempOut = tempOut,
            videoMimeType = config.codec.mimeType,
            displayName = "${config.outputName}.mp4",
            isAudioOnly = false,
            sendState = { trySend(it) }
        ) { close() }

        awaitClose()
    }

    fun compressVideo(config: CompressionConfig): Flow<ProcessingState> = callbackFlow {
        val tempOut = File(storageManager.tempDir, "comp_${System.currentTimeMillis()}.mp4")
        val mediaItem = MediaItem.fromUri(Uri.parse(config.sourceUri))
        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(config.resolution.height))))
            .build()

        runTransformer(
            editedItems = listOf(editedItem),
            tempOut = tempOut,
            videoMimeType = MimeTypes.VIDEO_H264,
            displayName = "${config.outputName}_compressed.mp4",
            isAudioOnly = false,
            sendState = { trySend(it) }
        ) { close() }

        awaitClose()
    }

    fun extractAudio(sourceUri: String, outputName: String): Flow<ProcessingState> = callbackFlow {
        val tempOut = File(storageManager.tempDir, "audio_${System.currentTimeMillis()}.m4a")
        val mediaItem = MediaItem.fromUri(Uri.parse(sourceUri))
        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .build()

        runTransformer(
            editedItems = listOf(editedItem),
            tempOut = tempOut,
            videoMimeType = null,
            displayName = "$outputName.m4a",
            isAudioOnly = true,
            sendState = { trySend(it) }
        ) { close() }

        awaitClose()
    }

    fun trimAndTransformVideo(
        sourceUri: String,
        outputName: String,
        startMs: Long,
        endMs: Long,
        rotationDegrees: Float,
        targetHeight: Int,
        cropNormalized: FloatArray? = null
    ): Flow<ProcessingState> = callbackFlow {
        val tempOut = File(storageManager.tempDir, "edit_${System.currentTimeMillis()}.mp4")
        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(endMs)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(sourceUri))
            .setClippingConfiguration(clipping)
            .build()

        val videoEffects = mutableListOf<Effect>()
        if (cropNormalized != null && cropNormalized.size == 4) {
            videoEffects.add(Crop(cropNormalized[0], cropNormalized[1], cropNormalized[2], cropNormalized[3]))
        }
        if (rotationDegrees != 0f) {
            videoEffects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees(rotationDegrees)
                    .build()
            )
        }
        videoEffects.add(Presentation.createForHeight(targetHeight))

        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        runTransformer(
            editedItems = listOf(editedItem),
            tempOut = tempOut,
            videoMimeType = MimeTypes.VIDEO_H264,
            displayName = "${outputName}_edited.mp4",
            isAudioOnly = false,
            sendState = { trySend(it) }
        ) { close() }

        awaitClose()
    }

    fun mergeVideos(sourceUris: List<String>, outputName: String): Flow<ProcessingState> = callbackFlow {
        val tempOut = File(storageManager.tempDir, "merge_${System.currentTimeMillis()}.mp4")
        val items = sourceUris.map { uri ->
            EditedMediaItem.Builder(MediaItem.fromUri(Uri.parse(uri))).build()
        }
        runTransformer(
            editedItems = items,
            tempOut = tempOut,
            videoMimeType = MimeTypes.VIDEO_H264,
            displayName = "${outputName}_merged.mp4",
            isAudioOnly = false,
            sendState = { trySend(it) }
        ) { close() }

        awaitClose()
    }

    private fun runTransformer(
        editedItems: List<EditedMediaItem>,
        tempOut: File,
        videoMimeType: String?,
        displayName: String,
        isAudioOnly: Boolean,
        sendState: (ProcessingState) -> Unit,
        onFinished: () -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        val progressHolder = ProgressHolder()
        val startTimeMs = System.currentTimeMillis()
        var transformer: Transformer? = null

        val pollRunnable = object : Runnable {
            override fun run() {
                val active = transformer ?: return
                val state = active.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    val pct = progressHolder.progress.coerceIn(0, 99)
                    val elapsedSec = ((System.currentTimeMillis() - startTimeMs) / 1000L).coerceAtLeast(1L)
                    val eta = if (pct > 3) ((elapsedSec * (100 - pct)) / pct.toDouble()).roundToInt().toLong() else null
                    sendState(ProcessingState.Running(pct, eta, "Encoding on device"))
                }
                mainHandler.postDelayed(this, 350L)
            }
        }

        mainHandler.post {
            val builder = Transformer.Builder(context)
            if (videoMimeType != null) {
                builder.setVideoMimeType(videoMimeType)
            }
            if (isAudioOnly) {
                builder.setAudioMimeType(MimeTypes.AUDIO_AAC)
            }
            builder.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    mainHandler.removeCallbacks(pollRunnable)
                    val publicUri = commitTempToMediaStore(tempOut, displayName, isAudioOnly)
                    val sizeBytes = tempOut.length()
                    tempOut.delete()
                    sendState(ProcessingState.Completed(publicUri.toString(), sizeBytes))
                    onFinished()
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    mainHandler.removeCallbacks(pollRunnable)
                    tempOut.delete()
                    sendState(
                        ProcessingState.Failed(
                            message = exportException.localizedMessage ?: "Media3 export failed",
                            recoverable = true
                        )
                    )
                    onFinished()
                }
            })

            val activeTransformer = builder.build()
            transformer = activeTransformer
            val sequence = EditedMediaItemSequence(editedItems)
            val composition = Composition.Builder(listOf(sequence)).build()
            sendState(ProcessingState.Running(0, null, "Preparing hardware codec"))
            activeTransformer.start(composition, tempOut.absolutePath)
            mainHandler.postDelayed(pollRunnable, 350L)
        }
    }

    private fun commitTempToMediaStore(tempFile: File, displayName: String, isAudio: Boolean): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dest = File(storageManager.exportsFallbackDir, displayName)
            tempFile.copyTo(dest, overwrite = true)
            return Uri.fromFile(dest)
        }
        val resolver = context.contentResolver
        val collection = if (isAudio) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relativeDir = if (isAudio) {
            "${Environment.DIRECTORY_MUSIC}/FCFVideoConverter"
        } else {
            "${Environment.DIRECTORY_MOVIES}/FCFVideoConverter"
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, if (isAudio) "audio/mp4" else "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val targetUri = resolver.insert(collection, values)
            ?: return Uri.fromFile(tempFile.copyTo(File(storageManager.exportsFallbackDir, displayName), overwrite = true))

        resolver.openOutputStream(targetUri)?.use { out ->
            tempFile.inputStream().use { input -> input.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(targetUri, values, null, null)
        return targetUri
    }
}
