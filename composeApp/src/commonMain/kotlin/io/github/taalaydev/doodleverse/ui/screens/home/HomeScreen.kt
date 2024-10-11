package io.github.taalaydev.doodleverse.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.composables.icons.lucide.*
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.navigation.Destination
import io.github.taalaydev.doodleverse.ui.components.ComposeIcons
import io.github.taalaydev.doodleverse.ui.components.NewProjectDialog
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController = rememberNavController(),
) {
    var projects by remember { mutableStateOf<List<ProjectModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var showNewProjectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            delay(1000) // Simulating network delay
            projects = listOf(
                ProjectModel(
                    1,
                    "Project 1",
                    listOf(
                        LayerModel(
                            1,
                            "Layer 1",
                        ),
                    ),
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    lastModified = Clock.System.now().toEpochMilliseconds(),
                ),
                ProjectModel(
                    2,
                    "Project 2",
                    listOf(
                        LayerModel(
                            1,
                            "Layer 1",
                        ),
                    ),
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    lastModified = Clock.System.now().toEpochMilliseconds(),
                )
            )
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismissRequest = { showNewProjectDialog = false },
            onConfirm = { name, width, height ->
                // Handle new project creation
                showNewProjectDialog = false
                ProjectModel.currentProject = ProjectModel(
                    1,
                    name,
                    listOf(
                        LayerModel(
                            1,
                            "Layer 1",
                        ),
                    ),
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    lastModified = Clock.System.now().toEpochMilliseconds(),
                    aspectRatio = Size(width, height),
                )
                navController.navigate(Destination.Drawing(1))
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                actions = {
                    IconButton(onClick = { /* Implement info dialog */ }) {
                        Icon(ComposeIcons.Info, contentDescription = "Info")
                    }
                    IconButton(onClick = {
                        showNewProjectDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Create New Project")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> LoadingScreen()
            error != null -> ErrorScreen(error!!) { /* Implement retry logic */ }
            projects.isEmpty() -> EmptyProjectsScreen {
                showNewProjectDialog = true
            }
            else -> ProjectGrid(
                projects = projects,
                onProjectClick = {
                    ProjectModel.currentProject = it
                    navController.navigate(Destination.Drawing(it.id))
                },
                onDeleteProject = { /* Implement delete project */ },
                onEditProject = { p, n -> /* Implement edit project */ },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(ComposeIcons.Error, contentDescription = null, modifier = Modifier.size(64.dp))
        Text(text = "An error occurred: $error", modifier = Modifier.padding(16.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}

@Composable
fun EmptyProjectsScreen(onCreateNew: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(ComposeIcons.Folder, contentDescription = null, modifier = Modifier.size(64.dp))
        Text("No projects found", modifier = Modifier.padding(16.dp))
        Button(onClick = onCreateNew) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Create New")
        }
    }
}

@Composable
fun ProjectGrid(
    projects: List<ProjectModel>,
    onProjectClick: (ProjectModel) -> Unit,
    onDeleteProject: (ProjectModel) -> Unit,
    onEditProject: (ProjectModel, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 250.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        items(projects.size) { index ->
            val project = projects[index]
            ProjectCard(
                project = project,
                onProjectClick = onProjectClick,
                onDeleteProject = onDeleteProject,
                onEditProject = onEditProject
            )
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectModel,
    onProjectClick: (ProjectModel) -> Unit,
    onDeleteProject: (ProjectModel) -> Unit,
    onEditProject: (ProjectModel, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick(project) }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    start = 16.dp,
                ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { /* Implement menu */ }) {
                    Icon(ComposeIcons.MoreVert, contentDescription = "More options")
                }
            }
            Column(modifier = Modifier.padding(
                horizontal = 16.dp,
            )) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Image, contentDescription = null, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoChip(
                        icon = Lucide.Grid3x3,
                        label = "${project.aspectRatio.width}x${project.aspectRatio.height}"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    InfoChip(icon = Lucide.Clock1, label = formatLastEdited(project.lastModified))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun formatLastEdited(lastEdited: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - lastEdited
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}