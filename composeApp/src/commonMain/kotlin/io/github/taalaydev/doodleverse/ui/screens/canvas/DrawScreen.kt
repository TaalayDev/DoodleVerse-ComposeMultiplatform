package io.github.taalaydev.doodleverse.ui.screens.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.composables.icons.lucide.Album
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.Save
import com.composables.icons.lucide.Undo2
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.back
import doodleverse.composeapp.generated.resources.import_image
import doodleverse.composeapp.generated.resources.layers
import doodleverse.composeapp.generated.resources.paper_texture
import doodleverse.composeapp.generated.resources.paper_texture_1
import doodleverse.composeapp.generated.resources.redo
import doodleverse.composeapp.generated.resources.save
import doodleverse.composeapp.generated.resources.save_image
import doodleverse.composeapp.generated.resources.stamp_pencil
import doodleverse.composeapp.generated.resources.texture_1
import doodleverse.composeapp.generated.resources.texture_2
import doodleverse.composeapp.generated.resources.texture_asfalt_dark
import doodleverse.composeapp.generated.resources.texture_asfalt_light
import doodleverse.composeapp.generated.resources.texture_basketball
import doodleverse.composeapp.generated.resources.texture_fabric_light
import doodleverse.composeapp.generated.resources.undo
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.engine.DragState
import io.github.taalaydev.doodleverse.engine.DrawTool
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.brush.BrushFactory
import io.github.taalaydev.doodleverse.engine.brush.PenBrush
import io.github.taalaydev.doodleverse.engine.brush.ShaderBrushFactory
import io.github.taalaydev.doodleverse.engine.brush.TextureStampBrushFactory
import io.github.taalaydev.doodleverse.engine.brush.shader.ShaderBrushPresets
import io.github.taalaydev.doodleverse.engine.components.BrushPreview
import io.github.taalaydev.doodleverse.engine.components.DrawBox
import io.github.taalaydev.doodleverse.ui.components.DraggableSlider
import io.github.taalaydev.doodleverse.ui.components.DrawControls
import io.github.taalaydev.doodleverse.ui.components.LayersPanel
import io.github.taalaydev.doodleverse.ui.components.BrushList
import io.github.taalaydev.doodleverse.ui.theme.ThemeManager
import io.github.taalaydev.doodleverse.ui.theme.rememberThemeManager
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun DrawingCanvasScreen(
    projectId: Long,
    navController: NavController = rememberNavController(),
    themeManager: ThemeManager = rememberThemeManager(),
    platform: Platform,
    viewModel: DrawViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        DrawViewModel(platform.projectRepo, platform.dispatcherIO)
    },
) {
    val projectModel by viewModel.project.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadProject(projectId)
    }

    if (projectModel == null) {
        Scaffold { paddingValues ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val drawController = viewModel.drawController
    val currentTool by drawController.currentTool.collectAsStateWithLifecycle()
    val brushParams by drawController.brushParams.collectAsStateWithLifecycle()
    val currentBrush by drawController.currentBrush.collectAsStateWithLifecycle(PenBrush())

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProject()
        }
    }

    var menuOpen by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }

    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var dragState = remember { mutableStateOf(DragState()) }

    val size = calculateWindowSizeClass()
    val isMobile = when (size.widthSizeClass) {
        WindowWidthSizeClass.Compact -> true
        else -> false
    }
    val isTablet = when (size.widthSizeClass) {
        WindowWidthSizeClass.Medium -> true
        else -> false
    }

    val brushes = BrushFactory.allBrushes()

    Scaffold(
        containerColor = Color.Gray.copy(alpha = 0.1f),
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            Lucide.ArrowLeft,
                            contentDescription = stringResource(Res.string.back),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                title = {},
                actions = {
                    if (!isMobile && !isTablet) {
                        IconButton(onClick = {
                            viewModel.undo()
                        }) {
                            Icon(
                                Lucide.Undo2,
                                contentDescription = stringResource(Res.string.undo),
                                tint = if (canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            viewModel.redo()
                        }) {
                            Icon(
                                Lucide.Redo2,
                                contentDescription = stringResource(Res.string.redo),
                                tint = if (canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (isMobile || isTablet) {
                        IconButton(onClick = {
                            showLayersSheet = true
                        }) {
                            Icon(
                                painter = painterResource(Res.drawable.layers),
                                contentDescription = stringResource(Res.string.layers),
                                tint = if (canRedo) Color.Black else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            menuOpen = true
                        }
                    ) {
                        Icon(
                            Lucide.Save,
                            contentDescription = stringResource(Res.string.save),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = {
                            menuOpen = false
                        },
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .heightIn(max = 200.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Lucide.Save,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            stringResource(Res.string.save_image),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            onClick = {
                                viewModel.saveAsPng()
                                menuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Lucide.ImagePlus,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            stringResource(Res.string.import_image),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            },
                            onClick = {
                                menuOpen = false
                                viewModel.importImage()
                            }
                        )
                    }
                }
            )
        },
    ) { paddingValues ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .align(Alignment.CenterVertically)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DrawBox(
                        controller = viewModel.drawController,
                        dragState = dragState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )

                    DrawControls(
                        viewModel = viewModel,
                        brushes = brushes,
                        brush = currentBrush,
                        color = brushParams.color,
                        brushSize = brushParams.size,
                        tool = currentTool,
                        isFloating = true,
                        showLayersSheet = showLayersSheet,
                        onToggleLayersSheet = { showLayersSheet = !showLayersSheet },
                        onBrushSelected = {
                            drawController.setBrush(it)
                        },
                        onColorSelected = {
                            drawController.setColor(it)
                        },
                        onSizeSelected = {
                            drawController.setBrushSize(it)
                        },
                        onToolSelected = {
                            drawController.setTool(it)
                        },
                    )
                }

                if (!isMobile) {
                    DraggableSlider(
                        value = brushParams.size,
                        onValueChange = {
                            drawController.setBrushSize(it)
                        },
                    ) {
                        Text(
                            text = "${brushParams.size.toInt()}px",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.9f),
                        )
                    }
                }

                if (isMobile || isTablet) {
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp),
                    ) {
                        val undoButtonShape = RoundedCornerShape(
                            topStart = 16.dp,
                            bottomStart = 16.dp
                        )
                        val redoButtonShape = RoundedCornerShape(
                            topEnd = 16.dp,
                            bottomEnd = 16.dp
                        )
                        IconButton(
                            onClick = {
                                viewModel.undo()
                            },
                            modifier = Modifier
                                .clip(undoButtonShape)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    undoButtonShape
                                )
                        ) {
                            Icon(
                                Lucide.Undo2,
                                contentDescription = stringResource(Res.string.undo),
                                tint = if (canUndo) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.redo()
                            },
                            modifier = Modifier
                                .clip(redoButtonShape)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    redoButtonShape
                                )
                        ) {
                            Icon(
                                Lucide.Redo2,
                                contentDescription = stringResource(Res.string.redo),
                                tint = if (canRedo) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (!isMobile && !isTablet) {
                Column(
                    modifier = Modifier.fillMaxHeight()
                        .width(250.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    LayersPanel(
                        drawViewModel = viewModel,
                        modifier = Modifier.weight(1f),
                    )
                    HorizontalDivider()
                    ToolPanel(
                        selectedTool = currentTool,
                        selectedColor = brushParams.color,
                        selectedBrush = currentBrush,
                        onBrushSelected = { brush ->
                            drawController.setBrush(brush)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun ToolPanel(
    selectedTool: DrawTool,
    selectedBrush: Brush? = null,
    selectedColor: Color = Color.Black,
    onBrushSelected: (Brush) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val brushes = BrushFactory.allBrushes()

    androidx.compose.material.Surface(
        modifier = modifier,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BrushList(
                brushes = brushes,
                selectedBrush = selectedBrush,
                onSelected = onBrushSelected,
            )
        }
    }
}