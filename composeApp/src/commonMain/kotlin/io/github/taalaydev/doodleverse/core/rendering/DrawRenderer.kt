package io.github.taalaydev.doodleverse.core.rendering

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.copy
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.random.Random

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

    internal fun pathPaint(path: DrawingPath): Paint {
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

            isAntiAlias = true
        }
    }

    internal fun smoothCurvePaint(
        path: DrawingPath,
    ): Paint {
        val brush = path.brush
        return Paint().apply {
            style = PaintingStyle.Stroke
            color = path.color
            strokeWidth = path.size

            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round

            pathEffect = brush.pathEffect?.invoke(path.size)
            blendMode = brush.blendMode
            colorFilter = if (brush.brush != null) {
                ColorFilter.tint(path.color)
            } else {
                null
            }
            alpha = calcOpacity(path.color.alpha, brush.opacityDiff)

            isAntiAlias = true
            filterQuality = androidx.compose.ui.graphics.FilterQuality.High
        }
    }

    internal fun stampPaint(
        path: DrawingPath,
        alpha: Float = 1f,
        rotation: Float = 0f
    ): Paint {
        val brush = path.brush
        return Paint().apply {
            color = path.color
            blendMode = brush.blendMode
            colorFilter = ColorFilter.tint(path.color)
            this.alpha = calcOpacity(path.color.alpha, brush.opacityDiff) * alpha
            isAntiAlias = true
            filterQuality = androidx.compose.ui.graphics.FilterQuality.High
        }
    }

    internal fun pathsToBitmap(
        paths: List<DrawingPath>,
        size: Size,
    ): ImageBitmap {
        val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
        val canvas = Canvas(bitmap)

        paths.forEach { drawingPath ->
            val pathBitmap = renderPathCanvas(canvas, drawingPath, size)
        }

        return bitmap
    }

    fun renderPathCanvas(
        canvas: Canvas,
        drawingPath: DrawingPath,
        canvasSize: Size,
        existingBitmap: ImageBitmap? = null,
        useSmoothing: Boolean = true,
        brushImage: ImageBitmap? = null
    ) {
        val brush = drawingPath.brush

        if (brush.customPainter != null) {
            brush.customPainter.invoke(canvas, canvasSize, drawingPath)
        } else if (brush.brush != null && brushImage != null) {
            // renderBrushStamps(canvas, drawingPath, canvasSize, brushImage, useSmoothing)
            drawBrushStampsBetweenPoints(
                canvas,
                drawingPath.startPoint,
                drawingPath.endPoint,
                pathPaint(drawingPath),
                drawingPath,
                brushImage,
                useSmoothing
            )
        } else {
            val paint = if (useSmoothing && !brush.isShape) {
                smoothCurvePaint(drawingPath)
            } else {
                pathPaint(drawingPath)
            }

            if (useSmoothing && drawingPath.points.size > 2 && !brush.isShape) {
                renderSmoothCurve(canvas, drawingPath, paint)
            } else {
                canvas.drawPath(drawingPath.path, paint)
            }
        }
    }


    private fun renderBrushStamps(
        canvas: Canvas,
        drawingPath: DrawingPath,
        canvasSize: Size,
        brushImage: ImageBitmap,
        useSmoothing: Boolean = true
    ) {
        val brush = drawingPath.brush

        if (drawingPath.points.isEmpty()) return

        // Handle single point case
        if (drawingPath.points.size == 1) {
            val point = drawingPath.points[0].toOffset()
            drawSingleBrushStamp(
                canvas = canvas,
                position = point,
                drawingPath = drawingPath,
                brushImage = brushImage,
                rotation = 0f,
                scale = 1f,
                alpha = 1f
            )
            return
        }

        val baseSpacing = calculateImprovedStampSpacing(drawingPath, useSmoothing, brushImage)
        if (drawingPath.points.size >= 2) {
            renderStampsAlongPath(
                canvas = canvas,
                drawingPath = drawingPath,
                brushImage = brushImage,
                spacing = baseSpacing,
                useSmoothing = useSmoothing
            )
        }
    }

    private fun calculateImprovedStampSpacing(
        drawingPath: DrawingPath,
        useSmoothing: Boolean,
        brushImage: ImageBitmap,
    ): Float {
        val brush = drawingPath.brush
        val brushSize = drawingPath.size

        val baseSpacing = if (useSmoothing) {
            (brushSize * 0.2f).coerceAtMost(8f).coerceAtLeast(1f)
        } else {
            (brushSize * 0.15f).coerceAtMost(4f).coerceAtLeast(0.5f)
        }

        val densityOffset = if (brush.useBrushWidthDensity) {
            brushImage.width
        } else {
            brush.densityOffset
        }
        val densityModifier = (densityOffset.toFloat() / 5f).coerceIn(0.5f, 2f)
        return baseSpacing * densityModifier
    }

    private fun renderStampsAlongPath(
        canvas: Canvas,
        drawingPath: DrawingPath,
        brushImage: ImageBitmap,
        spacing: Float,
        useSmoothing: Boolean
    ) {
        val brush = drawingPath.brush
        val points = drawingPath.points.map { it.toOffset() }

        if (points.size < 2) return

        // Always draw the first point
        drawSingleBrushStamp(
            canvas = canvas,
            position = points.first(),
            drawingPath = drawingPath,
            brushImage = brushImage,
            rotation = 0f,
            scale = 1f,
            alpha = 1f
        )

        // Interpolate between all consecutive points
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]

            drawBrushStampsBetweenPoints(
                canvas = canvas,
                start = start,
                end = end,
                drawingPath = drawingPath,
                brushImage = brushImage,
                paint = pathPaint(drawingPath)
//                spacing = spacing,
//                useSmoothing = useSmoothing
            )
        }

        // Always draw the last point to ensure complete coverage
        drawSingleBrushStamp(
            canvas = canvas,
            position = points.last(),
            drawingPath = drawingPath,
            brushImage = brushImage,
            rotation = 0f,
            scale = 1f,
            alpha = 1f
        )
    }

    private fun renderStampsBetweenPoints(
        canvas: Canvas,
        start: Offset,
        end: Offset,
        drawingPath: DrawingPath,
        brushImage: ImageBitmap,
        spacing: Float,
        useSmoothing: Boolean
    ) {
        val distance = getDistance(start, end)
        if (distance <= spacing) {
            // If distance is small, just draw at the end point
            drawSingleBrushStamp(
                canvas = canvas,
                position = end,
                drawingPath = drawingPath,
                brushImage = brushImage,
                rotation = 0f,
                scale = 1f,
                alpha = 1f
            )
            return
        }

        // Calculate number of steps to ensure complete coverage
        val steps = (distance / spacing).toInt().coerceAtLeast(1)
        val actualSpacing = distance / steps

        // Draw stamps along the line
        for (step in 1..steps) {
            val t = step.toFloat() / steps
            val position = Offset(
                start.x + (end.x - start.x) * t,
                start.y + (end.y - start.y) * t
            )

            val rotation = if (drawingPath.brush.rotationRandomness > 0) {
                val angle = atan2(end.y - start.y, end.x - start.x)
                val randomRotation = (Random.nextFloat() - 0.5f) * 2f * drawingPath.brush.rotationRandomness
                angle + randomRotation
            } else {
                atan2(end.y - start.y, end.x - start.x)
            }

            val progress = t
            val taperFactor = calculateTaperFactor(progress, useSmoothing)
            val scale = calculateStampScale(drawingPath.brush, taperFactor)
            val alpha = calculateStampAlpha(progress, drawingPath.brush, taperFactor)

            drawSingleBrushStamp(
                canvas = canvas,
                position = position,
                drawingPath = drawingPath,
                brushImage = brushImage,
                rotation = rotation,
                scale = scale,
                alpha = alpha
            )
        }
    }

    private fun calculateStampScale(brush: BrushData, taperFactor: Float): Float {
        val baseScale = taperFactor

        return if (brush.sizeRandom.isNotEmpty() && brush.sizeRandom[0] > 0) {
            val randomFactor = (Random.nextFloat() - 0.5f) * 2f * (brush.sizeRandom[0] / 100f)
            (baseScale * (1f + randomFactor)).coerceIn(0.1f, 2f)
        } else {
            baseScale
        }
    }

    private fun drawSingleBrushStamp(
        canvas: Canvas,
        position: Offset,
        drawingPath: DrawingPath,
        brushImage: ImageBitmap,
        rotation: Float = 0f,
        scale: Float = 1f,
        alpha: Float = 1f
    ) {
        val stampSize = drawingPath.size * scale
        val halfSize = stampSize / 2f

        val paint = stampPaint(drawingPath, alpha, rotation)

        if (rotation != 0f || scale != 1f) {
            canvas.save()

            canvas.translate(position.x, position.y)
            if (rotation != 0f) {
                canvas.rotate(toDegrees(rotation.toDouble()).toFloat())
            }
            if (scale != 1f) {
                canvas.scale(scale, scale)
            }
            canvas.translate(-halfSize, -halfSize)

            canvas.drawImageRect(
                image = brushImage,
                dstOffset = IntOffset(0, 0),
                dstSize = IntSize(stampSize.toInt(), stampSize.toInt()),
                paint = paint
            )

            canvas.restore()
        } else {
            canvas.drawImageRect(
                image = brushImage,
                dstOffset = IntOffset(
                    (position.x - halfSize).toInt(),
                    (position.y - halfSize).toInt()
                ),
                dstSize = IntSize(stampSize.toInt(), stampSize.toInt()),
                paint = paint
            )
        }
    }

    private fun toDegrees(radians: Double): Double {
        return radians * (180.0 / PI)
    }

    private fun calculateStampSpacing(
        drawingPath: DrawingPath,
        useSmoothing: Boolean
    ): Float {
        val brush = drawingPath.brush
        val baseSpacing = if (useSmoothing) {
            (drawingPath.size / 4f).coerceAtMost(12f).coerceAtLeast(2f)
        } else {
            drawingPath.size / 6f
        }

        return baseSpacing * (brush.densityOffset.toFloat() / 5f).coerceIn(0.3f, 3f)
    }

    private fun calculateStampAlpha(
        progress: Float,
        brush: BrushData,
        taperFactor: Float
    ): Float {
        val baseAlpha = taperFactor

        val variation = if (brush.random.isNotEmpty() && brush.random[0] > 0) {
            (Random.nextFloat() - 0.5f) * 0.2f * (brush.random[0] / 100f)
        } else {
            0f
        }

        return (baseAlpha + variation).coerceIn(0.1f, 1f)
    }

    private fun renderSmoothCurve(
        canvas: Canvas,
        drawingPath: DrawingPath,
        paint: Paint
    ) {
        val path = drawingPath.path

        if (drawingPath.points.size <= 2) {
            canvas.drawPath(path, paint)
            return
        }

        val pathMeasure = PathMeasure().apply { setPath(path, false) }
        val pathLength = pathMeasure.length

        if (pathLength <= 0) {
            canvas.drawPath(path, paint)
            return
        }

        canvas.drawPath(path, paint)

        if (drawingPath.size > 1f) {
            val smoothPaint = paint.copy().apply {
                alpha = alpha * 0.3f
                strokeWidth = strokeWidth * 0.8f
            }
            canvas.drawPath(path, smoothPaint)
        }
    }

    internal fun drawPath(
        canvas: Canvas,
        drawingPath: DrawingPath,
        paint: Paint,
        size: Size,
        useSmoothing: Boolean = true,
        brushImage: ImageBitmap? = null
    ) {
        val brush = drawingPath.brush

        if (brush.customPainter != null) {
            val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
            val brushCanvas = Canvas(bitmap)
            brush.customPainter.invoke(brushCanvas, size, drawingPath)
            canvas.drawImage(bitmap, Offset.Zero, Paint())
        } else if (brush.brush != null && brushImage != null) {
            renderBrushStamps(canvas, drawingPath, size, brushImage, useSmoothing)
        } else {
            if (useSmoothing && drawingPath.points.size > 2 && !brush.isShape) {
                val smoothPaint = smoothCurvePaint(drawingPath)
                renderSmoothCurve(canvas, drawingPath, smoothPaint)
            } else {
                canvas.drawPath(drawingPath.path, paint)
            }
        }
    }

    internal fun drawBrushStampsBetweenPoints(
        canvas: Canvas,
        start: Offset,
        end: Offset,
        paint: Paint,
        drawingPath: DrawingPath,
        brushImage: ImageBitmap,
        useSmoothing: Boolean = true
    ) {
        val brushSize = drawingPath.size
        val densityOffset = drawingPath.brush.densityOffset.toFloat()
        val useBrushWidthDensity = drawingPath.brush.useBrushWidthDensity

        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length
        val delta = Offset(end.x - start.x, end.y - start.y)

        val halfSize = brushSize / 8
        var i = 0f
        var currentPoint = start

        while (i < length) {
            val point = measure.getPosition(i)
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

    fun blendPathBitmap(
        pathBitmap: ImageBitmap,
        targetBitmap: ImageBitmap,
        blendMode: androidx.compose.ui.graphics.BlendMode = androidx.compose.ui.graphics.BlendMode.SrcOver,
        isEraser: Boolean = false
    ): ImageBitmap {
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

    private fun calculateTaperFactor(progress: Float, useSmoothing: Boolean = true): Float {
        return if (useSmoothing) {
            when {
                progress < 0.15f -> {
                    val normalizedProgress = progress / 0.15f
                    kotlin.math.sin(normalizedProgress * kotlin.math.PI / 2).toFloat()
                }
                progress > 0.85f -> {
                    val normalizedProgress = (1f - progress) / 0.15f
                    kotlin.math.sin(normalizedProgress * kotlin.math.PI / 2).toFloat()
                }
                else -> 1f
            }
        } else {
            when {
                progress < 0.1f -> progress * 10
                progress > 0.9f -> (1 - progress) * 10
                else -> 1f
            }
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

        val visited = Array(height) { BooleanArray(width) }

        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(Pair(x, y))

        while (stack.isNotEmpty()) {
            val (x, y) = stack.removeLast()

            if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x]) continue

            if (pixelMap[x, y].toArgb() != targetColor) continue

            visited[y][x] = true

            canvas.nativeCanvas.drawPoint(
                x.toFloat(),
                y.toFloat(),
                Paint().apply {
                    color = Color(replacement)
                    style = PaintingStyle.Fill
                }.asFrameworkPaint()
            )

            stack.add(Pair(x + 1, y))
            stack.add(Pair(x - 1, y))
            stack.add(Pair(x, y + 1))
            stack.add(Pair(x, y - 1))
        }
    }

    fun createSmoothPath(points: List<Offset>): Path {
        val path = Path()

        if (points.isEmpty()) return path

        path.moveTo(points[0].x, points[0].y)

        when (points.size) {
            1 -> {
                path.addOval(
                    androidx.compose.ui.geometry.Rect(
                        points[0].x - 1f,
                        points[0].y - 1f,
                        points[0].x + 1f,
                        points[0].y + 1f
                    )
                )
            }
            2 -> {
                path.lineTo(points[1].x, points[1].y)
            }
            else -> {
                var i = 1
                while (i < points.size) {
                    if (i == points.size - 1) {
                        path.lineTo(points[i].x, points[i].y)
                    } else {
                        val controlPoint = points[i]
                        val endPoint = Offset(
                            (points[i].x + points[i + 1].x) / 2f,
                            (points[i].y + points[i + 1].y) / 2f
                        )
                        path.quadraticBezierTo(
                            controlPoint.x, controlPoint.y,
                            endPoint.x, endPoint.y
                        )
                    }
                    i++
                }
            }
        }

        return path
    }

    private fun getDistance(point1: Offset, point2: Offset): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        return sqrt(dx * dx + dy * dy)
    }
}

private fun Paint.copy(): Paint {
    return Paint().apply {
        alpha = this@copy.alpha
        isAntiAlias = this@copy.isAntiAlias
        color = this@copy.color
        blendMode = this@copy.blendMode
        style = this@copy.style
        strokeWidth = this@copy.strokeWidth
        strokeCap = this@copy.strokeCap
        strokeJoin = this@copy.strokeJoin
        pathEffect = this@copy.pathEffect
        colorFilter = this@copy.colorFilter
    }
}