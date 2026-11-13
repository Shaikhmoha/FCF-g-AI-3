package com.fcf.videoconverter.feature.editor

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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcf.videoconverter.domain.model.ProcessingState
import com.fcf.videoconverter.media_engine.Media3VideoProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@androidx.media3.common.util.UnstableApi
class EditorViewModel(private val processor: Media3VideoProcessor) : ViewModel() {
    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    fun trimAndTransform(
        uri: String,
        startSec: Float,
        endSec: Float,
        rotationDeg: Float,
        height: Int
    ) {
        viewModelScope.launch {
            processor.trimAndTransformVideo(
                sourceUri = uri,
                outputName = "fcf_edit_${System.currentTimeMillis()}",
                startMs = (startSec * 1000).toLong(),
                endMs = (endSec * 1000).toLong(),
                rotationDegrees = rotationDeg,
                targetHeight = height
            ).collect { _state.value = it }
        }
    }

    fun merge(uris: List<String>) {
        viewModelScope.launch {
            processor.mergeVideos(uris, "fcf_merge_${System.currentTimeMillis()}")
                .collect { _state.value = it }
        }
    }

    fun extractAudio(uri: String) {
        viewModelScope.launch {
            processor.extractAudio(uri, "fcf_audio_${System.currentTimeMillis()}")
                .collect { _state.value = it }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    initialMode: String = "editor",
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var startSec by remember { mutableFloatStateOf(0f) }
    var endSec by remember { mutableFloatStateOf(15f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var targetHeight by remember { mutableIntStateOf(720) }

    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pickedUris = uris
    }

    val title = when (initialMode) {
        "merge" -> "Merge Videos"
        "trim" -> "Trim Video"
        "audio" -> "Audio Extractor"
        else -> "Video Editor"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { multiPicker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (pickedUris.isEmpty()) "Select Video(s)" else "${pickedUris.size} video(s) selected")
            }
            if (initialMode != "merge" && initialMode != "audio") {
                Text("Trim Window: ${startSec.toInt()}s to ${endSec.toInt()}s")
                Slider(value = startSec, onValueChange = { startSec = it.coerceAtMost(endSec - 1f) }, valueRange = 0f..120f)
                Slider(value = endSec, onValueChange = { endSec = it.coerceAtLeast(startSec + 1f) }, valueRange = 1f..180f)
                Text("Rotate: ${rotation.toInt()}° · Resize Height: ${targetHeight}p")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0f, 90f, 180f, 270f).forEach { deg ->
                        FilterChip(selected = rotation == deg, onClick = { rotation = deg }, label = { Text("${deg.toInt()}°") })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialMode == "merge") {
                    Button(
                        enabled = pickedUris.size >= 2,
                        onClick = { viewModel.merge(pickedUris.map { it.toString() }) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Merge Clips") }
                } else if (initialMode == "audio") {
                    Button(
                        enabled = pickedUris.isNotEmpty(),
                        onClick = { viewModel.extractAudio(pickedUris.first().toString()) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Extract AAC Audio") }
                } else {
                    Button(
                        enabled = pickedUris.isNotEmpty(),
                        onClick = { viewModel.trimAndTransform(pickedUris.first().toString(), startSec, endSec, rotation, targetHeight) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export Edited Video") }
                }
            }
            when (val s = state) {
                is ProcessingState.Running -> LinearProgressIndicator(progress = { s.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                is ProcessingState.Completed -> Text("Saved: ${s.outputUri}")
                is ProcessingState.Failed -> Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
        }
    }
}
