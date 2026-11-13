package com.fcf.videoconverter.media_engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.fcf.videoconverter.core.system.MemoryMonitor
import com.fcf.videoconverter.core.system.StorageManager
import com.fcf.videoconverter.domain.model.CompressionConfig
import com.fcf.videoconverter.domain.model.ConversionConfig
import com.fcf.videoconverter.domain.model.ProcessingState
import com.fcf.videoconverter.domain.model.ResolutionPreset
import com.fcf.videoconverter.domain.model.VideoContainer

/**
 * WorkManager foreground worker enabling resilient offline background conversion
 * and recovery across process interruptions.
 */
@androidx.media3.common.util.UnstableApi
class MediaProcessingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo(0, "FCF Video Converter processing locally"))

        val mode = inputData.getString("mode") ?: return Result.failure()
        val sourceUri = inputData.getString("sourceUri") ?: return Result.failure()
        val outputName = inputData.getString("outputName") ?: "fcf_output"

        val storage = StorageManager(applicationContext)
        val memory = MemoryMonitor(applicationContext)
        val processor = Media3VideoProcessor(applicationContext, storage, memory)

        val flow = when (mode) {
            "compress" -> processor.compressVideo(
                CompressionConfig(
                    sourceUri = sourceUri,
                    outputName = outputName,
                    resolution = ResolutionPreset.valueOf(inputData.getString("resolution") ?: ResolutionPreset.P720.name)
                )
            )
            else -> processor.convertVideo(
                ConversionConfig(
                    sourceUri = sourceUri,
                    outputName = outputName,
                    container = VideoContainer.valueOf(inputData.getString("container") ?: VideoContainer.MP4.name),
                    resolution = ResolutionPreset.valueOf(inputData.getString("resolution") ?: ResolutionPreset.P720.name)
                )
            )
        }

        var succeeded = false
        flow.collect { state ->
            when (state) {
                is ProcessingState.Running -> {
                    setForeground(createForegroundInfo(state.progressPercent, state.stageLabel))
                }
                is ProcessingState.Completed -> succeeded = true
                is ProcessingState.Failed, ProcessingState.Cancelled -> succeeded = false
                ProcessingState.Idle -> Unit
            }
        }
        return if (succeeded) Result.success() else Result.retry()
    }

    private fun createForegroundInfo(progress: Int, text: String): ForegroundInfo {
        val channelId = "fcf_media_processing"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Media Processing", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("FCF Video Converter")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1001, notification)
        }
    }
}
