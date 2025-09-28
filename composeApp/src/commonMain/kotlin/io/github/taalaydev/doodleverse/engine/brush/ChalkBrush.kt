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
 * Chalk brush that simulates the texture and opacity variations of real chalk
 * on a rough surface. Creates irregular opacity patterns and grainy texture.
 */
class ChalkBrush(
    private val textureIntensity: Float = 0.7f, // 0-1, intensity of texture variations
    private val grainSize: Float = 2f, // Size of texture grains
    private val coverage: Float = 0.8f, // 0-1, how much of the stroke area is covered
    override val id: ToolId = ToolId("chalk"),
    override val name: String = "Chalk"
) : Brush() {

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = ChalkStrokeSession(canvas, params, textureIntensity, grainSize, coverage)

    private class ChalkStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val textureIntensity: Float,
        private val grainSize: Float,
        private val coverage: Float
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = false // Chalk has rough edges
            color = params.color
            blendMode = params.blendMode
        }

        private val stepPx: Float = params.size * 0.4f
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

        private fun radiusFor(pressure: Float): Float = max(1f, params.size * pressure * 0.5f)

        private fun drawChalkStamp(canvas: Canvas, center: Offset, radius: Float): Rect {
            val baseRadius = radius
            val grainCount = (baseRadius * baseRadius * 0.3f).toInt().coerceAtMost(50) // Limit for performance

            // Create chalk texture by drawing many small, randomly placed and sized circles
            for (i in 0 until grainCount) {
                // Random position within the stroke area
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val distance = random.nextFloat() * baseRadius * 0.9f
                val grainCenter = Offset(
                    center.x + cos(angle) * distance,
                    center.y + sin(angle) * distance
                )

                // Random grain size
                val grainRadius = grainSize * (0.5f + random.nextFloat() * 0.5f)

                // Random opacity based on texture intensity and coverage
                val baseAlpha = params.color.alpha * coverage
                val alphaVariation = textureIntensity * 0.6f
                val alpha = (baseAlpha * (1f - alphaVariation + random.nextFloat() * alphaVariation)).coerceIn(0f, 1f)

                // Only draw if this grain should be visible (simulates paper texture)
                if (random.nextFloat() < coverage) {
                    val grainColor = params.color.copy(alpha = alpha)
                    val grainPaint = paint.copy().apply { color = grainColor }

                    canvas.drawCircle(grainCenter, grainRadius, grainPaint)
                }
            }

            // Add some larger, more solid areas for chalk consistency
            val solidSpots = (baseRadius * 0.1f).toInt().coerceAtLeast(1).coerceAtMost(5)
            for (i in 0 until solidSpots) {
                val angle = random.nextFloat() * 2f * PI.toFloat()
                val distance = random.nextFloat() * baseRadius * 0.6f
                val spotCenter = Offset(
                    center.x + cos(angle) * distance,
                    center.y + sin(angle) * distance
                )

                val spotRadius = baseRadius * (0.2f + random.nextFloat() * 0.3f)
                val spotAlpha = params.color.alpha * (0.6f + random.nextFloat() * 0.3f)
                val spotColor = params.color.copy(alpha = spotAlpha)
                val spotPaint = paint.copy().apply { color = spotColor }

                canvas.drawCircle(spotCenter, spotRadius, spotPaint)
            }

            // Return conservative bounding rect
            val padding = baseRadius + grainSize
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
            val subdivisions = max(8, ceil(approxLen / 4f).toInt())
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

                    dirty = dirty.union(drawChalkStamp(canvas, hit, radiusFor(press)))
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

            return drawChalkStamp(canvas, event.position, radiusFor(lastPressure))
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

            // Final stamp for clean ending
            dirty = dirty.union(drawChalkStamp(canvas, endPos, radiusFor(endPressure)))
            return dirty
        }
    }
}