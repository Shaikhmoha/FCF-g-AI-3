package com.fcf.videoconverter.feature.converter

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
import com.fcf.videoconverter.domain.model.*
import com.fcf.videoconverter.domain.repository.ProjectRepository
import com.fcf.videoconverter.media_engine.Media3VideoProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

@androidx.media3.common.util.UnstableApi
class ConverterViewModel(
    private val mediaProcessor: Media3VideoProcessor,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val state: StateFlow<ProcessingState> = _state.asStateFlow()
    private var activeJob: Job? = null

    fun startConversion(config: ConversionConfig) {
        activeJob?.cancel()
        val projectId = UUID.randomUUID().toString()
        activeJob = viewModelScope.launch {
            mediaProcessor.convertVideo(config).collect { procState ->
                _state.value = procState
                val now = System.currentTimeMillis()
                val status = when (procState) {
                    is ProcessingState.Running -> ProjectStatus.RUNNING
                    is ProcessingState.Completed -> ProjectStatus.COMPLETED
                    is ProcessingState.Failed -> ProjectStatus.FAILED
                    ProcessingState.Cancelled -> ProjectStatus.CANCELLED
                    ProcessingState.Idle -> ProjectStatus.QUEUED
                }
                val pct = (procState as? ProcessingState.Running)?.progressPercent
                    ?: if (procState is ProcessingState.Completed) 100 else 0
                projectRepository.upsertProject(
                    ProjectRecord(
                        id = projectId,
                        title = config.outputName,
                        taskType = ProjectTaskType.CONVERT,
                        sourceUri = config.sourceUri,
                        outputUri = (procState as? ProcessingState.Completed)?.outputUri,
                        originalSizeBytes = 0L,
                        outputSizeBytes = (procState as? ProcessingState.Completed)?.outputSizeBytes ?: 0L,
                        status = status,
                        progressPercent = pct,
                        resumablePayloadJson = null,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now
                    )
                )
            }
        }
    }

    fun cancel() {
        activeJob?.cancel()
        _state.value = ProcessingState.Cancelled
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    viewModel: ConverterViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var outputName by remember { mutableStateOf("fcf_converted") }
    var selectedContainer by remember { mutableStateOf(VideoContainer.MP4) }
    var selectedResolution by remember { mutableStateOf(ResolutionPreset.P720) }
    var selectedCodec by remember { mutableStateOf(VideoCodecOption.H264) }
    var selectedAspect by remember { mutableStateOf(AspectRatioOption.ORIGINAL) }
    var bitrateKbps by remember { mutableIntStateOf(2500) }
    var fps by remember { mutableIntStateOf(30) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickedUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Converter") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { picker.launch(arrayOf("video/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (pickedUri == null) "Select Video (Gallery / SAF)" else "Change Selected Video")
            }
            pickedUri?.let {
                Text("Source: $it", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedTextField(
                value = outputName,
                onValueChange = { outputName = it },
                label = { Text("Output File Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Container Format", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VideoContainer.entries.take(4).forEach { fmt ->
                    FilterChip(
                        selected = selectedContainer == fmt,
                        onClick = { selectedContainer = fmt },
                        label = { Text(fmt.name) }
                    )
                }
            }
            Text("Resolution (720p default for 6 GB RAM)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ResolutionPreset.P360, ResolutionPreset.P480, ResolutionPreset.P720, ResolutionPreset.P1080).forEach { res ->
                    FilterChip(
                        selected = selectedResolution == res,
                        onClick = { selectedResolution = res },
                        label = { Text("${res.height}p") }
                    )
                }
            }
            Text("Video Bitrate: $bitrateKbps kbps · $fps fps", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = bitrateKbps.toFloat(),
                onValueChange = { bitrateKbps = it.toInt() },
                valueRange = 500f..8000f
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = pickedUri != null && state !is ProcessingState.Running,
                    onClick = {
                        pickedUri?.let { uri ->
                            viewModel.startConversion(
                                ConversionConfig(
                                    sourceUri = uri.toString(),
                                    outputName = outputName.ifBlank { "fcf_converted" },
                                    container = selectedContainer,
                                    resolution = selectedResolution,
                                    videoBitrateKbps = bitrateKbps,
                                    frameRate = fps,
                                    codec = selectedCodec,
                                    aspectRatio = selectedAspect
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Convert Offline") }

                if (state is ProcessingState.Running) {
                    OutlinedButton(onClick = { viewModel.cancel() }) { Text("Cancel") }
                }
            }
            when (val s = state) {
                is ProcessingState.Running -> {
                    LinearProgressIndicator(progress = { s.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${s.progressPercent}% · ${s.stageLabel}${s.etaSeconds?.let { " · ~${it}s left" } ?: ""}")
                }
                is ProcessingState.Completed -> Text("Saved to MediaStore: ${s.outputUri}")
                is ProcessingState.Failed -> Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                ProcessingState.Cancelled -> Text("Conversion cancelled.")
                ProcessingState.Idle -> Unit
            }
        }
    }
}
