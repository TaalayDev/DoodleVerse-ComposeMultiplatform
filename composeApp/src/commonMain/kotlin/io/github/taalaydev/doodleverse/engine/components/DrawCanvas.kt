package io.github.taalaydev.doodleverse.engine.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.taalaydev.doodleverse.core.toIntOffset
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.engine.CanvasDrawState
import io.github.taalaydev.doodleverse.engine.DrawTool
import io.github.taalaydev.doodleverse.engine.DrawingBitmapState
import io.github.taalaydev.doodleverse.engine.Viewport
import io.github.taalaydev.doodleverse.engine.brush.PenBrush
import io.github.taalaydev.doodleverse.engine.controller.DrawEngineController
import io.github.taalaydev.doodleverse.engine.controller.VelocityTracker
import io.github.taalaydev.doodleverse.engine.copy
import io.github.taalaydev.doodleverse.engine.floodFillFast
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.CurveParams
import io.github.taalaydev.doodleverse.engine.tool.CurveTool
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.util.currentTimeMillis
import io.github.taalaydev.doodleverse.engine.util.distanceTo
import io.github.taalaydev.doodleverse.engine.util.handleDrawing
import io.github.taalaydev.doodleverse.engine.viewToImage
import io.github.taalaydev.doodleverse.engine.tool.shape.ShapeTool
import io.github.taalaydev.doodleverse.engine.tool.shape.ShapeType
import io.github.taalaydev.doodleverse.getColorFromBitmap
import io.github.taalaydev.doodleverse.getPlatformType
import kotlinx.datetime.Clock

@Composable
fun DrawCanvas(
    controller: DrawEngineController,
    gestureEnabled: Boolean = true,
    onColorPicked: (Color) -> Unit = {},
    referenceImage: ImageBitmap? = null,
    referenceBlendMode: BlendMode = BlendMode.SrcOver,
    modifier: Modifier = Modifier,
) {
    var invalidationCounter by remember { mutableIntStateOf(0) }

    val drawingState by controller.state.collectAsStateWithLifecycle()
    val brush by controller.currentBrush.collectAsStateWithLifecycle(PenBrush())
    val tool by controller.currentTool.collectAsStateWithLifecycle()
    val brushParams by controller.brushParams.collectAsStateWithLifecycle()

    val currentColor = remember(brushParams) { brushParams.color }
    val brushSize = remember(brushParams) { brushParams.size }

    val layers = remember(drawingState) { drawingState.currentFrame.layers }
    val currentLayer = remember(drawingState) { drawingState.currentLayer }
    val currentLayerIndex = remember(drawingState) { drawingState.currentLayerIndex }

    val layerBitmap = remember(currentLayer.id, invalidationCounter) {
        controller.getOrCreateLayerBitmap(currentLayer.id)
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val imageSize = controller.imageSize
    var drawingBitmapState by remember {
        val bitmap = ImageBitmap(
            width = imageSize.width.coerceAtLeast(1),
            height = imageSize.height.coerceAtLeast(1)
        )
        mutableStateOf(DrawingBitmapState(
            bitmap = bitmap,
            canvas = androidx.compose.ui.graphics.Canvas(bitmap)
        ))
    }
    var session by remember { mutableStateOf<StrokeSession?>(null) }

    var canvasDrawState by remember { mutableStateOf(CanvasDrawState.Idle) }
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var eyedropperColor by remember { mutableStateOf<Color?>(null) }

    val velocityTracker by remember { mutableStateOf(VelocityTracker()) }

    val viewport by remember(canvasSize, imageSize) {
        derivedStateOf {
            val cw = canvasSize.width.toFloat().coerceAtLeast(1f)
            val ch = canvasSize.height.toFloat().coerceAtLeast(1f)
            val iw = imageSize.width.toFloat().coerceAtLeast(1f)
            val ih = imageSize.height.toFloat().coerceAtLeast(1f)

            val scale = kotlin.math.min(cw / iw, ch / ih)
            val dstW = iw * scale
            val dstH = ih * scale
            val left = (cw - dstW) * 0.5f
            val top  = (ch - dstH) * 0.5f
            Viewport(scale = scale, offset = Offset(left, top))
        }
    }

    val curveTool = remember { CurveTool(CurveParams(brush, brushParams, imageSize)) }
    val shapeTool = remember {
        ShapeTool(
            shapeType = ShapeType.Line,
            brush = brush,
            params = brushParams,
            imageSize = imageSize
        )
    }

    var lastPoint by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(tool) {
        if (tool !is DrawTool.Curve) {
            curveTool.clear()
        }
        if (tool !is DrawTool.Shape) {
            shapeTool.clear()
        }
    }

    LaunchedEffect(brush, brushParams, imageSize, tool) {
        if (tool.isCurve) {
            curveTool.updateParams(brush, brushParams, imageSize)
        }
        if (tool is DrawTool.Shape) {
            shapeTool.updateParams(brush, brushParams, (tool as DrawTool.Shape).shape)
        }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { size ->
                    canvasSize = size
                }
                .pointerInput(tool, curveTool.curvePoints) {
                    if (!tool.isCurve || !curveTool.isComplete()) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Only preview-drag after two anchors are set:
                        if (!curveTool.isComplete()) return@awaitEachGesture

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val pos = event.changes.first().position
                            val posImg = viewToImage(
                                pos,
                                viewport,
                                imageSize.width,
                                imageSize.height
                            )

                            curveTool.handleDrag(drawingBitmapState.canvas, posImg)

                            invalidationCounter++
                        }
                    }
                }
                .pointerInput(brush, gestureEnabled, tool, currentColor, brushSize) {
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
                                    controller.startSelection(offset)
                                },
                                onDrag = { _, new, _ ->
                                    controller.updateSelection(new)
                                },
                                onEnd = { _, _ ->
                                    controller.endSelection(viewport)
                                }
                            )
                        }

                        tool.isCurve -> {
                            detectTapGestures { offset ->
                                val posImg = viewToImage(
                                    offset,
                                    viewport,
                                    imageSize.width,
                                    imageSize.height
                                )
                                val isComplete = curveTool.isComplete()
                                curveTool.handleTap(drawingBitmapState.canvas, posImg)
                                if (isComplete) {
                                    finishDrawing(
                                        drawingState = drawingBitmapState,
                                        controller = controller,
                                        onComplete = {
                                            drawingBitmapState.reset()
                                        }
                                    )
                                }
                                invalidationCounter++
                            }
                        }

                        tool.isShape -> {
                            handleDrawing(
                                onStart = { offset, pressure ->
                                    val posImg = viewToImage(
                                        offset,
                                        viewport,
                                        imageSize.width,
                                        imageSize.height
                                    )

                                    shapeTool.handleStart(GestureEvent(
                                        position = posImg,
                                        timeMillis = Clock.currentTimeMillis(),
                                        pressure = pressure
                                    ))

                                    invalidationCounter++
                                    canvasDrawState = CanvasDrawState.Start
                                },
                                onDrag = { _, new, pressure ->
                                    val posImg = viewToImage(
                                        new,
                                        viewport,
                                        imageSize.width,
                                        imageSize.height
                                    )

                                    shapeTool.handleMove(GestureEvent(
                                        position = posImg,
                                        timeMillis = Clock.currentTimeMillis(),
                                        pressure = pressure
                                    ))

                                    invalidationCounter++
                                    canvasDrawState = CanvasDrawState.Drawing
                                },
                                onEnd = { point, pressure ->
                                    val endImg = viewToImage(
                                        point,
                                        viewport,
                                        imageSize.width,
                                        imageSize.height
                                    )

                                    shapeTool.handleEnd(GestureEvent(
                                        position = endImg,
                                        timeMillis = Clock.currentTimeMillis(),
                                        pressure = pressure
                                    ))

                                    val preview = shapeTool.getPreviewImage()
                                    drawingBitmapState.canvas.drawImage(preview, Offset.Zero, Paint())
                                    finishDrawing(
                                        drawingState = drawingBitmapState,
                                        controller = controller,
                                        onComplete = {
                                            drawingBitmapState.reset()
                                        }
                                    )

                                    invalidationCounter = 0
                                    canvasDrawState = CanvasDrawState.Ended
                                }
                            )
                        }

                        tool.isEyedropper -> {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                currentPosition = down.position
                                canvasDrawState = CanvasDrawState.Drawing
                                invalidationCounter++

                                while (true) {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Main)
                                    val change = event.changes.firstOrNull() ?: break

                                    if (change.pressed) {
                                        // live preview while pressed/moving
                                        currentPosition = change.position
                                        val combined = controller.getCombinedBitmap()
                                        if (combined != null) {
                                            val img = viewToImage(currentPosition, viewport, combined.width, combined.height)
                                            val argb = getColorFromBitmap(combined, img.x.toInt(), img.y.toInt())
                                            eyedropperColor = argb?.let { col ->
                                                val c = Color(col)
                                                if (c.alpha > 0f) c else Color.White
                                            }
                                        }
                                        invalidationCounter++
                                    } else {
                                        // release: finalize pick
                                        val combined = controller.getCombinedBitmap()
                                        val finalColor = combined?.let {
                                            val img = viewToImage(currentPosition, viewport, it.width, it.height)
                                            val argb = getColorFromBitmap(it, img.x.toInt(), img.y.toInt())
                                            argb?.let { a -> Color(a) }
                                        }
                                        finalColor?.let(onColorPicked)
                                        canvasDrawState = CanvasDrawState.Ended
                                        invalidationCounter++
                                        break
                                    }
                                }
                            }
                        }

                        else -> {
                            handleDrawing(
                                onStart = { offset, pressure ->
                                    val targetCanvas = if (tool.isEraser) {
                                        val layerBmp = controller.getOrCreateLayerBitmap(currentLayer.id)
                                        androidx.compose.ui.graphics.Canvas(layerBmp)
                                    } else {
                                        drawingBitmapState.canvas.also { it.save() }
                                    }

                                    velocityTracker.reset()
                                    velocityTracker.updateVelocity(
                                        offset,
                                        Clock.System.now().toEpochMilliseconds()
                                    )

                                    val posImg = viewToImage(
                                        offset,
                                        viewport,
                                        imageSize.width,
                                        imageSize.height
                                    )

                                    session = brush.startSession(targetCanvas, brushParams)
                                    session?.start(GestureEvent(
                                        position = posImg,
                                        timeMillis = Clock.currentTimeMillis(),
                                        pressure = pressure
                                    ))

                                    lastPoint = offset
                                    invalidationCounter++
                                    canvasDrawState = CanvasDrawState.Start
                                },
                                onDrag = { _, new, pressure ->
                                    val currentTime = Clock.System.now().toEpochMilliseconds()
                                    velocityTracker.updateVelocity(new, currentTime)

                                    val dynamicDistance = velocityTracker.calculateDynamicDistance()
                                    if (lastPoint != null && lastPoint!!.distanceTo(new) < dynamicDistance) return@handleDrawing

                                    val posImg = viewToImage(
                                        new,
                                        viewport,
                                        imageSize.width,
                                        imageSize.height
                                    )

                                    session?.move(GestureEvent(
                                        position = posImg,
                                        timeMillis = Clock.currentTimeMillis(),
                                        pressure = pressure,
                                        velocity = velocityTracker.velocity
                                    ))

                                    lastPoint = new
                                    invalidationCounter++
                                    canvasDrawState = CanvasDrawState.Drawing
                                },
                                onEnd = { point, pressure ->
                                    val endImg = viewToImage(
                                        lastPoint ?: point,
                                        viewport,
                                        imageSize.width,
                                        imageSize.height
                                    )

                                    session?.end(GestureEvent(
                                        position = endImg,
                                        timeMillis = Clock.currentTimeMillis(),
                                        pressure = pressure,
                                        velocity = velocityTracker.velocity
                                    ))

                                    if (tool.isEraser) {
                                        val layerBmp = controller.getOrCreateLayerBitmap(currentLayer.id)
                                        controller.updateLayerBitmap(layerBmp)
                                        return@handleDrawing
                                    }

                                    finishDrawing(
                                        drawingState = drawingBitmapState,
                                        controller = controller,
                                        onComplete = {
                                            session = null
                                            drawingBitmapState.reset()
                                            drawingBitmapState.canvas.restore()
                                            drawingBitmapState.clearCanvas()
                                        }
                                    )

                                    velocityTracker.reset()
                                    invalidationCounter = 0
                                    lastPoint = null
                                    canvasDrawState = CanvasDrawState.Ended
                                }
                            )
                        }
                    }
                }
        ) {
            val count = invalidationCounter

            when {
                tool.isFill && canvasDrawState == CanvasDrawState.Drawing -> {
                    handleFloodFill(
                        position = currentPosition,
                        color = currentColor,
                        layerBitmap = layerBitmap,
                        controller = controller,
                        viewport = viewport
                    )
                    controller.updateLayerBitmap(layerBitmap)

                    canvasDrawState = CanvasDrawState.Idle
                }

                tool.isEyedropper && canvasDrawState != CanvasDrawState.Idle -> {
                    eyedropperColor = handleEyedropper(
                        position = currentPosition,
                        controller = controller,
                        drawContext = this,
                        viewport = viewport
                    )

                    if (canvasDrawState == CanvasDrawState.Ended) {
                        eyedropperColor = null
                        canvasDrawState = CanvasDrawState.Idle
                    }
                }
            }

            renderLayers(
                layers = layers,
                currentLayerIndex = currentLayerIndex,
                controller = controller,
                tool = tool,
                drawingBitmapState = drawingBitmapState,
                viewport = viewport,
            )

            if (tool.isShape && shapeTool.isActive()) {
                drawScaledImage(shapeTool.getPreviewImage(), viewport)
            }

            renderSelectionOverlay(
                controller = controller,
                viewport = viewport,
            )

            referenceImage?.let { refImage ->
                renderReferenceImage(
                    image = refImage,
                    viewport = viewport,
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

        if (controller.selectionState.isActive) {
            SelectionOverlay(
                state = controller.selectionState,
                onTransformStart = { transform, point ->
                    controller.startTransform(transform)
                },
                onTransformDelta = { offset ->
                    controller.updateSelectionTransform(offset)
                },
                onTransformEnd = {
                },
                onTapOutside = {
                    controller.applySelection()
                },
                modifier = Modifier.fillMaxSize(),
                isMobile = getPlatformType().isAndroid || getPlatformType().isIos
            )
        }
    }
}

private fun handleFloodFill(
    position: Offset,
    color: Color,
    layerBitmap: ImageBitmap,
    controller: DrawEngineController,
    viewport: Viewport
) {
    val img = viewToImage(position, viewport, layerBitmap.width, layerBitmap.height)
    floodFillFast(layerBitmap, img.x.toInt(), img.y.toInt(), color.toArgb())
}

private fun handleEyedropper(
    position: Offset,
    controller: DrawEngineController,
    drawContext: DrawScope,
    viewport: Viewport
): Color? {
    val combined = controller.getCombinedBitmap() ?: return null
    val img = viewToImage(position, viewport, combined.width, combined.height)
    val argb = getColorFromBitmap(combined, img.x.toInt(), img.y.toInt()) ?: return null
    return Color(argb).takeIf { it.alpha > 0 } ?: Color.White
}


private fun DrawScope.renderLayers(
    layers: List<LayerModel>,
    currentLayerIndex: Int,
    tool: DrawTool,
    controller: DrawEngineController,
    drawingBitmapState: DrawingBitmapState? = null,
    viewport: Viewport,
) {
    layers.forEachIndexed { index, layer ->
        if (!layer.isVisible || layer.opacity <= 0.0) return@forEachIndexed

        drawLayerBitmap(layer = layer, controller = controller, viewport)
        if (index == currentLayerIndex && drawingBitmapState != null) {
            drawScaledImage(drawingBitmapState.bitmap, viewport)
        }
    }
}

fun DrawScope.drawScaledImage(
    image: ImageBitmap,
    viewport: Viewport,
    srcOffset: IntOffset = IntOffset(0, 0),
    alpha: Float = 1.0f,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    val dstW = (image.width * viewport.scale).toInt()
    val dstH = (image.height * viewport.scale).toInt()

    drawImage(
        image = image,
        srcOffset = srcOffset,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset(viewport.offset.x.toInt(), viewport.offset.y.toInt()),
        dstSize = IntSize(dstW, dstH),
        alpha = alpha
    )
}

private fun DrawScope.drawLayerBitmap(
    layer: LayerModel,
    controller: DrawEngineController,
    viewport: Viewport,
) {
    controller.getLayerBitmap(layer.id)?.let { bitmap ->
        drawScaledImage(
            image = bitmap,
            viewport = viewport,
            alpha = layer.opacity.toFloat()
        )
    }
}

private fun DrawScope.renderSelectionOverlay(
    controller: DrawEngineController,
    viewport: Viewport,
) {
    if (controller.selectionState.isActive) {
        val bounds = controller.selectionState.bounds
        val transformedBounds = controller.selectionState.getTransformedBounds()

        drawContext.canvas.withSave {
            val offset = controller.selectionState.offset
            val center = transformedBounds.center

            translate(offset.x + center.x, offset.y + center.y) {
                rotate(controller.selectionState.rotation) {
                    scale(controller.selectionState.scale, controller.selectionState.scale) {}
                    translate(-center.x, -center.y) {}
                }
            }

            drawRect(
                topLeft = transformedBounds.topLeft,
                size = transformedBounds.size,
                color = Color.Blue.copy(alpha = 0.3f),
                style = Stroke(width = 2.dp.toPx())
            )

            controller.selectionState.transformedBitmap?.let { bitmap ->
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
    viewport: Viewport,
    alpha: Float = 0.5f,
    blendMode: BlendMode = BlendMode.SrcOver
) {
    drawImage(
        image = image,
        dstOffset = IntOffset(0,0),
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        alpha = alpha,
        blendMode = blendMode,
        colorFilter = ColorFilter.tint(Color(0x40FF0000)),
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

private fun finishDrawing(
    drawingState: DrawingBitmapState?,
    controller: DrawEngineController,
    onComplete: () -> Unit
) {
    val state = drawingState ?: return

    val existingBitmap = controller.getOrCreateLayerBitmap(
        controller.state.value.currentLayer.id
    )

    val finalBitmap = combineDrawingWithLayer(
        drawingBitmap = state.bitmap,
        layerBitmap = existingBitmap
    )

    controller.updateLayerBitmap(finalBitmap)

    onComplete()
}

private fun combineDrawingWithLayer(
    drawingBitmap: ImageBitmap,
    layerBitmap: ImageBitmap,
): ImageBitmap {
    val resultBitmap = layerBitmap.copy()
    val canvas = androidx.compose.ui.graphics.Canvas(resultBitmap)

    val paint = Paint()

    canvas.drawImage(drawingBitmap, Offset.Zero, paint)
    return resultBitmap
}