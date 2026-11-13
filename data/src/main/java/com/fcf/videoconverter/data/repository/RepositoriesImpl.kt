package com.fcf.videoconverter.data.repository

import android.content.Context
import android.net.Uri
import com.fcf.videoconverter.core.system.StorageManager
import com.fcf.videoconverter.data.local.*
import com.fcf.videoconverter.domain.model.*
import com.fcf.videoconverter.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class RoomProjectRepository(
    private val projectDao: ProjectDao
) : ProjectRepository {

    override fun observeProjects(): Flow<List<ProjectRecord>> =
        projectDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsertProject(project: ProjectRecord) {
        projectDao.upsert(project.toEntity())
    }

    override suspend fun deleteProject(projectId: String) {
        projectDao.deleteById(projectId)
    }

    override suspend fun getProject(projectId: String): ProjectRecord? =
        projectDao.findById(projectId)?.toDomain()

    private fun ProjectEntity.toDomain() = ProjectRecord(
        id = id,
        title = title,
        taskType = ProjectTaskType.valueOf(taskType),
        sourceUri = sourceUri,
        outputUri = outputUri,
        originalSizeBytes = originalSizeBytes,
        outputSizeBytes = outputSizeBytes,
        status = ProjectStatus.valueOf(status),
        progressPercent = progressPercent,
        resumablePayloadJson = resumablePayloadJson,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )

    private fun ProjectRecord.toEntity() = ProjectEntity(
        id = id,
        title = title,
        taskType = taskType.name,
        sourceUri = sourceUri,
        outputUri = outputUri,
        originalSizeBytes = originalSizeBytes,
        outputSizeBytes = outputSizeBytes,
        status = status.name,
        progressPercent = progressPercent,
        resumablePayloadJson = resumablePayloadJson,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

class LocalSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("fcf_settings", Context.MODE_PRIVATE)
    private val darkThemeFlow = MutableStateFlow(prefs.getBoolean("dark_theme", true))
    private val languageFlow = MutableStateFlow(prefs.getString("language_code", "en") ?: "en")

    override fun observeDarkTheme(): Flow<Boolean> = darkThemeFlow
    override fun observeLanguageCode(): Flow<String> = languageFlow

    override suspend fun setDarkTheme(enabled: Boolean) {
        prefs.edit().putBoolean("dark_theme", enabled).apply()
        darkThemeFlow.value = enabled
    }

    override suspend fun setLanguageCode(code: String) {
        prefs.edit().putString("language_code", code).apply()
        languageFlow.value = code
    }
}

class LocalModelRepository(
    private val context: Context,
    private val storageManager: StorageManager,
    private val dao: InstalledModelDao
) : ModelRepository {

    private val modelSpecs = listOf(
        AiModelSpec(
            id = "whisper-base-q5_1",
            displayName = "Whisper Base Multilingual (Q5_1)",
            capability = AiModelCapability.ASR_SPEECH_TO_TEXT,
            supportedLanguages = listOf(AiLanguage.ENGLISH, AiLanguage.BANGLA, AiLanguage.HINDI),
            expectedSizeBytes = 60L * 1024L * 1024L,
            estimatedRamMb = 380L,
            license = "MIT",
            formatHint = "ggml-base-q5_1.bin"
        ),
        AiModelSpec(
            id = "nllb-200-distilled-600m-int8",
            displayName = "NLLB-200 Distilled 600M (INT8)",
            capability = AiModelCapability.TRANSLATION,
            supportedLanguages = listOf(AiLanguage.BANGLA, AiLanguage.ENGLISH, AiLanguage.HINDI),
            expectedSizeBytes = 620L * 1024L * 1024L,
            estimatedRamMb = 920L,
            license = "CC-BY-NC-4.0",
            formatHint = "nllb-600m-int8.onnx"
        ),
        AiModelSpec(
            id = "piper-vits-bn-en-hi",
            displayName = "VITS / Piper Multilingual TTS Pack",
            capability = AiModelCapability.TTS_VOICE_SYNTHESIS,
            supportedLanguages = listOf(AiLanguage.BANGLA, AiLanguage.ENGLISH, AiLanguage.HINDI),
            expectedSizeBytes = 95L * 1024L * 1024L,
            estimatedRamMb = 260L,
            license = "MIT",
            formatHint = "piper-voices-bn-en-hi.onnx"
        )
    )

    override fun catalog(): List<AiModelSpec> = modelSpecs

    override fun observeInstalledModels(): Flow<List<InstalledAiModel>> =
        dao.observeAll().map { list ->
            list.map {
                InstalledAiModel(
                    specId = it.specId,
                    localPath = it.localPath,
                    fileSizeBytes = it.fileSizeBytes,
                    sha256 = it.sha256,
                    status = AiModelStatus.valueOf(it.status),
                    installedAtEpochMs = it.installedAtEpochMs
                )
            }
        }

    override suspend fun installModelFromUri(specId: String, sourceUri: String): Result<InstalledAiModel> =
        withContext(Dispatchers.IO) {
            runCatching {
                val spec = modelSpecs.firstOrNull { it.id == specId }
                    ?: error("Unknown model spec: $specId")
                val targetFile = File(storageManager.modelsDir, "${spec.id}.bin")
                val uri = Uri.parse(sourceUri)
                val digest = MessageDigest.getInstance("SHA-256")
                var bytesCopied = 0L

                context.contentResolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            bytesCopied += read
                        }
                    }
                } ?: error("Unable to open selected local model file")

                require(bytesCopied > 1024L) { "Selected file is too small to be a valid model package." }

                val sha = digest.digest().joinToString("") { "%02x".format(it) }
                val entity = InstalledModelEntity(
                    specId = spec.id,
                    localPath = targetFile.absolutePath,
                    fileSizeBytes = bytesCopied,
                    sha256 = sha,
                    status = AiModelStatus.VERIFIED_READY.name,
                    installedAtEpochMs = System.currentTimeMillis()
                )
                dao.upsert(entity)
                InstalledAiModel(
                    specId = entity.specId,
                    localPath = entity.localPath,
                    fileSizeBytes = entity.fileSizeBytes,
                    sha256 = entity.sha256,
                    status = AiModelStatus.VERIFIED_READY,
                    installedAtEpochMs = entity.installedAtEpochMs
                )
            }
        }

    override suspend fun removeModel(specId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = dao.findBySpecId(specId)
            existing?.let { File(it.localPath).delete() }
            dao.deleteBySpecId(specId)
        }
    }

    override suspend fun verifyIntegrity(specId: String): AiModelStatus = withContext(Dispatchers.IO) {
        val existing = dao.findBySpecId(specId) ?: return@withContext AiModelStatus.NOT_INSTALLED
        val file = File(existing.localPath)
        if (!file.exists() || file.length() != existing.fileSizeBytes) {
            dao.upsert(existing.copy(status = AiModelStatus.CORRUPTED.name))
            return@withContext AiModelStatus.CORRUPTED
        }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val r = input.read(buf)
                if (r <= 0) break
                digest.update(buf, 0, r)
            }
        }
        val currentSha = digest.digest().joinToString("") { "%02x".format(it) }
        val status = if (currentSha == existing.sha256) AiModelStatus.VERIFIED_READY else AiModelStatus.CORRUPTED
        dao.upsert(existing.copy(status = status.name))
        status
    }
}
