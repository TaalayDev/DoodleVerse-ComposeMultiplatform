package io.github.taalaydev.doodleverse.brush

import io.github.taalaydev.doodleverse.data.models.BrushData
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.util.lerp
import io.github.taalaydev.doodleverse.engine.util.distanceTo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

fun Color.hsvToColor(hsv: FloatArray): Int {
    return Color.hsv(hsv[0], hsv[1], hsv[2]).toArgb()
}

fun Color.colorToHSV(value: Int): FloatArray {
    val color = Color(value)
    val hsv = FloatArray(3)
    return hsv
}

fun Offset.normalize(): Offset {
    val length = kotlin.math.sqrt(x * x + y * y)
    return Offset(x / length, y / length)
}

val oilPaintBrush = BrushData(
    id = 80,
    name = "Oil Paint",
    stroke = "oil_paint_stroke",
    opacityDiff = 0.2f,
    densityOffset = 3.0,
    customPainter = { canvas, size, drawingPath ->
        val paint = Paint().apply {
            color = drawingPath.color.copy(alpha = 0.8f)
            strokeWidth = drawingPath.size * 0.8f
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
            style = PaintingStyle.Stroke
        }

        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        // Create main stroke with varying thickness
        val strokePath = Path()
        var distance = 0f
        val step = max(2f, drawingPath.size / 8f)
        var prevPoint: Offset? = null

        while (distance <= length) {
            val point = measure.getPosition(distance)

            if (prevPoint == null) {
                strokePath.moveTo(point.x, point.y)
            } else {
                // Add some randomness to create an oil paint texture
                val random1 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), 1))
                val random2 = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), 2))

                val controlPoint = Offset(
                    lerp(prevPoint.x, point.x, 0.5f) + (random1 - 0.5f) * drawingPath.size * 0.3f,
                    lerp(prevPoint.y, point.y, 0.5f) + (random2 - 0.5f) * drawingPath.size * 0.3f
                )

                strokePath.quadraticBezierTo(
                    controlPoint.x,
                    controlPoint.y,
                    point.x,
                    point.y
                )
            }

            prevPoint = point
            distance += step
        }

        // Draw the main stroke
        canvas.drawPath(strokePath, paint)

        // Add thick paint texture along the path
        distance = 0f
        val textureStep = drawingPath.size / 2f

        while (distance <= length) {
            val point = measure.getPosition(distance)

            for (i in 0 until 3) {
                val random1 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), i, 1))
                val random2 = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), i, 2))
                val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                val offset = Offset(
                    point.x + (random1 - 0.5f) * drawingPath.size * 0.7f,
                    point.y + (random2 - 0.5f) * drawingPath.size * 0.7f
                )

                val texturePaint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.3f + random3 * 0.2f)
                    style = PaintingStyle.Stroke
                    strokeWidth = drawingPath.size * (0.2f + random3 * 0.2f)
                }

                // Create short strokes to simulate oil paint texture
                val textureLength = drawingPath.size * (0.3f + random3 * 0.7f)
                val angle = random1 * PI.toFloat() * 2f
                val endPoint = Offset(
                    offset.x + cos(angle) * textureLength,
                    offset.y + sin(angle) * textureLength
                )

                canvas.drawLine(offset, endPoint, texturePaint)
            }

            distance += textureStep
        }
    }
)

val impressionistBrush = BrushData(
    id = 81,
    name = "Impressionist",
    stroke = "impressionist_stroke",
    densityOffset = 4.0,
    opacityDiff = 0.3f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        var distance = 0f
        val step = drawingPath.size / 3f

        while (distance <= length) {
            val point = measure.getPosition(distance)

            // Create multiple short, visible strokes for impressionist effect
            for (i in 0 until 6) {
                val random1 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), i, 1))
                val random2 = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), i, 2))
                val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))
                val random4 = drawingPath.getRandom(listOf(distance.toInt(), i, 4))

                val offset = Offset(
                    point.x + (random1 - 0.5f) * drawingPath.size * 0.8f,
                    point.y + (random2 - 0.5f) * drawingPath.size * 0.8f
                )

                // Vary stroke length, direction, and thickness
                val strokeLength = drawingPath.size * (0.5f + random3 * 1.0f)
                val angle = random4 * PI.toFloat() * 2f
                val endPoint = Offset(
                    offset.x + cos(angle) * strokeLength,
                    offset.y + sin(angle) * strokeLength
                )

                // Use slightly different color variations for impressionistic effect
//                val hue = Color.hsvToColor(floatArrayOf(
//                    Color.colorToHSV(drawingPath.color.toArgb())[0] + (random3 - 0.5f) * 10f,
//                    Color.colorToHSV(drawingPath.color.toArgb())[1],
//                    Color.colorToHSV(drawingPath.color.toArgb())[2]
//                ))

                val paint = Paint().apply {
                    color = drawingPath.color.copy(alpha = 0.4f + random4 * 0.3f)
                    strokeWidth = drawingPath.size * (0.2f + random3 * 0.4f)
                    strokeCap = StrokeCap.Round
                }

                canvas.drawLine(offset, endPoint, paint)
            }

            distance += step
        }
    }
)

// 2. Nature Brush Pack

val foliageBrush = BrushData(
    id = 82,
    name = "Foliage",
    stroke = "foliage_stroke",
    densityOffset = 15.0,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        var distance = 0f
        val step = drawingPath.size * 2  // Adjust spacing between leaves

        while (distance <= length) {
            val point = measure.getPosition(distance)

            // Generate several leaves/foliage elements at each point
            for (i in 0 until 4) {
                val random1 = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), i, 1))
                val random2 = drawingPath.getRandom(listOf(distance.toInt(), point.y.toInt(), i, 2))
                val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))
                val random4 = drawingPath.getRandom(listOf(distance.toInt(), i, 4))

                val leafSize = drawingPath.size * (0.7f + random1 * 0.6f)
                val rotation = random2 * 360f

                // Slightly different shade for each leaf
                val leafColor = drawingPath.color.copy(
                    alpha = 0.7f + random3 * 0.3f
                )

                val paint = Paint().apply {
                    color = leafColor
                    style = PaintingStyle.Fill
                }

                // Create a leaf shape
                val leafPath = Path()

                // Position with some randomness
                val leafPos = Offset(
                    point.x + (random3 - 0.5f) * drawingPath.size,
                    point.y + (random4 - 0.5f) * drawingPath.size
                )

                // Generate a leaf shape based on leaf type
                val leafType = (random1 * 3).toInt()

                canvas.withSave {
                    canvas.translate(leafPos.x, leafPos.y)
                    canvas.rotate(rotation)

                    when (leafType) {
                        0 -> { // Simple oval leaf
                            leafPath.addOval(
                                Rect(
                                    -leafSize/2, -leafSize/4,
                                    leafSize/2, leafSize/4
                                )
                            )

                            // Add a stem
                            leafPath.moveTo(0f, 0f)
                            leafPath.lineTo(-leafSize/2, 0f)
                        }
                        1 -> { // Pointed leaf
                            leafPath.moveTo(0f, -leafSize/2)
                            leafPath.cubicTo(
                                leafSize/2, -leafSize/4,
                                leafSize/2, leafSize/4,
                                0f, leafSize/2
                            )
                            leafPath.cubicTo(
                                -leafSize/2, leafSize/4,
                                -leafSize/2, -leafSize/4,
                                0f, -leafSize/2
                            )
                        }
                        else -> { // Compound leaf
                            val leafletCount = 5
                            val stemLength = leafSize * 0.8f

                            leafPath.moveTo(0f, 0f)
                            leafPath.lineTo(0f, stemLength)

                            for (j in 0 until leafletCount) {
                                val t = j / (leafletCount - 1f)
                                val stemPoint = Offset(0f, t * stemLength)
                                val leafletSize = leafSize * 0.3f * (1f - abs(t - 0.5f) * 1.5f)

                                val leafletPath = Path()
                                leafletPath.addOval(
                                    Rect(
                                        stemPoint.x - leafletSize/2, stemPoint.y - leafletSize/4,
                                        stemPoint.x + leafletSize/2, stemPoint.y + leafletSize/4
                                    )
                                )

                                // Rotate the leaflet slightly
                                val leafletRotation = if (j % 2 == 0)  30f else -30f
                                canvas.withSave {
                                    canvas.translate(stemPoint.x, stemPoint.y)
                                    canvas.rotate(leafletRotation)
                                    canvas.translate(-stemPoint.x, -stemPoint.y)
                                    canvas.drawPath(leafletPath, paint)
                                }
                            }
                        }
                    }

                    // Draw the leaf for types 0 and 1
                    if (leafType < 2) {
                        canvas.drawPath(leafPath, paint)

                        // Add veins
                        val veinPaint = Paint().apply {
                            color = leafColor.copy(alpha = 0.4f)
                            style = PaintingStyle.Stroke
                            strokeWidth = leafSize * 0.03f
                        }

                        val veinPath = Path()
                        veinPath.moveTo(0f, 0f)
                        veinPath.lineTo(0f, leafSize/4)

                        // Add side veins
                        val veinCount = 3
                        for (v in 1..veinCount) {
                            val t = v / (veinCount + 1f)
                            veinPath.moveTo(0f, -leafSize/4 + t * leafSize/2)
                            veinPath.lineTo(leafSize/4 * cos(PI.toFloat()/4), -leafSize/4 + t * leafSize/2)

                            veinPath.moveTo(0f, -leafSize/4 + t * leafSize/2)
                            veinPath.lineTo(-leafSize/4 * cos(PI.toFloat()/4), -leafSize/4 + t * leafSize/2)
                        }

                        canvas.drawPath(veinPath, veinPaint)
                    }
                }
            }

            distance += step
        }
    }
)

val waterRippleBrush = BrushData(
    id = 83,
    name = "Water Ripple",
    stroke = "water_ripple_stroke",
    densityOffset = 12.0,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        var distance = 0f
        val step = drawingPath.size * 1.5f

        while (distance <= length) {
            val point = measure.getPosition(distance)

            val rippleRadius = drawingPath.size * 2f
            val baseColor = drawingPath.color

            // Draw multiple concentric circles for ripple effect
            for (i in 3 downTo 0) {
                val random = drawingPath.getRandom(listOf(distance.toInt(), point.x.toInt(), i))
                val rippleSize = rippleRadius * (0.4f + i * 0.2f) * (0.8f + random * 0.4f)

                val paint = Paint().apply {
                    style = PaintingStyle.Stroke
                    color = baseColor.copy(alpha = 0.3f - i * 0.07f)
                    strokeWidth = drawingPath.size * 0.2f
                }

                // Add some waviness to the circles
                val ripplePath = Path()
                val segments = 24
                val angleStep = (2 * PI.toFloat()) / segments

                for (j in 0..segments) {
                    val angle = j * angleStep
                    val waveFactor = drawingPath.getRandom(listOf(distance.toInt(), i, j)) * 0.3f + 0.85f
                    val x = point.x + cos(angle) * rippleSize * waveFactor
                    val y = point.y + sin(angle) * rippleSize * waveFactor

                    if (j == 0) {
                        ripplePath.moveTo(x, y)
                    } else {
                        ripplePath.lineTo(x, y)
                    }
                }

                ripplePath.close()
                canvas.drawPath(ripplePath, paint)
            }

            // Add small highlights
            for (i in 0 until 6) {
                val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                val highlightPos = Offset(
                    point.x + (random1 - 0.5f) * rippleRadius * 2f,
                    point.y + (random2 - 0.5f) * rippleRadius * 2f
                )

                val highlightPaint = Paint().apply {
                    style = PaintingStyle.Fill
                    color = Color.White.copy(alpha = 0.2f + random3 * 0.3f)
                }

                val highlightSize = drawingPath.size * 0.2f * random3
                canvas.drawCircle(highlightPos, highlightSize, highlightPaint)
            }

            distance += step
        }
    }
)

// 3. Special Effects Pack

val neonGlowBrush = BrushData(
    id = 84,
    name = "Neon Glow",
    stroke = "neon_glow_stroke",
    densityOffset = 1.0,
    opacityDiff = 0.0f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path

        // Create the glow effect with multiple layers
        for (i in 5 downTo 0) {
            val alpha = 0.1f + (i / 5f) * 0.5f  // Increasing alpha for inner layers
            val glowSize = drawingPath.size * (3.0f - i * 0.4f)

            val glowPaint = Paint().apply {
                color = drawingPath.color.copy(alpha = alpha)
                style = PaintingStyle.Stroke
                strokeWidth = glowSize
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round

                // Apply blur effect for outer glow
                if (i < 3) {
                    // maskFilter = BlurMaskFilter(glowSize * 0.8f, BlurMaskFilter.Blur.NORMAL)
                }
            }

            canvas.drawPath(path, glowPaint)
        }

        // Draw the bright core of the neon light
        val corePaint = Paint().apply {
            color = Color.White.copy(alpha = 0.9f)
            style = PaintingStyle.Stroke
            strokeWidth = drawingPath.size * 0.4f
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
        }

        canvas.drawPath(path, corePaint)
    }
)

val magicParticlesBrush = BrushData(
    id = 85,
    name = "Magic Particles",
    stroke = "magic_particles_stroke",
    densityOffset = 3.0,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        var distance = 0f
        val step = 5f  // Close particles

        // Define how many particles to generate at each point
        val particleDensity = (drawingPath.size / 2).toInt().coerceIn(5, 30)

        // Core path with particles along it
        while (distance <= length) {
            val point = measure.getPosition(distance)

            // Generate particles around the point
            for (i in 0 until particleDensity) {
                val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))
                val random4 = drawingPath.getRandom(listOf(distance.toInt(), i, 4))

                // Particle distance from path increases with drawingPath.size
                val radius = drawingPath.size * 1.5f * random1
                val angle = random2 * 2f * PI.toFloat()

                val particlePos = Offset(
                    point.x + cos(angle) * radius,
                    point.y + sin(angle) * radius
                )

                // Particle size varies
                val particleSize = drawingPath.size * 0.2f * random3

                // Create a color cycle effect for magical feeling
                val hueShift = (distance / 100f + random4) * 360f % 360f
                val saturation = 0.7f + random3 * 0.3f
                val brightness = 0.7f + random4 * 0.3f
                val alpha = 0.2f + random3 * 0.6f * (1f - (radius / (drawingPath.size * 2f)))

                val particleColor = Color.hsv(hueShift, saturation, brightness, alpha)

                val particlePaint = Paint().apply {
                    color = particleColor
                    style = PaintingStyle.Fill
                }

                // Draw the particle - mix of circles and stars for magic effect
                if (random1 > 0.7f) {
                    // Star shape for some particles
                    val starPath = Path()
                    val points = 5
                    val innerRadius = particleSize * 0.4f
                    val outerRadius = particleSize

                    for (p in 0 until points * 2) {
                        val starAngle = p * PI.toFloat() / points
                        val starRadius = if (p % 2 == 0) outerRadius else innerRadius
                        val x = particlePos.x + cos(starAngle) * starRadius
                        val y = particlePos.y + sin(starAngle) * starRadius

                        if (p == 0) {
                            starPath.moveTo(x, y)
                        } else {
                            starPath.lineTo(x, y)
                        }
                    }

                    starPath.close()
                    canvas.drawPath(starPath, particlePaint)

                    // Add a glow to stars
                    val glowPaint = Paint().apply {
                        color = particleColor.copy(alpha = 0.3f)
                        style = PaintingStyle.Fill
                        // maskFilter = BlurMaskFilter(particleSize * 0.8f, BlurMaskFilter.Blur.NORMAL)
                    }

                    canvas.drawPath(starPath, glowPaint)
                } else {
                    // Circle for most particles
                    canvas.drawCircle(particlePos, particleSize, particlePaint)

                    // Add glow to some particles
                    if (random4 > 0.7f) {
                        val glowPaint = Paint().apply {
                            color = particleColor.copy(alpha = 0.3f)
                            style = PaintingStyle.Fill
                            // maskFilter = BlurMaskFilter(particleSize * 1.5f, BlurMaskFilter.Blur.NORMAL)
                        }

                        canvas.drawCircle(particlePos, particleSize * 1.5f, glowPaint)
                    }
                }
            }

            distance += step
        }

        // Add a subtle path along the stroke for continuity
        val corePaint = Paint().apply {
            color = drawingPath.color.copy(alpha = 0.3f)
            style = PaintingStyle.Stroke
            strokeWidth = drawingPath.size * 0.5f
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 10f), 0f)
        }

        canvas.drawPath(path, corePaint)
    }
)

// 4. Artistic Style Pack

val comicInkBrush = BrushData(
    id = 86,
    name = "Comic Ink",
    stroke = "comic_ink_stroke",
    densityOffset = 1.0,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path

        // Main stroke - solid black line with variable pressure
        val mainPaint = Paint().apply {
            color = Color.Black.copy(alpha = 0.9f)
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
        }

        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        // Create variable line width for expressive inking
        val variablePath = Path()
        var distance = 0f
        val step = 2f
        var prevPoint: Offset? = null
        var prevWidth = drawingPath.size

        while (distance <= length) {
            val point = measure.getPosition(distance)

            // Simulate pen pressure by varying stroke width
            val random = drawingPath.getRandom(listOf(distance.toInt(), 1))
            val pressure = 0.7f + (random * 0.6f) // Random pressure between 0.7 and 1.3
            val strokeWidth = drawingPath.size * pressure

            if (prevPoint == null) {
                variablePath.moveTo(point.x, point.y)
                prevPoint = point
                prevWidth = strokeWidth
            } else {
                // Create a smooth transition between different widths
                val midPoint = Offset(
                    (prevPoint.x + point.x) / 2,
                    (prevPoint.y + point.y) / 2
                )

                mainPaint.strokeWidth = (prevWidth + strokeWidth) / 2
                canvas.drawLine(prevPoint, midPoint, mainPaint)

                mainPaint.strokeWidth = strokeWidth
                canvas.drawLine(midPoint, point, mainPaint)

                prevPoint = point
                prevWidth = strokeWidth
            }

            distance += step
        }

        // Add ink splatters/blobs at ends and random points
        val splatterCount = (drawingPath.size / 5).toInt().coerceIn(1, 5)

        for (i in 0 until splatterCount) {
            val random1 = drawingPath.getRandom(listOf(i, 1))
            val distance = random1 * length
            val point = measure.getPosition(distance)

            // Only add splatters 20% of the time or at ends
            if (random1 > 0.8f || distance < 5f || distance > length - 5f) {
                val splatterPaint = Paint().apply {
                    color = Color.Black
                    style = PaintingStyle.Fill
                }

                val splatterSize = drawingPath.size * (0.5f + random1 * 0.5f)

                // Create an irregular blob shape
                val blobPath = Path()
                val blobPoints = 8
                val angleStep = (2 * PI.toFloat()) / blobPoints

                for (j in 0..blobPoints) {
                    val angle = j * angleStep
                    val random2 = drawingPath.getRandom(listOf(i, j, 2))
                    val radius = splatterSize * (0.7f + random2 * 0.6f)

                    val x = point.x + cos(angle) * radius
                    val y = point.y + sin(angle) * radius

                    if (j == 0) {
                        blobPath.moveTo(x, y)
                    } else {
                        // Use quadratic curves for a smoother blob
                        val prevAngle = (j - 1) * angleStep
                        val prevRadius = splatterSize * (0.7f + drawingPath.getRandom(listOf(i, j-1, 2)) * 0.6f)
                        val prevX = point.x + cos(prevAngle) * prevRadius
                        val prevY = point.y + sin(prevAngle) * prevRadius

                        val ctrlAngle = (prevAngle + angle) / 2
                        val ctrlRadius = splatterSize * (0.7f + drawingPath.getRandom(listOf(i, j, 3)) * 0.8f)
                        val ctrlX = point.x + cos(ctrlAngle) * ctrlRadius * 1.2f
                        val ctrlY = point.y + sin(ctrlAngle) * ctrlRadius * 1.2f

                        blobPath.quadraticBezierTo(ctrlX, ctrlY, x, y)
                    }
                }

                blobPath.close()
                canvas.drawPath(blobPath, splatterPaint)
            }
        }
    }
)

val chalkBrush = BrushData(
    id = 200,
    name = "Chalk",
    stroke = "chalk_stroke",
    densityOffset = 1.5,
    opacityDiff = 0.6f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        var distance = 0f
        val step = max(1f, drawingPath.size / 10f)
        var prevPoint: Offset? = null

        // Main chalk stroke - chalk has a rough, grainy texture
        val paint = Paint().apply {
            color = drawingPath.color.copy(alpha = 0.8f)
            strokeCap = StrokeCap.Round
            style = PaintingStyle.Stroke
        }

        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += step
                continue
            }

            if (prevPoint != null) {
                // Create multiple strands for chalk texture
                for (i in 0..3) {
                    val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                    val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))

                    val offset = Offset(
                        (random1 - 0.5f) * drawingPath.size * 0.3f,
                        (random2 - 0.5f) * drawingPath.size * 0.3f
                    )

                    // Vary opacity and width for realistic chalk texture
                    paint.apply {
                        alpha = (0.4f + random1 * 0.5f)
                        strokeWidth = drawingPath.size * (0.2f + random2 * 0.3f)
                    }

                    // Add small gaps in the chalk stroke
                    if (random1 > 0.15f) {
                        canvas.drawLine(
                            prevPoint + offset,
                            point + offset,
                            paint
                        )
                    }
                }
            }

            // Create chalk dust particles
            if (prevPoint != null && drawingPath.getRandom(listOf(distance.toInt(), 10)) > 0.7f) {
                val dustCount = (drawingPath.size / 3).toInt().coerceIn(1, 8)
                for (i in 0 until dustCount) {
                    val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 100))
                    val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 200))
                    val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 300))

                    val dustOffset = Offset(
                        (random1 - 0.5f) * drawingPath.size * 2f,
                        (random2 - 0.5f) * drawingPath.size * 2f
                    )

                    val dustSize = drawingPath.size * 0.08f * random3

                    canvas.drawCircle(
                        point + dustOffset,
                        dustSize,
                        Paint().apply {
                            color = drawingPath.color.copy(alpha = 0.2f * random3)
                            style = PaintingStyle.Fill
                        }
                    )
                }
            }

            prevPoint = point
            distance += step
        }
    }
)

val pastelBrush = BrushData(
    id = 201,
    name = "Pastel",
    stroke = "pastel_stroke",
    densityOffset = 2.0,
    opacityDiff = 0.4f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        var distance = 0f
        val step = max(1f, drawingPath.size / 8f)

        val mainPaint = Paint().apply {
            color = drawingPath.color.copy(alpha = 0.7f)
            strokeCap = StrokeCap.Round
            strokeWidth = drawingPath.size * 0.8f
            style = PaintingStyle.Stroke
        }

        // Draw the main stroke
        canvas.drawPath(path, mainPaint)

        // Draw the grainy texture that makes pastels unique
        var prevPoint: Offset? = null

        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += step
                continue
            }

            if (prevPoint != null) {
                // Create textured strokes to simulate pastel on paper
                for (i in 0..12) {
                    val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                    val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                    val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                    // Pastels have wide coverage with grainy edges
                    val offset = Offset(
                        (random1 - 0.5f) * drawingPath.size * 1.2f,
                        (random2 - 0.5f) * drawingPath.size * 1.2f
                    )

                    // Only draw some points for grainy texture
                    if (random3 > 0.4f) {
                        // Size of pastel grain
                        val grainSize = drawingPath.size * 0.15f * random3

                        canvas.drawCircle(
                            point + offset,
                            grainSize,
                            Paint().apply {
                                color = drawingPath.color.copy(alpha = 0.1f + random3 * 0.2f)
                                style = PaintingStyle.Fill
                            }
                        )
                    }
                }
            }

            prevPoint = point
            distance += step
        }

        // Add smudging effect typical of pastels
        distance = 0f
        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += drawingPath.size
                continue
            }

            val random = drawingPath.getRandom(listOf(distance.toInt(), 1000))

            if (random > 0.7f) {
                // Occasional smudges in the direction of the stroke
                val smudgeLength = drawingPath.size * 2f * random
                val angle = random * PI.toFloat() * 2

                val smudgePath = Path().apply {
                    moveTo(point.x, point.y)
                    quadraticBezierTo(
                        point.x + cos(angle) * smudgeLength * 0.5f,
                        point.y + sin(angle) * smudgeLength * 0.5f,
                        point.x + cos(angle) * smudgeLength,
                        point.y + sin(angle) * smudgeLength
                    )
                }

                canvas.drawPath(
                    smudgePath,
                    Paint().apply {
                        color = drawingPath.color.copy(alpha = 0.1f)
                        style = PaintingStyle.Stroke
                        strokeWidth = drawingPath.size * 0.5f
                    }
                )
            }

            distance += drawingPath.size
        }
    }
)

val gouacheBrush = BrushData(
    id = 202,
    name = "Gouache",
    stroke = "gouache_stroke",
    densityOffset = 2.0,
    opacityDiff = 0.2f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        // Main stroke with flat color characteristic of gouache
        val mainPaint = Paint().apply {
            color = drawingPath.color.copy(alpha = 0.9f)  // Gouache is opaque
            style = PaintingStyle.Stroke
            strokeWidth = drawingPath.size
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
        }

        // Draw main stroke
        canvas.drawPath(path, mainPaint)

        // Gouache has a characteristic uneven edge and matte finish
        var distance = 0f
        val step = max(1f, drawingPath.size / 6f)
        var prevPoint: Offset? = null

        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += step
                continue
            }

            if (prevPoint != null) {
                // Create the flat, slightly textured appearance of gouache
                val direction = Offset(
                    point.x - prevPoint.x,
                    point.y - prevPoint.y
                ).normalize()

                // Perpendicular to stroke direction for edge effects
                val perpendicular = Offset(-direction.y, direction.x)

                // Create varying edges on both sides of the stroke
                for (i in -1..1 step 2) {
                    val edgeCount = (drawingPath.size / 3).toInt().coerceIn(2, 6)

                    for (j in 0 until edgeCount) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, j, 1))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, j, 2))

                        // Edge texture positions
                        val edgeOffset = Offset(
                            perpendicular.x * drawingPath.size * 0.5f * i +
                                    (random1 - 0.5f) * drawingPath.size * 0.3f,
                            perpendicular.y * drawingPath.size * 0.5f * i +
                                    (random2 - 0.5f) * drawingPath.size * 0.3f
                        )

                        // Edge texture
                        val edgeSize = drawingPath.size * 0.3f * random1

                        canvas.drawCircle(
                            point + edgeOffset,
                            edgeSize,
                            Paint().apply {
                                color = drawingPath.color.copy(alpha = 0.3f + random2 * 0.3f)
                                style = PaintingStyle.Fill
                            }
                        )
                    }
                }
            }

            prevPoint = point
            distance += step
        }

        // Add characteristic gouache brush strokes
        distance = 0f
        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (distance > 0 && distance < length - step && point.isUnspecified == false) {

                // Create the subtle brush stroke texture
                val brushTextureCount = (drawingPath.size / 4).toInt().coerceIn(3, 8)

                for (i in 0 until brushTextureCount) {
                    val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 500))
                    val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 600))

                    // Subtle variations in base color for texture
                    val textureColor = drawingPath.color.copy(alpha = 0.15f)

                    // Create short strokes to simulate brush texture
                    val strokeLength = drawingPath.size * 0.8f
                    val angle = random1 * PI.toFloat() * 2

                    val start = point + Offset(
                        (random2 - 0.5f) * drawingPath.size * 0.8f,
                        (random2 - 0.5f) * drawingPath.size * 0.8f
                    )

                    val end = start + Offset(
                        cos(angle) * strokeLength,
                        sin(angle) * strokeLength
                    )

                    canvas.drawLine(
                        start,
                        end,
                        Paint().apply {
                            color = textureColor
                            strokeWidth = drawingPath.size * 0.1f
                            strokeCap = StrokeCap.Round
                        }
                    )
                }
            }

            distance += drawingPath.size
        }
    }
)

val blueprintPenBrush = BrushData(
    id = 203,
    name = "Blueprint Pen",
    stroke = "blueprint_pen_stroke",
    densityOffset = 1.0,
    opacityDiff = 0.1f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path

        // Technical pens produce consistent, precise lines
        val mainPaint = Paint().apply {
            color = Color(0xFF0F5298)  // Blueprint blue color
            style = PaintingStyle.Stroke
            strokeWidth = drawingPath.size * 0.8f
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
        }

        // Draw the precise main line
        canvas.drawPath(path, mainPaint)

        // Add the characteristic paper bleed of blueprint/technical drawings
        val bleedPaint = Paint().apply {
            color = Color(0xFF0F5298).copy(alpha = 0.2f)
            style = PaintingStyle.Stroke
            strokeWidth = drawingPath.size * 1.2f
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
        }

        canvas.drawPath(path, bleedPaint)

        // Measure the path for adding technical details
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        // Add dimension marks or technical indicators at ends or key points
        if (length > drawingPath.size * 5) {
            // Only add indicators if line is long enough

            // Add tick marks at ends of lines
            val startPoint = measure.getPosition(0f)
            val endPoint = measure.getPosition(length)

            // Direction of the line
            val direction = Offset(
                endPoint.x - startPoint.x,
                endPoint.y - startPoint.y
            ).normalize()

            // Perpendicular for tick marks
            val perpendicular = Offset(-direction.y, direction.x)

            // Create tick marks perpendicular to the line at each end
            val tickLength = drawingPath.size * 1.5f

            // Start tick
            canvas.drawLine(
                startPoint - (perpendicular * tickLength),
                startPoint + (perpendicular * tickLength),
                mainPaint
            )

            // End tick
            canvas.drawLine(
                endPoint - (perpendicular * tickLength),
                endPoint + (perpendicular * tickLength),
                mainPaint
            )

            // Add small dots at equidistant points for measurement guides
            val numDots = (length / (drawingPath.size * 10)).toInt().coerceIn(2, 20)
            if (numDots >= 2) {
                val dotSpacing = length / (numDots - 1)
                for (i in 0 until numDots) {
                    val dotPoint = measure.getPosition(i * dotSpacing)
                    canvas.drawCircle(
                        dotPoint,
                        drawingPath.size * 0.2f,
                        Paint().apply {
                            color = Color(0xFF0F5298)
                            style = PaintingStyle.Fill
                        }
                    )
                }
            }
        }
    }
)

val mangaGPenBrush = BrushData(
    id = 204,
    name = "Manga G-Pen",
    stroke = "manga_gpen_stroke",
    densityOffset = 1.0,
    opacityDiff = 0.1f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        // G-pens create strokes with dramatic line variation and crisp edges
        var distance = 0f
        val step = 2f
        var prevPoint: Offset? = null
        var prevDirection: Offset? = null

        val inkPaint = Paint().apply {
            color = Color.Black.copy(alpha = 0.9f)
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
        }

        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += step
                continue
            }

            if (prevPoint != null) {
                val direction = Offset(
                    point.x - prevPoint.x,
                    point.y - prevPoint.y
                ).normalize()

                // Calculate stroke width based on speed and direction change
                var strokeMultiplier = 1.0f

                if (prevDirection != null) {
                    // Measure direction change - G-pens create thinner lines on direction changes
                    val directionChange = direction.distanceTo(prevDirection)
                    strokeMultiplier *= (1.0f - directionChange * 0.5f).coerceIn(0.3f, 1.0f)

                    // Simulate pressure variation - manga artists apply more pressure at start/end
                    val normalizedDist = min(distance / (length * 0.2f), (length - distance) / (length * 0.2f))
                    strokeMultiplier *= (0.5f + normalizedDist * 0.5f).coerceIn(0.3f, 1.0f)

                    // Add randomness for hand tremor/texture
                    val random = drawingPath.getRandom(listOf(distance.toInt(), 1))
                    strokeMultiplier *= (0.9f + random * 0.2f)
                }

                // G-pens have high contrast and line variation
                val strokeWidth = drawingPath.size * strokeMultiplier
                inkPaint.strokeWidth = strokeWidth

                canvas.drawLine(prevPoint, point, inkPaint)

                // Add ink splatters at direction changes (characteristic of manga inking)
                if (prevDirection != null && direction.distanceTo(prevDirection) > 0.5f) {
                    val splatterPaint = Paint().apply {
                        color = Color.Black.copy(alpha = 0.7f)
                        style = PaintingStyle.Fill
                    }

                    val splatterCount = (3 * direction.distanceTo(prevDirection)).toInt().coerceIn(1, 5)

                    for (i in 0 until splatterCount) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 100))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 200))

                        val splatterOffset = Offset(
                            (random1 - 0.5f) * drawingPath.size * 1.5f,
                            (random2 - 0.5f) * drawingPath.size * 1.5f
                        )

                        val splatterSize = drawingPath.size * 0.2f * random1

                        canvas.drawCircle(
                            point + splatterOffset,
                            splatterSize,
                            splatterPaint
                        )
                    }
                }

                prevDirection = direction
            }

            prevPoint = point
            distance += step
        }

        // Add tapered ends - G-pens typically create sharp terminals
        if (length > drawingPath.size * 3) {
            val startPoint = measure.getPosition(0f)
            val startDirection = Offset(
                measure.getPosition(10f).x - startPoint.x,
                measure.getPosition(10f).y - startPoint.y
            ).normalize()

            val endPoint = measure.getPosition(length)
            val endDirection = Offset(
                endPoint.x - measure.getPosition(length - 10f).x,
                endPoint.y - measure.getPosition(length - 10f).y
            ).normalize()

            // Add tapered end strokes
            val taperLength = drawingPath.size * 2f

            val startTaperEnd = startPoint - (startDirection * taperLength)
            val endTaperEnd = endPoint + (endDirection * taperLength)

            // Draw tapered ends with decreasing width
            for (i in 0 until 5) {
                val taperedPaint = Paint().apply {
                    color = Color.Black.copy(alpha = (0.4f - i * 0.1f).coerceAtLeast(0.1f))
                    strokeWidth = drawingPath.size * (0.3f - i * 0.05f).coerceAtLeast(0.05f)
                    strokeCap = StrokeCap.Round
                }

                canvas.drawLine(startPoint, startTaperEnd, taperedPaint)
                canvas.drawLine(endPoint, endTaperEnd, taperedPaint)
            }
        }
    }
)

val charcoalPencilBrush = BrushData(
    id = 205,
    name = "Charcoal Pencil",
    stroke = "charcoal_pencil_stroke",
    densityOffset = 1.5,
    opacityDiff = 0.5f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        // Main charcoal stroke
        val mainPaint = Paint().apply {
            color = drawingPath.color.copy(alpha = 0.6f)
            strokeWidth = drawingPath.size * 0.8f
            strokeCap = StrokeCap.Round
            style = PaintingStyle.Stroke
        }

        canvas.drawPath(path, mainPaint)

        // Charcoal has a rich, grainy texture with smudging
        var distance = 0f
        val step = max(1f, drawingPath.size / 10f)
        var prevPoint: Offset? = null

        // Texture particles along the stroke
        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += step
                continue
            }

            // Generate charcoal texture
            val particleCount = (drawingPath.size / 2).toInt().coerceIn(5, 20)

            for (i in 0 until particleCount) {
                val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                // Particles primarily along the stroke with some spread
                val particleOffset = Offset(
                    (random1 - 0.5f) * drawingPath.size * 1.5f,
                    (random2 - 0.5f) * drawingPath.size * 1.5f
                )

                // Charcoal breaks down into fine particles
                val particleSize = drawingPath.size * 0.1f * random3

                canvas.drawCircle(
                    point + particleOffset,
                    particleSize,
                    Paint().apply {
                        color = drawingPath.color.copy(alpha = 0.1f + random3 * 0.2f)
                        style = PaintingStyle.Fill
                    }
                )
            }

            // Add smudging effect
            if (prevPoint != null && drawingPath.getRandom(listOf(distance.toInt(), 100)) > 0.7f) {
                val direction = Offset(
                    point.x - prevPoint.x,
                    point.y - prevPoint.y
                ).normalize()

                // Perpendicular for smudging
                val perpendicular = Offset(-direction.y, direction.x)

                // Create smudge marks
                val smudgeCount = (drawingPath.size / 4).toInt().coerceIn(2, 5)
                for (i in 0 until smudgeCount) {
                    val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 200))
                    val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 300))

                    val smudgeLength = drawingPath.size * (1f + random1 * 2f)
                    val smudgeAngle = random2 * PI.toFloat()

                    val smudgeDir = Offset(
                        cos(smudgeAngle),
                        sin(smudgeAngle)
                    )

                    val smudgePath = Path().apply {
                        moveTo(point.x, point.y)

                        // Create smudge curve
                        quadraticBezierTo(
                            point.x + smudgeDir.x * smudgeLength * 0.5f,
                            point.y + smudgeDir.y * smudgeLength * 0.5f,
                            point.x + smudgeDir.x * smudgeLength,
                            point.y + smudgeDir.y * smudgeLength
                        )
                    }

                    canvas.drawPath(
                        smudgePath,
                        Paint().apply {
                            color = drawingPath.color.copy(alpha = 0.05f + random1 * 0.05f)
                            style = PaintingStyle.Stroke
                            strokeWidth = drawingPath.size * 0.5f * random2
                        }
                    )
                }
            }

            prevPoint = point
            distance += step
        }

        // Add characteristic variation in pressure - charcoal marks have darker and lighter areas
        distance = 0f
        while (distance <= length) {
            val point = measure.getPosition(distance)

            if (distance > 0 && distance < length - 5f && point.isUnspecified == false) {
                val pressureRandom = drawingPath.getRandom(listOf(distance.toInt(), 500))

                if (pressureRandom > 0.8f) {
                    // Create a pressure point - darker area where artist pressed harder
                    val pressureRadius = drawingPath.size * 0.8f * pressureRandom

                    canvas.drawCircle(
                        point,
                        pressureRadius,
                        Paint().apply {
                            color = drawingPath.color.copy(alpha = 0.3f)
                            style = PaintingStyle.Fill
                        }
                    )
                }
            }

            distance += drawingPath.size
        }
    }
)

val patternBrush = BrushData(
    id = 206,
    name = "Pattern Brush",
    stroke = "pattern_brush_stroke",
    densityOffset = 10.0,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        // Distance between pattern elements
        val patternSpacing = drawingPath.size * 2f
        var distance = 0f

        // Paint for pattern elements
        val paint = Paint().apply {
            color = drawingPath.color
            style = PaintingStyle.Fill
        }

        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += patternSpacing
                continue
            }

            // Generate direction tangent to path for orienting pattern elements
            val tangent = if (distance + 5f <= length) {
                val nextPoint = measure.getPosition(distance + 5f)
                Offset(
                    nextPoint.x - point.x,
                    nextPoint.y - point.y
                ).normalize()
            } else {
                Offset(1f, 0f)
            }

            // Perpendicular for pattern orientation
            val normal = Offset(-tangent.y, tangent.x)

            // Random selection between pattern elements
            val patternType = ((drawingPath.getRandom(listOf(distance.toInt(), 1)) * 4).toInt() % 4)

            val patternSize = drawingPath.size

            // Draw different pattern elements
            when (patternType) {
                0 -> {
                    // Circle pattern
                    canvas.drawCircle(
                        point,
                        patternSize * 0.5f,
                        paint
                    )
                }
                1 -> {
                    // Square pattern
                    canvas.withSave {
                        // Rotate to align with path
                        val angle = atan2(tangent.y, tangent.x) * 180f / PI.toFloat()
                        canvas.translate(point.x, point.y)
                        canvas.rotate(angle)

                        canvas.drawRect(
                            Rect(
                                -patternSize/2, -patternSize/2,
                                patternSize/2, patternSize/2
                            ),
                            paint
                        )
                    }
                }
                2 -> {
                    // Star pattern
                    canvas.withSave {
                        val angle = atan2(tangent.y, tangent.x) * 180f / PI.toFloat()
                        canvas.translate(point.x, point.y)
                        canvas.rotate(angle)

                        val starPath = Path()
                        val points = 5
                        val innerRadius = patternSize * 0.25f
                        val outerRadius = patternSize * 0.5f

                        for (i in 0 until points * 2) {
                            val starAngle = i * PI.toFloat() / points
                            val radius = if (i % 2 == 0) outerRadius else innerRadius
                            val x = cos(starAngle) * radius
                            val y = sin(starAngle) * radius

                            if (i == 0) {
                                starPath.moveTo(x, y)
                            } else {
                                starPath.lineTo(x, y)
                            }
                        }

                        starPath.close()
                        canvas.drawPath(starPath, paint)
                    }
                }
                3 -> {
                    // Diamond pattern
                    canvas.withSave {
                        val angle = atan2(tangent.y, tangent.x) * 180f / PI.toFloat()
                        canvas.translate(point.x, point.y)
                        canvas.rotate(angle)

                        val diamondPath = Path().apply {
                            moveTo(0f, -patternSize/2)
                            lineTo(patternSize/2, 0f)
                            lineTo(0f, patternSize/2)
                            lineTo(-patternSize/2, 0f)
                            close()
                        }

                        canvas.drawPath(diamondPath, paint)
                    }
                }
            }

            distance += patternSpacing
        }

        // Add connecting line between pattern elements for continuity
        val connectPaint = Paint().apply {
            color = drawingPath.color.copy(alpha = 0.3f)
            style = PaintingStyle.Stroke
            strokeWidth = drawingPath.size * 0.2f
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)
        }

        canvas.drawPath(path, connectPaint)
    }
)

val texturedSpongeBrush = BrushData(
    id = 207,
    name = "Textured Sponge",
    stroke = "textured_sponge_stroke",
    densityOffset = 2.0,
    opacityDiff = 0.6f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        var distance = 0f
        val step = drawingPath.size * 0.5f

        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += step
                continue
            }

            // Generate a sponge texture at each point
            val textureRadius = drawingPath.size * 1.5f
            val cellSize = drawingPath.size * 0.3f

            // Create the sponge pattern - clusters of irregular cells
            val cellCount = (PI * textureRadius * textureRadius / (cellSize * cellSize)).toInt().coerceIn(10, 100)

            for (i in 0 until cellCount) {
                val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))
                val random4 = drawingPath.getRandom(listOf(distance.toInt(), i, 4))

                // Create clusters with varying density
                val angle = random1 * PI.toFloat() * 2
                // Distribute cells in clusters rather than evenly
                val radius = textureRadius * sqrt(random2) * (0.7f + 0.3f * sin(angle * 3))

                val cellPosition = point + Offset(
                    cos(angle) * radius,
                    sin(angle) * radius
                )

                // Vary cell sizes for natural sponge texture
                val cellRadius = cellSize * (0.5f + random3 * 0.8f)

                // Some cells are holes (negative space) in the sponge
                if (random4 > 0.3f) {
                    // Create an irregular blob shape for each sponge cell
                    val cellPath = Path()
                    val blobPoints = 6
                    val angleStep = (2 * PI.toFloat()) / blobPoints

                    for (j in 0..blobPoints) {
                        val blobAngle = j * angleStep
                        val blobRandom = drawingPath.getRandom(listOf(distance.toInt(), i, j, 100))
                        val blobRadius = cellRadius * (0.8f + blobRandom * 0.4f)

                        val x = cellPosition.x + cos(blobAngle) * blobRadius
                        val y = cellPosition.y + sin(blobAngle) * blobRadius

                        if (j == 0) {
                            cellPath.moveTo(x, y)
                        } else {
                            // Use quadratic curves for smoother, more organic shapes
                            val prevAngle = (j - 1) * angleStep
                            val prevX = cellPosition.x + cos(prevAngle) * cellRadius * (0.8f + drawingPath.getRandom(listOf(distance.toInt(), i, j-1, 100)) * 0.4f)
                            val prevY = cellPosition.y + sin(prevAngle) * cellRadius * (0.8f + drawingPath.getRandom(listOf(distance.toInt(), i, j-1, 100)) * 0.4f)

                            val ctrlX = (prevX + x) / 2 + (drawingPath.getRandom(listOf(distance.toInt(), i, j, 200)) - 0.5f) * cellRadius * 0.5f
                            val ctrlY = (prevY + y) / 2 + (drawingPath.getRandom(listOf(distance.toInt(), i, j, 300)) - 0.5f) * cellRadius * 0.5f

                            cellPath.quadraticBezierTo(ctrlX, ctrlY, x, y)
                        }
                    }

                    cellPath.close()

                    // Vary opacity for texture depth
                    val cellOpacity = 0.2f + random3 * 0.4f

                    canvas.drawPath(
                        cellPath,
                        Paint().apply {
                            color = drawingPath.color.copy(alpha = cellOpacity)
                            style = PaintingStyle.Fill
                        }
                    )
                }
            }

            distance += step
        }
    }
)

val watercolorDryBrush = BrushData(
    id = 208,
    name = "Watercolor Dry Brush",
    stroke = "watercolor_dry_brush_stroke",
    densityOffset = 2.0,
    opacityDiff = 0.7f,
    customPainter = { canvas, size, drawingPath ->
        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length

        var distance = 0f
        val step = max(1f, drawingPath.size / 8f)
        var prevPoint: Offset? = null

        // Dry brush has a characteristic broken, textured look
        while (distance <= length) {
            val point = measure.getPosition(distance)
            if (point.isUnspecified) {
                distance += step
                continue
            }

            if (prevPoint != null) {
                // Direction of stroke
                val direction = Offset(
                    point.x - prevPoint.x,
                    point.y - prevPoint.y
                ).normalize()

                // Perpendicular for bristle spread
                val perpendicular = Offset(-direction.y, direction.x)

                // Create multiple bristle lines with gaps - characteristic of dry brush
                val bristleCount = (drawingPath.size / 2).toInt().coerceIn(4, 15)

                for (i in 0 until bristleCount) {
                    val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 1))
                    val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 2))
                    val random3 = drawingPath.getRandom(listOf(distance.toInt(), i, 3))

                    // Calculate spread across the brush width
                    val spreadFactor = (i.toFloat() / (bristleCount - 1)) * 2f - 1f // -1 to 1

                    // Add randomness to create broken, textured look
                    val bristleOffset = perpendicular * drawingPath.size * 0.7f * spreadFactor +
                            Offset(
                                (random1 - 0.5f) * drawingPath.size * 0.3f,
                                (random2 - 0.5f) * drawingPath.size * 0.3f
                            )

                    // Only draw some bristles for gaps (dry brush effect)
                    if (random3 > 0.4f) {
                        val bristlePaint = Paint().apply {
                            color = drawingPath.color.copy(alpha = 0.1f + random3 * 0.2f)
                            strokeWidth = drawingPath.size * 0.1f * (0.5f + random2 * 0.5f)
                            strokeCap = StrokeCap.Round
                            style = PaintingStyle.Stroke
                        }

                        canvas.drawLine(
                            prevPoint + bristleOffset,
                            point + bristleOffset,
                            bristlePaint
                        )
                    }
                }

                // Add dry brush texture splatters
                if (Random(distance.toInt()).nextFloat() > 0.8f) {
                    val splatterCount = (drawingPath.size / 5).toInt().coerceIn(2, 8)

                    for (i in 0 until splatterCount) {
                        val random1 = drawingPath.getRandom(listOf(distance.toInt(), i, 100))
                        val random2 = drawingPath.getRandom(listOf(distance.toInt(), i, 200))

                        val splatterOffset = Offset(
                            (random1 - 0.5f) * drawingPath.size * 2f,
                            (random2 - 0.5f) * drawingPath.size * 2f
                        )

                        val splatterSize = drawingPath.size * 0.15f * random1

                        canvas.drawCircle(
                            point + splatterOffset,
                            splatterSize,
                            Paint().apply {
                                color = drawingPath.color.copy(alpha = 0.05f + random2 * 0.1f)
                                style = PaintingStyle.Fill
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

val fineArtBrushes = listOf(
    impressionistBrush
)

val natureBrushes = listOf(
    foliageBrush,
    waterRippleBrush
)

val specialEffectsBrushes = listOf(
    magicParticlesBrush,
    chalkBrush
)

val artisticStyleBrushes = listOf(
    comicInkBrush,
    pastelBrush,
    gouacheBrush,
    blueprintPenBrush,
    mangaGPenBrush,
    charcoalPencilBrush,
    patternBrush,
    texturedSpongeBrush,
    watercolorDryBrush,
)

val allPremiumBrushes = fineArtBrushes + natureBrushes + specialEffectsBrushes + artisticStyleBrushes

// Add this function to BrushData.Companion to easily get all premium brushes
fun BrushData.Companion.getAllPremiumBrushes(): List<BrushData> {
    return allPremiumBrushes
}