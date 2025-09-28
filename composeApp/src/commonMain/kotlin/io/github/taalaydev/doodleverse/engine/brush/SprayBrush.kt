package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlin.random.Random

/**
 * Spray/Airbrush that creates particle-like spray effects with random distribution.
 * Simulates an airbrush by placing many small particles within a circular area,
 * with higher density in the center and lower density at the edges.
 */
class SprayBrush(
    private val particleCount: Int = 30, // Number of particles per stamp
    private val particleSize: Float = 1.5f, // Base size of individual particles
    private val falloffPower: Float = 2f, // Controls density falloff from center (higher = more concentrated)
    private val opacityVariation: Float = 0.3f, // 0-1, opacity variation between particles
    override val id: ToolId = ToolId("spray"),
    override val name: String = "Spray"
) : Brush() {

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = SprayStrokeSession(canvas, params, particleCount, particleSize, falloffPower, opacityVariation)

    private class SprayStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val particleCount: Int,
        private val particleSize: Float,
        private val falloffPower: Float,
        private val opacityVariation: Float
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            color = params.color
            blendMode = params.blendMode
        }

        private val stepPx: Float = params.size * 0.3f
        private val path = Path()

        // Random number generator with consistent seed per stroke
        private val random: Random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

        // State tracking
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float = max(2f, params.size * pressure * 0.5f)

        /**
         * Generates a random point within a circle using polar coordinates
         * with bias towards the center based on falloffPower
         */
        private fun randomPointInCircle(center: Offset, radius: Float): Offset {
            // Generate biased distance from center
            val u = random.nextFloat()
            val biasedDistance = radius * u.pow(1f / falloffPower)

            // Generate random angle
            val angle = random.nextFloat() * 2f * PI.toFloat()

            return Offset(
                center.x + biasedDistance * cos(angle),
                center.y + biasedDistance * sin(angle)
            )
        }

        private fun drawSprayStamp(canvas: Canvas, center: Offset, radius: Float, pressure: Float): Rect {
            val adjustedParticleCount = (particleCount * pressure).toInt().coerceAtLeast(5)

            for (i in 0 until adjustedParticleCount) {
                val particlePos = randomPointInCircle(center, radius)

                // Distance from center affects particle size and opacity
                val distanceFromCenter = center.distanceTo(particlePos)
                val normalizedDistance = (distanceFromCenter / radius).coerceIn(0f, 1f)

                // Particle size decreases with distance from center
                val sizeMultiplier = 1f - normalizedDistance * 0.5f
                val currentParticleSize = particleSize * sizeMultiplier * pressure

                // Opacity decreases with distance and has random variation
                val baseOpacity = params.color.alpha * (1f - normalizedDistance * 0.3f)
                val opacityVar = 1f + (random.nextFloat() - 0.5f) * opacityVariation
                val particleOpacity = (baseOpacity * opacityVar).coerceIn(0f, 1f)

                // Create particle paint
                val particleColor = params.color.copy(alpha = particleOpacity)
                val particlePaint = paint.copy().apply { color = particleColor }

                // Draw particle as small circle
                canvas.drawCircle(particlePos, currentParticleSize, particlePaint)
            }

            // Return bounding rect
            val padding = radius + particleSize
            return Rect(
                center.x - padding,
                center.y - padding,
                center.x + padding,
                center.y + padding
            )
        }

        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            val x = one * one * a.x + 2f * one * t * b.x + t * t * c.x
            val y = one * one * a.y + 2f * one * t * b.y + t * t * c.y
            return Offset(x, y)
        }

        private fun walkQuadratic(
            canvas: Canvas,
            a: Offset,
            b: Offset,
            c: Offset,
            pStart: Float,
            pEnd: Float
        ): DirtyRect {
            val approxLen = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            val subdivisions = max(8, ceil(approxLen / 5f).toInt())
            val dt = 1f / subdivisions

            var dirty: DirtyRect = null
            var prev = a
            var acc = residual

            var t = dt
            while (t <= 1f + 1e-4f) {
                val cur = qPoint(a, b, c, t.coerceAtMost(1f))
                val seg = dist(prev, cur)

                var remain = seg
                while (acc + remain >= stepPx && remain > 0f) {
                    val need = stepPx - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)
                    val tt = (t - dt) + dt * f
                    val press = pStart + (pEnd - pStart) * tt

                    dirty = dirty.union(drawSprayStamp(canvas, hit, radiusFor(press), press))
                    prev = hit
                    remain -= need
                    acc = 0f
                }

                acc += remain
                prev = cur
                t += dt
            }

            residual = acc
            return dirty
        }

        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            residual = 0f
            lastPressure = event.pressure ?: params.pressure

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return drawSprayStamp(canvas, event.position, radiusFor(lastPressure), lastPressure)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure

            var dirty: DirtyRect = null
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
                    dirty = dirty.union(
                        walkQuadratic(
                            canvas = canvas,
                            a = lastMid!!,
                            b = a,
                            c = m01,
                            pStart = lastPressure,
                            pEnd = newPressure
                        )
                    )
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
                    dirty = dirty.union(
                        walkQuadratic(
                            canvas = canvas,
                            a = lastMid!!,
                            b = a1,
                            c = m2,
                            pStart = lastPressure,
                            pEnd = newPressure
                        )
                    )

                    lastMid = m2
                    p0 = a1
                    p1 = a2
                }
            }

            lastPressure = newPressure
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val endPos = event.position
            val endPressure = event.pressure ?: params.pressure
            val moved = move(event)

            var dirty: DirtyRect = moved
            val a = lastMid
            val ctrl = p1
            if (a != null && ctrl != null) {
                path.quadraticBezierTo(ctrl.x, ctrl.y, ctrl.x, ctrl.y)
                dirty = dirty.union(
                    walkQuadratic(
                        canvas = canvas,
                        a = a,
                        b = ctrl,
                        c = ctrl,
                        pStart = lastPressure,
                        pEnd = endPressure
                    )
                )
            }

            // Final spray burst at end position
            dirty = dirty.union(drawSprayStamp(canvas, endPos, radiusFor(endPressure), endPressure))
            return dirty
        }
    }
}