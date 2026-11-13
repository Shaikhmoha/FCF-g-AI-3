package com.fcf.videoconverter.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val taskType: String,
    val sourceUri: String,
    val outputUri: String?,
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val status: String,
    val progressPercent: Int,
    val resumablePayloadJson: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "installed_models")
data class InstalledModelEntity(
    @PrimaryKey val specId: String,
    val localPath: String,
    val fileSizeBytes: Long,
    val sha256: String,
    val status: String,
    val installedAtEpochMs: Long
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface InstalledModelDao {
    @Query("SELECT * FROM installed_models ORDER BY installedAtEpochMs DESC")
    fun observeAll(): Flow<List<InstalledModelEntity>>

    @Query("SELECT * FROM installed_models WHERE specId = :specId LIMIT 1")
    suspend fun findBySpecId(specId: String): InstalledModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InstalledModelEntity)

    @Query("DELETE FROM installed_models WHERE specId = :specId")
    suspend fun deleteBySpecId(specId: String)
}

@Database(
    entities = [ProjectEntity::class, InstalledModelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FcfDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun installedModelDao(): InstalledModelDao
}
