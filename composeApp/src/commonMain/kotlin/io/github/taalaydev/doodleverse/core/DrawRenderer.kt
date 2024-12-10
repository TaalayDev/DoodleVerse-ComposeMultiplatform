package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.data.models.BrushData
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
        return (max(alpha, brushOpacity) - min(alpha, brushOpacity))
            .coerceAtLeast(0f)
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
        borderPath: Path? = null,
    ) {
        if (x < 0 || y < 0 || x >= imageBitmap.width || y >= imageBitmap.height) return

        val width = imageBitmap.width
        val height = imageBitmap.height
        val pixelMap = imageBitmap.toPixelMap()
        val targetColor = pixelMap[x, y].toArgb()

        if (targetColor == replacement) return

        // Create a boolean array to track visited pixels and those inside the border path
        val visited = Array(height) { BooleanArray(width) }

        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(Pair(x, y))

        while (stack.isNotEmpty()) {
            val (x, y) = stack.removeLast()

            // Skip if outside bounds, already visited, or outside border path
            if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x]) continue

            // Skip if pixel doesn't match target color
            if (pixelMap[x, y].toArgb() != targetColor) continue

            visited[y][x] = true

            // Draw the pixel with the pattern color
            canvas.nativeCanvas.drawPoint(
                x.toFloat(),
                y.toFloat(),
                Paint().apply {
                    color = Color(replacement)
                    style = PaintingStyle.Fill
                }.asFrameworkPaint()
            )

            // Add neighboring pixels to stack
            stack.add(Pair(x + 1, y))
            stack.add(Pair(x - 1, y))
            stack.add(Pair(x, y + 1))
            stack.add(Pair(x, y - 1))
        }
    }

    fun getBorderPath(
        imageBitmap: ImageBitmap,
        x: Int,
        y: Int,
        targetColor: Int,
    ): Path {
        val width = imageBitmap.width
        val height = imageBitmap.height
        val pixelMap = imageBitmap.toPixelMap()

        // For marking visited border pixels
        val visited = Array(height) { BooleanArray(width) }
        val borderPath = Path()

        // Find first border point
        var startX = x
        var startY = y
        while (startY < height && pixelMap[startX, startY].toArgb() == targetColor) {
            startY++
        }
        if (startY == height) return borderPath
        startY--

        // Define 8-directional movement
        val directions = listOf(
            -1 to 0,  // left
            -1 to 1,  // down-left
            0 to 1,   // down
            1 to 1,   // down-right
            1 to 0,   // right
            1 to -1,  // up-right
            0 to -1,  // up
            -1 to -1  // up-left
        )

        // Start tracing the border
        var currentX = startX
        var currentY = startY
        var currentDir = 0 // Start moving right
        borderPath.moveTo(currentX.toFloat(), currentY.toFloat())

        do {
            visited[currentY][currentX] = true

            // Try all directions, starting from the current direction
            var found = false
            for (i in 0 until 8) {
                val nextDir = (currentDir + i) % 8
                val (dx, dy) = directions[nextDir]
                val newX = currentX + dx
                val newY = currentY + dy

                // Check if the new position is valid and on the border
                if (newX in 0 until width && newY in 0 until height &&
                    !visited[newY][newX] &&
                    isOnBorder(pixelMap, newX, newY, targetColor, width, height)
                ) {
                    borderPath.lineTo(newX.toFloat(), newY.toFloat())
                    currentX = newX
                    currentY = newY
                    currentDir = nextDir
                    found = true
                    break
                }
            }

            if (!found) break

        } while (currentX != startX || currentY != startY)

        borderPath.close()
        return borderPath
    }

    private fun isOnBorder(
        pixelMap: PixelMap,
        x: Int,
        y: Int,
        targetColor: Int,
        width: Int,
        height: Int
    ): Boolean {
        val current = pixelMap[x, y].toArgb() == targetColor

        // Check if any adjacent pixel has different color (including diagonals)
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue

                val newX = x + dx
                val newY = y + dy

                if (newX in 0 until width && newY in 0 until height) {
                    val neighbor = pixelMap[newX, newY].toArgb() == targetColor
                    if (current != neighbor) return true
                }
            }
        }

        return false
    }

}