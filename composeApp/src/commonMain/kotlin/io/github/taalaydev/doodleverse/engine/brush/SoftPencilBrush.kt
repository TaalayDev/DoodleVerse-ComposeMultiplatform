package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.ProceduralBrush
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlinx.datetime.Clock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Enhanced soft pencil with realistic texture, pressure sensitivity, and natural variation.
 * Features:
 * - Pressure-sensitive opacity and size
 * - Speed-sensitive density (slower = denser)
 * - Directional texture aligned with stroke direction
 * - Paper texture simulation with noise
 * - Natural hair distribution with clustering
 * - Build-up effect for overlapping strokes
 * - Organic edge variation
 */
class SoftPencilBrush : ProceduralBrush() {

    override val id = ToolId("soft_pencil")
    override val name: String = "Soft Pencil"

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = SoftPencilStrokeSession(canvas, params)
}

private class SoftPencilStrokeSession(
    private val canvas: Canvas,
    params: BrushParams,
) : StrokeSession(params) {

    // Smoothed path we extend as the user moves
    private val path = Path()
    private val measure = PathMeasure()
    private var lastMeasuredLen: Float = 0f

    // Anchor history for quadratic smoothing
    private var p0: Offset? = null
    private var p1: Offset? = null
    private var lastMid: Offset? = null
    private var lastPressure: Float = 1.0f
    private var lastTimestamp: Long = 0L
    private var lastSpeed: Float = 0f

    // Stable RNG seed per stroke
    private val seed: Float

    // Enhanced pencil properties
    private val paint = Paint().apply {
        isAntiAlias = true
        color = params.color
        style = PaintingStyle.Stroke
        strokeCap = StrokeCap.Round
        blendMode = params.blendMode
    }

    // Enhanced constants for realistic pencil simulation
    private val baseNumHairs = 8 // More hair lines for better texture
    private val maxNumHairs = 16 // Variable hair count based on pressure
    private val baseAlpha = 0.15f // Softer base opacity
    private val maxAlpha = 0.4f // Maximum opacity for high pressure
    private val baseWidth = params.size * 0.03f // Thinner base width
    private val segmentLen = 6f // Shorter segments for smoother texture
    private val spreadFactor = 0.4f // Wider spread for softer edges
    private val textureScale = 0.8f // Scale for paper texture simulation
    private val speedSensitivity = 0.3f // How much speed affects density
    private val pressureSensitivity = 0.7f // How much pressure affects appearance
    private val clusterTendency = 0.6f // Tendency for hair lines to cluster

    // Step size - smaller for smoother rendering
    private val delta: Float = min(params.size / 4f, 2f)

    init {
        val base = (0xFF127755 xor params.hashCode().toLong()).toFloat()
        seed = base * 0.017f
    }

    // Enhanced random function with more variation
    private fun rand01(a: Float, b: Float, c: Float, d: Int, e: Int): Float {
        val x = a * 12.9898f + b * 78.233f + c * 37.719f + d * 0.123f + e * 0.517f + seed
        val s = sin(x.toDouble())
        val u = abs(s * 43758.5453123)
        return (u - floor(u)).toFloat()
    }

    // Simulate paper texture using noise
    private fun paperTexture(x: Float, y: Float): Float {
        val scale = textureScale
        val noise1 = rand01(x * scale, y * scale, 0f, 0, 0)
        val noise2 = rand01(x * scale * 2f, y * scale * 2f, 1f, 1, 1)
        val noise3 = rand01(x * scale * 4f, y * scale * 4f, 2f, 2, 2)

        // Combine multiple octaves of noise for realistic paper texture
        return (noise1 * 0.5f + noise2 * 0.3f + noise3 * 0.2f).coerceIn(0.3f, 1.0f)
    }

    // Calculate speed from gesture events
    private fun calculateSpeed(event: GestureEvent): Float {
        val currentTime = Clock.currentTimeMillis()
        if (lastTimestamp == 0L) {
            lastTimestamp = currentTime
            return 0f
        }

        val deltaTime = (currentTime - lastTimestamp).toFloat()
        lastTimestamp = currentTime

        if (deltaTime <= 0f) return lastSpeed

        val lastPos = p1 ?: p0 ?: event.position
        val distance = sqrt((event.position.x - lastPos.x).pow(2) + (event.position.y - lastPos.y).pow(2))
        val speed = distance / (deltaTime + 1f) // Add 1 to avoid division by zero

        // Smooth speed changes
        lastSpeed = lastSpeed * 0.7f + speed * 0.3f
        return lastSpeed
    }

    // Enhanced initial mark with pressure and texture
    private fun drawInitialMark(canvas: Canvas, p: Offset, pressure: Float): DirtyRect {
        var dirty: DirtyRect = null
        val effectivePressure = pressure.coerceIn(0.1f, 1.0f)
        val numHairs = (baseNumHairs + (maxNumHairs - baseNumHairs) * effectivePressure).toInt()

        for (j in 0 until numHairs) {
            // Create clustered distribution for more natural look
            val clusterAngle = rand01(0f, p.x, p.y, j, 0) * 2f * PI.toFloat()
            val clusterRadius = rand01(0f, p.x, p.y, j, 1) * params.size * spreadFactor
            val clusterOffset = if (rand01(0f, p.x, p.y, j, 2) < clusterTendency) {
                Offset(
                    (cos(clusterAngle) * clusterRadius * 0.5f).toFloat(),
                    (sin(clusterAngle) * clusterRadius * 0.5f).toFloat()
                )
            } else {
                Offset(
                    (rand01(0f, p.x, p.y, j, 3) - 0.5f) * params.size * spreadFactor,
                    (rand01(0f, p.x, p.y, j, 4) - 0.5f) * params.size * spreadFactor
                )
            }

            val a = p + clusterOffset

            // Apply paper texture
            val texture = paperTexture(a.x, a.y)

            // Pressure-sensitive alpha and width
            val alphaMod = rand01(0f, p.x, p.y, j, 5)
            val alpha = (baseAlpha + (maxAlpha - baseAlpha) * effectivePressure) * texture * (0.6f + alphaMod * 0.4f)
            paint.alpha = alpha.coerceIn(0f, 1f)

            val widthMod = rand01(0f, p.x, p.y, j, 6)
            paint.strokeWidth = baseWidth * (1f + effectivePressure * pressureSensitivity) * (0.7f + widthMod * 0.3f)

            canvas.drawLine(a, a, paint)

            val hw = paint.strokeWidth * 0.5f
            val rect = Rect(a.x - hw, a.y - hw, a.x + hw, a.y + hw)
            dirty = dirty.union(rect)
        }
        return dirty ?: Rect.Zero
    }

    // Enhanced tail rendering with pressure, speed, and texture
    private fun renderNewTail(canvas: Canvas, currentPressure: Float, currentSpeed: Float): DirtyRect {
        measure.setPath(path, false)
        val total = measure.length
        if (total <= lastMeasuredLen || delta <= 0.01f) return Rect.Zero

        var dirty: DirtyRect = null
        var i = lastMeasuredLen
        val effectivePressure = currentPressure.coerceIn(0.1f, 1.0f)

        // Speed affects density - slower strokes are denser
        val speedFactor = 1f - (currentSpeed * speedSensitivity).coerceIn(0f, 0.8f)
        val densityMultiplier = 0.5f + speedFactor * 1.5f

        while (i < total) {
            val progress = i / total
            val p = measure.getPosition(i)
            val tan = measure.getTangent(i)
            val tanLen = sqrt(tan.x * tan.x + tan.y * tan.y)
            if (tanLen < 0.001f) {
                i += delta
                continue
            }

            val unitTan = Offset(tan.x / tanLen, tan.y / tanLen)
            val unitPerp = Offset(-tan.y / tanLen, tan.x / tanLen)

            val nextAt = min(total, i + segmentLen)
            val q = measure.getPosition(nextAt)

            // Variable hair count based on pressure and speed
            val numHairs = ((baseNumHairs + (maxNumHairs - baseNumHairs) * effectivePressure) * densityMultiplier).toInt()

            for (j in 0 until numHairs) {
                // Enhanced hair distribution with directional bias
                val perpOffset = (rand01(i, p.x, p.y, j, 0) - 0.5f) * params.size * spreadFactor * 2f
                val tangentOffset = (rand01(i, p.x, p.y, j, 1) - 0.5f) * segmentLen * 0.3f

                val off1 = unitPerp * perpOffset + unitTan * tangentOffset
                val off2Variation = (rand01(i, p.x, p.y, j, 2) - 0.5f) * 0.4f
                val off2 = unitPerp * (perpOffset * (1f + off2Variation)) + unitTan * tangentOffset

                val a = p + off1
                val b = q + off2

                // Apply paper texture
                val textureMid = paperTexture((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

                // Enhanced alpha calculation with build-up simulation
                val alphaMod = rand01(i, p.x, p.y, j, 3)
                val baseAlphaForStroke = baseAlpha + (maxAlpha - baseAlpha) * effectivePressure
                val alpha = baseAlphaForStroke * textureMid * densityMultiplier * (0.4f + alphaMod * 0.6f)
                paint.alpha = alpha.coerceIn(0.05f, maxAlpha)

                // Enhanced width calculation
                val widthMod = rand01(i, p.x, p.y, j, 4)
                val pressureWidth = 1f + effectivePressure * pressureSensitivity
                paint.strokeWidth = baseWidth * pressureWidth * (0.6f + widthMod * 0.4f)

                canvas.drawLine(a, b, paint)

                // Calculate dirty rect
                val hw = paint.strokeWidth * 0.5f
                val left = min(a.x, b.x) - hw
                val top = min(a.y, b.y) - hw
                val right = max(a.x, b.x) + hw
                val bottom = max(a.y, b.y) + hw
                dirty = dirty.union(Rect(left, top, right, bottom))
            }

            i += delta
        }

        lastMeasuredLen = total
        return dirty ?: Rect.Zero
    }

    private fun midevent(a: Offset, b: Offset) =
        Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

    override fun start(event: GestureEvent): DirtyRect {
        p0 = event.position
        p1 = null
        lastMid = null
        lastMeasuredLen = 0f
        lastPressure = event.pressure ?: params.pressure
        lastTimestamp = Clock.currentTimeMillis()
        lastSpeed = 0f

        path.reset()
        path.moveTo(event.position.x, event.position.y)

        return drawInitialMark(canvas, event.position, event.pressure ?: params.pressure)
    }

    override fun move(event: GestureEvent): DirtyRect? {
        val newP = event.position
        val currentSpeed = calculateSpeed(event)
        var dirty: DirtyRect = null

        // Smooth pressure changes
        lastPressure = lastPressure * 0.8f + (event.pressure ?: params.pressure) * 0.2f

        when {
            p0 == null -> {
                dirty = start(event)
            }
            p1 == null -> {
                p1 = newP
                val a = p0!!
                val m01 = midevent(a, newP)
                if (lastMid == null) {
                    path.moveTo(a.x, a.y)
                    lastMid = a
                }
                path.quadraticBezierTo(a.x, a.y, m01.x, m01.y)
                dirty = dirty.union(renderNewTail(canvas, lastPressure, currentSpeed))
                lastMid = m01
            }
            else -> {
                val a0 = p0!!
                val a1 = p1!!
                val a2 = newP

                val m1 = midevent(a0, a1)
                val m2 = midevent(a1, a2)

                if (lastMid == null) {
                    path.moveTo(m1.x, m1.y)
                    lastMid = m1
                }

                path.quadraticBezierTo(a1.x, a1.y, m2.x, m2.y)
                dirty = dirty.union(renderNewTail(canvas, lastPressure, currentSpeed))

                lastMid = m2
                p0 = a1
                p1 = a2
            }
        }

        return dirty
    }

    override fun end(event: GestureEvent): DirtyRect {
        val currentSpeed = calculateSpeed(event)
        val moved = move(event)
        val rest = renderNewTail(canvas, lastPressure, currentSpeed)
        return moved.union(rest)
    }
}