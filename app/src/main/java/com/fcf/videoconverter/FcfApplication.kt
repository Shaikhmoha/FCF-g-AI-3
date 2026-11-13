package com.fcf.videoconverter

import android.app.Application
import android.content.ComponentCallbacks2
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import com.fcf.videoconverter.ai_engine.OnnxSessionManager
import com.fcf.videoconverter.core.system.MemoryMonitor
import com.fcf.videoconverter.core.system.StorageManager
import com.fcf.videoconverter.data.local.FcfDatabase
import com.fcf.videoconverter.data.repository.LocalModelRepository
import com.fcf.videoconverter.data.repository.LocalSettingsRepository
import com.fcf.videoconverter.data.repository.RoomProjectRepository
import com.fcf.videoconverter.media_engine.Media3VideoProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@UnstableApi
class AppContainer(application: Application) {
    val storageManager = StorageManager(application)
    val memoryMonitor = MemoryMonitor(application)
    val database: FcfDatabase = Room.databaseBuilder(
        application,
        FcfDatabase::class.java,
        "fcf_video_converter.db"
    ).build()

    val projectRepository = RoomProjectRepository(database.projectDao())
    val settingsRepository = LocalSettingsRepository(application)
    val modelRepository = LocalModelRepository(application, storageManager, database.installedModelDao())
    val mediaProcessor = Media3VideoProcessor(application, storageManager, memoryMonitor)
    val onnxSessionManager = OnnxSessionManager(memoryMonitor)
}

@UnstableApi
class FcfApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            appScope.launch {
                container.onnxSessionManager.unloadCurrentModel()
            }
        }
    }
}
