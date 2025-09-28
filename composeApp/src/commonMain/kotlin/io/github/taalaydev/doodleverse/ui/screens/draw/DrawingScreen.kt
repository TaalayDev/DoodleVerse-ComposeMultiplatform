package io.github.taalaydev.doodleverse.ui.screens.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.back
import doodleverse.composeapp.generated.resources.brush
import doodleverse.composeapp.generated.resources.brush_settings
import doodleverse.composeapp.generated.resources.brush_size
import doodleverse.composeapp.generated.resources.color
import doodleverse.composeapp.generated.resources.eraser
import doodleverse.composeapp.generated.resources.eyedropper
import doodleverse.composeapp.generated.resources.fill
import doodleverse.composeapp.generated.resources.import_image
import doodleverse.composeapp.generated.resources.layers
import doodleverse.composeapp.generated.resources.move_tool
import doodleverse.composeapp.generated.resources.redo
import doodleverse.composeapp.generated.resources.save
import doodleverse.composeapp.generated.resources.save_image
import doodleverse.composeapp.generated.resources.selection_tool
import doodleverse.composeapp.generated.resources.shapes
import doodleverse.composeapp.generated.resources.undo
import doodleverse.composeapp.generated.resources.zoom
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.engine.DragState
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.color.ColorPaletteGenerator
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.Shape
import io.github.taalaydev.doodleverse.purchase.PurchaseViewModel
import io.github.taalaydev.doodleverse.ui.components.BrushGrid
import io.github.taalaydev.doodleverse.ui.components.BrushPicker
import io.github.taalaydev.doodleverse.ui.components.ColorPalettePanel
import io.github.taalaydev.doodleverse.ui.components.ColorPicker
import io.github.taalaydev.doodleverse.ui.components.DraggableSlider
import io.github.taalaydev.doodleverse.ui.components.DrawBox
import io.github.taalaydev.doodleverse.ui.components.EnhancedColorPickerSheet
import io.github.taalaydev.doodleverse.ui.components.LayersPanel
import io.github.taalaydev.doodleverse.ui.components.ShapePickerSheet
import io.github.taalaydev.doodleverse.ui.components.SizeSelector
import io.github.taalaydev.doodleverse.ui.components.ToolPanel
import io.github.taalaydev.doodleverse.ui.theme.AnimatedScaffold
import io.github.taalaydev.doodleverse.ui.theme.ThemeManager
import io.github.taalaydev.doodleverse.ui.theme.rememberThemeManager
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class DpSize(val width: Dp, val height: Dp)

@Composable
fun DrawingScreen(
    projectId: Long,
    navController: NavHostController = rememberNavController(),
    themeManager: ThemeManager = rememberThemeManager(),
    platform: Platform,
    viewModel: DrawViewModel = viewModel {
        DrawViewModel(platform.projectRepo, platform.dispatcherIO)
    },
    purchaseViewModel: PurchaseViewModel,
) {
    val projectModel by viewModel.project.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadProject(projectId)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveProject()
        }
    }

    if (projectModel != null) {
        DrawScreenBody(
            projectModel = projectModel!!,
            navController = navController,
            themeManager = themeManager,
            modifier = Modifier.fillMaxSize(),
            viewModel = viewModel,
            platform = platform,
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(paddingValues)
                        .align(Alignment.Center)
                )
            }
        }
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
    themeManager: ThemeManager = rememberThemeManager(),
    viewModel: DrawViewModel,
    modifier: Modifier = Modifier,
    platform: Platform,
) {
    val brushSize by viewModel.brushSize.collectAsStateWithLifecycle()
    val currentColor by viewModel.currentColor.collectAsStateWithLifecycle()
    val currentTool by viewModel.currentTool.collectAsStateWithLifecycle()
    val currentBrush by viewModel.currentBrush.collectAsStateWithLifecycle(BrushData.solid)

    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    val dragState = remember { mutableStateOf(DragState()) }

    var menuOpen by remember { mutableStateOf(false) }
    var showLayersSheet by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    var contentSize by remember { mutableStateOf(IntSize(0, 0)) }

    val size = calculateWindowSizeClass()
    val isMobile = when (size.widthSizeClass) {
        WindowWidthSizeClass.Compact -> true
        else -> false
    }
    val isTablet = when (size.widthSizeClass) {
        WindowWidthSizeClass.Medium -> true
        else -> false
    }

    AnimatedScaffold(
        themeManager = themeManager,
        modifier = modifier.fillMaxSize(),
        animateBackground = false,
        // containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                    modifier = Modifier.fillMaxSize().clipToBounds(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DrawBox(
                        drawProvider = viewModel,
                        drawController = viewModel.drawingController,
                        dragState = dragState,
                        brushSize = brushSize,
                        currentColor = currentColor,
                        currentTool = currentTool,
                        currentBrush = currentBrush,
                        isMobile = isMobile,
                        aspectRatio = projectModel.aspectRatioValue,
                    )

                    DrawControlsWithAIPalette(
                        viewModel = viewModel,
                        tool = currentTool,
                        brushSize = brushSize,
                        brush = currentBrush,
                        color = currentColor,
                        isFloating = true,
                        showLayersSheet = showLayersSheet,
                        onToggleLayersSheet = { showLayersSheet = !showLayersSheet },
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
                                    dragState.value = dragState.value.copy(zoom = newZoom)
                                }
                            }
                    ) {
                        // Map zoom to slider position
                        val sliderPosition = remember(dragState.value.zoom, sliderHeightPx) {
                            val normalizedZoom = (dragState.value.zoom - minZoom) / (maxZoom - minZoom)
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
                                        dragState.value = dragState.value.copy(zoom = newZoom)
                                    }
                                )
                        )
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
                        selectedColor = currentColor,
                        selectedBrush = currentBrush,
                        onBrushSelected = { brush ->
                            viewModel.setBrush(brush)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Extension of DrawControls with AI Color Palette functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawControlsWithAIPalette(
    viewModel: DrawViewModel,
    tool: Tool,
    brushSize: Float,
    brush: BrushData,
    color: Color,
    isFloating: Boolean = false,
    isTablet: Boolean = false,
    showLayersSheet: Boolean = false,
    onToggleLayersSheet: () -> Unit = {},
    onBrushSelected: (BrushData) -> Unit = {},
    onColorSelected: (Color) -> Unit = {},
    onSizeSelected: (Float) -> Unit = {},
    onToolSelected: (Tool) -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    val colorPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showEnhancedColorPicker by remember { mutableStateOf(false) }
    var lastExtractedImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var extractedPalette by remember { mutableStateOf<ColorPaletteGenerator.Palette?>(null) }
    val colorPaletteGenerator = remember { ColorPaletteGenerator() }

    LaunchedEffect(lastExtractedImage) {
        if (lastExtractedImage != null) {
            extractedPalette = colorPaletteGenerator.extractPaletteFromImage(lastExtractedImage!!)
        }
    }

    if (showEnhancedColorPicker) {
//        ColorPaletteDialog(
//            initialColor = color,
//            onDismiss = { showEnhancedColorPicker = false },
//            onColorSelected = { color ->
//                onColorSelected(color)
//                showEnhancedColorPicker = false
//            }
//        )
        EnhancedColorPickerSheet(
            initialColor = color,
            onColorSelected = onColorSelected,
            bottomSheetState = colorPickerSheetState,
            onDismiss = { showEnhancedColorPicker = false }
        )
    }

    LaunchedEffect(showEnhancedColorPicker) {
        if (showEnhancedColorPicker) {
            colorPickerSheetState.show()
        } else {
            colorPickerSheetState.hide()
        }
    }

    DrawControls(
        viewModel = viewModel,
        tool = tool,
        brushSize = brushSize,
        brush = brush,
        color = color,
        isFloating = isFloating,
        showLayersSheet = showLayersSheet,
        onToggleLayersSheet = onToggleLayersSheet,
        onBrushSelected = onBrushSelected,
        onColorSelected = onColorSelected,
        onSizeSelected = onSizeSelected,
        onToolSelected = onToolSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun DrawControls(
    viewModel: DrawViewModel,
    brushSize: Float = 10f,
    brush: BrushData,
    tool: Tool,
    color: Color = Color(0xFF333333),
    isFloating: Boolean = false,
    showLayersSheet: Boolean = false,
    onToggleLayersSheet: () -> Unit = {},
    onBrushSelected: (BrushData) -> Unit = {},
    onColorSelected: (Color) -> Unit = {},
    onSizeSelected: (Float) -> Unit = {},
    onToolSelected: (Tool) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val size = calculateWindowSizeClass()

    val isTablet = when (size.widthSizeClass) {
        WindowWidthSizeClass.Medium -> true
        else -> false
    }
    val isMobile = when (size.widthSizeClass) {
        WindowWidthSizeClass.Compact -> true
        else -> false
    }

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

    // var showLayersSheet by remember { mutableStateOf(false) }
    val layersSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    if (showBrushPicker) {
        BrushPicker(
            bottomSheetState = brushPickerBottomSheetState,
            selectedBrush = brush,
            onBrushSelected = { brush ->
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

    if (showLayersSheet) {
        ModalBottomSheet(
            onDismissRequest = { onToggleLayersSheet() },
            sheetState = layersSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            LayersPanel(
                drawViewModel = viewModel,
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            )
        }
    }

    LaunchedEffect(showLayersSheet) {
        if (showLayersSheet) {
            layersSheetState.show()
        } else {
            layersSheetState.hide()
        }
    }

    if (!isFloating) {
        BottomAppBar(
            modifier = modifier.fillMaxWidth().height(50.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { showColorPicker = true }) {
                    Icon(
                        Lucide.Palette,
                        contentDescription = stringResource(Res.string.color),
                        tint = color
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (tool.isBrush) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setBrush() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Lucide.Brush,
                            contentDescription = stringResource(Res.string.brush),
                            tint = if (tool.isBrush) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showBrushPicker = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Lucide.ChevronUp,
                            contentDescription = null,
                            tint = if (tool.isBrush) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(onClick = {
                    onBrushSelected(BrushData.cleanEraser)
                }) {
                    Icon(
                        Lucide.Eraser,
                        contentDescription = stringResource(Res.string.eraser),
                        tint = if (tool.isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    showSizeSelector = true
                }) {
                    Icon(
                        Lucide.SlidersHorizontal,
                        contentDescription = stringResource(Res.string.brush_settings),
                    )
                }
                IconButton(onClick = {
                    showShapePicker = true
                }) {
                    Icon(
                        Lucide.Shapes,
                        contentDescription = stringResource(Res.string.shapes),
                        tint = if (tool.isShape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    onToolSelected(Tool.Fill)
                }) {
                    Icon(
                        Lucide.PaintBucket,
                        contentDescription = stringResource(Res.string.fill),
                        tint = if (tool.isFill) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = {
                    onToggleLayersSheet()
                }) {
                    Icon(
                        Lucide.Layers,
                        contentDescription = stringResource(Res.string.layers),
                        tint = if (showLayersSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    showColorPicker = true
                },
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(28.dp)
                        .clip(CircleShape)
                        .border(2.dp, Brush.radialGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                        ), CircleShape)
                        .background(color),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (tool.isBrush) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setBrush() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Lucide.Brush,
                            contentDescription = stringResource(Res.string.brush),
                            tint = if (tool.isBrush) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (isMobile || isTablet) {
                        IconButton(
                            onClick = { showBrushPicker = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Lucide.ChevronUp,
                                contentDescription = null,
                                tint = if (tool.isBrush) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { onBrushSelected(BrushData.cleanEraser) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (tool.isEraser) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .size(36.dp)
                ) {
                    Icon(
                        Lucide.Eraser,
                        contentDescription = stringResource(Res.string.eraser),
                        tint = if (tool.isEraser) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            VerticalDivider()

            // Shape tools
            IconButton(
                onClick = { showShapePicker = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (tool.isShape) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .size(36.dp)
            ) {
                Icon(
                    Lucide.Shapes,
                    contentDescription = stringResource(Res.string.shapes),
                    tint = if (tool.isShape) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = {
                    val selectBrush = if (brush.blendMode != BlendMode.Clear && !brush.isShape) {
                        brush
                    } else {
                        BrushData.solid
                    }
                    onToolSelected(Tool.Curve(selectBrush))
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (tool.isCurve) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .size(36.dp)
            ) {
                Icon(
                    Lucide.Spline,
                    contentDescription = "Curve",
                    tint = if (tool.isCurve) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onToolSelected(Tool.Fill) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (tool.isFill) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .size(36.dp)
                ) {
                    Icon(
                        Lucide.PaintBucket,
                        contentDescription = stringResource(Res.string.fill),
                        tint = if (tool.isFill) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!isMobile) {
                    IconButton(
                        onClick = { onToolSelected(Tool.Eyedropper) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tool.isEyedropper) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .size(36.dp)
                    ) {
                        Icon(
                            Lucide.Pipette,
                            contentDescription = stringResource(Res.string.eyedropper),
                            tint = if (tool.isEyedropper) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            VerticalDivider()

            // View tools group
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isMobile && !isTablet) {
                    IconButton(
                        onClick = { onToolSelected(Tool.Zoom) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tool.isZoom) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .size(36.dp)
                    ) {
                        Icon(
                            Lucide.ZoomIn,
                            contentDescription = stringResource(Res.string.zoom),
                            tint = if (tool.isZoom) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (!isMobile && !isTablet) {
                    IconButton(
                        onClick = { onToolSelected(Tool.Drag) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tool.isDrag) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .size(36.dp)
                    ) {
                        Icon(
                            Lucide.Move,
                            contentDescription = stringResource(Res.string.move_tool),
                            tint = if (tool.isDrag) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isMobile) {
                    IconButton(
                        onClick = { showSizeSelector = true },
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            Lucide.SlidersHorizontal,
                            contentDescription = stringResource(Res.string.brush_settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onToolSelected(Tool.Selection) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (tool.isSelection) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                        .size(36.dp)
                ) {
                    Icon(
                        Lucide.BoxSelect,
                        contentDescription = stringResource(Res.string.selection_tool),
                        tint = if (tool.isSelection) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalDivider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                RoundedCornerShape(1.dp)
            )
    )
}
