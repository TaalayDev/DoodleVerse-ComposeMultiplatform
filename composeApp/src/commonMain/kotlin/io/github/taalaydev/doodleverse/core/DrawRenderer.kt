package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import kotlin.math.max
import kotlin.math.min

enum class BrushModifier {
    Stroke,
    Fill,
    Mirror,
}

object DrawRenderer {
    internal fun calcOpacity(alpha: Float, brushOpacity: Float): Float {
        return max(alpha, brushOpacity) - min(alpha, brushOpacity)
    }

    internal fun pathPaint(
        path: DrawingPath,
    ): Paint {
        val brush = path.brush
        return Paint().apply {
            style = PaintingStyle.Stroke
            color = path.color
            strokeWidth = path.size
            strokeCap = brush.strokeCap
            strokeJoin = brush.strokeJoin
            pathEffect = brush.pathEffect?.invoke(path.size)
            blendMode = brush.blendMode
            colorFilter = if (brush.brush != null) {
                ColorFilter.tint(path.color)
            } else {
                null
            }
            alpha = calcOpacity(path.color.alpha, brush.opacityDiff)
        }
    }

    internal fun pathsToBitmap(
        paths: List<DrawingPath>,
        size: Size,
    ): ImageBitmap {
        val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
        val canvas = Canvas(bitmap)

        paths.forEach { drawingPath ->
            val paint = pathPaint(drawingPath)

            drawPath(canvas, drawingPath, paint, size)
        }

        return bitmap
    }

    private fun calculateTaperFactor(progress: Float): Float {
        // This function creates a tapered effect at the start and end of the stroke
        return when {
            progress < 0.1f -> progress * 10 // Taper at the start
            progress > 0.9f -> (1 - progress) * 10 // Taper at the end
            else -> 1f // Full size in the middle
        }
    }

    internal fun drawPath(
        canvas: Canvas,
        drawingPath: DrawingPath,
        paint: Paint,
        size: Size,
    ) {
        val brush = drawingPath.brush
        if (brush.customPainter != null) {
            brush.customPainter.invoke(
                canvas,
                size,
                drawingPath,
            )
        } else {
            canvas.drawPath(
                drawingPath.path,
                paint
            )
        }
    }

    internal fun drawBrushStampsBetweenPoints(
        canvas: Canvas,
        start: Offset,
        end: Offset,
        paint: Paint,
        drawingPath: DrawingPath,
        brushImage: ImageBitmap,
    ) {
        val brushSize = drawingPath.size
        val densityOffset = drawingPath.brush.densityOffset.toFloat()
        val useBrushWidthDensity = drawingPath.brush.useBrushWidthDensity

        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length
        val delta = Offset(end.x - start.x, end.y - start.y)

        val halfSize = brushSize / 8f
        var i = 0f
        var currentPoint = start

        while (i < length) {
            val point = measure.getPosition(i)
            val progress = i / length
            val taperFactor = calculateTaperFactor(progress)
            val currentBrushSize = brushSize * taperFactor

            canvas.drawImageRect(
                brushImage,
                dstOffset = IntOffset(
                    (point.x - halfSize).toInt(),
                    (point.y - halfSize).toInt()
                ),
                dstSize = IntSize(brushSize.toInt(), brushSize.toInt()),
                paint = paint
            )
            currentPoint += delta
            i += halfSize
        }
    }

    fun floodFill(
        canvas: Canvas,
        imageBitmap: ImageBitmap,
        x: Int,
        y: Int,
        replacement: Int,
    ) {
        if (x < 0 || y < 0 || x >= imageBitmap.width || y >= imageBitmap.height) return

        val width = imageBitmap.width
        val height = imageBitmap.height

        val pixelMap = imageBitmap.toPixelMap()
        val targetColor = pixelMap[x, y].toArgb()

        if (targetColor == replacement) return

        val pixels = pixelMap.buffer

        val stack = ArrayDeque<Offset>()
        stack.add(Offset(x.toFloat(), y.toFloat()))

        while (stack.isNotEmpty()) {
            val point = stack.removeLast()
            val px = point.x.toInt()
            val py = point.y.toInt()

            if (px < 0 || px >= width || py < 0 || py >= height) continue

            val index = py * width + px
            if (pixels[index] != targetColor) continue

            pixels[index] = replacement

            stack.add(Offset((px + 1).toFloat(), py.toFloat()))
            stack.add(Offset((px - 1).toFloat(), py.toFloat()))
            stack.add(Offset(px.toFloat(), (py + 1).toFloat()))
            stack.add(Offset(px.toFloat(), (py - 1).toFloat()))
        }

        canvas.drawPoints(
            pointMode = androidx.compose.ui.graphics.PointMode.Points,
            points = pixels.mapIndexed { index, color ->
                val x = index % width
                val y = index / width
                Offset(x.toFloat(), y.toFloat())
            }.filter { it.x >= 0 && it.y >= 0 && it.x < width && it.y < height && pixels[(it.y.toInt() * width + it.x.toInt())] == replacement },
            paint = Paint().apply {
                color = Color(replacement)
                style = PaintingStyle.Fill
            }
        )
    }

}