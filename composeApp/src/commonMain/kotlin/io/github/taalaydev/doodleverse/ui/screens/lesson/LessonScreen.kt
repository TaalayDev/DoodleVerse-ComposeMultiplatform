package io.github.taalaydev.doodleverse.ui.screens.lesson

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.composables.icons.lucide.*
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.back
import doodleverse.composeapp.generated.resources.completed
import doodleverse.composeapp.generated.resources.eraser
import doodleverse.composeapp.generated.resources.more_options
import doodleverse.composeapp.generated.resources.next
import doodleverse.composeapp.generated.resources.not_yet
import doodleverse.composeapp.generated.resources.pen
import doodleverse.composeapp.generated.resources.previous
import doodleverse.composeapp.generated.resources.ready_to_draw
import doodleverse.composeapp.generated.resources.ready_to_draw_description
import doodleverse.composeapp.generated.resources.redo
import doodleverse.composeapp.generated.resources.shapes
import doodleverse.composeapp.generated.resources.start_drawing
import doodleverse.composeapp.generated.resources.step_count
import doodleverse.composeapp.generated.resources.step_count_of
import doodleverse.composeapp.generated.resources.undo
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.core.DragState
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.LessonModel
import io.github.taalaydev.doodleverse.data.models.LessonPartModel
import io.github.taalaydev.doodleverse.navigation.Destination
import io.github.taalaydev.doodleverse.ui.components.BrushPicker
import io.github.taalaydev.doodleverse.ui.components.DrawBox
import io.github.taalaydev.doodleverse.ui.screens.draw.ShapePickerSheet
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LessonDetailScreen(
    platform: Platform,
    lesson: LessonModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: LessonDrawViewModel = viewModel {
        LessonDrawViewModel(
            platform.projectRepo,
            platform.dispatcherIO
        )
    }
) {
    val scope = rememberCoroutineScope()
    var showStartDrawing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }
    var showDescription by remember { mutableStateOf(true) }

    LaunchedEffect(currentPage) {
        // showDescription = true
    }

    val brushSize by viewModel.brushSize.collectAsStateWithLifecycle()
    val currentColor by viewModel.currentColor.collectAsStateWithLifecycle()
    val currentTool by viewModel.currentTool.collectAsStateWithLifecycle()
    val currentBrush by viewModel.currentBrush.collectAsStateWithLifecycle(BrushData.solid)
    var dragState = remember { mutableStateOf(DragState()) }

    val windowSize = calculateWindowSizeClass()
    val isCompactWidth = windowSize.widthSizeClass == WindowWidthSizeClass.Compact
    val isMediumWidth = windowSize.widthSizeClass == WindowWidthSizeClass.Medium
    val isExpandedWidth = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded

    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    val brushPickerBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBrushPicker by remember { mutableStateOf(false) }

    val shapePickerBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showShapePicker by remember { mutableStateOf(false) }

    val title = stringResource(lesson.title)

    if (showStartDrawing) {
        DrawingPromptDialog(
            onDismiss = { showStartDrawing = false },
            onStartDrawing = {
                viewModel.createProject(title, 1,1) { project ->
                    showStartDrawing = false
                    navController.popBackStack()
                    navController.navigate(Destination.Drawing(project.id))
                }
            }
        )
    }

    if (showBrushPicker) {
        BrushPicker(
            bottomSheetState = brushPickerBottomSheetState,
            selectedBrush = currentBrush,
            onSelected = { brush ->
                viewModel.setBrush(brush)
                showBrushPicker = false
            },
            onDismiss = { showBrushPicker = false }
        )
    }

    LaunchedEffect(showBrushPicker) {
        if (showBrushPicker) {
            brushPickerBottomSheetState.show()
        } else {
            brushPickerBottomSheetState.hide()
        }
    }

    if (showShapePicker) {
        ShapePickerSheet(
            bottomSheetState = shapePickerBottomSheetState,
            brush = currentBrush,
            onSelected = { brush ->
                viewModel.setBrush(brush)
                showShapePicker = false
            },
            onDismiss = { showShapePicker = false }
        )
    }

    LaunchedEffect(showShapePicker) {
        if (showShapePicker) {
            shapePickerBottomSheetState.show()
        } else {
            shapePickerBottomSheetState.hide()
        }
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Left sidebar for expanded width (desktop)
        if (isExpandedWidth) {
            LessonSidebar(
                lesson = lesson,
                currentPage = currentPage,
                canMoveNext = canUndo,
                onPageSelected = {
                    currentPage = it
                    viewModel.drawingController.clearUndoRedoStack()
                },
                onStartDrawing = { showStartDrawing = true },
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            )
        }

        // Main content area
        Column(modifier = Modifier.weight(1f)) {
            // Top bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(lesson.title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(Res.string.step_count_of, currentPage + 1, lesson.parts.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Lucide.ArrowLeft,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.setTool(Tool.Eraser(BrushData.eraser))
                    }) {
                        Icon(
                            Lucide.Eraser,
                            contentDescription = stringResource(Res.string.eraser),
                            tint = if (currentTool.isEraser)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = {
                        showShapePicker = true
                    }) {
                        Icon(
                            Lucide.Circle,
                            contentDescription = stringResource(Res.string.shapes),
                            tint = if (currentTool.isShape)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = {
                        showBrushPicker = true
                    }) {
                        Icon(
                            Lucide.Pen,
                            contentDescription = stringResource(Res.string.pen),
                            tint = if (currentTool.isBrush)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize().clipToBounds()
                ) {
                    DrawBox(
                        drawProvider = viewModel,
                        drawController = viewModel.drawingController,
                        currentBrush = currentBrush,
                        currentColor = currentColor,
                        brushSize = brushSize,
                        currentTool = currentTool,
                        isMobile = !isExpandedWidth,
                        dragState = dragState,
                        aspectRatio = 1f,
                        referenceImage = if (currentPage != lesson.parts.size - 1) {
                            imageResource(lesson.parts[currentPage].image)
                        } else {
                            null
                        },
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.TopStart),
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
                            tint = if (canUndo) Color.Black else Color.Gray,
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
                            tint = if (canRedo) Color.Black else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Step description overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showDescription,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(lesson.parts[currentPage].description),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(end = 24.dp)
                            )

                            IconButton(
                                onClick = { showDescription = false },
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Lucide.X,
                                    contentDescription = "Close description",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Button to show description again if it's hidden
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showDescription,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showDescription = true },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Lucide.Info,
                            contentDescription = "Show description",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom navigation for compact/medium width
            if (!isExpandedWidth) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { currentPage = maxOf(0, currentPage - 1) },
                            enabled = currentPage > 0,
                            modifier = Modifier.width(100.dp)
                        ) {
                            Icon(Lucide.ChevronLeft, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(Res.string.previous))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text =  stringResource(Res.string.step_count, currentPage + 1),
                                style = MaterialTheme.typography.titleSmall
                            )
                            LinearProgressIndicator(
                                progress = { (currentPage + 1).toFloat() / lesson.parts.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

                        TextButton(
                            enabled = canUndo || currentPage == lesson.parts.size - 1,
                            onClick = {
                                if (currentPage == lesson.parts.size - 1) {
                                    showStartDrawing = true
                                } else {
                                    currentPage = minOf(lesson.parts.size - 1, currentPage + 1)
                                }
                                viewModel.drawingController.clearUndoRedoStack()
                            },
                            modifier = Modifier.width(100.dp)
                        ) {
                            Text(
                                if (currentPage == lesson.parts.size - 1)
                                    stringResource(Res.string.start_drawing)
                                else
                                    stringResource(Res.string.next)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Lucide.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonSidebar(
    lesson: LessonModel,
    currentPage: Int,
    canMoveNext: Boolean = true,
    onPageSelected: (Int) -> Unit,
    onStartDrawing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxHeight()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(lesson.title),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { /* Show more options */ }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(Res.string.more_options)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentPage + 1).toFloat() / lesson.parts.size },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            // Steps list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lesson.parts.size) { index ->
                    LessonStepCard(
                        step = lesson.parts[index],
                        stepNumber = index + 1,
                        isCompleted = index < currentPage,
                        isActive = index == currentPage,
                        onClick = {
                            if (index < currentPage || (canMoveNext && index == currentPage + 1)) {
                                onPageSelected(index)
                            }
                        }
                    )
                }
            }

            if (currentPage == lesson.parts.size - 1) {
                Spacer(Modifier.height(16.dp))

                // Start drawing button
                Button(
                    onClick = onStartDrawing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Lucide.Pen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.start_drawing))
                }
            }
        }
    }
}

@Composable
private fun LessonStepCard(
    step: LessonPartModel,
    stepNumber: Int,
    isCompleted: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isCompleted -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step status icon
            Surface(
                shape = CircleShape,
                color = if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(32.dp),
                contentColor = if (isCompleted) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCompleted) {
                        Icon(
                            Lucide.Check,
                            contentDescription = stringResource(Res.string.completed)
                        )
                    } else {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            // Step content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.step_count, stepNumber),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(step.description),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DrawingPromptDialog(
    onDismiss: () -> Unit,
    onStartDrawing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.ready_to_draw))
        },
        text = {
            Text(stringResource(Res.string.ready_to_draw_description))
        },
        confirmButton = {
            Button(onClick = onStartDrawing) {
                Text(stringResource(Res.string.start_drawing))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.not_yet))
            }
        }
    )
}