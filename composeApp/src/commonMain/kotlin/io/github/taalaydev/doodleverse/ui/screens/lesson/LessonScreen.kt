package io.github.taalaydev.doodleverse.ui.screens.lesson

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.composables.icons.lucide.*
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.core.DragState
import io.github.taalaydev.doodleverse.core.DrawRenderer
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.handleDrawing
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.LessonModel
import io.github.taalaydev.doodleverse.data.models.LessonPartModel
import io.github.taalaydev.doodleverse.navigation.Destination
import io.github.taalaydev.doodleverse.ui.components.BrushPicker
import io.github.taalaydev.doodleverse.ui.components.DraggableSlider
import io.github.taalaydev.doodleverse.ui.components.DrawBox
import io.github.taalaydev.doodleverse.ui.components.DrawState
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawControls
import io.github.taalaydev.doodleverse.ui.screens.draw.ShapePickerSheet
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun LessonDetailScreen(
    platform: Platform,
    lesson: LessonModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: LessonDrawViewModel = viewModel { LessonDrawViewModel(platform.dispatcherIO) }
) {
    val scope = rememberCoroutineScope()
    var showStartDrawing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }

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

    if (showStartDrawing) {
        DrawingPromptDialog(
            onDismiss = { showStartDrawing = false },
            onStartDrawing = {
                showStartDrawing = false
                navController.navigate(Destination.Drawing(0))
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
                            text = "Step ${currentPage + 1} of ${lesson.parts.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Lucide.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.setTool(Tool.Eraser(BrushData.eraser))
                    }) {
                        Icon(
                            Lucide.Eraser,
                            contentDescription = "Eraser",
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
                            contentDescription = "Shape",
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
                            contentDescription = "Pen",
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
                        referenceImage = imageResource(lesson.parts[currentPage].image),
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
                            contentDescription = "Undo",
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
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.Black else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Step description overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(lesson.parts[currentPage].description),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
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
                            Text("Prev")
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Step ${currentPage + 1}",
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
                            enabled = canUndo,
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
                            Text(if (currentPage == lesson.parts.size - 1) "Start Drawing" else "Next")
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
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
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
                    Text("Start Drawing")
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
                        Icon(Lucide.Check, contentDescription = "Completed")
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
                    text = "Step $stepNumber",
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
            Text("Ready to Draw?")
        },
        text = {
            Text(
                "You've learned all the steps! Would you like to start drawing now? " +
                        "You can always come back to review the lesson later.",
            )
        },
        confirmButton = {
            Button(onClick = onStartDrawing) {
                Text("Start Drawing")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Yet")
            }
        }
    )
}