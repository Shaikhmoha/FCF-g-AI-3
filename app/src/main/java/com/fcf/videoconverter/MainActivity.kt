package com.fcf.videoconverter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fcf.videoconverter.core.system.LocaleManager
import com.fcf.videoconverter.core.theme.FcfTheme
import com.fcf.videoconverter.feature.compressor.CompressorScreen
import com.fcf.videoconverter.feature.compressor.CompressorViewModel
import com.fcf.videoconverter.feature.converter.ConverterScreen
import com.fcf.videoconverter.feature.converter.ConverterViewModel
import com.fcf.videoconverter.feature.dubbing.DubbingScreen
import com.fcf.videoconverter.feature.editor.EditorScreen
import com.fcf.videoconverter.feature.editor.EditorViewModel
import com.fcf.videoconverter.feature.projects.ProjectsScreen
import com.fcf.videoconverter.feature.settings.SettingsScreen
import com.fcf.videoconverter.feature.subtitles.SubtitlesScreen
import com.fcf.videoconverter.feature.translation.TranslationScreen
import com.fcf.videoconverter.feature.voice.VoiceChangerScreen
import com.fcf.videoconverter.ui.DashboardScreen
import kotlinx.coroutines.launch

@UnstableApi
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FcfApplication).container

        setContent {
            val isDark by container.settingsRepository.observeDarkTheme().collectAsState(initial = true)
            val langCode by container.settingsRepository.observeLanguageCode().collectAsState(initial = "en")
            val installedModels by container.modelRepository.observeInstalledModels().collectAsState(initial = emptyList())
            val projects by container.projectRepository.observeProjects().collectAsState(initial = emptyList())
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()

            val converterVm = remember { ConverterViewModel(container.mediaProcessor, container.projectRepository) }
            val compressorVm = remember { CompressorViewModel(container.mediaProcessor) }
            val editorVm = remember { EditorViewModel(container.mediaProcessor) }

            FcfTheme(darkTheme = isDark) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            memorySnapshot = container.memoryMonitor.snapshot(),
                            freeStorageMb = container.storageManager.overview().freeBytes / (1024L * 1024L),
                            onNavigate = { route -> navController.navigate(route) }
                        )
                    }
                    composable("converter") {
                        ConverterScreen(viewModel = converterVm, onBack = { navController.popBackStack() })
                    }
                    composable("compressor") {
                        CompressorScreen(viewModel = compressorVm, onBack = { navController.popBackStack() })
                    }
                    composable("voice") {
                        VoiceChangerScreen(onBack = { navController.popBackStack() })
                    }
                    composable("translation") {
                        TranslationScreen(
                            installedModels = installedModels,
                            onOpenModelManager = { navController.navigate("settings") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("dubbing") {
                        DubbingScreen(
                            installedModels = installedModels,
                            onOpenModelManager = { navController.navigate("settings") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("subtitles") {
                        SubtitlesScreen(
                            installedModels = installedModels,
                            onOpenModelManager = { navController.navigate("settings") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("audio") {
                        EditorScreen(viewModel = editorVm, initialMode = "audio", onBack = { navController.popBackStack() })
                    }
                    composable("editor") {
                        EditorScreen(viewModel = editorVm, initialMode = "editor", onBack = { navController.popBackStack() })
                    }
                    composable("merge") {
                        EditorScreen(viewModel = editorVm, initialMode = "merge", onBack = { navController.popBackStack() })
                    }
                    composable("trim") {
                        EditorScreen(viewModel = editorVm, initialMode = "trim", onBack = { navController.popBackStack() })
                    }
                    composable("projects") {
                        ProjectsScreen(
                            projects = projects,
                            onDeleteProject = { id -> scope.launch { container.projectRepository.deleteProject(id) } },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            isDarkTheme = isDark,
                            currentLanguageCode = langCode,
                            memorySnapshot = container.memoryMonitor.snapshot(),
                            storageOverview = container.storageManager.overview(),
                            modelCatalog = container.modelRepository.catalog(),
                            installedModels = installedModels,
                            onToggleTheme = { enabled -> scope.launch { container.settingsRepository.setDarkTheme(enabled) } },
                            onChangeLanguage = { lang ->
                                scope.launch {
                                    container.settingsRepository.setLanguageCode(lang.tag)
                                    LocaleManager.applyLanguage(lang)
                                }
                            },
                            onClearTempCache = { container.storageManager.clearTemporaryFiles() },
                            onInstallModelFromUri = { specId, uri ->
                                scope.launch { container.modelRepository.installModelFromUri(specId, uri.toString()) }
                            },
                            onRemoveModel = { specId ->
                                scope.launch { container.modelRepository.removeModel(specId) }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
