package com.fcf.videoconverter.feature.compressor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcf.videoconverter.domain.model.*
import com.fcf.videoconverter.media_engine.Media3VideoProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@androidx.media3.common.util.UnstableApi
class CompressorViewModel(
    private val mediaProcessor: Media3VideoProcessor
) : ViewModel() {
    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    fun compress(config: CompressionConfig) {
        viewModelScope.launch {
            mediaProcessor.compressVideo(config).collect { _state.value = it }
        }
    }

    fun estimateOutputMb(durationSeconds: Long, videoKbps: Int, audioKbps: Int): Double {
        return ((videoKbps + audioKbps) * durationSeconds.coerceAtLeast(1)) / 8192.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressorScreen(viewModel: CompressorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var targetBitrate by remember { mutableIntStateOf(1400) }
    var resolution by remember { mutableStateOf(ResolutionPreset.P720) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { sourceUri = it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Compressor") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (sourceUri == null) "Choose Video to Compress" else "Selected: ${sourceUri?.lastPathSegment}")
            }
            Text("Target Bitrate: $targetBitrate kbps")
            Slider(value = targetBitrate.toFloat(), onValueChange = { targetBitrate = it.toInt() }, valueRange = 400f..4000f)
            Text("Estimated size (for 60s clip): ${"%.1f".format(viewModel.estimateOutputMb(60, targetBitrate, 96))} MB")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ResolutionPreset.P480, ResolutionPreset.P720, ResolutionPreset.P1080).forEach { r ->
                    FilterChip(selected = resolution == r, onClick = { resolution = r }, label = { Text("${r.height}p") })
                }
            }
            Button(
                enabled = sourceUri != null && state !is ProcessingState.Running,
                onClick = {
                    sourceUri?.let {
                        viewModel.compress(
                            CompressionConfig(
                                sourceUri = it.toString(),
                                outputName = "fcf_comp_${System.currentTimeMillis()}",
                                resolution = resolution,
                                targetVideoBitrateKbps = targetBitrate
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Compress Video") }
            when (val s = state) {
                is ProcessingState.Running -> LinearProgressIndicator(progress = { s.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                is ProcessingState.Completed -> Text("Compressed output: ${s.outputSizeBytes / 1024} KB")
                is ProcessingState.Failed -> Text("Failed: ${s.message}", color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}
