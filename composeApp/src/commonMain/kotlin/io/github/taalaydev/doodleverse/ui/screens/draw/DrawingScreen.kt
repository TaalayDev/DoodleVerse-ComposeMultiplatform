package io.github.taalaydev.doodleverse.ui.screens.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import io.github.taalaydev.doodleverse.core.DragState
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.Shape
import io.github.taalaydev.doodleverse.navigation.Destination
import io.github.taalaydev.doodleverse.ui.components.BrushGrid
import io.github.taalaydev.doodleverse.ui.components.BrushPicker
import io.github.taalaydev.doodleverse.ui.components.ColorPalettePanel
import io.github.taalaydev.doodleverse.ui.components.ColorPicker
import io.github.taalaydev.doodleverse.ui.components.DraggableSlider
import io.github.taalaydev.doodleverse.ui.components.DrawCanvas
import io.github.taalaydev.doodleverse.ui.components.LayersPanel
import io.github.taalaydev.doodleverse.ui.components.NewProjectDialog
import kotlinx.datetime.Clock
import org.jetbrains.compose.ui.tooling.preview.Preview

data class DpSize(val width: Dp, val height: Dp)

@Composable
fun DrawingScreen(
    navController: NavHostController = rememberNavController(),
) {
    var projectModel by remember { mutableStateOf(ProjectModel.currentProject) }

    if (projectModel == null) {
        NewProjectDialog(
            onDismissRequest = {  },
            onConfirm = { name, width, height ->
                projectModel = ProjectModel(
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
            },
            showCancelButton = false,
            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        )
    }

    if (projectModel != null) {
        DrawScreenBody(
            projectModel = projectModel!!,
            navController = navController,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

inline fun toDp(value: Float, density: Density): Dp {
    return with(density) { value.toDp() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun DrawScreenBody(
    projectModel: ProjectModel,
    navController: NavController = rememberNavController(),
    viewModel: DrawViewModel = viewModel { DrawViewModel() },
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val focusRequester = remember { FocusRequester() }

    val brushSize by viewModel.brushSize.collectAsStateWithLifecycle()
    val currentColor by viewModel.currentColor.collectAsStateWithLifecycle()
    val currentTool by viewModel.currentTool.collectAsStateWithLifecycle()
    val currentBrush by viewModel.currentBrush.collectAsStateWithLifecycle(BrushData.solid)

    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var dragState by remember { mutableStateOf(DragState()) }

    val density = LocalDensity.current

    val pxToDp: (Int) -> Dp = { px ->
        with(density) { px.toDp() }
    }

    var contentSize by remember { mutableStateOf(IntSize(0, 0)) }

    val size = calculateWindowSizeClass()
    val isMobile = when (size.widthSizeClass) {
        WindowWidthSizeClass.Compact -> true
        else -> false
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                title = {},
                actions = {
                    IconButton(onClick = {
                        viewModel.undo()
                    }) {
                        Icon(
                            Lucide.Undo2,
                            contentDescription = "Undo",
                            tint = if (canUndo) Color.Black else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.redo()
                    }) {
                        Icon(
                            Lucide.Redo2,
                            contentDescription = "Redo",
                            tint = if (canRedo) Color.Black else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.saveAsPng()
                        }
                    ) {
                        Icon(
                            Lucide.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(paddingValues),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .onGloballyPositioned {
                        contentSize = it.size
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    var oldTool by remember { mutableStateOf<Tool?>(null) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = dragState.zoom
                                scaleY = dragState.zoom
                                translationX = dragState.draggedTo.x
                                translationY = dragState.draggedTo.y
                                rotationZ = dragState.rotation
                            }
                            .padding(10.dp)
                            .focusRequester(focusRequester)
                            .focusable()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    val isControlPressed = event.isCtrlPressed || event.isMetaPressed
                                    val isShiftPressed = event.isShiftPressed
                                    val isSpacePressed = event.key == Key.Spacebar

                                    when {
                                        isControlPressed && !isShiftPressed && event.key == Key.Z -> {
                                            viewModel.undo()
                                            true
                                        }
                                        isControlPressed && isShiftPressed && event.key == Key.Z -> {
                                            viewModel.redo()
                                            true
                                        }
                                        isControlPressed && event.key == Key.B -> {
                                            true
                                        }
                                        isControlPressed && event.key == Key.Equals -> {
                                            dragState = dragState.copy(
                                                zoom = (dragState.zoom * 1.1f).coerceAtMost(5f)
                                            )
                                            true
                                        }
                                        isControlPressed && event.key == Key.Minus -> {
                                            dragState = dragState.copy(
                                                zoom = (dragState.zoom / 1.1f).coerceAtLeast(0.2f)
                                            )
                                            true
                                        }
                                        isSpacePressed -> {
                                            if (oldTool == null) {
                                                oldTool = currentTool
                                                viewModel.setTool(Tool.Drag)
                                            }
                                            true
                                        }
                                        else -> {
                                            if (oldTool != null) {
                                                viewModel.setTool(oldTool!!)
                                                oldTool = null
                                            }
                                            false
                                        }
                                    }
                                } else {
                                    false
                                }
                            }
                            .pointerInput(currentTool) {
                                if (currentTool == Tool.Zoom || currentTool == Tool.Drag) {
                                    detectDragGestures { change, dragAmount ->
                                        if (currentTool == Tool.Zoom) {
                                            dragState = dragState.copy(
                                                zoom = (dragState.zoom * (1 + dragAmount.y / 1000)).coerceIn(0.5f, 5f)
                                            )
                                        } else {
                                            dragState = dragState.copy(
                                                draggedTo = dragState.draggedTo + dragAmount
                                            )
                                        }
                                    }

                                    return@pointerInput
                                }
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    dragState = dragState.copy(
                                        zoom = (dragState.zoom * zoom).coerceIn(0.5f, 5f),
                                        draggedTo = dragState.draggedTo + pan,
                                        rotation = dragState.rotation + rotation
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        DrawCanvas(
                            currentBrush = currentBrush,
                            currentColor = currentColor,
                            brushSize = brushSize,
                            tool = currentTool,
                            gestureEnabled = !currentTool.isZoom && !currentTool.isDrag,
                            controller = viewModel.drawingController,
                            onColorPicked = { color ->
                                viewModel.setColor(color)
                            },
                            modifier = Modifier
                                .aspectRatio(projectModel.aspectRatioValue),
                        )
                    }

                    DrawControls(
                        tool = currentTool,
                        brushSize = brushSize,
                        brush = currentBrush,
                        color = currentColor,
                        isFloating = !isMobile,
                        onBrushSelected = {
                            viewModel.setBrush(it)
                        },
                        onColorSelected = {
                            viewModel.setColor(it)
                        },
                        onSizeSelected = {
                            viewModel.setBrushSize(it)
                        },
                        onToolSelected = {
                            viewModel.setTool(it)
                        },
                    )
                }

                if (!isMobile) {
                    DraggableSlider(
                        value = brushSize,
                        onValueChange = {
                            viewModel.setBrushSize(it)
                        },
                    ) {
                        Text(
                            text = "${brushSize.toInt()}px",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.9f),
                        )
                    }
                }

                if (!isMobile && currentTool.isZoom) {
                    var sliderHeightPx by remember { mutableStateOf(0f) }

                    val minZoom = 0.5f
                    val maxZoom = 5f

                    val thumbHeightDp = 30.dp
                    val thumbHeightPx = with(LocalDensity.current) { thumbHeightDp.toPx() }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(vertical = 10.dp, horizontal = 5.dp)
                            .width(10.dp)
                            .fillMaxHeight()
                            .border(1.dp, Color.White, RoundedCornerShape(5.dp))
                            .clip(RoundedCornerShape(5.dp))
                            .onGloballyPositioned { layoutCoordinates ->
                                sliderHeightPx = layoutCoordinates.size.height.toFloat()
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val y = offset.y.coerceIn(0f, sliderHeightPx - thumbHeightPx)
                                    val normalizedPosition = 1f - y / (sliderHeightPx - thumbHeightPx)
                                    val newZoom = minZoom + normalizedPosition * (maxZoom - minZoom)
                                    dragState = dragState.copy(zoom = newZoom)
                                }
                            }
                    ) {
                        // Map zoom to slider position
                        val sliderPosition = remember(dragState.zoom, sliderHeightPx) {
                            val normalizedZoom = (dragState.zoom - minZoom) / (maxZoom - minZoom)
                            (sliderHeightPx - thumbHeightPx) * (1 - normalizedZoom)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(thumbHeightDp)
                                .offset { IntOffset(0, sliderPosition.toInt()) }
                                .background(Color.Gray.copy(alpha = 0.5f))
                                .clip(RoundedCornerShape(5.dp))
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { deltaY ->
                                        val newSliderPosition = (sliderPosition + deltaY)
                                            .coerceIn(0f, sliderHeightPx - thumbHeightPx)
                                        val normalizedPosition = 1f - newSliderPosition / (sliderHeightPx - thumbHeightPx)
                                        val newZoom = minZoom + normalizedPosition * (maxZoom - minZoom)
                                        dragState = dragState.copy(zoom = newZoom)
                                    }
                                )
                        )
                    }
                }
            }
            if (!isMobile) {
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
                    ColorPalettePanel(
                        currentColor = currentColor,
                        onColorSelected = { color ->
                            viewModel.setColor(color)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawControls(
    brushSize: Float = 10f,
    brush: BrushData,
    tool: Tool,
    color: Color = Color(0xFF333333),
    isFloating: Boolean = false,
    onBrushSelected: (BrushData) -> Unit = {},
    onColorSelected: (Color) -> Unit = {},
    onSizeSelected: (Float) -> Unit = {},
    onToolSelected: (Tool) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    var showBrushPicker by remember { mutableStateOf(false) }
    val brushPickerBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var showColorPicker by remember { mutableStateOf(false) }
    val colorPickerBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var showSizeSelector by remember { mutableStateOf(false) }
    val sizeSelectorBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var showShapePicker by remember { mutableStateOf(false) }
    val shapePickerBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showBrushPicker) {
        BrushPicker(
            bottomSheetState = brushPickerBottomSheetState,
            selectedBrush = brush,
            onSelected = { brush ->
                onBrushSelected(brush)
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

    if (showColorPicker) {
        ColorPicker(
            bottomSheetState = colorPickerBottomSheetState,
            initialColor = color,
            onColorSelected = { color ->
                onColorSelected(color)
            },
            onDismiss = { showColorPicker = false }
        )
    }

    LaunchedEffect(showColorPicker) {
        if (showColorPicker) {
            colorPickerBottomSheetState.show()
        } else {
            colorPickerBottomSheetState.hide()
        }
    }

    if (showSizeSelector) {
        SizeSelector(
            bottomSheetState = sizeSelectorBottomSheetState,
            initialSize = brushSize,
            onSelected = { size ->
                onSizeSelected(size)
                showSizeSelector = false
            },
            onDismiss = { showSizeSelector = false }
        )
    }

    LaunchedEffect(showSizeSelector) {
        if (showSizeSelector) {
            sizeSelectorBottomSheetState.show()
        } else {
            sizeSelectorBottomSheetState.hide()
        }
    }

    if (showShapePicker) {
        ShapePickerSheet(
            bottomSheetState = shapePickerBottomSheetState,
            brush = brush,
            onSelected = { brush ->
                onBrushSelected(brush)
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

    if (!isFloating) {
        BottomAppBar(
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { showColorPicker = true }) {
                    Icon(
                        Lucide.Palette,
                        contentDescription = "Color",
                        tint = color
                    )
                }
                IconButton(
                    onClick = {
                        showBrushPicker = true
                    }
                ) {
                    Icon(
                        Lucide.Brush,
                        contentDescription = "Brush",
                        tint = if (tool.isBrush) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    onBrushSelected(BrushData.eraser)
                }) {
                    Icon(
                        Lucide.Eraser,
                        contentDescription = "Eraser",
                        tint = if (tool.isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    showSizeSelector = true
                }) {
                    Icon(
                        Lucide.SlidersHorizontal,
                        contentDescription = "Brush Settings",
                    )
                }
                IconButton(onClick = {
                    showShapePicker = true
                }) {
                    Icon(
                        Lucide.Shapes,
                        contentDescription = "Shapes",
                        tint = if (tool.isShape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    onToolSelected(Tool.Fill)
                }) {
                    Icon(
                        Lucide.PaintBucket,
                        contentDescription = "Fill",
                        tint = if (tool.isFill) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .padding(bottom = 5.dp)
                .widthIn(max = 450.dp)
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    showBrushPicker = true
                }
            ) {
                Icon(
                    Lucide.Brush,
                    contentDescription = "Brush",
                    tint = if (tool.isBrush) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                onBrushSelected(BrushData.eraser)
            }) {
                Icon(
                    Lucide.Eraser,
                    contentDescription = "Eraser",
                    tint = if (tool.isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                showShapePicker = true
            }) {
                Icon(
                    Lucide.Shapes,
                    contentDescription = "Shapes",
                    tint = if (tool.isShape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                onToolSelected(Tool.Fill)
            }) {
                Icon(
                    Lucide.PaintBucket,
                    contentDescription = "Fill",
                    tint = if (tool.isFill) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                onToolSelected(Tool.Eyedropper)
            }) {
                Icon(
                    Lucide.Pipette,
                    contentDescription = "Eyedropper",
                    tint = if (tool.isEyedropper) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                onToolSelected(Tool.Zoom)
            }) {
                Icon(
                    Lucide.ZoomIn,
                    contentDescription = "Zoom",
                    tint = if (tool.isZoom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = {
                onToolSelected(Tool.Drag)
            }) {
                Icon(
                    Lucide.Grab,
                    contentDescription = "Grab",
                    tint = if (tool.isDrag) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapePickerSheet(
    bottomSheetState: SheetState,
    brush: BrushData? = null,
    onSelected: (BrushData) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val shapes = remember { Shape.all }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            BrushGrid(
                brushes = shapes,
                selectedBrush = brush,
                onSelected = onSelected,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeSelector(
    bottomSheetState: SheetState,
    initialSize: Float = 10f,
    onSelected: (Float) -> Unit = {},
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(initialSize) }

    DisposableEffect(Unit) {
        onDispose {
            onSelected(value)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Lucide.Brush,
                        contentDescription = "Brush Size",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${value.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = value,
                        onValueChange = { values ->
                            value = values
                        },
                        valueRange = 10f..80f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Preview
@Composable
private fun DrawingScreenPreview() {
    MaterialTheme {
        DrawingScreen()
    }
}