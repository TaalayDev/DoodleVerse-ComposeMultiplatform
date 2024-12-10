package io.github.taalaydev.doodleverse.data.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.*
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.stamp_pencil
import io.github.taalaydev.doodleverse.core.PathEffects
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.util.lerp
import io.github.taalaydev.doodleverse.core.DrawRenderer
import io.github.taalaydev.doodleverse.core.getDensityOffsetBetweenPoints
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

private fun calcOpacity(alpha: Float, brushOpacity: Float): Float {
    return DrawRenderer.calcOpacity(alpha, brushOpacity)
}

data class BrushData(
    val id: Int,
    val name: String,
    val stroke: String,
    val brush:  DrawableResource? = null,
    val texture:  DrawableResource? = null,
    val isLocked: Boolean = false,
    val isNew: Boolean = false,
    val opacityDiff: Float = 0f,
    val colorFilter: ColorFilter? = null,
    val strokeCap: StrokeCap = StrokeCap.Butt,
    val strokeJoin: StrokeJoin = StrokeJoin.Round,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val densityOffset: Double = 5.0,
    val useBrushWidthDensity: Boolean = true,
    val random: List<Int> = listOf(0, 0),
    val sizeRandom: List<Int> = listOf(0, 0),
    val rotationRandomness: Float = 0f,
    val pathEffect: ((width: Float) -> PathEffect?)? = null,
    val customPainter: ((canvas: Canvas, size: Size, path: DrawingPath) -> Unit)? = null,
    val isShape: Boolean = false,
) {
    internal fun sizeInPixels(brushSize: Float): Int {
        if (brush != null) {
            return lerp(1, 80, brushSize)
        } else {
            return lerp(1, 80, brushSize)
        }
    }

    companion object {
        val solid = BrushData(
            id = 0,
            name = "Solid",
            stroke = "solid",
            strokeCap = StrokeCap.Round,
            strokeJoin = StrokeJoin.Round,
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeWidth = drawingPath.size
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    style = PaintingStyle.Stroke
                    alpha = drawingPath.color.alpha
                }

                val path = drawingPath.path
                val metrics = PathMeasure().apply { setPath(path, false) }
                val length = metrics.length

                val brushSize = drawingPath.size
                val delta = min(brushSize * 0.2f, 10f)

                var i = 0f
                while (i < length) {
                    val point = metrics.getPosition(i)
                    val nextPoint = metrics.getPosition(i + delta)

                    canvas.drawLine(point, nextPoint, paint)

                    i += delta
                }
            }
        )

        val pencil = BrushData(
            id = 1,
            name = "Pencil",
            stroke = "pencil",
            brush = Res.drawable.stamp_pencil,
            opacityDiff = 0.5f,
            densityOffset = 10.0,
            rotationRandomness = 15f,
            useBrushWidthDensity = true
        )

        val marker = BrushData(
            id = 2,
            name = "Marker",
            stroke = "marker",
            opacityDiff = 0.5f,
            blendMode = BlendMode.Multiply,
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.4f)
                    strokeWidth = drawingPath.size * 1.2f
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    style = PaintingStyle.Stroke
                    alpha = calcOpacity(drawingPath.color.alpha, drawingPath.brush.opacityDiff)
                }

                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                val brushSize = drawingPath.size
                val delta = min(brushSize * 0.2f, 8f)

                // Main stroke
                canvas.drawPath(path, paint)

                // Create marker texture effect
                var i = 0f
                while (i < length) {
                    val point = measure.getPosition(i)

                    // Create parallel strokes for marker texture
                    for (j in -1..1) {
                        val texturePoint = point + Offset(
                            drawingPath.getRandom(listOf(i, point.x, j, 1)) * brushSize * 0.1f,
                            j * brushSize * 0.15f
                        )

                        val nextPoint = if (i + delta < length) {
                            measure.getPosition(i + delta)
                        } else {
                            measure.getPosition(length)
                        }

                        val texturePaint = paint.apply {
                            strokeWidth = drawingPath.size * 0.3f
                            alpha = (alpha * 0.5f)
                        }

                        canvas.drawLine(
                            texturePoint,
                            nextPoint + Offset(
                                drawingPath.getRandom(listOf(i + delta, nextPoint.x, j, 1)) * brushSize * 0.1f,
                                j * brushSize * 0.15f
                            ),
                            texturePaint
                        )
                    }

                    i += delta
                }

                // Add slight variation in pressure
                var pressureI = 0f
                val pressureDelta = brushSize * 0.5f
                while (pressureI < length) {
                    val point = measure.getPosition(pressureI)

                    val pressurePaint = paint.apply {
                        strokeWidth = drawingPath.size * 0.8f
                        alpha = (alpha * 0.3f)
                    }

                    val pressureOffset = Offset(
                        drawingPath.getRandom(listOf(pressureI, point.x, 1)) * brushSize * 0.05f,
                        drawingPath.getRandom(listOf(pressureI, point.y, 2)) * brushSize * 0.05f
                    )

                    val offset = point + pressureOffset
                    canvas.nativeCanvas.drawPoint(offset.x, offset.y, pressurePaint.asFrameworkPaint())

                    pressureI += pressureDelta
                }
            }
        )

        val star = BrushData(
            id = 3,
            name = "Star",
            stroke = "star",
            brush = null,
            pathEffect = { width ->
                PathEffects.createStarPathEffect(
                    width = width,
                    points = 5,
                    advance = 30f,
                    innerRadius = 0.5f,
                    outerRadius = 1.0f
                )
            }
        )

        val zigzag = BrushData(
            id = 5,
            name = "Zigzag",
            stroke = "zigzag",
            strokeCap = StrokeCap.Round,
            strokeJoin = StrokeJoin.Bevel,
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeWidth = drawingPath.size
                    style = PaintingStyle.Stroke
                    strokeCap = drawingPath.brush.strokeCap
                    strokeJoin = drawingPath.brush.strokeJoin
                    alpha = drawingPath.color.alpha
                }

                // Create a PathMeasure and set the path to be measured
                val path = drawingPath.path
                val pathMeasure = PathMeasure().apply {
                    setPath(path, false)
                }
                val pathLength = pathMeasure.length

                // Define the step size for the zigzag effect
                val step = 20f
                var distance = 0f

                // Iterate through the path, applying the zigzag effect
                while (distance < pathLength) {
                    // Get the current position on the path
                    val point = pathMeasure.getPosition(distance)

                    // Generate random values to simulate the zigzag effect
                    val random1 = drawingPath.getRandom(listOf(distance, point.x, point.y, 1))
                    val random2 = drawingPath.getRandom(listOf(distance, point.x, point.y, 2))

                    // Control point with a zigzagging offset
                    val controlPoint = point + Offset(
                        (random1 - 0.5f) * step,  // Slight left/right variation
                        (random2 - 0.5f) * step   // Slight up/down variation
                    )

                    // Move to the next segment in the path
                    distance += step
                    val nextPoint = pathMeasure.getPosition(distance)

                    // Draw the zigzag line
                    canvas.drawLine(point, controlPoint, paint)
                    canvas.drawLine(controlPoint, nextPoint, paint)
                }
            }
        )

        val bubble = BrushData(
            id = 6,
            name = "Bubble",
            stroke = "bubble",
            customPainter = { canvas, size, drawingPath ->
                val measure = PathMeasure().apply { setPath(drawingPath.path, false) }
                val length = measure.length
                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.3f)
                    style = PaintingStyle.Stroke
                    strokeWidth = 1.0f
                    alpha = drawingPath.color.alpha
                }

                for (i in 0 until length.toInt() step 50) {
                    val point = measure.getPosition(i.toFloat())

                    val random1 = drawingPath.getRandom(listOf(point.x, point.y, 1))
                    val random2 = drawingPath.getRandom(listOf(point.y, point.x, 2))
                    val random3 = drawingPath.getRandom(listOf(point.x, point.y, 3))

                    val bubbleRadius = (drawingPath.size / 2) + random1 * drawingPath.size

                    val bubblePosition = point + Offset((random2 * 10f) - 5f, (random3 * 10f) - 5f)

                    canvas.drawCircle(bubblePosition, bubbleRadius, paint)
                }
            }
        )

        val heart = BrushData(
            id = 9,
            name = "Heart",
            stroke = "heart",
            pathEffect = { width ->
                PathEffects.heartPathEffect(width)
            }
        )

        val sketchyPencil = BrushData(
            id = 10,
            name = "Sketchy Pencil",
            stroke = "sketchy_pencil",
            opacityDiff = 0.4f,
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.4f)
                    strokeWidth = drawingPath.size * 0.2f
                    strokeCap = StrokeCap.Round
                    style = PaintingStyle.Stroke
                    alpha = calcOpacity(drawingPath.color.alpha, drawingPath.brush.opacityDiff)
                }

                val path = drawingPath.path
                val metrics = PathMeasure().apply { setPath(path, false) }
                val length = metrics.length

                val brushSize = drawingPath.size
                val delta = min(brushSize * 0.2f, 10f)

                var i = 0f
                while (i < length) {
                    val point = metrics.getPosition(i)

                    for (j in 0..3) {
                        val random1 = Random.nextFloat()
                        val random2 = Random.nextFloat()
                        val random3 = Random.nextFloat()
                        val random4 = Random.nextFloat()

                        val offset = Offset(
                            (random1 - 0.5f) * drawingPath.size * 0.5f,
                            (random2 - 0.5f) * drawingPath.size * 0.5f
                        )

                        // Create the end point with another randomized offset
                        val controlPoint = point + offset
                        val endPoint = point +
                                Offset(
                                (random3 - 0.5f) * 10.0f,
                                (random4 - 0.5f) * 10.0f,
                                )

                        // Draw sketchy lines between the points
                        canvas.drawLine(controlPoint, endPoint, paint)
                    }

                    i += delta
                }
            }
        )

        val softPencilBrush = BrushData(
            id = 11,
            name = "Soft Pencil",
            stroke = "soft_pencil_stroke",
            densityOffset = 1.0,
            opacityDiff = 0.5f,
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.3f)
                    strokeWidth = drawingPath.size * 0.5f
                    strokeCap = StrokeCap.Round
                    style = PaintingStyle.Stroke
                    alpha = calcOpacity(drawingPath.color.alpha, drawingPath.brush.opacityDiff)
                }

                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                val brushSize = drawingPath.size
                val delta = min(brushSize / 3f, 10f)

                var i = 0f
                while (i < length) {
                    val point = measure.getPosition(i)

                    for (j in 0..2) {
                        val x = drawingPath.getRandom(listOf(i, point.x, point.y, j, 1))
                        val y = drawingPath.getRandom(listOf(i, point.x, point.y, j, 2))

                        val offset = Offset(
                            (x - 0.5f) * drawingPath.size * 0.5f,
                            (y - 0.5f) * drawingPath.size * 0.5f
                        )

                        val nextPoint = measure.getPosition(i.toFloat() + 10)

                        canvas.drawLine(point + offset, nextPoint + offset, paint)
                    }

                    i += delta
                }
            }
        )

        val rainbowBrush = BrushData(
            id = 12,
            name = "Rainbow",
            stroke = "rainbow_stroke",
            densityOffset = 1.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path

                // Set up the paint object for rainbow strokes
                val paint = Paint().apply {
                    style = PaintingStyle.Stroke
                    strokeWidth = drawingPath.size
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    alpha = drawingPath.color.alpha
                }

                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                // Define the step size for the rainbow gradient effect
                val step = 10f

                // Iterate through the path, applying the rainbow effect
                for (i in 0 until length.toInt() step step.toInt()) {
                    val point = measure.getPosition(i.toFloat())

                    // Use HSV color space to generate smooth rainbow transitions
                    val hue = (i.toFloat() / length) * 360f // Hue cycles from 0 to 360
                    val color = Color.hsv(hue, 1.0f, 1.0f) // Full saturation and brightness

                    // Set the color of the paint object
                    paint.color = color

                    // Draw the rainbow stroke at each point
                    val nextPoint = measure.getPosition(i.toFloat() + step)
                    canvas.drawLine(point, nextPoint, paint)
                }

            }
        )

        val eraser = BrushData(
            id = 13,
            name = "Eraser",
            stroke = "eraser",
            opacityDiff = 0.3f,
            densityOffset = 18.0,
            blendMode = BlendMode.Clear
        )

        val hardPencilBrush = BrushData(
            id = 14,
            name = "Hard Pencil",
            stroke = "hard_pencil_stroke",
            densityOffset = 1.0,
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeWidth = drawingPath.size * 0.3f
                    strokeCap = StrokeCap.Square
                    style = PaintingStyle.Stroke
                    alpha = drawingPath.color.alpha
                }

                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var i = 0f
                var prevPoint: Offset? = null

                while (i <= length) {
                    val point = measure.getPosition(i)

                    if (prevPoint != null) {
                        canvas.drawLine(prevPoint, point, paint)
                    }

                    prevPoint = point
                    i += 1f  // You can adjust this value to change the density of lines
                }
            }
        )

        val watercolorBrush = BrushData(
            id = 15,
            name = "Watercolor",
            stroke = "watercolor_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.5f,
            sizeRandom = listOf(-20, 20),
            useBrushWidthDensity = false,
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Stroke
                    strokeWidth = drawingPath.size
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    alpha = calcOpacity(drawingPath.color.alpha, drawingPath.brush.opacityDiff)
                }

                // Draw the main stroke
                canvas.drawPath(drawingPath.path, paint)

                // Create a second paint object for the blurred overlay
                val paint2 = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Stroke
                    strokeWidth = drawingPath.size
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    alpha = calcOpacity(drawingPath.color.alpha, drawingPath.brush.opacityDiff)
                }

                // Simulate blur effect by drawing multiple paths with slight offsets
                val blurRadius = 20f
                for (i in 0 until 10) {
                    val offset = Offset(
                        (Random.nextFloat() * 2 - 1) * blurRadius,
                        (Random.nextFloat() * 2 - 1) * blurRadius
                    )
                    canvas.save()
                    canvas.translate(offset.x, offset.y)
                    canvas.drawPath(drawingPath.path, paint2)
                    canvas.restore()
                }
            }
        )

        val crayonBrush = BrushData(
            id = 16,
            name = "Crayon",
            stroke = "crayon_stroke",
            opacityDiff = 0.1f,
            strokeCap = StrokeCap.Round,
            strokeJoin = StrokeJoin.Round,
            random = listOf(-2, 2),
            sizeRandom = listOf(-1, 1),
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.9f)
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    strokeWidth = drawingPath.size
                    style = PaintingStyle.Stroke
                    alpha = calcOpacity(drawingPath.color.alpha, drawingPath.brush.opacityDiff)
                }

                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var prevPoint: Offset? = null
                var distance = 0f

                while (distance <= length) {
                    val point = measure.getPosition(distance)

                    if (prevPoint != null) {
                        // Draw multiple lines with slight random offsets to mimic crayon texture
                        for (j in 0 until 3) {
                            val x = drawingPath.getRandom(listOf(distance, point.x, point.y, j, 1))
                            val y = drawingPath.getRandom(listOf(distance, point.x, point.y, j, 2))
                            val w = drawingPath.getRandom(listOf(distance, point.x, point.y, j, 3))

                            val offset = Offset(
                                (x - 0.5f) * drawingPath.size * 0.3f,
                                (y - 0.5f) * drawingPath.size * 0.3f
                            )

                            canvas.drawLine(
                                prevPoint + offset,
                                point + offset,
                                paint.apply {
                                    strokeWidth = drawingPath.size * (0.8f + w * 0.4f)
                                }
                            )
                        }
                    }

                    prevPoint = point
                    distance += 1f  // You can adjust this value to change the density of lines
                }
            }
        )

        val sprayPaintBrush = BrushData(
            id = 17,
            name = "Spray Paint",
            stroke = "spray_paint_stroke",
            opacityDiff = 0.9f,
            densityOffset = 5.0,
            random = listOf(-5, 5),
            sizeRandom = listOf(-5, 5),
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.1f)
                    style = PaintingStyle.Fill
                    alpha = calcOpacity(drawingPath.color.alpha, drawingPath.brush.opacityDiff)
                }

                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size / 2  // Adjust this value to change the density of spray

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    for (i in 0 until 20) {
                        val dx = point.x + (Random.nextDouble() - 0.5) * drawingPath.size * 4
                        val dy = point.y + (Random.nextDouble() - 0.5) * drawingPath.size * 4
                        val radius = Random.nextDouble() * 1.5f

                        canvas.drawCircle(
                            Offset(dx.toFloat(), dy.toFloat()),
                            radius.toFloat(),
                            paint
                        )
                    }

                    distance += step
                }
            }
        )

        val charcoalBrush = BrushData(
            id = 18,
            name = "Charcoal",
            stroke = "charcoal_stroke",
            opacityDiff = 0.5f,
            densityOffset = 5.0,
            strokeCap = StrokeCap.Square,
            strokeJoin = StrokeJoin.Bevel,
            random = listOf(-3, 3),
            sizeRandom = listOf(-2, 2),
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.5f)
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    strokeWidth = drawingPath.size * 1.5f
                    style = PaintingStyle.Stroke
                    alpha = calcOpacity(drawingPath.color.alpha, drawingPath.brush.opacityDiff)
                }

                val path = Path()
                val measure = PathMeasure().apply { setPath(drawingPath.path, false) }
                val length = measure.length

                var prevPoint: Offset? = null
                var distance = 0f

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += 1f
                        prevPoint = null
                        continue
                    }

                    if (prevPoint == null) {
                        path.moveTo(point.x, point.y)
                    } else {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), 2))

                        // Add random offsets to create a rough, smudged effect
                        val controlPoint = Offset(
                            prevPoint.x + (random1 - 0.5f) * drawingPath.size * 0.5f,
                            prevPoint.y + (random2 - 0.5f) * drawingPath.size * 0.5f
                        )

                        path.quadraticBezierTo(
                            controlPoint.x,
                            controlPoint.y,
                            (prevPoint.x + point.x) / 2,
                            (prevPoint.y + point.y) / 2
                        )
                    }

                    prevPoint = point
                    distance += 1f  // You can adjust this value to change the density of points
                }

                // Simulate blur effect
                val blurRadius = 10f
                for (i in 0 until 10) {
                    val offset = Offset(
                        (Random.nextFloat() * 2 - 1) * blurRadius,
                        (Random.nextFloat() * 2 - 1) * blurRadius
                    )
                    canvas.save()
                    canvas.translate(offset.x, offset.y)
                    canvas.drawPath(path, paint)
                    canvas.restore()
                }
            }
        )

        val sketchyBrush = BrushData(
            id = 19,
            name = "Sketchy",
            stroke = "sketchy_stroke",
            densityOffset = 30.0,
            customPainter = { canvas, size, drawingPath ->
                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    style = PaintingStyle.Stroke
                    alpha = drawingPath.color.alpha
                }

                val originalPath = drawingPath.path
                val measure = PathMeasure().apply { setPath(originalPath, false) }
                val length = measure.length

                for (j in 0 until 3) {
                    val firstPoint = measure.getPosition(0f)
                    if (firstPoint.isUnspecified || firstPoint.x.isNaN() || firstPoint.y.isNaN()) {
                        continue
                    }

                    val random1 = drawingPath.getRandom(listOf(j, firstPoint.x.toInt(), firstPoint.y.toInt(), 1))
                    val random2 = drawingPath.getRandom(listOf(j, firstPoint.y.toInt(), firstPoint.x.toInt(), 2))
                    val random3 = drawingPath.getRandom(listOf(j, firstPoint.x.toInt(), firstPoint.y.toInt(), 3))

                    paint.strokeWidth = drawingPath.size * (0.5f + random1 * 0.5f)

                    val path = Path()
                    path.moveTo(
                        firstPoint.x + (random2 - 0.5f) * drawingPath.size * 0.5f,
                        firstPoint.y + (random3 - 0.5f) * drawingPath.size * 0.5f
                    )

                    var prevPoint = firstPoint
                    var distance = 0f
                    val step = 5f // Adjust this value to change the density of points

                    while (distance <= length) {
                        val point = measure.getPosition(distance)
                        if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                            distance += step
                            continue
                        }

                        val random4 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), j, 4))
                        val random5 = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), point.x.toInt(), j, 5))
                        val random6 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), j, 6))
                        val random7 = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), point.x.toInt(), j, 7))

                        path.quadraticBezierTo(
                            prevPoint.x + (random4 - 0.5f) * drawingPath.size * 0.5f,
                            prevPoint.y + (random5 - 0.5f) * drawingPath.size * 0.5f,
                            (prevPoint.x + point.x) / 2 + (random6 - 0.5f) * drawingPath.size * 0.5f,
                            (prevPoint.y + point.y) / 2 + (random7 - 0.5f) * drawingPath.size * 0.5f
                        )

                        prevPoint = point
                        distance += step
                    }

                    canvas.drawPath(path, paint)
                }
            }
        )

        val glitterBrush = BrushData(
            id = 20,
            name = "Glitter",
            stroke = "glitter_stroke",
            densityOffset = 20.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = min(drawingPath.size / 2, 10f)  // Adjust this value to change the density of glitter

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val random1 = drawingPath.getRandom(listOf(point.x.toInt(), point.y.toInt(), 1))
                    val random2 = drawingPath.getRandom(listOf(point.y.toInt(), point.x.toInt(), 2))
                    val random3 = drawingPath.getRandom(listOf(point.x.toInt(), point.y.toInt(), 3))
                    val random4 = drawingPath.getRandom(listOf(point.y.toInt(), point.x.toInt(), 4))

                    val paint = Paint().apply {
                        color = drawingPath.color.copy(alpha = random1)
                        style = PaintingStyle.Fill
                        alpha = drawingPath.color.alpha
                    }

                    val glitterSize = random2 * drawingPath.size / 2

                    canvas.drawCircle(
                        center = Offset(
                            point.x + random3 * 10 - 5,
                            point.y + random4 * 10 - 5
                        ),
                        radius = glitterSize,
                        paint = paint
                    )

                    distance += step
                }
            }
        )

        val grassBrush = BrushData(
            id = 22,
            name = "Grass",
            stroke = "grass_stroke",
            densityOffset = 8.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size / 2  // Adjust this value to change the density of grass blades

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val randomLength = drawingPath.getRandom(listOf(point.x.toInt(), point.y.toInt(), 1))
                    val randomAngle = drawingPath.getRandom(listOf(point.y.toInt(), point.x.toInt(), 2))
                    val randomCurvature = drawingPath.getRandom(listOf(point.x.toInt(), point.y.toInt(), 3))
                    val randomColorVariation = drawingPath.getRandom(listOf(point.y.toInt(), point.x.toInt(), 4))

                    val paint = Paint().apply {
                        color = drawingPath.color.copy(alpha = randomColorVariation)
                        style = PaintingStyle.Stroke
                        strokeWidth = drawingPath.size * 0.1f
                        strokeCap = StrokeCap.Round
                        alpha = drawingPath.color.alpha
                    }

                    val bladeLength = drawingPath.size * 2 * (0.5f + randomLength * 0.5f)
                    val angle = (randomAngle - 0.5f) * PI.toFloat() / 6f // -30 to +30 degrees
                    val curvature = (randomCurvature - 0.5f) * bladeLength / 2

                    val bladePath = Path().apply {
                        moveTo(point.x, point.y)
                        quadraticBezierTo(
                            point.x + curvature * cos(angle),
                            point.y - bladeLength / 2,
                            point.x + curvature * cos(angle),
                            point.y - bladeLength
                        )
                    }

                    canvas.drawPath(bladePath, paint)

                    distance += step
                }
            }
        )

        val pixelBrush = BrushData(
            id = 23,
            name = "Pixel",
            stroke = "pixel_stroke",
            densityOffset = 5.0,
            customPainter = { canvas, size, drawingPath ->
                val pixelSize = ceil(drawingPath.size).toInt()
                val paint = Paint().apply {
                    color = drawingPath.color
                    alpha = drawingPath.color.alpha
                }

                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var prevPoint: Offset? = null
                var distance = 0f
                val step = pixelSize / 2f  // Adjust this value to change the density of pixels

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val x = point.x - (point.x % pixelSize)
                    val y = point.y - (point.y % pixelSize)

                    if (prevPoint != null) {
                        Offset(x, y).getDensityOffsetBetweenPoints(
                            startPoint = prevPoint,
                            density = pixelSize.toFloat()
                        ) { offset ->
                            canvas.drawRect(
                                Rect(
                                    left = offset.x - (offset.x % pixelSize),
                                    top = offset.y - (offset.y % pixelSize),
                                    right = offset.x - (offset.x % pixelSize) + pixelSize,
                                    bottom = offset.y - (offset.y % pixelSize) + pixelSize
                                ),
                                paint
                            )
                        }
                    } else {
                        canvas.drawRect(
                            Rect(
                                left = x,
                                top = y,
                                right = x + pixelSize,
                                bottom = y + pixelSize
                            ),
                            paint
                        )
                    }

                    prevPoint = Offset(x, y)
                    distance += step
                }
            }
        )

        val mosaicBrush = BrushData(
            id = 25,
            name = "Mosaic",
            stroke = "mosaic_stroke",
            densityOffset = 10.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.75f  // Adjust this value to change the density of mosaic tiles

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val tileSize = drawingPath.size * 1.5f
                    val rect = Rect(
                        left = point.x - tileSize / 2,
                        top = point.y - tileSize / 2,
                        right = point.x + tileSize / 2,
                        bottom = point.y + tileSize / 2
                    )

                    val paint = Paint().apply {
                        color = drawingPath.color.copy(alpha = 0.7f)
                        style = PaintingStyle.Fill
                        alpha = drawingPath.color.alpha
                    }
                    canvas.drawRect(rect, paint)

                    // Add inner details
                    val innerPaint = Paint().apply {
                        color = drawingPath.color.copy(alpha = 0.3f)
                        style = PaintingStyle.Stroke
                        strokeWidth = 1f
                        alpha = drawingPath.color.alpha
                    }
                    canvas.drawLine(
                        p1 = Offset(rect.left + tileSize * 0.2f, rect.top + tileSize * 0.2f),
                        p2 = Offset(rect.right - tileSize * 0.2f, rect.bottom - tileSize * 0.2f),
                        paint = innerPaint
                    )
                    canvas.drawLine(
                        p1 = Offset(rect.right - tileSize * 0.2f, rect.top + tileSize * 0.2f),
                        p2 = Offset(rect.left + tileSize * 0.2f, rect.bottom - tileSize * 0.2f),
                        paint = innerPaint
                    )

                    distance += step
                }
            }
        )

        val splatBrush = BrushData(
            id = 26,
            name = "Splat",
            stroke = "splat_stroke",
            densityOffset = 30.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 2  // Adjust this value to change the density of splats

                while (distance <= length) {
                    val center = measure.getPosition(distance)
                    if (center.isUnspecified || center.x.isNaN() || center.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val random = listOf(
                        drawingPath.getRandom(listOf(center.x.toInt(), center.y.toInt(), 1)),
                        drawingPath.getRandom(listOf(center.y.toInt(), center.x.toInt(), 2)),
                        drawingPath.getRandom(listOf(center.x.toInt(), center.y.toInt(), 3)),
                        drawingPath.getRandom(listOf(center.y.toInt(), center.x.toInt(), 4)),
                        drawingPath.getRandom(listOf(center.x.toInt(), center.y.toInt(), 5))
                    )

                    for (i in 0 until 5) {
                        val radius = drawingPath.size * (0.5f + random[i] * 1.5f)
                        val angle = random[i] * 2 * PI.toFloat()
                        val offset = Offset(cos(angle) * radius, sin(angle) * radius)
                        val paint = Paint().apply {
                            color = drawingPath.color.copy(alpha = 0.3f + random[i] * 0.4f)
                            style = PaintingStyle.Fill
                            alpha = drawingPath.color.alpha
                        }

                        val splatPath = Path().apply {
                            moveTo(center.x + offset.x, center.y + offset.y)

                            for (j in 0 until 5) {
                                val controlAngle = angle + (j / 5f) * 2 * PI.toFloat()
                                val controlRadius = radius * (0.8f + random[(i + j) % 5] * 0.4f)
                                val controlPoint = center + Offset(
                                    cos(controlAngle) * controlRadius,
                                    sin(controlAngle) * controlRadius
                                )
                                quadraticBezierTo(
                                    controlPoint.x,
                                    controlPoint.y,
                                    center.x + cos(angle + ((j + 1) / 5f) * 2 * PI.toFloat()) * radius,
                                    center.y + sin(angle + ((j + 1) / 5f) * 2 * PI.toFloat()) * radius
                                )
                            }
                            close()
                        }

                        canvas.drawPath(splatPath, paint)
                    }

                    distance += step
                }
            }
        )

        fun createRadialGradientShader(
            center: Offset,
            radius: Float,
            colors: List<Color>,
            stops: List<Float>
        ): Shader {
            return RadialGradientShader(
                center = center,
                radius = radius,
                colors = colors,
                colorStops = stops,
                tileMode = TileMode.Clamp,
            )
        }

        val galaxyBrush = BrushData(
            id = 30,
            name = "Galaxy",
            stroke = "galaxy_stroke",
            densityOffset = 10.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 2  // Adjust this value to change the density of galaxy effects

                while (distance <= length) {
                    val center = measure.getPosition(distance)
                    if (center.isUnspecified || center.x.isNaN() || center.y.isNaN()) {
                        distance += step
                        continue
                    }

                    // Create a radial gradient for the galaxy background
                    val gradientRadius = drawingPath.size * 2
                    val gradientShader = createRadialGradientShader(
                        center = center,
                        radius = gradientRadius,
                        colors = listOf(
                            drawingPath.color,
                            drawingPath.color.copy(alpha = 0.7f),
                            drawingPath.color.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        stops = listOf(0f, 0.3f, 0.7f, 1f)
                    )

                    val paint = Paint().apply {
                        shader = gradientShader
                        alpha = drawingPath.color.alpha
                    }

                    canvas.drawCircle(center, drawingPath.size * 2, paint)

                    // Add stars
                    for (i in 0 until 20) {
                        val randomOffset = List(20) { j ->
                            drawingPath.getRandom(listOf(i, center.x.toInt(), center.y.toInt(), j))
                        }

                        val starOffset = Offset(
                            center.x + (randomOffset[i] - 0.5f) * drawingPath.size * 4,
                            center.y + (randomOffset[(i + 1) % 20] - 0.5f) * drawingPath.size * 4
                        )
                        val starSize = drawingPath.size * 0.1f * randomOffset[(i + 2) % 20]
                        val starOpacity = 0.5f + randomOffset[(i + 3) % 20] * 0.5f
                        val starColor = Color.hsl(
                            hue = 0f,
                            saturation = 0f,
                            lightness = 0.7f + randomOffset[(i + 4) % 20] * 0.3f,
                            alpha = starOpacity
                        )

                        val starPaint = Paint().apply {
                            color = starColor
                            style = PaintingStyle.Fill
                            alpha = drawingPath.color.alpha
                        }

                        canvas.drawCircle(starOffset, starSize, starPaint)
                    }

                    distance += step
                }
            }
        )

        val fireBrush = BrushData(
            id = 32,
            name = "Fire",
            stroke = "fire_stroke",
            densityOffset = 10.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size / 2  // Adjust this value to change the density of flames

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val randomSize = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 1))
                    val randomAngle = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), point.x.toInt(), 2))
                    val randomOpacity = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 3))
                    val randomColorVariation = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), point.x.toInt(), 4))

                    val flameHeight = drawingPath.size * (1.0f + randomSize)
                    val flameWidth = drawingPath.size * (0.5f + randomSize * 0.5f)

                    val shader = LinearGradientShader(
                        point,
                        Offset(point.x, point.y - flameHeight),
                        colors = listOf(
                            drawingPath.color.copy(alpha = 0.5f + randomOpacity * 0.5f),
                            drawingPath.color.copy(alpha = 0.3f + randomOpacity * 0.7f),
                            drawingPath.color.copy(alpha = 0.1f),
                        ),
                        colorStops = listOf(0f, 0.5f, 1f),
                    )

                    val paint = Paint().apply {
                        this.shader = shader
                        blendMode = BlendMode.Screen
                        alpha = drawingPath.color.alpha
                    }

                    val flamePath = Path().apply {
                        moveTo(point.x, point.y)
                        quadraticBezierTo(
                            point.x - flameWidth / 2,
                            point.y - flameHeight / 2,
                            point.x,
                            point.y - flameHeight
                        )
                        quadraticBezierTo(
                            point.x + flameWidth / 2,
                            point.y - flameHeight / 2,
                            point.x,
                            point.y
                        )
                        close()
                    }

                    canvas.drawPath(flamePath, paint)

                    distance += step
                }
            }
        )

        val snowflakeBrush = BrushData(
            id = 33,
            name = "Snowflake",
            stroke = "snowflake_stroke",
            densityOffset = 15.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 1.5f  // Adjust this value to change the density of snowflakes

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val randomSize = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 1))
                    val randomRotation = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), point.x.toInt(), 2))
                    val randomOpacity = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 3))

                    val size = drawingPath.size * (0.5f + randomSize * 1.0f)

                    val paint = Paint().apply {
                        color = drawingPath.color.copy(alpha = 0.5f + randomOpacity * 0.5f)
                        style = PaintingStyle.Stroke
                        strokeWidth = size * 0.1f
                        alpha = drawingPath.color.alpha
                    }

                    val snowflakePath = Path().apply {
                        moveTo(point.x, point.y - size)
                        lineTo(point.x, point.y + size)
                        moveTo(point.x - size, point.y)
                        lineTo(point.x + size, point.y)
                        moveTo(point.x - size, point.y - size)
                        lineTo(point.x + size, point.y + size)
                        moveTo(point.x + size, point.y - size)
                        lineTo(point.x - size, point.y + size)
                    }

                    canvas.drawPath(snowflakePath, paint)

                    distance += step
                }
            }
        )

        val cloudBrush = BrushData(
            id = 35,
            name = "Cloud",
            stroke = "cloud_stroke",
            densityOffset = 5.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.5f  // Adjust this value to change the density of clouds

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val randomSize = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 1))
                    val randomOffsetX = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), point.x.toInt(), 2))
                    val randomOffsetY = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 3))

                    val cloudSize = drawingPath.size * (2.0f + randomSize * 2.0f)

                    val paint = Paint().apply {
                        color = drawingPath.color.copy(alpha = 0.5f)
                        style = PaintingStyle.Fill
                        alpha = drawingPath.color.alpha
                    }

                    val offset = Offset(
                        point.x + (randomOffsetX - 0.5f) * cloudSize * 0.5f,
                        point.y + (randomOffsetY - 0.5f) * cloudSize * 0.5f
                    )

                    canvas.drawCircle(offset, cloudSize * 0.3f, paint)
                    canvas.drawCircle(
                        Offset(offset.x + cloudSize * 0.2f, offset.y - cloudSize * 0.1f),
                        cloudSize * 0.25f,
                        paint
                    )
                    canvas.drawCircle(
                        Offset(offset.x - cloudSize * 0.2f, offset.y - cloudSize * 0.1f),
                        cloudSize * 0.25f,
                        paint
                    )
                    canvas.drawCircle(
                        Offset(offset.x, offset.y - cloudSize * 0.2f),
                        cloudSize * 0.2f,
                        paint
                    )

                    distance += step
                }
            }
        )

        val confettiBrush = BrushData(
            id = 39,
            name = "Confetti",
            stroke = "confetti_stroke",
            densityOffset = 10.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.5f  // Adjust this value to change the density of confetti

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val random1 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 1))
                    val random2 = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), point.x.toInt(), 2))
                    val random3 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 3))

                    val confettiSize = drawingPath.size * (0.5f + random1)
                    val rotation = random2 * 360f  // Degrees for Compose
                    val hueShift = random3 * 360f

                    val paint = Paint().apply {
                        color = Color.hsv(hueShift, 0.8f, 1.0f)
                        style = PaintingStyle.Fill
                        alpha = drawingPath.color.alpha
                    }

                    canvas.withSave {
                        canvas.translate(point.x, point.y)
                        canvas.rotate(rotation)

                        val rect = Rect(
                            offset = Offset(-confettiSize / 2, -confettiSize / 4),
                            size = Size(confettiSize, confettiSize / 2)
                        )
                        canvas.drawRect(rect, paint)
                    }

                    distance += step
                }
            }
        )

        val particleFieldBrush = BrushData(
            id = 44,
            name = "Particle Field",
            stroke = "particle_field_stroke",
            densityOffset = 10.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 2  // Adjust this value to change the density of particle fields

                while (distance <= length) {
                    val center = measure.getPosition(distance)
                    if (center.isUnspecified || center.x.isNaN() || center.y.isNaN()) {
                        distance += step
                        continue
                    }

                    for (i in 0 until 50) {
                        val random = List(50) { j ->
                            drawingPath.getRandom(listOf(i, center.x.toInt(), center.y.toInt(), j))
                        }

                        val radius = drawingPath.size * 2 * random[i]
                        val angle = random[(i + 1) % 50] * 2 * PI.toFloat()
                        val particleOffset = Offset(
                            center.x + cos(angle) * radius,
                            center.y + sin(angle) * radius
                        )

                        val particleSize = drawingPath.size * 0.2f * random[(i + 2) % 50]
                        val particleOpacity = 0.1f + random[(i + 3) % 50] * 0.4f

                        val particlePaint = Paint().apply {
                            color = drawingPath.color.copy(alpha = particleOpacity)
                            style = PaintingStyle.Fill
                            alpha = drawingPath.color.alpha
                        }

                        canvas.drawCircle(particleOffset, particleSize, particlePaint)
                    }

                    distance += step
                }
            }
        )

        val stainedGlassBrush = BrushData(
            id = 42,
            name = "Stained Glass",
            stroke = "stained_glass_stroke",
            densityOffset = 20.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 1.5f  // Adjust this value to change the density of glass pieces

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified || point.x.isNaN() || point.y.isNaN()) {
                        distance += step
                        continue
                    }

                    val random1 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 1))
                    val random2 = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), point.x.toInt(), 2))
                    val random3 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), point.y.toInt(), 3))

                    val hueShift = random1 * 360f
                    val shapeType = random2
                    val rotation = random3 * 360f  // Degrees for Compose

                    val baseColor = Color.hsv(hueShift, 0.7f, 0.9f)

                    canvas.withSave {
                        canvas.translate(point.x, point.y)
                        canvas.rotate(rotation)

                        val shapeSize = drawingPath.size * 1.5f

                        val shapePath = Path().apply {
                            when {
                                shapeType < 0.33f -> {
                                    // Triangle
                                    moveTo(0f, -shapeSize / 2)
                                    lineTo(shapeSize / 2, shapeSize / 2)
                                    lineTo(-shapeSize / 2, shapeSize / 2)
                                }
                                shapeType < 0.66f -> {
                                    // Rectangle
                                    addRect(Rect(-shapeSize/2, -shapeSize/2, shapeSize/2, shapeSize/2))
                                }
                                else -> {
                                    // Circle
                                    addOval(Rect(-shapeSize/2, -shapeSize/2, shapeSize/2, shapeSize/2))
                                }
                            }
                            close()
                        }

                        // Simulate glass texture
                        val glassShader = RadialGradientShader(
                            Offset(0f, 0f),
                            radius = shapeSize / 2f,
                            colors = listOf(
                                baseColor.copy(alpha = 0.8f),
                                baseColor.copy(alpha = 0.4f),
                                baseColor.copy(alpha = 0.1f)
                            ),
                            colorStops = listOf(0f, 0.7f, 1f),
                        )

                        val glassPaint = Paint().apply {
                            shader = glassShader
                            style = PaintingStyle.Fill
                            alpha = drawingPath.color.alpha
                        }

                        canvas.drawPath(shapePath, glassPaint)

                        // Draw leading (the black lines between glass pieces)
                        val leadingPaint = Paint().apply {
                            color = Color.Black
                            style = PaintingStyle.Stroke
                            strokeWidth = drawingPath.size * 0.1f
                            alpha = drawingPath.color.alpha
                        }

                        canvas.drawPath(shapePath, leadingPaint)
                    }

                    distance += step
                }
            }
        )

        val flowFieldBrush = BrushData(
            id = 83,
            name = "Flow Field",
            stroke = "flow_field_stroke",
            densityOffset = 8.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.5f

                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeWidth = drawingPath.size * 0.2f
                    strokeCap = StrokeCap.Round
                    style = PaintingStyle.Stroke
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create flowing lines influenced by noise
                    for (i in 0..5) {
                        var currentPoint = point
                        val flowPath = Path()
                        flowPath.moveTo(currentPoint.x, currentPoint.y)

                        // Generate flow line
                        for (j in 0..10) {
                            val noise1 = drawingPath.getRandom(listOf(currentPoint.x.toInt(), currentPoint.y.toInt(), i, j, 1))
                            val noise2 = drawingPath.getRandom(listOf(currentPoint.x.toInt(), currentPoint.y.toInt(), i, j, 2))

                            val angle = noise1 * PI.toFloat() * 2
                            val length = drawingPath.size * 0.5f

                            currentPoint += Offset(
                                cos(angle) * length,
                                sin(angle) * length
                            )

                            flowPath.lineTo(currentPoint.x, currentPoint.y)
                        }

                        canvas.drawPath(
                            flowPath,
                            paint.apply {
                                alpha = 0.1f + (distance / length) * 0.2f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val dotCloudBrush = BrushData(
            id = 82,
            name = "Dot Cloud",
            stroke = "dot_cloud_stroke",
            densityOffset = 12.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.3f

                while (distance <= length) {
                    val center = measure.getPosition(distance)
                    if (center.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create cloud of dots around path
                    for (i in 0..8) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                        val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                        val radius = drawingPath.size * random1 * 2
                        val angle = random2 * PI.toFloat() * 2
                        val dotSize = drawingPath.size * 0.2f * random3

                        val position = Offset(
                            center.x + cos(angle) * radius,
                            center.y + sin(angle) * radius
                        )

                        val paint = Paint().apply {
                            color = drawingPath.color.copy(
                                alpha = 0.2f + random3 * 0.3f
                            )
                            style = PaintingStyle.Fill
                        }

                        canvas.drawCircle(position, dotSize, paint)
                    }

                    distance += step
                }
            }
        )

        val pencilShadingBrush = BrushData(
            id = 90,
            name = "Pencil Shading",
            stroke = "pencil_shading_stroke",
            densityOffset = 3.0,
            opacityDiff = 0.6f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.1f

                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.3f)
                    strokeWidth = 1f
                    strokeCap = StrokeCap.Round
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create hatching lines for shading
                    for (i in 0..4) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                        val lineLength = drawingPath.size * (0.5f + random1 * 0.5f)
                        val angle = PI.toFloat() / 4f + random2 * PI.toFloat() / 6f // 45° ±30°

                        val startPoint = point + Offset(
                            cos(angle) * lineLength * -0.5f,
                            sin(angle) * lineLength * -0.5f
                        )
                        val endPoint = point + Offset(
                            cos(angle) * lineLength * 0.5f,
                            sin(angle) * lineLength * 0.5f
                        )

                        canvas.drawLine(
                            startPoint,
                            endPoint,
                            paint.apply {
                                alpha = 0.1f + random1 * 0.2f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val blendingBrush = BrushData(
            id = 91,
            name = "Blending",
            stroke = "blending_stroke",
            densityOffset = 5.0,
            opacityDiff = 0.3f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.5f

                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.1f)
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val center = measure.getPosition(distance)
                    if (center.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create soft circular strokes for blending
                    for (i in 0..5) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                        val radius = drawingPath.size * (0.5f + random1 * 1.0f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size,
                            (random2 - 0.5f) * drawingPath.size
                        )

                        canvas.drawCircle(
                            center + offset,
                            radius,
                            paint.apply {
                                alpha = 0.05f + random1 * 0.05f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val textureBlendBrush = BrushData(
            id = 93,
            name = "Texture Blend",
            stroke = "texture_blend_stroke",
            densityOffset = 8.0,
            opacityDiff = 0.4f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.3f

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create textured effect
                    for (i in 0..12) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                        val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                        val size = drawingPath.size * (0.2f + random1 * 0.3f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size * 2,
                            (random3 - 0.5f) * drawingPath.size * 2
                        )

                        when (i % 3) {
                            0 -> canvas.drawCircle(
                                point + offset,
                                size,
                                paint.apply { alpha = 0.05f + random1 * 0.05f }
                            )
                            1 -> {
                                val rect = Rect(
                                    point + offset - Offset(size, size),
                                    Size(size * 2, size * 2)
                                )
                                canvas.drawRect(rect, paint.apply { alpha = 0.03f + random1 * 0.03f })
                            }
                            2 -> {
                                val dotPath = Path().apply {
                                    moveTo(point.x + offset.x, point.y + offset.y - size)
                                    lineTo(point.x + offset.x + size, point.y + offset.y)
                                    lineTo(point.x + offset.x, point.y + offset.y + size)
                                    lineTo(point.x + offset.x - size, point.y + offset.y)
                                    close()
                                }
                                canvas.drawPath(dotPath, paint.apply { alpha = 0.04f + random1 * 0.04f })
                            }
                        }
                    }

                    distance += step
                }
            }
        )

        val crossHatchBrush = BrushData(
            id = 94,
            name = "Cross Hatch",
            stroke = "cross_hatch_stroke",
            densityOffset = 4.0,
            opacityDiff = 0.5f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.3f

                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.4f)
                    strokeWidth = 1f
                    strokeCap = StrokeCap.Round
                }

                // Define hatch angles
                val angles = listOf(
                    PI.toFloat() / 4f,     // 45°
                    -PI.toFloat() / 4f,    // -45°
                    PI.toFloat() / 2f,     // 90°
                    0f                     // 0°
                )

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    angles.forEach { baseAngle ->
                        val lineLength = drawingPath.size * 0.8f

                        // Add slight randomness to angle
                        val random = drawingPath.getRandom(listOf(distance.toInt(), baseAngle.toInt(), 1))
                        val angle = baseAngle + (random - 0.5f) * PI.toFloat() / 12f // ±15°

                        val startPoint = point + Offset(
                            cos(angle) * lineLength * -0.5f,
                            sin(angle) * lineLength * -0.5f
                        )
                        val endPoint = point + Offset(
                            cos(angle) * lineLength * 0.5f,
                            sin(angle) * lineLength * 0.5f
                        )

                        canvas.drawLine(
                            startPoint,
                            endPoint,
                            paint.apply {
                                alpha = 0.1f + random * 0.1f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val oilBrush = BrushData(
            id = 100,
            name = "Oil Paint",
            stroke = "oil_paint_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.4f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = 0.2f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create impasto effect with multiple overlapping strokes
                    for (i in 0..5) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                        val brushWidth = drawingPath.size * (0.8f + random1 * 0.4f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size * 0.5f,
                            (random2 - 0.5f) * drawingPath.size * 0.5f
                        )

                        if (prevPoint != null) {
                            val controlPoint = prevPoint + (point - prevPoint) * 0.5f + offset
                            val strokePath = Path().apply {
                                moveTo(prevPoint!!.x, prevPoint!!.y)
                                quadraticBezierTo(
                                    controlPoint.x, controlPoint.y,
                                    point.x, point.y
                                )
                            }

                            canvas.drawPath(
                                strokePath,
                                paint.apply {
                                    strokeWidth = brushWidth
                                    alpha = 0.1f + random1 * 0.1f
                                }
                            )
                        }
                    }

                    prevPoint = point
                    distance += step
                }
            }
        )

        val wetBrush = BrushData(
            id = 101,
            name = "Wet Paint",
            stroke = "wet_paint_stroke",
            densityOffset = 3.0,
            opacityDiff = 0.6f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create wet paint spreading effect
                    for (i in 0..8) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                        val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                        val dropSize = drawingPath.size * (0.3f + random1 * 0.7f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size * 2f,
                            (random3 - 0.5f) * drawingPath.size * 2f
                        )

                        // Create irregular water drops
                        val dropPath = Path().apply {
                            val centerPoint = point + offset
                            moveTo(centerPoint.x, centerPoint.y - dropSize)

                            for (angle in 0..360 step 45) {
                                val angleRad = angle * PI.toFloat() / 180f
                                val randomRadius = dropSize * (0.8f + random1 * 0.4f)
                                val x = centerPoint.x + cos(angleRad) * randomRadius
                                val y = centerPoint.y + sin(angleRad) * randomRadius

                                if (angle == 0) {
                                    moveTo(x, y)
                                } else {
                                    lineTo(x, y)
                                }
                            }
                            close()
                        }

                        canvas.drawPath(
                            dropPath,
                            paint.apply {
                                alpha = 0.05f + random1 * 0.05f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val acrylicBrush = BrushData(
            id = 102,
            name = "Acrylic",
            stroke = "acrylic_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.3f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeCap = StrokeCap.Round
                    strokeJoin = StrokeJoin.Round
                    style = PaintingStyle.Stroke
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    if (prevPoint != null) {
                        // Create textured stroke effect
                        for (i in 0..3) {
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                            val strokeWidth = drawingPath.size * (0.7f + random1 * 0.6f)
                            val offset = Offset(
                                (random2 - 0.5f) * drawingPath.size * 0.3f,
                                (random2 - 0.5f) * drawingPath.size * 0.3f
                            )

                            val strokePath = Path().apply {
                                moveTo(prevPoint!!.x, prevPoint!!.y)
                                lineTo(point.x + offset.x, point.y + offset.y)
                            }

                            canvas.drawPath(
                                strokePath,
                                paint.apply {
                                    this.strokeWidth = strokeWidth
                                    alpha = 0.3f + random1 * 0.2f
                                }
                            )
                        }
                    }

                    prevPoint = point
                    distance += step
                }
            }
        )

        val glazingBrush = BrushData(
            id = 103,
            name = "Glazing",
            stroke = "glazing_stroke",
            densityOffset = 4.0,
            opacityDiff = 0.8f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.3f

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create translucent glazing layers
                    for (i in 0..4) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                        val radius = drawingPath.size * (1f + random1 * 1f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size * 0.5f,
                            (random2 - 0.5f) * drawingPath.size * 0.5f
                        )

                        canvas.drawCircle(
                            point + offset,
                            radius,
                            paint.apply {
                                alpha = 0.03f + random1 * 0.02f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val impastoBrush = BrushData(
            id = 104,
            name = "Impasto",
            stroke = "impasto_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.2f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create thick, textured strokes
                    for (i in 0..6) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                        val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                        val strokeWidth = drawingPath.size * (0.5f + random1 * 0.5f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size,
                            (random3 - 0.5f) * drawingPath.size
                        )

                        if (prevPoint != null) {
                            val angle = atan2(
                                point.y - prevPoint.y,
                                point.x - prevPoint.x
                            )

                            val perpendicular = angle + PI.toFloat() / 2
                            val p1 = point + offset + Offset(
                                cos(perpendicular) * strokeWidth,
                                sin(perpendicular) * strokeWidth
                            )
                            val p2 = point + offset - Offset(
                                cos(perpendicular) * strokeWidth,
                                sin(perpendicular) * strokeWidth
                            )
                            val p3 = prevPoint + offset - Offset(
                                cos(perpendicular) * strokeWidth,
                                sin(perpendicular) * strokeWidth
                            )
                            val p4 = prevPoint + offset + Offset(
                                cos(perpendicular) * strokeWidth,
                                sin(perpendicular) * strokeWidth
                            )

                            val strokePath = Path().apply {
                                moveTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                lineTo(p3.x, p3.y)
                                lineTo(p4.x, p4.y)
                                close()
                            }

                            canvas.drawPath(
                                strokePath,
                                paint.apply {
                                    alpha = 0.2f + random1 * 0.3f
                                }
                            )
                        }
                    }

                    prevPoint = point
                    distance += step
                }
            }
        )

        val spongeTextureBrush = BrushData(
            id = 105,
            name = "Sponge Texture",
            stroke = "sponge_texture_stroke",
            densityOffset = 3.0,
            opacityDiff = 0.5f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create sponged texture effect
                    for (i in 0..15) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                        val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                        val size = drawingPath.size * (0.2f + random1 * 0.3f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size * 2,
                            (random3 - 0.5f) * drawingPath.size * 2
                        )

                        // Create irregular texture dots
                        when ((random1 * 3).toInt()) {
                            0 -> canvas.drawCircle(
                                point + offset,
                                size,
                                paint.apply { alpha = 0.1f + random2 * 0.1f }
                            )
                            1 -> {
                                val rect = Rect(
                                    left = point.x + offset.x - size,
                                    top = point.y + offset.y - size,
                                    right = point.x + offset.x + size,
                                    bottom = point.y + offset.y + size
                                )
                                canvas.drawRect(
                                    rect,
                                    paint.apply { alpha = 0.1f + random2 * 0.1f }
                                )
                            }
                            2 -> {
                                val dotPath = Path().apply {
                                    val centerPoint = point + offset
                                    moveTo(centerPoint.x, centerPoint.y - size)

                                    for (angle in 0..360 step 60) {
                                        val angleRad = angle * PI.toFloat() / 180f
                                        val radius = size * (0.8f + random3 * 0.4f)
                                        val x = centerPoint.x + cos(angleRad) * radius
                                        val y = centerPoint.y + sin(angleRad) * radius

                                        if (angle == 0) {
                                            moveTo(x, y)
                                        } else {
                                            lineTo(x, y)
                                        }
                                    }
                                    close()
                                }
                                canvas.drawPath(
                                    dotPath,
                                    paint.apply { alpha = 0.1f + random2 * 0.1f }
                                )
                            }
                        }
                    }

                    distance += step
                }
            }
        )

        val bristleBrush = BrushData(
            id = 106,
            name = "Bristle Brush",
            stroke = "bristle_brush_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.4f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.1f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeCap = StrokeCap.Round
                    style = PaintingStyle.Stroke
                }

                // Number of bristles
                val bristleCount = 12
                val bristles = List(bristleCount) { index ->
                    val random1 = drawingPath.getRandom(listOf(index, 1))
                    val random2 = drawingPath.getRandom(listOf(index, 2))
                    BristleState(
                        offset = Offset(
                            (random1 - 0.5f) * drawingPath.size * 0.5f,
                            (random2 - 0.5f) * drawingPath.size * 0.5f
                        ),
                        pressure = 0.5f + random1 * 0.5f
                    )
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    if (prevPoint != null) {
                        val angle = atan2(
                            point.y - prevPoint.y,
                            point.x - prevPoint.x
                        )

                        // Draw individual bristle strokes
                        bristles.forEach { bristle ->
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), bristle.hashCode(), 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), bristle.hashCode(), 2))

                            // Update bristle state with some randomness
                            bristle.offset += Offset(
                                (random1 - 0.5f) * 2f,
                                (random2 - 0.5f) * 2f
                            ).coerceIn(
                                Offset(-drawingPath.size * 0.5f, -drawingPath.size * 0.5f),
                                Offset(drawingPath.size * 0.5f, drawingPath.size * 0.5f)
                            )

                            bristle.pressure = (bristle.pressure + (random1 * 0.2f)).coerceIn(0.3f, 1f)

                            // Create bristle stroke
                            val strokePath = Path().apply {
                                val startOffset = bristle.offset.rotate(angle)
                                val endOffset = bristle.offset.rotate(angle)

                                moveTo(prevPoint!!.x + startOffset.x, prevPoint!!.y + startOffset.y)
                                lineTo(point.x + endOffset.x, point.y + endOffset.y)
                            }

                            canvas.drawPath(
                                strokePath,
                                paint.apply {
                                    strokeWidth = drawingPath.size * 0.1f * bristle.pressure
                                    alpha = 0.1f + bristle.pressure * 0.2f
                                }
                            )
                        }
                    }

                    prevPoint = point
                    distance += step
                }
            }
        )

        val watercolorWashBrush = BrushData(
            id = 110,
            name = "Watercolor Wash",
            stroke = "watercolor_wash_stroke",
            densityOffset = 4.0,
            opacityDiff = 0.8f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.3f

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create soft, spreading wash effect
                    for (i in 0..8) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                        val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                        val radius = drawingPath.size * (1f + random1 * 2f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size * 2f,
                            (random3 - 0.5f) * drawingPath.size * 2f + drawingPath.size // Slight downward bias
                        )

                        // Create irregular water spreads
                        val waterPath = Path().apply {
                            val centerPoint = point + offset
                            for (angle in 0..360 step 30) {
                                val angleRad = angle * PI.toFloat() / 180f
                                val edgeRandom = drawingPath.getRandom(listOf(distance.toInt(), i, angle))
                                val edgeRadius = radius * (0.8f + edgeRandom * 0.4f)
                                val x = centerPoint.x + cos(angleRad) * edgeRadius
                                val y = centerPoint.y + sin(angleRad) * edgeRadius

                                if (angle == 0) moveTo(x, y)
                                else lineTo(x, y)
                            }
                            close()
                        }

                        canvas.drawPath(
                            waterPath,
                            paint.apply {
                                alpha = 0.03f + random1 * 0.02f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val watercolorDryBrushBrush = BrushData(
            id = 111,
            name = "Watercolor Dry Brush",
            stroke = "watercolor_dry_brush_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.6f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Stroke
                    strokeCap = StrokeCap.Round
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    if (prevPoint != null) {
                        // Create dry brush texture effect
                        for (i in 0..6) {
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                            if (random1 > 0.3f) { // Create gaps in stroke
                                val strokeWidth = drawingPath.size * 0.2f * random1
                                val offset = Offset(
                                    (random2 - 0.5f) * drawingPath.size * 0.5f,
                                    (random2 - 0.5f) * drawingPath.size * 0.5f
                                )

                                val strokePath = Path().apply {
                                    moveTo(prevPoint!!.x + offset.x, prevPoint!!.y + offset.y)
                                    lineTo(point.x + offset.x, point.y + offset.y)
                                }

                                canvas.drawPath(
                                    strokePath,
                                    paint.apply {
                                        this.strokeWidth = strokeWidth
                                        alpha = 0.1f + random1 * 0.1f
                                    }
                                )
                            }
                        }
                    }

                    prevPoint = point
                    distance += step
                }
            }
        )

        val watercolorBleedBrush = BrushData(
            id = 112,
            name = "Watercolor Bleed",
            stroke = "watercolor_bleed_stroke",
            densityOffset = 3.0,
            opacityDiff = 0.7f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.3f

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create bleeding effect
                    for (i in 0..12) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                        val radius = drawingPath.size * (0.5f + random1 * 1.5f)
                        val angle = random2 * PI.toFloat() * 2

                        // Create irregular bleeding shapes
                        val bleedPath = Path().apply {
                            val startPoint = point + Offset(cos(angle) * radius * 0.3f, sin(angle) * radius * 0.3f)
                            moveTo(startPoint.x, startPoint.y)

                            for (j in 0..8) {
                                val segmentRandom = drawingPath.getRandom(listOf(distance.toInt(), i, j))
                                val segmentAngle = angle + (j / 8f) * PI.toFloat() * 2
                                val segmentRadius = radius * (0.7f + segmentRandom * 0.6f)

                                val controlPoint = point + Offset(
                                    cos(segmentAngle - 0.3f) * segmentRadius * 1.2f,
                                    sin(segmentAngle - 0.3f) * segmentRadius * 1.2f
                                )
                                val endPoint = point + Offset(
                                    cos(segmentAngle) * segmentRadius,
                                    sin(segmentAngle) * segmentRadius
                                )

                                quadraticBezierTo(
                                    controlPoint.x, controlPoint.y,
                                    endPoint.x, endPoint.y
                                )
                            }
                            close()
                        }

                        canvas.drawPath(
                            bleedPath,
                            paint.apply {
                                alpha = 0.04f + random1 * 0.03f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val watercolorSplatterBrush = BrushData(
            id = 113,
            name = "Watercolor Splatter",
            stroke = "watercolor_splatter_stroke",
            densityOffset = 15.0,
            opacityDiff = 0.6f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 2f

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val center = measure.getPosition(distance)
                    if (center.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create main splatter
                    for (i in 0..20) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                        val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                        val radius = drawingPath.size * (0.2f + random1 * 0.8f)
                        val angle = random2 * PI.toFloat() * 2
                        val distance = drawingPath.size * (0.5f + random3 * 2f)

                        val position = center + Offset(
                            cos(angle) * distance,
                            sin(angle) * distance
                        )

                        // Create irregular splatter drops
                        val splatterPath = Path().apply {
                            moveTo(position.x, position.y - radius)

                            for (j in 0..8) {
                                val dropRandom = drawingPath.getRandom(listOf(distance.toInt(), i, j))
                                val dropAngle = (j / 8f) * PI.toFloat() * 2
                                val dropRadius = radius * (0.8f + dropRandom * 0.4f)

                                val x = position.x + cos(dropAngle) * dropRadius
                                val y = position.y + sin(dropAngle) * dropRadius

                                if (j == 0) moveTo(x, y)
                                else lineTo(x, y)
                            }
                            close()
                        }

                        // Add small satellite drops
                        if (random1 > 0.7f) {
                            for (j in 0..3) {
                                val satelliteRandom1 = drawingPath.getRandom(listOf(distance.toInt(), i, j, 1))
                                val satelliteRandom2 = drawingPath.getRandom(listOf(distance.toInt(), i, j, 2))

                                val satelliteRadius = radius * 0.2f * satelliteRandom1
                                val satelliteAngle = angle + (satelliteRandom2 - 0.5f) * PI.toFloat() / 2
                                val satelliteDistance = distance * (1.2f + satelliteRandom1 * 0.3f)

                                val satellitePosition = center + Offset(
                                    cos(satelliteAngle) * satelliteDistance,
                                    sin(satelliteAngle) * satelliteDistance
                                )

                                canvas.drawCircle(
                                    satellitePosition,
                                    satelliteRadius,
                                    paint.apply {
                                        alpha = 0.1f * satelliteRandom1
                                    }
                                )
                            }
                        }

                        canvas.drawPath(
                            splatterPath,
                            paint.apply {
                                alpha = 0.1f + random1 * 0.1f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val watercolorGranulationBrush = BrushData(
            id = 114,
            name = "Watercolor Granulation",
            stroke = "watercolor_granulation_stroke",
            densityOffset = 3.0,
            opacityDiff = 0.5f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f

                val paint = Paint().apply {
                    color = drawingPath.color
                    style = PaintingStyle.Fill
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create granulation effect with tiny dots
                    for (i in 0..30) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                        val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                        if (random1 > 0.3f) { // Create uneven distribution
                            val grainSize = drawingPath.size * 0.1f * random2
                            val offset = Offset(
                                (random2 - 0.5f) * drawingPath.size * 2f,
                                (random3 - 0.5f) * drawingPath.size * 2f
                            )

                            canvas.drawCircle(
                                point + offset,
                                grainSize,
                                paint.apply {
                                    alpha = 0.05f + random1 * 0.15f
                                }
                            )
                        }
                    }

                    distance += step
                }
            }
        )

        val blendingSmudgeBrush = BrushData(
            id = 122,
            name = "Blending Smudge",
            stroke = "blending_smudge_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.4f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f

                val paint = Paint().apply {
                    color = drawingPath.color
                    blendMode = BlendMode.Softlight
                    style = PaintingStyle.Fill
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (point.isUnspecified) {
                        distance += step
                        continue
                    }

                    // Create soft blending strokes
                    for (i in 0..5) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                        val radius = drawingPath.size * (0.5f + random1 * 0.5f)
                        val offset = Offset(
                            (random2 - 0.5f) * drawingPath.size * 0.5f,
                            (random2 - 0.5f) * drawingPath.size * 0.5f
                        )

                        canvas.drawCircle(
                            point + offset,
                            radius,
                            paint.apply {
                                alpha = 0.05f + random1 * 0.05f
                            }
                        )
                    }

                    distance += step
                }
            }
        )

        val cleanEraser = BrushData(
            id = 124,
            name = "Clean Eraser",
            stroke = "clean_eraser_stroke",
            densityOffset = 1.0,
            customPainter = { canvas, size, drawingPath ->
                canvas.drawPath(
                    drawingPath.path,
                    Paint().apply {
                        strokeWidth = drawingPath.size
                        strokeCap = StrokeCap.Round
                        strokeJoin = StrokeJoin.Round
                        style = PaintingStyle.Stroke
                        blendMode = BlendMode.Clear
                    }
                )
            }
        )

        val pixelBlurBrush = BrushData(
            id = 133,
            name = "Pixel Blur",
            stroke = "pixel_blur_stroke",
            densityOffset = 2.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f
                val pixelSize = (drawingPath.size * 0.5f).toInt().coerceAtLeast(1)

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified) {
                        // Create pixelated blur effect
                        for (i in 0..8) {
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                            val offset = Offset(
                                (random1 - 0.5f) * drawingPath.size * 2,
                                (random2 - 0.5f) * drawingPath.size * 2
                            )

                            val pixelPoint = (point + offset)
                            val snappedX = (pixelPoint.x / pixelSize).toInt() * pixelSize
                            val snappedY = (pixelPoint.y / pixelSize).toInt() * pixelSize

                            canvas.drawRect(
                                Rect(
                                    offset = Offset(snappedX.toFloat(), snappedY.toFloat()),
                                    size = Size(pixelSize.toFloat(), pixelSize.toFloat())
                                ),
                                Paint().apply {
                                    color = drawingPath.color
                                    alpha = 0.1f + random1 * 0.1f
                                }
                            )
                        }
                    }
                    distance += step
                }
            }
        )

        val distortionEffectBrush = BrushData(
            id = 134,
            name = "Distortion Effect",
            stroke = "distortion_effect_stroke",
            densityOffset = 2.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f
                var points = mutableListOf<Offset>()

                // Collect points
                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified) {
                        points.add(point)
                    }
                    distance += step
                }

                // Create distortion effect
                points.forEachIndexed { index, point ->
                    val distortedPath = Path()
                    val segments = 8

                    for (i in 0 until segments) {
                        val random1 = drawingPath.getRandom(listOf(index, i, 1))
                        val random2 = drawingPath.getRandom(listOf(index, i, 2))

                        val angle = (i.toFloat() / segments) * PI.toFloat() * 2
                        val radius = drawingPath.size * (0.5f + random1 * 0.5f)
                        val offset = Offset(
                            cos(angle) * radius + (random2 - 0.5f) * drawingPath.size,
                            sin(angle) * radius + (random2 - 0.5f) * drawingPath.size
                        )

                        if (i == 0) {
                            distortedPath.moveTo(point.x + offset.x, point.y + offset.y)
                        } else {
                            distortedPath.lineTo(point.x + offset.x, point.y + offset.y)
                        }
                    }
                    distortedPath.close()

                    canvas.drawPath(
                        distortedPath,
                        Paint().apply {
                            color = drawingPath.color
                            alpha = 0.1f
                            style = PaintingStyle.Fill
                        }
                    )
                }
            }
        )

        val noiseTextureBrush = BrushData(
            id = 135,
            name = "Noise Texture",
            stroke = "noise_texture_stroke",
            densityOffset = 2.0,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified) {
                        // Create noise pattern
                        for (i in 0..20) {
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                            val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                            if (random1 > 0.5f) {
                                val size = drawingPath.size * 0.1f * random2
                                val offset = Offset(
                                    (random2 - 0.5f) * drawingPath.size * 2,
                                    (random3 - 0.5f) * drawingPath.size * 2
                                )

                                canvas.drawCircle(
                                    point + offset,
                                    size,
                                    Paint().apply {
                                        color = drawingPath.color
                                        alpha = 0.1f + random1 * 0.1f
                                    }
                                )
                            }
                        }
                    }
                    distance += step
                }
            }
        )

        val graphitePencilBrush = BrushData(
            id = 140,
            name = "Graphite Pencil",
            stroke = "graphite_pencil_stroke",
            densityOffset = 1.0,
            opacityDiff = 0.5f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = 1f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeCap = StrokeCap.Round
                    strokeWidth = drawingPath.size * 0.5f
                    alpha = 0.3f
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified && prevPoint != null) {
                        // Create texture variation
                        for (i in 0..2) {
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                            val offset = Offset(
                                (random1 - 0.5f) * drawingPath.size * 0.3f,
                                (random2 - 0.5f) * drawingPath.size * 0.3f
                            )

                            canvas.drawLine(
                                prevPoint + offset,
                                point + offset,
                                paint.apply {
                                    alpha = 0.2f + random1 * 0.2f
                                    strokeWidth = drawingPath.size * 0.3f * (0.8f + random2 * 0.4f)
                                }
                            )
                        }
                    }
                    prevPoint = point
                    distance += step
                }
            }
        )

        val softPencilBrush6B = BrushData(
            id = 141,
            name = "Soft Pencil",
            stroke = "soft_pencil_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.7f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.2f

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified) {
                        // Create soft, layered strokes
                        for (i in 0..8) {
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                            val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                            val offset = Offset(
                                (random1 - 0.5f) * drawingPath.size * 0.8f,
                                (random2 - 0.5f) * drawingPath.size * 0.8f
                            )

                            canvas.drawCircle(
                                point + offset,
                                drawingPath.size * 0.2f * random3,
                                Paint().apply {
                                    color = drawingPath.color
                                    alpha = 0.05f + random1 * 0.05f
                                }
                            )
                        }
                    }
                    distance += step
                }
            }
        )

        val hardPencilBrush2H = BrushData(
            id = 142,
            name = "Hard Pencil",
            stroke = "hard_pencil_stroke",
            densityOffset = 1.0,
            opacityDiff = 0.3f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = 1f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeCap = StrokeCap.Round
                    style = PaintingStyle.Stroke
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified && prevPoint != null) {
                        // Create sharp, precise lines
                        canvas.drawLine(
                            prevPoint,
                            point,
                            paint.apply {
                                strokeWidth = drawingPath.size * 0.3f
                                alpha = 0.8f
                            }
                        )

                        // Add subtle texture
                        val random = drawingPath.getRandom(listOf(distance.toInt(), 1))
                        if (random > 0.7f) {
                            canvas.drawLine(
                                prevPoint,
                                point,
                                paint.apply {
                                    strokeWidth = drawingPath.size * 0.1f
                                    alpha = 0.2f
                                }
                            )
                        }
                    }
                    prevPoint = point
                    distance += step
                }
            }
        )

        val sketchPencilBrush = BrushData(
            id = 143,
            name = "Sketch Pencil",
            stroke = "sketch_pencil_stroke",
            densityOffset = 1.5,
            opacityDiff = 0.6f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = 1f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeCap = StrokeCap.Round
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified && prevPoint != null) {
                        // Create multiple overlapping sketch lines
                        for (i in 0..2) {
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                            val offset = Offset(
                                (random1 - 0.5f) * drawingPath.size * 0.5f,
                                (random2 - 0.5f) * drawingPath.size * 0.5f
                            )

                            // Break the line occasionally for sketchy effect
                            if (random1 > 0.2f) {
                                canvas.drawLine(
                                    prevPoint + offset,
                                    point + offset,
                                    paint.apply {
                                        strokeWidth = drawingPath.size * 0.2f
                                        alpha = 0.3f + random2 * 0.2f
                                    }
                                )
                            }
                        }
                    }
                    prevPoint = point
                    distance += step
                }
            }
        )

        val hatchingPencilBrush = BrushData(
            id = 144,
            name = "Hatching Pencil",
            stroke = "hatching_pencil_stroke",
            densityOffset = 2.0,
            opacityDiff = 0.5f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = drawingPath.size * 0.3f

                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeCap = StrokeCap.Round
                    strokeWidth = 1f
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified) {
                        // Create hatching pattern
                        val angles = listOf(
                            PI.toFloat() / 4f,     // 45°
                            -PI.toFloat() / 4f     // -45°
                        )

                        angles.forEach { angle ->
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), angle.toInt(), 1))
                            val length = drawingPath.size * (0.5f + random1 * 0.5f)

                            val start = Offset(
                                point.x + cos(angle) * length,
                                point.y + sin(angle) * length
                            )
                            val end = Offset(
                                point.x - cos(angle) * length,
                                point.y - sin(angle) * length
                            )

                            canvas.drawLine(
                                start,
                                end,
                                paint.apply {
                                    alpha = 0.1f + random1 * 0.1f
                                }
                            )
                        }
                    }
                    distance += step
                }
            }
        )

        val coloredPencilBrush = BrushData(
            id = 145,
            name = "Colored Pencil",
            stroke = "colored_pencil_stroke",
            densityOffset = 1.5,
            opacityDiff = 0.4f,
            customPainter = { canvas, size, drawingPath ->
                val path = drawingPath.path
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length

                var distance = 0f
                val step = 1f
                var prevPoint: Offset? = null

                val paint = Paint().apply {
                    color = drawingPath.color
                    strokeCap = StrokeCap.Round
                    blendMode = BlendMode.Multiply
                }

                while (distance <= length) {
                    val point = measure.getPosition(distance)
                    if (!point.isUnspecified && prevPoint != null) {
                        // Create wax pencil texture
                        for (i in 0..3) {
                            val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                            val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                            val offset = Offset(
                                (random1 - 0.5f) * drawingPath.size * 0.3f,
                                (random2 - 0.5f) * drawingPath.size * 0.3f
                            )

                            canvas.drawLine(
                                prevPoint + offset,
                                point + offset,
                                paint.apply {
                                    strokeWidth = drawingPath.size * 0.2f * (0.8f + random1 * 0.4f)
                                    alpha = 0.2f + random2 * 0.2f
                                }
                            )
                        }

                        // Add waxy texture overlay
                        if (random(distance.toInt()) > 0.8f) {
                            canvas.drawLine(
                                prevPoint,
                                point,
                                paint.apply {
                                    strokeWidth = drawingPath.size * 0.5f
                                    alpha = 0.05f
                                }
                            )
                        }
                    }
                    prevPoint = point
                    distance += step
                }
            }
        )

        private fun random(seed: Int): Float {
            return (seed * 16807L % 2147483647L).toFloat() / 2147483647L
        }

        fun all(): List<BrushData> = listOf(
            solid,
            pencil,
            sketchyPencil,
            softPencilBrush,
            hardPencilBrush,
            pencilShadingBrush,
            star,
            marker,
            zigzag,
            bubble,
            heart,
            rainbowBrush,
            watercolorBrush,
            crayonBrush,
            sprayPaintBrush,
            charcoalBrush,
            sketchyBrush,
            glitterBrush,
            grassBrush,
            pixelBrush,
            mosaicBrush,
            splatBrush,
            galaxyBrush,
            fireBrush,
            snowflakeBrush,
            cloudBrush,
            confettiBrush,
            particleFieldBrush,
            stainedGlassBrush,
            flowFieldBrush,
            dotCloudBrush,
            blendingBrush,
            textureBlendBrush,
            crossHatchBrush,
            oilBrush,
            wetBrush,
            acrylicBrush,
            glazingBrush,
            impastoBrush,
            spongeTextureBrush,
            bristleBrush,
            watercolorWashBrush,
            watercolorDryBrushBrush,
            watercolorBleedBrush,
            watercolorSplatterBrush,
            watercolorGranulationBrush,
            blendingSmudgeBrush,
            pixelBlurBrush,
            distortionEffectBrush,
            noiseTextureBrush,
            graphitePencilBrush,
            softPencilBrush6B,
            hardPencilBrush2H,
            sketchPencilBrush,
            hatchingPencilBrush,
            coloredPencilBrush
        )

        @Composable
        fun getById(id: Int): BrushData {
            return all().first { it.id == id }
        }
    }
}

private data class BristleState(
    var offset: Offset,
    var pressure: Float
)


fun Offset.normalize(): Offset {
    val magnitude = sqrt(x * x + y * y)
    return if (magnitude > 0) Offset(x / magnitude, y / magnitude) else this
}


private fun Offset.rotate(angle: Float): Offset {
    val cos = cos(angle)
    val sin = sin(angle)
    return Offset(
        x = x * cos - y * sin,
        y = x * sin + y * cos
    )
}

private fun Offset.coerceIn(min: Offset, max: Offset): Offset {
    return Offset(
        x = x.coerceIn(min.x, max.x),
        y = y.coerceIn(min.y, max.y)
    )
}
