package com.fcf.videoconverter.feature.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fcf.videoconverter.domain.model.ProjectRecord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projects: List<ProjectRecord>,
    onDeleteProject: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Projects") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(modifier = Modifier.padding(padding).padding(24.dp)) {
                Text("No exported projects yet. Convert, compress, or edit a video to track it locally in Room.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(projects, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text("${item.taskType.name} · ${item.status.name} · ${item.progressPercent}%")
                            }
                            TextButton(onClick = { onDeleteProject(item.id) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}
