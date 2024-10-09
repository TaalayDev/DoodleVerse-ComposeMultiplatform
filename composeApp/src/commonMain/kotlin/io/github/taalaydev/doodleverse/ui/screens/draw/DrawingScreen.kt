package io.github.taalaydev.doodleverse.ui.screens.draw

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.pencil
import io.github.taalaydev.doodleverse.core.DragState
import io.github.taalaydev.doodleverse.core.handleDragAndZoomGestures
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.Shape
import io.github.taalaydev.doodleverse.ui.components.BrushPicker
import io.github.taalaydev.doodleverse.ui.components.ColorPalettePanel
import io.github.taalaydev.doodleverse.ui.components.ColorPicker
import io.github.taalaydev.doodleverse.ui.components.DrawCanvas
import io.github.taalaydev.doodleverse.ui.components.LayersPanel
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.min

data class DpSize(val width: Dp, val height: Dp)

@Composable
fun DrawingScreen(
    navController: NavHostController = rememberNavController(),
) {
    val projectModel = remember { ProjectModel.currentProject }
    DrawScreenBody(
        projectModel = projectModel ?: ProjectModel(
            id = 1L,
            name = "Untitled",
            layers = listOf(
                LayerModel(
                    id = 1L,
                    name = "Layer 1",
                )
            ),
            createdAt = Clock.System.now().toEpochMilliseconds(),
            lastModified = Clock.System.now().toEpochMilliseconds(),
        ),
        navController = navController,
        modifier = Modifier.fillMaxSize(),
    )
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

    val brushSize by viewModel.brushSize.collectAsStateWithLifecycle()
    val currentColor by viewModel.currentColor.collectAsStateWithLifecycle()
    val currentBrush by viewModel.currentBrush.collectAsStateWithLifecycle()

    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var dragState by remember { mutableStateOf(DragState()) }

    val density = LocalDensity.current
    var canvasSize by remember { mutableStateOf(DpSize(500.dp, 500.dp)) }

    val pxToDp: (Int) -> Dp = { px ->
        with(density) { px.toDp() }
    }

    val size = calculateWindowSizeClass()
    val isMobile = when (size.widthSizeClass) {
        WindowWidthSizeClass.Compact -> true
        else -> false
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
                        Icon(Lucide.ArrowLeft, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    }
                },
                title = {  },
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

                        }
                    ) {
                        Icon(Lucide.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (!isMobile) {
                    DrawControls(
                        brushSize = brushSize,
                        brush = currentBrush,
                        color = currentColor,
                        isHorizontal = false,
                        onBrushSelected = {
                            viewModel.setBrush(it)
                        },
                        onColorSelected = {
                            viewModel.setColor(it)
                        },
                        onSizeSelected = {
                            viewModel.setBrushSize(it)
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { layoutCoordinates ->
                            val size = layoutCoordinates.size
                            canvasSize = DpSize(
                                width = pxToDp(size.width),
                                height = pxToDp(size.height)
                            )
                        }
                        .pointerInput(Unit) {
                            focusManager.clearFocus()
                            keyboardController?.hide()

                            handleDragAndZoomGestures(dragState) {
                                dragState = it
                            }
                        }
                        .graphicsLayer {
                            scaleX = dragState.zoom
                            scaleY = dragState.zoom
                            translationX = dragState.draggedTo.x
                            translationY = dragState.draggedTo.y
                        }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DrawCanvas(
                        currentBrush = currentBrush,
                        currentColor = currentColor,
                        brushSize = brushSize,
                        controller = viewModel.drawingController,
                        modifier = Modifier
                            .aspectRatio(projectModel.aspectRatioValue),
                    )
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
            if (isMobile) {
                DrawControls(
                    brushSize = brushSize,
                    brush = currentBrush,
                    color = currentColor,
                    onBrushSelected = {
                        viewModel.setBrush(it)
                    },
                    onColorSelected = {
                        viewModel.setColor(it)
                    },
                    onSizeSelected = {
                        viewModel.setBrushSize(it)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawControls(
    brushSize: Float = 10f,
    brush: BrushData,
    color: Color = Color(0xFF333333),
    isHorizontal: Boolean = true,
    onBrushSelected: (BrushData) -> Unit = {},
    onColorSelected: (Color) -> Unit = {},
    onSizeSelected: (Float) -> Unit = {},
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

    if (isHorizontal) {
        BottomAppBar(
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { showColorPicker = true }) {
                    Icon(Lucide.Palette, contentDescription = "Color")
                }
                IconButton(
                    onClick = {
                        showBrushPicker = true
                    }
                ) {
                    Icon(Lucide.Brush, contentDescription = "Brush")
                }
                IconButton(onClick = {
                    onBrushSelected(BrushData.eraser)
                }) {
                    Icon(Lucide.Eraser, contentDescription = "Eraser")
                }
                IconButton(onClick = {
                    showSizeSelector = true
                }) {
                    Icon(Lucide.SlidersHorizontal, contentDescription = "Brush Settings")
                }
                IconButton(onClick = {
                    showShapePicker = true
                }) {
                    Icon(Lucide.Shapes, contentDescription = "Shapes")
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxHeight().width(60.dp).background(MaterialTheme.colorScheme.surface),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconButton(
                onClick = {
                    showBrushPicker = true
                }
            ) {
                Icon(Lucide.Brush, contentDescription = "Brush")
            }
            IconButton(onClick = {
                onBrushSelected(BrushData.eraser)
            }) {
                Icon(Lucide.Eraser, contentDescription = "Eraser")
            }
            IconButton(onClick = {
                showSizeSelector = true
            }) {
                Icon(Lucide.SlidersHorizontal, contentDescription = "Brush Settings")
            }
            IconButton(onClick = {
                showShapePicker = true
            }) {
                Icon(Lucide.Shapes, contentDescription = "Shapes")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapePickerSheet(
    bottomSheetState: SheetState,
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shapes.size) { index ->
                    val shape = shapes[index]
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(4.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable {
                                onSelected(shape)
                            }
                    ) {
                        Text(
                            text = shape.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
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
                Slider(
                    value = value,
                    onValueChange = { values ->
                        value = values
                    },
                    valueRange = 10f..80f,
                    modifier = Modifier.fillMaxWidth(),
                )
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