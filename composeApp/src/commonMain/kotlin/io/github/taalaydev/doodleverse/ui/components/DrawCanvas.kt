package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FabPosition
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.taalaydev.doodleverse.core.DrawRenderer
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.core.handleDrawing
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawingController
import io.github.taalaydev.doodleverse.ui.screens.draw.currentLayer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class DrawState {
    Idle,
    Started,
    Drag,
    End,
}

internal fun calcOpacity(alpha: Float, brushOpacity: Float): Float {
    return max(alpha, brushOpacity) - min(alpha, brushOpacity)
}

@Composable
fun DrawCanvas(
    currentBrush: BrushData,
    currentColor: Color,
    brushSize: Float,
    initialPath: DrawingPath? = null,
    controller: DrawingController,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val drawState = remember { mutableStateOf(DrawState.Idle) }

    var prevPosition by remember { mutableStateOf(Offset.Zero) }
    var currentPosition by remember { mutableStateOf(Offset.Zero) }

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
            alpha = calcOpacity(currentColor.alpha, currentBrush.opacityDiff)
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                handleDrawing(
                    onStart = { offset ->
                        currentPosition = offset
                        drawState.value = DrawState.Started
                        prevPosition = currentPosition
                        drawingPath = DrawingPath(
                            path = Path().apply {
                                moveTo(currentPosition.x, currentPosition.y)
                            },
                            brush = currentBrush,
                            color = currentColor,
                            size = brushSize,
                            startPoint = currentPosition,
                            endPoint = currentPosition
                        )
                        println("DrawingPath: $drawingPath")
                    },
                    onDrag = { _, new ->
                        currentPosition = new
                        drawState.value = DrawState.Drag
                    },
                    onEnd = {
                        drawState.value = DrawState.End
                    }
                )
            }
            .background(Color.White)
    ) {
        val canvasWidth = size.width.toInt().coerceAtLeast(1)
        val canvasHeight = size.height.toInt().coerceAtLeast(1)
        if (bitmap == null || bitmap?.width != canvasWidth || bitmap?.height != canvasHeight || isDirty) {
            val oldBitmap = if (isDirty) restoreImage else bitmap
            bitmap = ImageBitmap(canvasWidth, canvasHeight)
            imageCanvas = Canvas(bitmap!!)

            if (oldBitmap != null) {
                imageCanvas?.drawImage(oldBitmap, Offset.Zero, paint = Paint())
            }

            canvasSize = size
            isDirty = false
            restoreImage = null
        }

        when (drawState.value) {
            DrawState.Started -> {
                // prevPosition = currentPosition

                if (currentBrush.isShape) {
                    shapeBitmap = ImageBitmap(canvasWidth, canvasHeight)
                    shapeCanvas = Canvas(shapeBitmap!!)

                    DrawRenderer.drawPath(
                        shapeCanvas!!,
                        drawingPath!!,
                        currentBrush,
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
                    DrawRenderer.drawPath(imageCanvas!!, drawingPath!!, currentBrush, paint, size)
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
                        currentBrush,
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
                        currentBrush,
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
