package io.github.taalaydev.doodleverse.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.composables.icons.lucide.*
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.navigation.Destination
import io.github.taalaydev.doodleverse.ui.components.ComposeIcons
import io.github.taalaydev.doodleverse.ui.components.EditProjectDialog
import io.github.taalaydev.doodleverse.ui.components.NewProjectDialog
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController = rememberNavController(),
    platform: Platform,
    viewModel: HomeViewModel = viewModel { HomeViewModel(platform.projectRepo, platform.dispatcherIO) },
) {
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val isLoading by remember { mutableStateOf(false) }
    val error by remember { mutableStateOf<String?>(null) }

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<ProjectModel?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismissRequest = { showNewProjectDialog = false },
            onConfirm = { name, width, height ->
                showNewProjectDialog = false

                scope.launch {
                    val project = viewModel.createProject(name, width, height)
                    navController.navigate(Destination.Drawing(project.id))
                }
            }
        )
    }

    // Show edit dialog when projectToEdit is not null
    projectToEdit?.let { project ->
        EditProjectDialog(
            project = project,
            onDismissRequest = { projectToEdit = null },
            onConfirm = { newName ->
                scope.launch {
                    viewModel.updateProject(project.copy(
                        name = newName,
                        lastModified = Clock.System.now().toEpochMilliseconds()
                    ))
                }
                projectToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Projects") },
                    actions = {
                        IconButton(onClick = {
                            showNewProjectDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Create New Project")
                        }
                    }
                )
                NavigationBanner(
                    selectedTab = selectedTab,
                    onTabSelected = { index, route ->
                        selectedTab = index
                        if (route != null) {
                            navController.navigate(route)
                        }
                    }
                )
            }
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
                    navController.navigate(Destination.Drawing(it.id))
                },
                onDeleteProject = {
                    viewModel.deleteProject(it)
                },
                onEditProject = { project ->
                    projectToEdit = project
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun NavigationBanner(
    selectedTab: Int,
    onTabSelected: (Int, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        NavigationItem("Projects", Lucide.FolderOpen, null),
        NavigationItem("Lessons", Lucide.GraduationCap, Destination.Lessons.route),
        NavigationItem("About", Lucide.Info, Destination.About.route),
    )

    Surface(
        modifier = modifier.fillMaxWidth().height(80.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, item ->
                NavigationItem(
                    item = item,
                    isSelected = selectedTab == index,
                    onClick = { onTabSelected(index, item.route) },
                    modifier = Modifier.width(80.dp)
                )
            }
        }
    }
}

private data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: String?
)

@Composable
private fun NavigationItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )

        // Animated indicator
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(
                        color = contentColor,
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
private fun NavigationIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(20.dp)
            .height(2.dp)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(1.dp)
            )
    )
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
    onEditProject: (ProjectModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 230.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp,
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
    onEditProject: (ProjectModel) -> Unit
) {
    var openMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick(project) },
        colors = CardDefaults.cardColors(

        ),
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
                Column {
                    IconButton(onClick = {
                        openMenu = true
                    }) {
                        Icon(ComposeIcons.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = openMenu,
                        onDismissRequest = { openMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                onDeleteProject(project)
                                openMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                onEditProject(project)
                                openMenu = false
                            }
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(
                horizontal = 16.dp,
            )) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(project.aspectRatioValue)
                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (project.cachedBitmap != null) {
                        Image(
                            bitmap = project.cachedBitmap,
                            contentDescription = project.name,
                            modifier = Modifier
                                .aspectRatio(project.aspectRatioValue)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Icon(Lucide.Image, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
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
        color = Color.White.copy(alpha = 0.5f),
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