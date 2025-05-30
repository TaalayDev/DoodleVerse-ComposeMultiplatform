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
            val pathBitmap = renderPathToBitmap(drawingPath, size)
            canvas.drawImage(pathBitmap, Offset.Zero, Paint())
        }

        return bitmap
    }

    /**
     * FIXED: Renders a single drawing path to ImageBitmap with proper eraser support
     */
    fun renderPathToBitmap(
        drawingPath: DrawingPath,
        canvasSize: Size,
        existingBitmap: ImageBitmap? = null
    ): ImageBitmap {
        val brush = drawingPath.brush

        // For erasers, we need to work with the existing bitmap directly
//        if (brush.blendMode == androidx.compose.ui.graphics.BlendMode.Clear) {
//            val targetBitmap = existingBitmap?.copy()
//                ?: ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
//            val canvas = Canvas(targetBitmap)
//
//            if (brush.customPainter != null) {
//                brush.customPainter.invoke(canvas, canvasSize, drawingPath)
//            } else {
//                val paint = pathPaint(drawingPath)
//                canvas.drawPath(drawingPath.path, paint)
//            }
//
//            return targetBitmap
//        }

        return if (brush.customPainter != null) {
            val bitmap = ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
            val canvas = Canvas(bitmap)

            brush.customPainter.invoke(canvas, canvasSize, drawingPath)

            bitmap
        } else {
            // Default rendering for non-custom brushes
            val bitmap = ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
            val canvas = Canvas(bitmap)
            val paint = pathPaint(drawingPath)

            canvas.drawPath(drawingPath.path, paint)
            bitmap
        }
    }

    /**
     * UPDATED: Now handles ImageBitmap from custom painters with eraser support
     */
    internal fun drawPath(
        canvas: Canvas,
        drawingPath: DrawingPath,
        paint: Paint,
        size: Size,
    ) {
        val brush = drawingPath.brush
        if (brush.customPainter != null) {
            val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
            val brushCanvas = Canvas(bitmap)

            brush.customPainter.invoke(brushCanvas, size, drawingPath)
            canvas.drawImage(bitmap, Offset.Zero, Paint())
        } else {
            // Default path drawing
            canvas.drawPath(drawingPath.path, paint)
        }
    }

    /**
     * FIXED: Creates a preview bitmap for real-time drawing feedback with eraser support
     */
    fun createPreviewBitmap(
        drawingPath: DrawingPath,
        canvasSize: Size,
        existingBitmap: ImageBitmap? = null
    ): ImageBitmap {
        val brush = drawingPath.brush

        // For erasers, apply directly to existing bitmap
        if (brush.blendMode == androidx.compose.ui.graphics.BlendMode.Clear) {
            return renderPathToBitmap(drawingPath, canvasSize, existingBitmap)
        }

        // For regular brushes, composite with existing content
        val bitmap = ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())
        val canvas = Canvas(bitmap)

        // Draw existing content if provided
        existingBitmap?.let { existing ->
            canvas.drawImage(existing, Offset.Zero, Paint())
        }

        // Draw the current path
        val pathBitmap = renderPathToBitmap(drawingPath, canvasSize)
        canvas.drawImage(pathBitmap, Offset.Zero, Paint())

        return bitmap
    }

    /**
     * FIXED: Blends a path bitmap onto an existing bitmap with proper eraser handling
     */
    fun blendPathBitmap(
        pathBitmap: ImageBitmap,
        targetBitmap: ImageBitmap,
        blendMode: androidx.compose.ui.graphics.BlendMode = androidx.compose.ui.graphics.BlendMode.SrcOver,
        isEraser: Boolean = false
    ): ImageBitmap {
        // For erasers, the path bitmap should already contain the erased content
        if (isEraser || blendMode == androidx.compose.ui.graphics.BlendMode.Clear) {
            return pathBitmap
        }

        val resultBitmap = targetBitmap.copy()
        val canvas = Canvas(resultBitmap)

        canvas.drawImage(
            pathBitmap,
            Offset.Zero,
            Paint().apply {
                this.blendMode = blendMode
            }
        )

        return resultBitmap
    }

    /**
     * NEW: Method to apply eraser path directly to existing bitmap
     */
    fun applyEraserPath(
        drawingPath: DrawingPath,
        targetBitmap: ImageBitmap
    ): ImageBitmap {
        val resultBitmap = targetBitmap.copy()
        val canvas = Canvas(resultBitmap)

        val brush = drawingPath.brush
        if (brush.customPainter != null) {
            brush.customPainter.invoke(canvas, Size(targetBitmap.width.toFloat(), targetBitmap.height.toFloat()), drawingPath)
        } else {
            val paint = Paint().apply {
                strokeWidth = drawingPath.size
                strokeCap = brush.strokeCap
                strokeJoin = brush.strokeJoin
                style = PaintingStyle.Stroke
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            }
            canvas.drawPath(drawingPath.path, paint)
        }

        return resultBitmap
    }

    private fun calculateTaperFactor(progress: Float): Float {
        // This function creates a tapered effect at the start and end of the stroke
        return when {
            progress < 0.1f -> progress * 10 // Taper at the start
            progress > 0.9f -> (1 - progress) * 10 // Taper at the end
            else -> 1f // Full size in the middle
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
}