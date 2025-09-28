package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.BoxSelect
import com.composables.icons.lucide.Brush
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Eraser
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.PaintBucket
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Pipette
import com.composables.icons.lucide.Shapes
import com.composables.icons.lucide.SlidersHorizontal
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.ZoomIn
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.brush
import doodleverse.composeapp.generated.resources.brush_settings
import doodleverse.composeapp.generated.resources.color
import doodleverse.composeapp.generated.resources.eraser
import doodleverse.composeapp.generated.resources.eyedropper
import doodleverse.composeapp.generated.resources.fill
import doodleverse.composeapp.generated.resources.layers
import doodleverse.composeapp.generated.resources.move_tool
import doodleverse.composeapp.generated.resources.selection_tool
import doodleverse.composeapp.generated.resources.shapes
import doodleverse.composeapp.generated.resources.zoom
import io.github.taalaydev.doodleverse.engine.DrawTool
import io.github.taalaydev.doodleverse.ui.screens.canvas.DrawViewModel
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.brush.EraserBrush
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun DrawControls(
    viewModel: DrawViewModel,
    brushes: List<Brush>,
    brushSize: Float = 10f,
    brush: Brush,
    tool: DrawTool,
    color: Color = Color(0xFF333333),
    isFloating: Boolean = false,
    showLayersSheet: Boolean = false,
    onToggleLayersSheet: () -> Unit = {},
    onBrushSelected: (Brush) -> Unit = {},
    onColorSelected: (Color) -> Unit = {},
    onSizeSelected: (Float) -> Unit = {},
    onToolSelected: (DrawTool) -> Unit = {},
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
            brushes = brushes,
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
            shape = if (tool is DrawTool.Shape) tool.shape else null,
            onSelected = { shape ->
                onToolSelected(DrawTool.Shape(shape, brush))
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
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
                    onToolSelected(DrawTool.Eraser(brush))
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
                    onToolSelected(DrawTool.Fill)
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
                        .border(
                            2.dp,
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            ),
                            CircleShape
                        )
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
                    onClick = {
                        onToolSelected(DrawTool.Eraser(EraserBrush()))
                    },
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
                onClick = {
                    showShapePicker = true
                },
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
                    onToolSelected(DrawTool.Curve(brush))
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
                    onClick = { onToolSelected(DrawTool.Fill) },
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
                        onClick = { onToolSelected(DrawTool.Eyedropper) },
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
//                if (!isMobile && !isTablet) {
//                    IconButton(
//                        onClick = { onToolSelected(DrawTool.Zoom) },
//                        modifier = Modifier
//                            .clip(RoundedCornerShape(6.dp))
//                            .background(if (tool.isZoom) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
//                            .size(36.dp)
//                    ) {
//                        Icon(
//                            Lucide.ZoomIn,
//                            contentDescription = stringResource(Res.string.zoom),
//                            tint = if (tool.isZoom) MaterialTheme.colorScheme.primary
//                            else MaterialTheme.colorScheme.onSurface,
//                            modifier = Modifier.size(20.dp)
//                        )
//                    }
//                }

//                if (!isMobile && !isTablet) {
//                    IconButton(
//                        onClick = { onToolSelected(DrawTool.Drag) },
//                        modifier = Modifier
//                            .clip(RoundedCornerShape(6.dp))
//                            .background(if (tool.isDrag) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
//                            .size(36.dp)
//                    ) {
//                        Icon(
//                            Lucide.Move,
//                            contentDescription = stringResource(Res.string.move_tool),
//                            tint = if (tool.isDrag) MaterialTheme.colorScheme.primary
//                            else MaterialTheme.colorScheme.onSurface,
//                            modifier = Modifier.size(20.dp)
//                        )
//                    }
//                }

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

//                IconButton(
//                    onClick = { onToolSelected(DrawTool.Selection) },
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(6.dp))
//                        .background(if (tool.isSelection) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
//                        .size(36.dp)
//                ) {
//                    Icon(
//                        Lucide.BoxSelect,
//                        contentDescription = stringResource(Res.string.selection_tool),
//                        tint = if (tool.isSelection) MaterialTheme.colorScheme.primary
//                        else MaterialTheme.colorScheme.onSurface,
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
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