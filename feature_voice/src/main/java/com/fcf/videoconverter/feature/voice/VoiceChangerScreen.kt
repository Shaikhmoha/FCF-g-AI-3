package com.fcf.videoconverter.feature.voice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcf.videoconverter.domain.model.DspVoicePreset
import com.fcf.videoconverter.domain.model.VoiceEffectConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChangerScreen(onBack: () -> Unit) {
    var config by remember { mutableStateOf(VoiceEffectConfig()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Effects (Offline DSP)") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Note: Uses offline 16-bit PCM DSP (pitch, speed, presence EQ, noise gate, peak normalization). " +
                    "Neural AI voice cloning requires separate ONNX conversion models and is not simulated.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DspVoicePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = config.preset == preset,
                        onClick = {
                            config = config.copy(
                                preset = preset,
                                pitchSemitones = preset.pitchSemitones,
                                speedFactor = preset.speedFactor,
                                toneGainDb = preset.toneGainDb
                            )
                        },
                        label = { Text(preset.label) }
                    )
                }
            }
            Text("Pitch: ${"%.1f".format(config.pitchSemitones)} semitones")
            Slider(
                value = config.pitchSemitones,
                onValueChange = { config = config.copy(pitchSemitones = it, preset = DspVoicePreset.CUSTOM) },
                valueRange = -12f..12f
            )
            Text("Speed: ${"%.2f".format(config.speedFactor)}x")
            Slider(
                value = config.speedFactor,
                onValueChange = { config = config.copy(speedFactor = it) },
                valueRange = 0.6f..1.6f
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Noise Gate & High-Pass Filter")
                Switch(checked = config.noiseReductionEnabled, onCheckedChange = { config = config.copy(noiseReductionEnabled = it) })
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Audio Peak Normalization")
                Switch(checked = config.normalizeAudio, onCheckedChange = { config = config.copy(normalizeAudio = it) })
            }
        }
    }
}
