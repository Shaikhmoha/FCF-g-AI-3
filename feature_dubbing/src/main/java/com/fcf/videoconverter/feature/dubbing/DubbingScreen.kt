package com.fcf.videoconverter.feature.dubbing

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcf.videoconverter.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DubbingScreen(
    installedModels: List<InstalledAiModel>,
    onOpenModelManager: () -> Unit,
    onBack: () -> Unit
) {
    var targetLanguage by remember { mutableStateOf(AiLanguage.BANGLA) }
    var keepOriginalAudio by remember { mutableStateOf(true) }
    var originalVol by remember { mutableFloatStateOf(0.25f) }
    var dubbedVol by remember { mutableFloatStateOf(1.0f) }

    val asrReady = installedModels.any { it.specId == "whisper-base-q5_1" && it.status == AiModelStatus.VERIFIED_READY }
    val trReady = installedModels.any { it.specId == "nllb-200-distilled-600m-int8" && it.status == AiModelStatus.VERIFIED_READY }
    val ttsReady = installedModels.any { it.specId == "piper-vits-bn-en-hi" && it.status == AiModelStatus.VERIFIED_READY }
    val allModelsReady = asrReady && trReady && ttsReady

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Video Dubbing") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!allModelsReady) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Required Offline Models Missing", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sequential dubbing loads Whisper ASR (${if (asrReady) "Ready" else "Missing"}), " +
                                "NLLB Translation (${if (trReady) "Ready" else "Missing"}), and " +
                                "Piper TTS (${if (ttsReady) "Ready" else "Missing"}) one at a time for 6 GB RAM.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onOpenModelManager) { Text("Install Local Models") }
                    }
                }
            }
            Text("Target Language", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AiLanguage.entries.forEach { lang ->
                    FilterChip(selected = targetLanguage == lang, onClick = { targetLanguage = lang }, label = { Text(lang.displayName) })
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Keep Original Background Audio")
                Switch(checked = keepOriginalAudio, onCheckedChange = { keepOriginalAudio = it })
            }
            Text("Original Audio Volume: ${(originalVol * 100).toInt()}%")
            Slider(value = originalVol, onValueChange = { originalVol = it }, enabled = keepOriginalAudio)
            Text("Dubbed Voice Volume: ${(dubbedVol * 100).toInt()}%")
            Slider(value = dubbedVol, onValueChange = { dubbedVol = it })
            Button(enabled = allModelsReady, onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Run Sequential Offline Dubbing")
            }
        }
    }
}
