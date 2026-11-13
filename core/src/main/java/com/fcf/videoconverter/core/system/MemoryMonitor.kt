package com.fcf.videoconverter.core.system

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class MemorySnapshot(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val lowMemory: Boolean,
    val javaHeapFreeMb: Long,
    val recommendedMaxResolutionHeight: Int,
    val recommendedAudioChunkSeconds: Int
)

/**
 * Monitors device memory continuously so 6 GB RAM phones avoid LMK kills
 * during long video transcodes or sequential ONNX/Whisper inference.
 */
class MemoryMonitor(private val context: Context) {

    fun snapshot(): MemorySnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024L * 1024L)
        val availMb = memInfo.availMem / (1024L * 1024L)

        val runtime = Runtime.getRuntime()
        val heapUsed = runtime.totalMemory() - runtime.freeMemory()
        val heapFreeMb = (runtime.maxMemory() - heapUsed) / (1024L * 1024L)

        // Conservative 4K gate: require >= 7.2 GB total RAM and >= 1.8 GB currently available
        val maxHeight = when {
            memInfo.lowMemory || availMb < 900L -> 720
            totalMb >= 7200L && availMb >= 1800L -> 2160
            else -> 1080
        }

        val chunkSecs = when {
            memInfo.lowMemory || availMb < 1000L -> 15
            availMb < 1800L -> 30
            else -> 45
        }

        return MemorySnapshot(
            totalRamMb = totalMb,
            availableRamMb = availMb,
            lowMemory = memInfo.lowMemory,
            javaHeapFreeMb = heapFreeMb,
            recommendedMaxResolutionHeight = maxHeight,
            recommendedAudioChunkSeconds = chunkSecs
        )
    }

    fun isSafeForHeavyAiModel(requiredMb: Long): Boolean {
        val snap = snapshot()
        return !snap.lowMemory && snap.availableRamMb >= (requiredMb + 450L)
    }

    fun supports4kHardware(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            snapshot().recommendedMaxResolutionHeight >= 2160
    }
}
