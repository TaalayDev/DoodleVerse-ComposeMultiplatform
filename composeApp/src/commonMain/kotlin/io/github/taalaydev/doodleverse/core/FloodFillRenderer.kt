package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

/**
 * Optimized flood fill implementation with scan-line algorithm
 */
object FloodFillRenderer {

    /**
     * Represents a horizontal span to be filled
     */
    private data class Span(
        val y: Int,
        val leftX: Int,
        val rightX: Int,
        val parentY: Int,
        val direction: Int // 1 for down, -1 for up
    )

    /**
     * Configuration for flood fill operation
     */
    data class FloodFillConfig(
        val tolerance: Int = 0, // Color tolerance (0-255)
        val maxIterations: Int = 1000000, // Prevent infinite loops
        val useAntiAliasing: Boolean = false,
        val respectBorders: Boolean = false
    )

    /**
     * Result of flood fill operation
     */
    data class FloodFillResult(
        val pixelsFilled: Int,
        val success: Boolean,
        val bounds: IntBounds? = null
    )

    /**
     * Represents integer bounds
     */
    data class IntBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val width: Int get() = right - left + 1
        val height: Int get() = bottom - top + 1
        val area: Int get() = width * height
    }

    /**
     * Enhanced flood fill with scan-line algorithm for better performance
     */
    fun floodFill(
        canvas: Canvas,
        imageBitmap: ImageBitmap,
        startX: Int,
        startY: Int,
        replacementColor: Color,
        config: FloodFillConfig = FloodFillConfig()
    ): FloodFillResult {
        return try {
            floodFillInternal(canvas, imageBitmap, startX, startY, replacementColor, config)
        } catch (e: Exception) {
            FloodFillResult(0, false)
        }
    }

    private fun floodFillInternal(
        canvas: Canvas,
        imageBitmap: ImageBitmap,
        startX: Int,
        startY: Int,
        replacementColor: Color,
        config: FloodFillConfig
    ): FloodFillResult {
        val width = imageBitmap.width
        val height = imageBitmap.height

        // Validate input parameters
        if (startX < 0 || startX >= width || startY < 0 || startY >= height) {
            return FloodFillResult(0, false)
        }

        val pixelMap = imageBitmap.toPixelMap()
        val targetColor = pixelMap[startX, startY].toArgb()
        val replacementArgb = replacementColor.toArgb()

        // Early exit if target and replacement colors are the same
        if (colorsMatch(targetColor, replacementArgb, config.tolerance)) {
            return FloodFillResult(0, true)
        }

        // Use optimized approach based on expected fill size
        return if (shouldUseOptimizedApproach(width, height)) {
            scanLineFill(canvas, pixelMap, width, height, startX, startY,
                targetColor, replacementColor, config)
        } else {
            simpleFill(canvas, pixelMap, width, height, startX, startY,
                targetColor, replacementColor, config)
        }
    }

    /**
     * Scan-line flood fill algorithm - more efficient for large areas
     */
    private fun scanLineFill(
        canvas: Canvas,
        pixelMap: androidx.compose.ui.graphics.PixelMap,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        targetColor: Int,
        replacementColor: Color,
        config: FloodFillConfig
    ): FloodFillResult {
        val stack = ArrayDeque<Span>()
        val processedLines = mutableSetOf<Int>()
        var pixelsFilled = 0
        var iterations = 0

        var bounds = IntBounds(startX, startY, startX, startY)

        // Add initial span
        val initialSpan = findHorizontalSpan(pixelMap, width, height, startX, startY, targetColor, config.tolerance)
        if (initialSpan != null) {
            stack.add(Span(startY, initialSpan.first, initialSpan.second, startY, 0))
        }

        while (stack.isNotEmpty() && iterations < config.maxIterations) {
            iterations++
            val span = stack.removeFirst()

            if (span.y < 0 || span.y >= height) continue
            if (processedLines.contains(span.y * width + span.leftX)) continue

            // Fill the current span
            val actualSpan = findHorizontalSpan(pixelMap, width, height, span.leftX, span.y, targetColor, config.tolerance)
            if (actualSpan == null) continue

            val (leftX, rightX) = actualSpan
            fillHorizontalLine(canvas, span.y, leftX, rightX, replacementColor)

            pixelsFilled += (rightX - leftX + 1)
            processedLines.add(span.y * width + leftX)

            // Update bounds
            bounds = IntBounds(
                min(bounds.left, leftX),
                min(bounds.top, span.y),
                max(bounds.right, rightX),
                max(bounds.bottom, span.y)
            )

            // Check lines above and below
            checkAndAddSpans(stack, pixelMap, width, height, leftX, rightX, span.y - 1, span.y, -1, targetColor, config.tolerance)
            checkAndAddSpans(stack, pixelMap, width, height, leftX, rightX, span.y + 1, span.y, 1, targetColor, config.tolerance)
        }

        return FloodFillResult(pixelsFilled, true, bounds)
    }

    /**
     * Simple stack-based fill for smaller areas
     */
    private fun simpleFill(
        canvas: Canvas,
        pixelMap: androidx.compose.ui.graphics.PixelMap,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        targetColor: Int,
        replacementColor: Color,
        config: FloodFillConfig
    ): FloodFillResult {
        val stack = ArrayDeque<Pair<Int, Int>>()
        val visited = Array(height) { BooleanArray(width) }
        var pixelsFilled = 0
        var iterations = 0

        var bounds = IntBounds(startX, startY, startX, startY)

        stack.add(startX to startY)

        while (stack.isNotEmpty() && iterations < config.maxIterations) {
            iterations++
            val (x, y) = stack.removeFirst()

            if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x]) continue

            if (!colorsMatch(pixelMap[x, y].toArgb(), targetColor, config.tolerance)) continue

            visited[y][x] = true
            fillPixel(canvas, x, y, replacementColor)
            pixelsFilled++

            // Update bounds
            bounds = IntBounds(
                min(bounds.left, x),
                min(bounds.top, y),
                max(bounds.right, x),
                max(bounds.bottom, y)
            )

            // Add neighbors
            stack.add((x + 1) to y)
            stack.add((x - 1) to y)
            stack.add(x to (y + 1))
            stack.add(x to (y - 1))
        }

        return FloodFillResult(pixelsFilled, iterations < config.maxIterations, bounds)
    }

    /**
     * Find horizontal span of matching pixels
     */
    private fun findHorizontalSpan(
        pixelMap: androidx.compose.ui.graphics.PixelMap,
        width: Int,
        height: Int,
        startX: Int,
        y: Int,
        targetColor: Int,
        tolerance: Int
    ): Pair<Int, Int>? {
        if (y < 0 || y >= height) return null
        if (!colorsMatch(pixelMap[startX, y].toArgb(), targetColor, tolerance)) return null

        // Find left boundary
        var leftX = startX
        while (leftX > 0 && colorsMatch(pixelMap[leftX - 1, y].toArgb(), targetColor, tolerance)) {
            leftX--
        }

        // Find right boundary
        var rightX = startX
        while (rightX < width - 1 && colorsMatch(pixelMap[rightX + 1, y].toArgb(), targetColor, tolerance)) {
            rightX++
        }

        return leftX to rightX
    }

    /**
     * Fill a horizontal line efficiently
     */
    private fun fillHorizontalLine(
        canvas: Canvas,
        y: Int,
        leftX: Int,
        rightX: Int,
        color: Color
    ) {
        val paint = Paint().apply {
            this.color = color
            style = PaintingStyle.Fill
        }

        // Draw as a rectangle for better performance
        canvas.drawRect(
            left = leftX.toFloat(),
            top = y.toFloat(),
            right = (rightX + 1).toFloat(),
            bottom = (y + 1).toFloat(),
            paint = paint
        )
    }

    /**
     * Fill a single pixel
     */
    private fun fillPixel(canvas: Canvas, x: Int, y: Int, color: Color) {
        val paint = Paint().apply {
            this.color = color
            style = PaintingStyle.Fill
        }

        canvas.drawRect(
            left = x.toFloat(),
            top = y.toFloat(),
            right = (x + 1).toFloat(),
            bottom = (y + 1).toFloat(),
            paint = paint
        )
    }

    /**
     * Check and add spans for adjacent lines
     */
    private fun checkAndAddSpans(
        stack: ArrayDeque<Span>,
        pixelMap: androidx.compose.ui.graphics.PixelMap,
        width: Int,
        height: Int,
        leftX: Int,
        rightX: Int,
        checkY: Int,
        parentY: Int,
        direction: Int,
        targetColor: Int,
        tolerance: Int
    ) {
        if (checkY < 0 || checkY >= height) return

        var x = leftX
        while (x <= rightX) {
            // Skip non-matching pixels
            while (x <= rightX && !colorsMatch(pixelMap[x, checkY].toArgb(), targetColor, tolerance)) {
                x++
            }

            if (x > rightX) break

            // Find the span of matching pixels
            val spanStart = x
            while (x <= rightX && colorsMatch(pixelMap[x, checkY].toArgb(), targetColor, tolerance)) {
                x++
            }
            val spanEnd = x - 1

            stack.add(Span(checkY, spanStart, spanEnd, parentY, direction))
        }
    }

    /**
     * Check if two colors match within tolerance
     */
    private fun colorsMatch(color1: Int, color2: Int, tolerance: Int): Boolean {
        if (tolerance == 0) return color1 == color2

        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        val a1 = (color1 shr 24) and 0xFF

        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        val a2 = (color2 shr 24) and 0xFF

        return kotlin.math.abs(r1 - r2) <= tolerance &&
                kotlin.math.abs(g1 - g2) <= tolerance &&
                kotlin.math.abs(b1 - b2) <= tolerance &&
                kotlin.math.abs(a1 - a2) <= tolerance
    }

    /**
     * Determine if we should use optimized approach based on image size
     */
    private fun shouldUseOptimizedApproach(width: Int, height: Int): Boolean {
        return width * height > 10000 // Use scan-line for images larger than 100x100
    }
}

// Extension function for easier access
fun DrawRenderer.floodFill(
    canvas: Canvas,
    imageBitmap: ImageBitmap,
    x: Int,
    y: Int,
    replacementColor: Color,
    config: FloodFillRenderer.FloodFillConfig = FloodFillRenderer.FloodFillConfig()
): FloodFillRenderer.FloodFillResult {
    return FloodFillRenderer.floodFill(canvas, imageBitmap, x, y, replacementColor, config)
}