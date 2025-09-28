package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taalaydev.doodleverse.core.DrawProvider
import io.github.taalaydev.doodleverse.core.rendering.DrawRenderer
import io.github.taalaydev.doodleverse.core.DrawingController
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.engine.copy
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import io.github.taalaydev.doodleverse.data.models.PointModel
import io.github.taalaydev.doodleverse.getColorFromBitmap
import io.github.taalaydev.doodleverse.getPlatformType
import org.jetbrains.compose.resources.imageResource
import io.github.taalaydev.doodleverse.engine.CanvasDrawState
import io.github.taalaydev.doodleverse.engine.CanvasDrawingState
import io.github.taalaydev.doodleverse.engine.DrawingBitmapState
import io.github.taalaydev.doodleverse.engine.components.SelectionOverlay
import io.github.taalaydev.doodleverse.engine.controller.VelocityTracker
import io.github.taalaydev.doodleverse.engine.controller.SelectionState
import io.github.taalaydev.doodleverse.engine.floodFillFast
import io.github.taalaydev.doodleverse.engine.util.distanceTo
import io.github.taalaydev.doodleverse.engine.util.handleDrawing
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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
    referenceBlendMode: BlendMode = BlendMode.SrcOver,
    modifier: Modifier = Modifier,
) {
    val drawingState by controller.state.collectAsStateWithLifecycle()
    val layers = drawingState.currentFrame.layers
    val currentLayer = drawingState.currentLayer
    val currentLayerIndex = drawingState.currentLayerIndex

    var canvasDrawState by remember { mutableStateOf(CanvasDrawState.Idle) }
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var eyedropperColor by remember { mutableStateOf<Color?>(null) }
    var currentPressure by remember { mutableFloatStateOf(1f) }

    var drawingBitmapState by remember { mutableStateOf<DrawingBitmapState?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val velocityTracker by remember { mutableStateOf(VelocityTracker()) }

    var curvePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var livePreviewPosition by remember { mutableStateOf<Offset?>(null) }

    val brushImage = if (currentBrush.brush != null) {
        imageResource(currentBrush.brush)
    } else {
        null
    }

    LaunchedEffect(canvasSize) {
        if (canvasSize != Size.Zero) {
            controller.canvasSize = canvasSize
        }
    }

    LaunchedEffect(tool) {
        if (tool !is Tool.Curve) {
            curvePoints = emptyList()
            livePreviewPosition = null
        }
    }

    LaunchedEffect(canvasSize) {
        if (canvasSize != Size.Zero && drawingBitmapState?.bitmap?.let {
                it.width != canvasSize.width.toInt() || it.height != canvasSize.height.toInt()
            } != false) {
            val bitmap = ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
            val canvas = Canvas(bitmap)
            drawingBitmapState = DrawingBitmapState(bitmap, canvas)
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(tool, curvePoints.size) {
                    if (tool !is Tool.Curve || curvePoints.size != 2) return@pointerInput
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            livePreviewPosition = event.changes.first().position
                        }
                    }
                }
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
                                onEnd = { _, _ ->
                                    provider.endSelection()
                                }
                            )
                        }

                        tool.isCurve -> {
                            detectTapGestures { offset ->
                                if (curvePoints.size < 2) {
                                    curvePoints = curvePoints + offset
                                } else {
                                    // Finalize the curve with the third tap
                                    val p0 = curvePoints[0]
                                    val p2 = curvePoints[1]
                                    val p1 = offset // Control point

                                    val finalPath = Path().apply {
                                        moveTo(p0.x, p0.y)
                                        quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
                                    }

                                    val drawingPath = DrawingPath(
                                        brush = currentBrush,
                                        color = currentColor,
                                        size = brushSize,
                                        path = finalPath,
                                        startPoint = p0,
                                        endPoint = p2
                                    )

                                    val pathBitmap = ImageBitmap(size.width, size.height)
                                    val pathCanvas = Canvas(pathBitmap)
                                    drawPathToCanvas(pathCanvas, drawingPath, Size(size.width.toFloat(), size.height.toFloat()), brushImage)

                                    val layerBitmap = controller.getLayerBitmap(currentLayer.id) ?: ImageBitmap(size.width, size.height)
                                    val finalBitmap = combineDrawingWithLayer(pathBitmap, layerBitmap, currentBrush)
                                    controller.updateLayerBitmap(finalBitmap)

                                    // Reset for the next curve
                                    curvePoints = emptyList()
                                    livePreviewPosition = null
                                }
                            }
                        }

                        else -> {
                            handleDrawing(
                                onStart = { offset, pressure ->
                                    currentPosition = offset
                                    currentPressure = pressure
                                    canvasDrawState = CanvasDrawState.Drawing

                                    velocityTracker.reset()
                                    velocityTracker.updateVelocity(offset, Clock.System.now().toEpochMilliseconds())

                                    startDrawing(
                                        offset = currentPosition,
                                        pressure = currentPressure,
                                        brush = currentBrush,
                                        color = currentColor,
                                        size = brushSize,
                                        canvasSize = canvasSize,
                                        cache = if (!currentBrush.isShape) controller.getLayerBitmap(currentLayer.id) else null,
                                        onStateUpdate = { newState ->
                                            drawingBitmapState = newState
                                        }
                                    )
                                },
                                onDrag = { _, new, pressure ->
                                    val currentTime = Clock.System.now().toEpochMilliseconds()
                                    velocityTracker.updateVelocity(new, currentTime)
                                    val dynamicDistance = velocityTracker.calculateDynamicDistance(currentBrush.isShape)

                                    if (currentPosition.distanceTo(new) < dynamicDistance) return@handleDrawing

                                    currentPosition = new
                                    currentPressure = pressure

                                    if (canvasDrawState == CanvasDrawState.Drawing) {
                                        continueDrawing(
                                            offset = currentPosition,
                                            pressure = currentPressure,
                                            currentState = drawingBitmapState,
                                            canvasSize = canvasSize,
                                            brushImage = brushImage,
                                            onStateUpdate = { newState ->
                                                drawingBitmapState = newState
                                            }
                                        )
                                    }
                                },
                                onEnd = { _, _ ->
                                    if (tool.isEyedropper) {
                                        eyedropperColor?.let { color ->
                                            onColorPicked(color)
                                        }
                                        eyedropperColor = null
                                    }

                                    finishDrawing(
                                        drawingState = drawingBitmapState,
                                        controller = controller,
                                        canvasSize = canvasSize,
                                        brushImage = brushImage,
                                        onComplete = {
                                            drawingBitmapState?.let { state ->
                                                // clearDrawingBitmap(state.canvas, canvasSize)
                                                state.reset()
                                            }
                                        }
                                    )

                                    velocityTracker.reset()
                                    canvasDrawState = CanvasDrawState.Ended
                                }
                            )
                        }
                    }
                }
        ) {
            if (canvasSize != size) {
                canvasSize = size
            }

            when {
                tool.isFill && canvasDrawState == CanvasDrawState.Drawing -> {
                    handleFloodFill(
                        position = currentPosition,
                        color = currentColor,
                        layerBitmap = controller.getLayerBitmap(currentLayer.id) ?: ImageBitmap(size.width.toInt(), size.height.toInt()),
                        controller = controller
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
                canvasDrawState = canvasDrawState,
                drawingBitmapState = drawingBitmapState,
            )

            if (tool is Tool.Curve) {
                // Draw circles for the anchor points
                curvePoints.forEach { point ->
                    drawCircle(Color.Black.copy(alpha = 0.5f), radius = 10f, center = point)
                    drawCircle(Color.White, radius = 8f, center = point)
                }

                // Draw the live preview curve
                if (curvePoints.size == 2 && livePreviewPosition != null) {
                    val p0 = curvePoints[0]
                    val p2 = curvePoints[1]
                    val p1 = livePreviewPosition!!

                    val previewPath = Path().apply {
                        moveTo(p0.x, p0.y)
                        quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
                    }

                    drawPath(
                        path = previewPath,
                        color = currentColor,
                        style = Stroke(
                            width = brushSize,
                            cap = currentBrush.strokeCap,
                            join = currentBrush.strokeJoin,
                            pathEffect = currentBrush.pathEffect?.invoke(brushSize)
                        ),
                        alpha = 0.7f
                    )
                }
            }

            renderSelectionOverlay(
                provider = provider,
                drawContext = this
            )

            referenceImage?.let { refImage ->
                renderReferenceImage(
                    image = refImage,
                    canvasSize = size,
                    blendMode = referenceBlendMode
                )
            }

            eyedropperColor?.let { color ->
                renderEyedropperFeedback(
                    position = currentPosition,
                    color = color,
                )
            }
        }

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
                    provider.applySelection()
                },
                onTapOutside = {
                    controller.clearSelection()
                },
                modifier = Modifier.fillMaxSize(),
                isMobile = getPlatformType().isAndroid || getPlatformType().isIos
            )
        }
    }
}

private fun startDrawing(
    offset: Offset,
    pressure: Float,
    brush: BrushData,
    color: Color,
    size: Float,
    canvasSize: Size,
    cache: ImageBitmap? = null,
    onStateUpdate: (DrawingBitmapState) -> Unit
) {
    val bitmap = cache?.copy() ?: ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
    val canvas = Canvas(bitmap)

    val path = Path().apply { moveTo(offset.x, offset.y) }
    val drawingPath = DrawingPath(
        brush = brush,
        color = color,
        size = size,
        path = path,
        startPoint = offset,
        endPoint = offset,
        points = mutableListOf(PointModel(offset.x, offset.y))
    )
    val drawingState = CanvasDrawingState(
        points = mutableListOf(offset),
        prevPosition = offset,
        currentPosition = offset
    )
    val initialState = DrawingBitmapState(
        bitmap = bitmap,
        canvas = canvas,
        currentPath = drawingPath,
        drawingState = drawingState,
        lastPoint = offset
    )

    initialState.drawingState.addPoint(offset)
    initialState.drawingState.createIncrementalPath { path ->
        val updatedPath = drawingPath.copy(
            path = path,
            endPoint = offset
        )

        // clearDrawingBitmap(canvas, canvasSize)
        // drawPathToCanvas(canvas, updatedPath, canvasSize)

        onStateUpdate(initialState.copy(currentPath = updatedPath))
    }
}

private fun continueDrawing(
    offset: Offset,
    pressure: Float,
    currentState: DrawingBitmapState?,
    canvasSize: Size,
    brushImage: ImageBitmap? = null,
    onStateUpdate: (DrawingBitmapState) -> Unit
) {
    val state = currentState ?: return
    val currentPath = state.currentPath ?: return
    state.drawingState.currentPosition = offset

    if (!state.drawingState.addPoint(offset)) {
        return
    }

    val newPoints = currentPath.points.toMutableList().apply {
        add(PointModel(offset.x, offset.y))
    }

    state.drawingState.createIncrementalPath { path ->
        val updatedPath = currentPath.copy(
            path = path,
            endPoint = offset,
            points = newPoints
        )

        if (updatedPath.brush.isShape) {
            clearDrawingBitmap(state.canvas, canvasSize)
        }
        drawPathToCanvas(state.canvas, updatedPath, canvasSize, brushImage)

        val newState = state.copy(
            currentPath = updatedPath,
            lastPoint = offset
        )

        onStateUpdate(newState)
    }
}

private fun finishDrawing(
    drawingState: DrawingBitmapState?,
    controller: DrawingController,
    canvasSize: Size,
    brushImage: ImageBitmap? = null,
    onComplete: () -> Unit
) {
    val state = drawingState ?: return
    val currentPath = state.currentPath ?: return

    state.drawingState.createIncrementalPath { path ->
        val finalPath = currentPath.copy(path = path)

        drawPathToCanvas(state.canvas, finalPath, canvasSize, brushImage)

        val existingBitmap = controller.getLayerBitmap(
            controller.state.value.currentLayer.id
        ) ?: ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())

        val finalBitmap = if (finalPath.brush.isShape) {
            combineDrawingWithLayer(
                drawingBitmap = state.bitmap,
                layerBitmap = existingBitmap,
                brush = finalPath.brush
            )
        } else {
            state.bitmap
        }

        controller.updateLayerBitmap(finalBitmap)

        onComplete()
    }
}

private fun drawPathToCanvas(
    canvas: Canvas,
    drawingPath: DrawingPath,
    size: Size,
    brushImage: ImageBitmap? = null,
) {
    DrawRenderer.renderPathCanvas(
        canvas,
        drawingPath,
        size,
        useSmoothing = true,
        brushImage = brushImage
    )
}

private fun clearDrawingBitmap(canvas: Canvas, canvasSize: Size) {
    canvas.drawRect(
        left = 0f,
        top = 0f,
        right = canvasSize.width,
        bottom = canvasSize.height,
        paint = Paint().apply {
            color = Color.Transparent
            blendMode = BlendMode.Clear
        }
    )
}

private fun combineDrawingWithLayer(
    drawingBitmap: ImageBitmap,
    layerBitmap: ImageBitmap,
    brush: BrushData
): ImageBitmap {
    val resultBitmap = layerBitmap.copy()
    val canvas = Canvas(resultBitmap)

    val paint = Paint()

    canvas.drawImage(drawingBitmap, Offset.Zero, paint)
    return resultBitmap
}

private fun handleFloodFill(
    position: Offset,
    color: Color,
    layerBitmap: ImageBitmap,
    controller: DrawingController
) {
    val x = position.x.toInt()
    val y = position.y.toInt()
    // controller.floodFill(x, y, color)
    floodFillFast(layerBitmap, x, y, color.toArgb())
    // controller.upda(layerBitmap)
}

private fun handleEyedropper(
    position: Offset,
    controller: DrawingController,
    drawContext: DrawScope
): Color? {
    val x = position.x.toInt()
    val y = position.y.toInt()

    val combinedBitmap = controller.getCombinedBitmap() ?: return null
    val colorArgb = getColorFromBitmap(combinedBitmap, x, y) ?: return null

    return Color(colorArgb).takeIf { it.alpha > 0 } ?: Color.White
}

private fun DrawScope.renderLayers(
    layers: List<io.github.taalaydev.doodleverse.data.models.LayerModel>,
    currentLayerIndex: Int,
    controller: DrawingController,
    canvasDrawState: CanvasDrawState,
    drawingBitmapState: DrawingBitmapState? = null,
) {
    layers.forEachIndexed { index, layer ->
        if (!layer.isVisible || layer.opacity <= 0.0) return@forEachIndexed

        if (index == currentLayerIndex && drawingBitmapState?.bitmap != null && canvasDrawState == CanvasDrawState.Drawing) {
            if (drawingBitmapState.currentPath?.brush?.isShape == true) {
                drawLayerBitmap(layer = layer, controller = controller)
            }

            drawImage(image = drawingBitmapState.bitmap, alpha = 1.0f)
        } else {
            drawLayerBitmap(layer = layer, controller = controller)
        }
    }
}

private fun DrawScope.drawLayerBitmap(
    layer: io.github.taalaydev.doodleverse.data.models.LayerModel,
    controller: DrawingController,
) {
    controller.getLayerBitmap(layer.id)?.let { bitmap ->
        drawImage(
            image = bitmap,
            alpha = layer.opacity.toFloat()
        )
    }
}

private fun DrawScope.renderSelectionOverlay(
    provider: DrawProvider,
    drawContext: DrawScope
) {
    if (provider.selectionState.isActive) {
        val bounds = provider.selectionState.bounds
        val transformedBounds = provider.selectionState.getTransformedBounds()

        drawContext.drawContext.canvas.withSave {
            val offset = provider.selectionState.offset
            val center = transformedBounds.center

            translate(offset.x + center.x, offset.y + center.y) {
                rotate(provider.selectionState.rotation) {
                    scale(provider.selectionState.scale, provider.selectionState.scale) {}
                    translate(-center.x, -center.y) {}
                }
            }

            drawRect(
                topLeft = transformedBounds.topLeft,
                size = transformedBounds.size,
                color = Color.Blue.copy(alpha = 0.3f),
                style = Stroke(width = 2.dp.toPx())
            )

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

private fun DrawScope.renderReferenceImage(
    image: ImageBitmap,
    canvasSize: Size,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    drawImage(
        image = image,
        srcOffset = IntOffset(0, 0),
        srcSize = IntSize(image.width, image.height),
        dstSize = IntSize(canvasSize.width.toInt(), canvasSize.height.toInt()),
        colorFilter = ColorFilter.tint(Color(0x40FF0000)),
        alpha = 0.5f,
        blendMode = blendMode,
    )
}

private fun DrawScope.renderEyedropperFeedback(
    position: Offset,
    color: Color,
) {
    val crosshairSize = 15f
    val previewSize = 40f
    val previewOffset = Offset(position.x + 20, position.y - 50)

    drawCircle(
        color = color,
        radius = previewSize / 2,
        center = previewOffset
    )

    drawCircle(
        color = Color.Black,
        radius = previewSize / 2,
        center = previewOffset,
        style = Stroke(width = 2.dp.toPx())
    )

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

private fun SelectionState.getTransformedBounds(): androidx.compose.ui.geometry.Rect {
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