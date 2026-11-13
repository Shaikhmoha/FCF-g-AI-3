package com.fcf.videoconverter.domain.repository

import com.fcf.videoconverter.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<ProjectRecord>>
    suspend fun upsertProject(project: ProjectRecord)
    suspend fun deleteProject(projectId: String)
    suspend fun getProject(projectId: String): ProjectRecord?
}

interface ModelRepository {
    fun catalog(): List<AiModelSpec>
    fun observeInstalledModels(): Flow<List<InstalledAiModel>>
    suspend fun installModelFromUri(specId: String, sourceUri: String): Result<InstalledAiModel>
    suspend fun removeModel(specId: String): Result<Unit>
    suspend fun verifyIntegrity(specId: String): AiModelStatus
}

interface SettingsRepository {
    fun observeDarkTheme(): Flow<Boolean>
    fun observeLanguageCode(): Flow<String>
    suspend fun setDarkTheme(enabled: Boolean)
    suspend fun setLanguageCode(code: String)
}
