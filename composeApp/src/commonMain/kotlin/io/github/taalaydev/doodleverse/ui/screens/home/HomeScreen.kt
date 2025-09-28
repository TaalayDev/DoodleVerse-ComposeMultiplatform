package io.github.taalaydev.doodleverse.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.composables.icons.lucide.*
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.about
import doodleverse.composeapp.generated.resources.create_new
import doodleverse.composeapp.generated.resources.create_new_project
import doodleverse.composeapp.generated.resources.days_ago
import doodleverse.composeapp.generated.resources.delete
import doodleverse.composeapp.generated.resources.edit
import doodleverse.composeapp.generated.resources.hours_ago
import doodleverse.composeapp.generated.resources.just_now
import doodleverse.composeapp.generated.resources.lessons
import doodleverse.composeapp.generated.resources.minutes_ago
import doodleverse.composeapp.generated.resources.more_options
import doodleverse.composeapp.generated.resources.no_projects_found
import doodleverse.composeapp.generated.resources.projects
import io.github.taalaydev.doodleverse.Analytics
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.core.toHex
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.getAnalytics
import io.github.taalaydev.doodleverse.navigation.Destination
import io.github.taalaydev.doodleverse.purchase.PremiumFeature
import io.github.taalaydev.doodleverse.purchase.PurchaseViewModel
import io.github.taalaydev.doodleverse.ui.components.ComposeIcons
import io.github.taalaydev.doodleverse.ui.components.EditProjectDialog
import io.github.taalaydev.doodleverse.ui.components.NewProjectDialog
import io.github.taalaydev.doodleverse.ui.components.ThemeSelectorSheet
import io.github.taalaydev.doodleverse.ui.components.premium.PremiumBadge
import io.github.taalaydev.doodleverse.ui.components.premium.PremiumFeatureButton
import io.github.taalaydev.doodleverse.ui.theme.AnimatedScaffold
import io.github.taalaydev.doodleverse.ui.theme.DoodleTheme
import io.github.taalaydev.doodleverse.ui.theme.DoodleVerseCardDefaults
import io.github.taalaydev.doodleverse.ui.theme.ThemeManager
import io.github.taalaydev.doodleverse.ui.theme.ThemedSurface
import io.github.taalaydev.doodleverse.ui.theme.rememberThemeManager
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController = rememberNavController(),
    platform: Platform,
    viewModel: HomeViewModel = viewModel { HomeViewModel(platform.projectRepo, platform.dispatcherIO) },
    purchaseViewModel: PurchaseViewModel,
    themeManager: ThemeManager = rememberThemeManager(),
    analytics: Analytics = getAnalytics()
) {
    val scope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val isLoading by remember { mutableStateOf(false) }
    val error by remember { mutableStateOf<String?>(null) }

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var projectToEdit by remember { mutableStateOf<ProjectModel?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val currentTheme by themeManager.currentTheme.collectAsState()
    var showThemeSelector by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val premiumStatus by purchaseViewModel.premiumStatus.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadProjects()
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismissRequest = { showNewProjectDialog = false },
            onConfirm = { name, width, height ->
                showNewProjectDialog = false
                analytics.logEvent("project_created", mapOf("name" to name))
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

    if (showThemeSelector) {
        ThemeSelectorSheet(
            currentThemeId = currentTheme.id,
            purchaseViewModel = purchaseViewModel,
            onThemeSelected = { theme ->
                themeManager.setTheme(theme)
                if (theme.isDark) {
                    themeManager.setDarkMode(true)
                } else {
                    themeManager.setDarkMode(false)
                }

                showThemeSelector = false
            },
            onDismiss = { showThemeSelector = false },
            sheetState = sheetState
        )
    }

    AnimatedScaffold(
        themeManager = themeManager,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(Res.string.projects)) },
                    actions = {
                        if (!premiumStatus.isPremium) {
                            OutlinedButton(
                                onClick = {
                                    navController.navigate(Destination.Purchase())
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Lucide.Crown,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Upgrade")
                            }
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        IconButton(onClick = {
                            showThemeSelector = true
                        }) {
                            Icon(
                                Lucide.Palette,
                                contentDescription = "Change Theme",
                            )
                        }
                        IconButton(onClick = {
                            showNewProjectDialog = true
                        }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(Res.string.create_new_project)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    NavigationBanner(
                        selectedTab = selectedTab,
                        onTabSelected = { index, route ->
                            selectedTab = index
                            analytics.logEvent("tab_selected", mapOf("tab_index" to index))
                            if (route != null) {
                                navController.navigate(route)
                            }
                        }
                    )
                }
            }
        },
    ) { padding ->
        when {
            isLoading -> LoadingScreen()
            error != null -> ErrorScreen(error!!) { /* Implement retry logic */ }
            projects.isEmpty() -> EmptyProjectsScreen {
                showNewProjectDialog = true
            }
            else -> ProjectGrid(
                projects = projects,
                themeManager = themeManager,
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
        NavigationItem(stringResource(Res.string.projects), Lucide.FolderOpen, null),
        NavigationItem(stringResource(Res.string.lessons), Lucide.GraduationCap, Destination.Lessons.route),
        NavigationItem(stringResource(Res.string.about), Lucide.Info, Destination.About.route),
    )

    Surface(
        modifier = modifier
            .widthIn(max = 400.dp)
            .fillMaxWidth(),
        color = Color.Transparent
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
                    modifier = Modifier.weight(1f)
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
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
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
        Icon(ComposeIcons.Folder, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(Res.string.no_projects_found), modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurface)
        Button(onClick = onCreateNew) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.create_new))
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ProjectGrid(
    projects: List<ProjectModel>,
    themeManager: ThemeManager,
    onProjectClick: (ProjectModel) -> Unit,
    onDeleteProject: (ProjectModel) -> Unit,
    onEditProject: (ProjectModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val size = calculateWindowSizeClass()
    val minSize = when (size.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 250
        WindowWidthSizeClass.Medium -> 220
        else -> 180
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = minSize.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp,
        modifier = modifier
    ) {
        items(projects.size) { index ->
            val project = projects[index]
            ProjectCard(
                project = project,
                themeManager = themeManager,
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
    themeManager: ThemeManager,
    onProjectClick: (ProjectModel) -> Unit,
    onDeleteProject: (ProjectModel) -> Unit,
    onEditProject: (ProjectModel) -> Unit
) {
    var openMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick(project) },
        colors = DoodleVerseCardDefaults.primaryCardColors(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Column {
                    IconButton(onClick = {
                        openMenu = true
                    }) {
                        Icon(
                            ComposeIcons.MoreVert,
                            contentDescription = stringResource(Res.string.more_options)
                        )
                    }

                    DropdownMenu(
                        expanded = openMenu,
                        onDismissRequest = { openMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.delete)) },
                            onClick = {
                                onDeleteProject(project)
                                openMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.edit)) },
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
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
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
                        label = "${project.aspectRatio.width.toInt()}x${project.aspectRatio.height.toInt()}"
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun formatLastEdited(lastEdited: Long): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val diff = now - lastEdited
    return when {
        diff < 60_000 -> stringResource(Res.string.just_now)
        diff < 3_600_000 -> stringResource(Res.string.minutes_ago, diff / 60_000)
        diff < 86_400_000 -> stringResource(Res.string.hours_ago, diff / 3_600_000)
        else -> stringResource(Res.string.days_ago, diff / 86_400_000)
    }
}