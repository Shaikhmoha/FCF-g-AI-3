package com.fcf.videoconverter.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcf.videoconverter.core.system.AppLanguage
import com.fcf.videoconverter.core.system.MemorySnapshot
import com.fcf.videoconverter.core.system.StorageOverview
import com.fcf.videoconverter.domain.model.AiModelSpec
import com.fcf.videoconverter.domain.model.InstalledAiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    currentLanguageCode: String,
    memorySnapshot: MemorySnapshot,
    storageOverview: StorageOverview,
    modelCatalog: List<AiModelSpec>,
    installedModels: List<InstalledAiModel>,
    onToggleTheme: (Boolean) -> Unit,
    onChangeLanguage: (AppLanguage) -> Unit,
    onClearTempCache: () -> Unit,
    onInstallModelFromUri: (specId: String, uri: Uri) -> Unit,
    onRemoveModel: (specId: String) -> Unit,
    onBack: () -> Unit
) {
    var pendingInstallSpecId by remember { mutableStateOf<String?>(null) }
    val modelFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val specId = pendingInstallSpecId
        if (uri != null && specId != null) {
            onInstallModelFromUri(specId, uri)
        }
        pendingInstallSpecId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & AI Model Manager") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Appearance & Language", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Dark Mode")
                Switch(checked = isDarkTheme, onCheckedChange = onToggleTheme)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { lang ->
                    FilterChip(
                        selected = currentLanguageCode == lang.tag,
                        onClick = { onChangeLanguage(lang) },
                        label = { Text(lang.displayName) }
                    )
                }
            }

            HorizontalDivider()
            Text("Device Memory & Storage (6 GB RAM Tuner)", style = MaterialTheme.typography.titleLarge)
            Text("RAM: ${memorySnapshot.availableRamMb} MB free / ${memorySnapshot.totalRamMb} MB total · Chunk window: ${memorySnapshot.recommendedAudioChunkSeconds}s")
            Text("Free Storage: ${storageOverview.freeBytes / (1024L * 1024L)} MB · Temp Cache: ${storageOverview.tempCacheBytes / 1024L} KB")
            OutlinedButton(onClick = onClearTempCache) { Text("Clear Temporary Processing Cache") }

            HorizontalDivider()
            Text("Offline AI Model Manager", style = MaterialTheme.typography.titleLarge)
            Text("Models are never downloaded automatically. Pick local .bin / .onnx model files from device storage.")

            modelCatalog.forEach { spec ->
                val installed = installedModels.firstOrNull { it.specId == spec.id }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(spec.displayName, style = MaterialTheme.typography.titleMedium)
                        Text("Languages: ${spec.supportedLanguages.joinToString { it.code.uppercase() }} · Est. RAM: ${spec.estimatedRamMb} MB · License: ${spec.license}")
                        if (installed != null) {
                            Text("Status: ${installed.status.name} · SHA-256: ${installed.sha256.take(16)}...")
                            TextButton(onClick = { onRemoveModel(spec.id) }) { Text("Remove Model") }
                        } else {
                            Text("Status: Not Installed (${spec.formatHint})")
                            Button(onClick = {
                                pendingInstallSpecId = spec.id
                                modelFilePicker.launch(arrayOf("*/*"))
                            }) { Text("Install from Local Storage") }
                        }
                    }
                }
            }
        }
    }
}
