package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.taalaydev.doodleverse.core.DrawRenderer
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.core.handleDrawing
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawingController
import io.github.taalaydev.doodleverse.ui.screens.draw.currentLayer
import kotlin.math.max
import kotlin.math.min

enum class DrawState {
    Idle,
    Started,
    Drag,
    End,
}

@Composable
fun DrawCanvas(
    currentBrush: BrushData,
    currentColor: Color,
    brushSize: Float,
    tool: Tool,
    gestureEnabled: Boolean = true,
    initialPath: DrawingPath? = null,
    controller: DrawingController,
    onColorPicked: (Color) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val drawState = remember { mutableStateOf(DrawState.Idle) }

    var prevPosition by remember { mutableStateOf(Offset.Zero) }
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    var savedSize by remember { mutableStateOf(Size.Zero) }

    var eyedropperColor by remember { mutableStateOf<Color?>(null) }

    val state = controller.state.value
    var bitmap by controller.bitmap
    var imageCanvas by controller.imageCanvas

    var shapeBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var shapeCanvas by remember { mutableStateOf<Canvas?>(null) }

    var canvasSize by controller.canvasSize
    var drawingPath by controller.currentPath
    var restoreImage by controller.restoreImage
    var isDirty by controller.isDirty

    val paint = remember { Paint() }

    LaunchedEffect(currentColor, brushSize, currentBrush) {
        paint.apply {
            style = PaintingStyle.Stroke
            color = currentColor
            strokeWidth = brushSize
            strokeCap = currentBrush.strokeCap
            strokeJoin = currentBrush.strokeJoin
            pathEffect = currentBrush.pathEffect?.invoke(brushSize)
            blendMode = currentBrush.blendMode
            colorFilter = if (currentBrush.brush != null) {
                ColorFilter.tint(currentColor)
            } else {
                null
            }
            alpha = DrawRenderer.calcOpacity(currentColor.alpha, currentBrush.opacityDiff)
        }
    }

    Box(
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(currentBrush, gestureEnabled, tool) {
                    if (!gestureEnabled) return@pointerInput
                    if (tool.isFill) {
                        detectTapGestures { offset ->
                            currentPosition = offset
                            drawState.value = DrawState.Started
                        }
                    } else {
                        handleDrawing(
                            onStart = { offset, pressure ->
                                currentPosition = offset
                                drawState.value = DrawState.Started
                                prevPosition = currentPosition

                                drawingPath = DrawingPath(
                                    path = Path().apply {
                                        moveTo(currentPosition.x, currentPosition.y)
                                    },
                                    brush = currentBrush,
                                    color = paint.color,
                                    size = paint.strokeWidth,
                                    startPoint = currentPosition,
                                    endPoint = currentPosition
                                )
                            },
                            onDrag = { _, new, pressure ->
                                currentPosition = new
                                drawState.value = DrawState.Drag
                            },
                            onEnd = {
                                drawState.value = DrawState.End
                                if (tool.isEyedropper) {
                                    eyedropperColor?.let { color ->
                                        onColorPicked(color)
                                    }
                                    eyedropperColor = null
                                }
                            }
                        )
                    }
                }
                .background(Color.White)
        ) {
            if (savedSize.width < size.width || savedSize.height < size.height) {
                savedSize = size
            }

            val canvasWidth = size.width.toInt().coerceAtLeast(1)
            val canvasHeight = size.height.toInt().coerceAtLeast(1)
            if (bitmap == null || bitmap?.width != canvasWidth || bitmap?.height != canvasHeight || isDirty) {
                val oldBitmap = if (isDirty) restoreImage else bitmap

                bitmap = ImageBitmap(savedSize.width.toInt(), savedSize.height.toInt())
                imageCanvas = Canvas(bitmap!!)

                if (oldBitmap != null) {
                    imageCanvas?.drawImageRect(
                        oldBitmap,
                        paint = Paint()
                    )
                }

                canvasSize = size
                isDirty = false
                restoreImage = null
            }

            if (tool.isFill && drawState.value == DrawState.Started) {
                val x = currentPosition.x.toInt()
                val y = currentPosition.y.toInt()
                val replacementColor = currentColor.toArgb()

                DrawRenderer.floodFill(
                    imageCanvas!!,
                    bitmap!!,
                    x,
                    y,
                    replacementColor,
                )
                drawState.value = DrawState.Idle
            }

            if (tool.isEyedropper && drawState.value != DrawState.Idle) {
                if (drawState.value == DrawState.End) {
                    drawState.value = DrawState.Idle
                }

                val x = currentPosition.x.toInt()
                val y = currentPosition.y.toInt()
                val colorArgb = bitmap?.asSkiaBitmap()?.getColor(x, y) ?: return@Canvas
                val color = Color(colorArgb)

                if (eyedropperColor != color) {
                    eyedropperColor = if (color.alpha > 0) color else Color.White
                }

                if (eyedropperColor != null) {
                    drawArc(
                        color = eyedropperColor!!,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = true,
                        topLeft = Offset(x.toFloat() + 10, y.toFloat() - 40),
                        size = Size(40f, 40f),
                    )

                    drawLine(
                        color = Color.Black,
                        start = Offset(x.toFloat(), y.toFloat() - 15),
                        end = Offset(x.toFloat(), y.toFloat() + 15),
                        strokeWidth = 1f,
                    )

                    drawLine(
                        color = Color.Black,
                        start = Offset(x.toFloat() - 15, y.toFloat()),
                        end = Offset(x.toFloat() + 15, y.toFloat()),
                        strokeWidth = 1f,
                    )
                }
            } else {
                when (drawState.value) {
                    DrawState.Started -> {
                        // prevPosition = currentPosition
                        if (currentBrush.isShape) {
                            shapeBitmap = ImageBitmap(canvasWidth, canvasHeight)
                            shapeCanvas = Canvas(shapeBitmap!!)

                            DrawRenderer.drawPath(
                                shapeCanvas!!,
                                drawingPath!!,
                                paint,
                                size
                            )
                        } else if (currentBrush.brush != null) {
                            DrawRenderer.drawBrushStampsBetweenPoints(
                                imageCanvas!!,
                                prevPosition,
                                currentPosition,
                                paint,
                                drawingPath!!
                            )
                        } else {
                            DrawRenderer.drawPath(imageCanvas!!, drawingPath!!, paint, size)
                        }
                    }

                    DrawState.Drag -> {
                        val lerpX = lerp(prevPosition.x, currentPosition.x, 0.5f)
                        val lerpY = lerp(prevPosition.y, currentPosition.y, 0.5f)

                        drawingPath?.endPoint = Offset(lerpX, lerpY)

                        drawingPath?.path?.quadraticBezierTo(
                            prevPosition.x,
                            prevPosition.y,
                            lerpX,
                            lerpY
                        )

                        if (currentBrush.isShape) {
                            shapeCanvas?.drawRect(
                                Rect(Offset.Zero, canvasSize),
                                Paint().apply {
                                    blendMode = BlendMode.Clear
                                }
                            )
                            DrawRenderer.drawPath(
                                shapeCanvas!!,
                                drawingPath!!,
                                paint,
                                size
                            )
                        } else if (currentBrush.brush != null) {
                            DrawRenderer.drawBrushStampsBetweenPoints(
                                imageCanvas!!,
                                prevPosition,
                                currentPosition,
                                paint,
                                drawingPath!!
                            )
                        } else {
                            DrawRenderer.drawPath(
                                imageCanvas!!,
                                drawingPath!!,
                                paint,
                                size
                            )
                        }

                        drawingPath?.path?.reset()
                        drawingPath?.path?.moveTo(lerpX, lerpY)
                        prevPosition = currentPosition
                    }

                    DrawState.End -> {
                        currentPosition = Offset.Zero
                        prevPosition = currentPosition
                        drawState.value = DrawState.Idle
                        if (currentBrush.isShape) {
                            imageCanvas?.drawImage(shapeBitmap!!, Offset.Zero, paint = Paint())
                            shapeBitmap = null
                            shapeCanvas = null
                        }
                        controller.addState(drawingPath!!, bitmap!!)

                        drawingPath = null
                    }

                    DrawState.Idle -> {
                        // Do nothing
                    }
                }
            }

            with(drawContext.canvas) {
                for (layer in state.layers) {
                    if (!layer.isVisible || layer.opacity == 0.0) continue
                    if (layer.id == state.currentLayer.id) {
                        bitmap?.let {
                            drawImage(it)
                        }

                        if (currentBrush.isShape) {
                            shapeBitmap?.let {
                                drawImage(it)
                            }
                        }
                    } else {
                        val image = state.caches[layer.id]
                        if (image != null) {
                            drawImage(image)
                        }
                    }
                }

            }
        }
    }

}
