package com.fcf.videoconverter.core.system

import android.content.Context
import android.os.StatFs
import java.io.File

data class StorageOverview(
    val freeBytes: Long,
    val totalBytes: Long,
    val tempCacheBytes: Long,
    val modelsBytes: Long
)

class StorageManager(private val context: Context) {

    val tempDir: File
        get() = File(context.cacheDir, "fcf_temp").apply { mkdirs() }

    val modelsDir: File
        get() = File(context.filesDir, "fcf_models").apply { mkdirs() }

    val exportsFallbackDir: File
        get() = File(context.filesDir, "fcf_exports").apply { mkdirs() }

    fun overview(): StorageOverview {
        val stat = StatFs(context.filesDir.absolutePath)
        return StorageOverview(
            freeBytes = stat.availableBytes,
            totalBytes = stat.totalBytes,
            tempCacheBytes = directorySize(tempDir),
            modelsBytes = directorySize(modelsDir)
        )
    }

    fun hasSufficientSpaceForExport(estimatedOutputBytes: Long): Boolean {
        val free = overview().freeBytes
        // Require 1.35x headroom for muxer temp buffers
        return free > (estimatedOutputBytes * 1.35).toLong() + (150L * 1024L * 1024L)
    }

    /**
     * Safely removes only internal temporary processing artifacts.
     * Never touches user gallery or SAF original media URIs.
     */
    fun clearTemporaryFiles(): Long {
        val before = directorySize(tempDir)
        tempDir.listFiles()?.forEach { it.deleteRecursively() }
        return before
    }

    private fun directorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var sum = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) sum += file.length()
        }
        return sum
    }
}
