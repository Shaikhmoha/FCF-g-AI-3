package com.fcf.videoconverter.ai_engine

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.fcf.videoconverter.core.system.MemoryMonitor
import com.fcf.videoconverter.domain.model.*
import com.fcf.videoconverter.domain.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Enforces strict single-model-at-a-time memory discipline for 6 GB RAM phones.
 * Unloads any active ONNX session before loading the next pipeline stage.
 */
class OnnxSessionManager(
    private val memoryMonitor: MemoryMonitor
) {
    private val mutex = Mutex()
    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }
    private var activeModelId: String? = null
    private var activeSession: OrtSession? = null

    suspend fun <T> withModelSession(
        spec: AiModelSpec,
        modelFile: File,
        block: suspend (OrtEnvironment, OrtSession) -> T
    ): Result<T> = mutex.withLock {
        withContext(Dispatchers.Default) {
            runCatching {
                if (activeModelId != spec.id) {
                    unloadLocked()
                    require(memoryMonitor.isSafeForHeavyAiModel(spec.estimatedRamMb)) {
                        "Insufficient available RAM for ${spec.displayName}. Needs ~${spec.estimatedRamMb} MB free."
                    }
                    val opts = OrtSession.SessionOptions().apply {
                        setIntraOpNumThreads(4)
                        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    }
                    activeSession = ortEnv.createSession(modelFile.absolutePath, opts)
                    activeModelId = spec.id
                }
                val session = activeSession ?: error("ONNX session failed to initialize")
                block(ortEnv, session)
            }
        }
    }

    suspend fun unloadCurrentModel() = mutex.withLock {
        unloadLocked()
    }

    private fun unloadLocked() {
        activeSession?.close()
        activeSession = null
        activeModelId = null
        System.gc()
    }
}

/**
 * Splits long 16kHz mono PCM buffers into memory-safe chunks based on current RAM pressure.
 */
class ChunkedAudioPipeline(private val memoryMonitor: MemoryMonitor) {
    fun splitIntoChunks(pcm16kMono: ShortArray, sampleRateHz: Int = 16000): List<ShortArray> {
        val chunkSecs = memoryMonitor.snapshot().recommendedAudioChunkSeconds
        val chunkSamples = chunkSecs * sampleRateHz
        if (pcm16kMono.size <= chunkSamples) return listOf(pcm16kMono)
        val chunks = mutableListOf<ShortArray>()
        var offset = 0
        while (offset < pcm16kMono.size) {
            val end = (offset + chunkSamples).coerceAtMost(pcm16kMono.size)
            chunks.add(pcm16kMono.copyOfRange(offset, end))
            offset = end
        }
        return chunks
    }
}

/**
 * Coordinates offline ASR, Translation, and TTS models without silent cloud fallback.
 * Returns explicit failure when required offline model packages have not been imported locally.
 */
class OfflineAiOrchestrator(
    private val modelRepository: ModelRepository,
    private val sessionManager: OnnxSessionManager
) {

    suspend fun requireVerifiedModel(specId: String): Result<InstalledAiModel> {
        val status = modelRepository.verifyIntegrity(specId)
        if (status != AiModelStatus.VERIFIED_READY) {
            return Result.failure(
                IllegalStateException(
                    "Offline model '$specId' is $status. Import it manually from local storage in Settings -> AI Model Manager."
                )
            )
        }
        val installed = modelRepository.observeInstalledModels().first().firstOrNull { it.specId == specId }
            ?: return Result.failure(IllegalStateException("Model '$specId' not registered in local database."))
        return Result.success(installed)
    }

    suspend fun releaseAll() {
        sessionManager.unloadCurrentModel()
    }
}
