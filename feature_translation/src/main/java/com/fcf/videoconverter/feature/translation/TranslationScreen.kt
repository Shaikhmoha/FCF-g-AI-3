package com.fcf.videoconverter.feature.translation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcf.videoconverter.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    installedModels: List<InstalledAiModel>,
    onOpenModelManager: () -> Unit,
    onBack: () -> Unit
) {
    var selectedPair by remember { mutableStateOf(LanguagePair.BN_TO_EN) }
    var inputText by remember { mutableStateOf("") }
    val translationModelReady = installedModels.any {
        it.specId == "nllb-200-distilled-600m-int8" && it.status == AiModelStatus.VERIFIED_READY
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline AI Translator") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!translationModelReady) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Offline Translation Model Required", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "NLLB-200 Distilled 600M (INT8) is not installed locally. Import the offline model package from storage to run Bangla ↔ English ↔ Hindi translation.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = onOpenModelManager) { Text("Open AI Model Manager") }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(LanguagePair.BN_TO_EN, LanguagePair.EN_TO_BN, LanguagePair.BN_TO_HI, LanguagePair.EN_TO_HI).forEach { pair ->
                    FilterChip(
                        selected = selectedPair == pair,
                        onClick = { selectedPair = pair },
                        label = { Text("${pair.source.code.uppercase()} → ${pair.target.code.uppercase()}") }
                    )
                }
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Source text (${selectedPair.source.displayName})") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
            Button(
                enabled = translationModelReady && inputText.isNotBlank(),
                onClick = { /* Executed via OnnxSessionManager when verified NLLB model is loaded */ },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Translate Locally") }
        }
    }
}
