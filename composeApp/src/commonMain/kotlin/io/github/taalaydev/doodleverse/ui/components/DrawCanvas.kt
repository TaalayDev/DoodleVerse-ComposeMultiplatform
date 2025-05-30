package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taalaydev.doodleverse.core.DrawProvider
import io.github.taalaydev.doodleverse.core.DrawRenderer
import io.github.taalaydev.doodleverse.core.DrawingController
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.copy
import io.github.taalaydev.doodleverse.core.handleDrawing
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import io.github.taalaydev.doodleverse.data.models.PointModel
import io.github.taalaydev.doodleverse.getColorFromBitmap
import io.github.taalaydev.doodleverse.getPlatformType
import org.jetbrains.compose.resources.imageResource
import kotlin.math.max
import kotlin.math.min

// Simplified drawing states
private enum class CanvasDrawState {
    Idle, Drawing, Ended
}

@Composable
fun DrawCanvas(
    provider: DrawProvider,
    currentBrush: BrushData,
    currentColor: Color,
    brushSize: Float,
    tool: Tool,
    gestureEnabled: Boolean = true,
    controller: DrawingController,
    onColorPicked: (Color) -> Unit = {},
    referenceImage: ImageBitmap? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Collect state from the refactored controller
    val drawingState by controller.state.collectAsStateWithLifecycle()
    val layers = drawingState.currentFrame.layers
    val currentLayer = drawingState.currentLayer
    val currentLayerIndex = drawingState.currentLayerIndex

    // Canvas drawing state with better reactivity
    var canvasDrawState by remember { mutableStateOf(CanvasDrawState.Idle) }
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var eyedropperColor by remember { mutableStateOf<Color?>(null) }

    // NEW: Preview bitmap system
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var currentDrawingPath by remember { mutableStateOf<DrawingPath?>(null) }

    // Brush image for custom brushes
    val brushImage = if (currentBrush.brush != null) {
        imageResource(currentBrush.brush)
    } else {
        null
    }

    // Canvas size tracking
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Update canvas size in controller when it changes
    LaunchedEffect(canvasSize) {
        if (canvasSize != Size.Zero) {
            controller.canvasSize = canvasSize
        }
    }

    // FIXED: Update preview bitmap when drawing path changes with eraser support
    LaunchedEffect(currentDrawingPath, canvasSize) {
        if (canvasDrawState == CanvasDrawState.Drawing && currentDrawingPath != null && canvasSize != Size.Zero) {
            previewBitmap = DrawRenderer.createPreviewBitmap(currentDrawingPath!!, canvasSize)
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(currentBrush, gestureEnabled, tool, currentColor, brushSize) {
                    if (!gestureEnabled) return@pointerInput

                    when {
                        tool.isFill -> {
                            detectTapGestures { offset ->
                                currentPosition = offset
                                canvasDrawState = CanvasDrawState.Drawing
                            }
                        }

                        tool.isSelection -> {
                            handleDrawing(
                                onStart = { offset, _ ->
                                    provider.startSelection(offset)
                                },
                                onDrag = { _, new, _ ->
                                    provider.updateSelection(new)
                                },
                                onEnd = {
                                    provider.endSelection()
                                }
                            )
                        }

                        else -> {
                            handleDrawing(
                                onStart = { offset, pressure ->
                                    currentPosition = offset
                                    canvasDrawState = CanvasDrawState.Drawing

                                    if (!tool.isEyedropper) {
                                        // Create initial drawing path
                                        val path = Path().apply {
                                            moveTo(offset.x, offset.y)
                                        }

                                        currentDrawingPath = DrawingPath(
                                            brush = currentBrush,
                                            color = currentColor,
                                            size = brushSize,
                                            path = path,
                                            startPoint = offset,
                                            endPoint = offset,
                                            points = mutableListOf(PointModel(offset.x, offset.y))
                                        )
                                    }
                                },
                                onDrag = { _, new, pressure ->
                                    currentPosition = new

                                    if (!tool.isEyedropper && canvasDrawState == CanvasDrawState.Drawing) {
                                        // Update drawing path
                                        currentDrawingPath?.let { currentPath ->
                                            val newPoints = currentPath.points.toMutableList().apply {
                                                add(PointModel(new.x, new.y))
                                            }

                                            val newPath = if (currentBrush.isShape) {
                                                // For shapes, recreate path with start and end points
                                                Path().apply {
                                                    moveTo(currentPath.startPoint.x, currentPath.startPoint.y)
                                                    lineTo(new.x, new.y)
                                                }
                                            } else {
                                                // For brushes, create new path with all points
                                                Path().apply {
                                                    if (newPoints.isNotEmpty()) {
                                                        moveTo(newPoints.first().x, newPoints.first().y)
                                                        newPoints.drop(1).forEach { point ->
                                                            lineTo(point.x, point.y)
                                                        }
                                                    }
                                                }
                                            }

                                            // Create new DrawingPath to trigger recomposition
                                            currentDrawingPath = currentPath.copy(
                                                path = newPath,
                                                endPoint = new,
                                                points = newPoints
                                            )
                                        }
                                    }
                                },
                                onEnd = {
                                    if (tool.isEyedropper) {
                                        eyedropperColor?.let { color ->
                                            onColorPicked(color)
                                        }
                                        eyedropperColor = null
                                    } else if (canvasDrawState == CanvasDrawState.Drawing && currentDrawingPath != null) {
                                        // Finish drawing and commit to controller
                                        finishDrawingPath(controller, currentDrawingPath!!, canvasSize)
                                        currentDrawingPath = null
                                        previewBitmap = null
                                    }

                                    canvasDrawState = CanvasDrawState.Ended
                                }
                            )
                        }
                    }
                }
        ) {
            // Update canvas size
            if (canvasSize != size) {
                canvasSize = size
            }

            // Handle drawing operations
            when {
                tool.isFill && canvasDrawState == CanvasDrawState.Drawing -> {
                    handleFloodFill(
                        position = currentPosition,
                        color = currentColor,
                        provider = provider
                    )
                    canvasDrawState = CanvasDrawState.Idle
                }

                tool.isEyedropper && canvasDrawState != CanvasDrawState.Idle -> {
                    eyedropperColor = handleEyedropper(
                        position = currentPosition,
                        controller = controller,
                        drawContext = this
                    )

                    if (canvasDrawState == CanvasDrawState.Ended) {
                        canvasDrawState = CanvasDrawState.Idle
                    }
                }
            }

            renderLayers(
                layers = layers,
                currentLayerIndex = currentLayerIndex,
                controller = controller,
                previewBitmap = previewBitmap,
                drawContext = this
            )

            // Render selection overlay
            renderSelectionOverlay(
                provider = provider,
                drawContext = this
            )

            // Render reference image if present
            referenceImage?.let { refImage ->
                renderReferenceImage(
                    image = refImage,
                    canvasSize = size,
                    drawContext = this
                )
            }

            // Render eyedropper feedback
            eyedropperColor?.let { color ->
                renderEyedropperFeedback(
                    position = currentPosition,
                    color = color,
                    drawContext = this
                )
            }
        }

        // Selection overlay UI (handles, etc.)
        if (provider.selectionState.isActive) {
            SelectionOverlay(
                state = provider.selectionState,
                onTransformStart = { transform, point ->
                    provider.startTransform(transform, point)
                },
                onTransformDelta = { offset ->
                    provider.updateSelectionTransform(offset)
                },
                onTransformEnd = {
                    // Selection transform completed
                },
                onTapOutside = {
                    provider.applySelection()
                },
                modifier = Modifier.fillMaxSize(),
                isMobile = getPlatformType().isAndroid || getPlatformType().isIos
            )
        }
    }
}

// FIXED: Improved drawing path finishing using ImageBitmap approach with eraser support
private fun finishDrawingPath(
    controller: DrawingController,
    drawingPath: DrawingPath,
    canvasSize: Size
) {
    // Get the existing layer bitmap
    val existingBitmap = controller.getLayerBitmap(
        controller.state.value.currentLayer.id
    ) ?: ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())

    val finalBitmap = if (drawingPath.brush.blendMode == BlendMode.Clear) {
        // For erasers, apply directly to the existing bitmap
        DrawRenderer.applyEraserPath(drawingPath, existingBitmap)
    } else {
        // For regular brushes, render the path and blend with existing content
        val pathBitmap = DrawRenderer.renderPathToBitmap(drawingPath, canvasSize)
        DrawRenderer.blendPathBitmap(
            pathBitmap = pathBitmap,
            targetBitmap = existingBitmap,
            blendMode = drawingPath.brush.blendMode
        )
    }

    // Add to controller
    controller.addDrawingPath(drawingPath, finalBitmap)
}

private fun handleFloodFill(
    position: Offset,
    color: Color,
    provider: DrawProvider
) {
    val x = position.x.toInt()
    val y = position.y.toInt()
    provider.floodFill(x, y)
}

private fun handleEyedropper(
    position: Offset,
    controller: DrawingController,
    drawContext: androidx.compose.ui.graphics.drawscope.DrawScope
): Color? {
    val x = position.x.toInt()
    val y = position.y.toInt()

    // Get combined bitmap of all visible layers
    val combinedBitmap = controller.getCombinedBitmap() ?: return null
    val colorArgb = getColorFromBitmap(combinedBitmap, x, y) ?: return null

    return Color(colorArgb).takeIf { it.alpha > 0 } ?: Color.White
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderLayers(
    layers: List<io.github.taalaydev.doodleverse.data.models.LayerModel>,
    currentLayerIndex: Int,
    controller: DrawingController,
    previewBitmap: ImageBitmap? = null,
    drawContext: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    layers.forEachIndexed { index, layer ->
        if (!layer.isVisible || layer.opacity <= 0.0) return@forEachIndexed

        val layerBitmap = controller.getLayerBitmap(layer.id)
        layerBitmap?.let { bitmap ->
            drawImage(
                image = bitmap,
                alpha = layer.opacity.toFloat()
            )
        }

        // FIXED: Only show preview on current layer
        if (index == currentLayerIndex) {
            previewBitmap?.let { preview ->
                drawImage(
                    image = preview,
                    alpha = 1.0f
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderSelectionOverlay(
    provider: DrawProvider,
    drawContext: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    if (provider.selectionState.isActive) {
        val bounds = provider.selectionState.bounds
        val transformedBounds = provider.selectionState.getTransformedBounds()

        // Draw selection bounds
        drawContext.drawContext.canvas.withSave {
            val offset = provider.selectionState.offset
            val center = transformedBounds.center

            translate(offset.x + center.x, offset.y + center.y) {
                rotate(provider.selectionState.rotation) {
                    scale(provider.selectionState.scale, provider.selectionState.scale) {}
                    translate(-center.x, -center.y) {}
                }
            }

            // Draw selection rectangle
            drawRect(
                topLeft = transformedBounds.topLeft,
                size = transformedBounds.size,
                color = Color.Blue.copy(alpha = 0.3f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw selected content if available
            provider.selectionState.transformedBitmap?.let { bitmap ->
                drawImage(
                    image = bitmap,
                    topLeft = transformedBounds.topLeft,
                    alpha = 0.8f
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderReferenceImage(
    image: ImageBitmap,
    canvasSize: Size,
    drawContext: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    drawImage(
        image = image,
        srcOffset = IntOffset(0, 0),
        srcSize = IntSize(image.width, image.height),
        dstSize = IntSize(canvasSize.width.toInt(), canvasSize.height.toInt()),
        colorFilter = ColorFilter.tint(Color(0x40FF0000)), // Semi-transparent red overlay
        alpha = 0.5f
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderEyedropperFeedback(
    position: Offset,
    color: Color,
    drawContext: androidx.compose.ui.graphics.drawscope.DrawScope
) {
    val crosshairSize = 15f
    val previewSize = 40f
    val previewOffset = Offset(position.x + 20, position.y - 50)

    // Draw color preview circle
    drawCircle(
        color = color,
        radius = previewSize / 2,
        center = previewOffset
    )

    // Draw preview circle border
    drawCircle(
        color = Color.Black,
        radius = previewSize / 2,
        center = previewOffset,
        style = Stroke(width = 2.dp.toPx())
    )

    // Draw crosshairs
    drawLine(
        color = Color.Black,
        start = Offset(position.x, position.y - crosshairSize),
        end = Offset(position.x, position.y + crosshairSize),
        strokeWidth = 1.dp.toPx()
    )

    drawLine(
        color = Color.Black,
        start = Offset(position.x - crosshairSize, position.y),
        end = Offset(position.x + crosshairSize, position.y),
        strokeWidth = 1.dp.toPx()
    )
}

// Extension function for cleaner bounds access
private fun io.github.taalaydev.doodleverse.core.SelectionState.getTransformedBounds(): androidx.compose.ui.geometry.Rect {
    val matrix = androidx.compose.ui.graphics.Matrix()
    val center = bounds.center

    matrix.translate(offset.x + center.x, offset.y + center.y)
    matrix.rotateZ(rotation)
    matrix.scale(scale, scale)
    matrix.translate(-center.x, -center.y)

    val path = Path().apply {
        addRect(bounds)
        transform(matrix)
    }

    return path.getBounds()
}