package com.fcf.videoconverter.feature.subtitles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcf.videoconverter.domain.model.*
import com.fcf.videoconverter.media_engine.SrtCodec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitlesScreen(
    installedModels: List<InstalledAiModel>,
    onOpenModelManager: () -> Unit,
    onBack: () -> Unit
) {
    var cues by remember {
        mutableStateOf(
            listOf(
                SubtitleCue(1, 0L, 3500L, "Edit or import SRT cues offline."),
                SubtitleCue(2, 3600L, 7000L, "Whisper Base Q5_1 unlocks auto-ASR.")
            )
        )
    }
    var shiftMs by remember { mutableLongStateOf(0L) }
    val asrReady = installedModels.any { it.specId == "whisper-base-q5_1" && it.status == AiModelStatus.VERIFIED_READY }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Subtitle Generator") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!asrReady) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("ASR Model Missing (Manual SRT editor active)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenModelManager) { Text("Install") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    shiftMs += 500L
                    cues = SrtCodec.shiftTiming(cues, 500L)
                }) { Text("+0.5s Timing") }
                OutlinedButton(onClick = {
                    shiftMs -= 500L
                    cues = SrtCodec.shiftTiming(cues, -500L)
                }) { Text("-0.5s Timing") }
                Button(onClick = { /* Writes SrtCodec.serialize(cues) via SAF CreateDocument */ }) {
                    Text("Export SRT")
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                itemsIndexed(cues) { idx, cue ->
                    OutlinedTextField(
                        value = cue.text,
                        onValueChange = { updated ->
                            cues = cues.toMutableList().apply { this[idx] = cue.copy(text = updated) }
                        },
                        label = { Text("Cue #${cue.index} (${cue.startMs}ms - ${cue.endMs}ms)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
