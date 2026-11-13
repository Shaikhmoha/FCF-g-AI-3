package com.fcf.videoconverter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fcf.videoconverter.R
import com.fcf.videoconverter.core.system.MemorySnapshot

data class DashboardFeature(
    val route: String,
    val titleRes: Int,
    val subtitle: String,
    val icon: ImageVector
)

private val dashboardFeatures = listOf(
    DashboardFeature("converter", R.string.feat_converter, "MP4 / MOV / MKV · 720p+", Icons.Rounded.SwapHoriz),
    DashboardFeature("compressor", R.string.feat_compressor, "Target bitrate & size", Icons.Rounded.Compress),
    DashboardFeature("voice", R.string.feat_voice, "Offline PCM DSP studio", Icons.Rounded.GraphicEq),
    DashboardFeature("translation", R.string.feat_translator, "BN ↔ EN ↔ HI (Local)", Icons.Rounded.Translate),
    DashboardFeature("dubbing", R.string.feat_dubbing, "Sequential offline dub", Icons.Rounded.RecordVoiceOver),
    DashboardFeature("subtitles", R.string.feat_subtitles, "SRT editor & Whisper", Icons.Rounded.Subtitles),
    DashboardFeature("audio", R.string.feat_audio, "MP3 / WAV / AAC", Icons.Rounded.AudioFile),
    DashboardFeature("editor", R.string.feat_editor, "Crop, rotate, filters", Icons.Rounded.Tune),
    DashboardFeature("merge", R.string.feat_merge, "Concatenate clips", Icons.Rounded.MergeType),
    DashboardFeature("trim", R.string.feat_trim, "Frame-accurate cut", Icons.Rounded.ContentCut),
    DashboardFeature("projects", R.string.feat_projects, "Room local history", Icons.Rounded.Folder),
    DashboardFeature("settings", R.string.feat_settings, "Theme, বাংলা, Models", Icons.Rounded.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    memorySnapshot: MemorySnapshot,
    freeStorageMb: Long,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${stringResource(R.string.tagline_offline)} · RAM ${memorySnapshot.availableRamMb}/${memorySnapshot.totalRamMb} MB · Free ${freeStorageMb / 1024} GB",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(dashboardFeatures) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(item.route) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(item.titleRes),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
