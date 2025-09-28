package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max

/**
 * Simple pressure-aware pen brush that draws a continuous round stroke
 * by stamping filled circles at fixed spacing along the path.
 *
 * Spacing is in destination pixels. If spacing <= 0f, a sensible default
 * based on size (0.4 * size) is used.
 */
class PenBrush : Brush() {

    override val id = ToolId("pen")
    override val name: String = "Pen"

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = PenStrokeSession(canvas, params)

    private class PenStrokeSession(
        private val canvas: Canvas,
        params: BrushParams
    ) : StrokeSession(params) {

        // ----- Style / state -----
        private val paint = Paint().also {
            it.isAntiAlias = true
            it.color = params.color
            it.blendMode = params.blendMode
        }
        private val stepPx: Float = params.size * 0.2f

        // Current working path (for geometry only; we stamp circles for thickness)
        private val path = Path()

        // Anchor history for midevent smoothing: p0, p1 are last two raw events.
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null      // The last midevent we've reached in the path.

        // Pressure interpolation across segments
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f         // distance since last stamp along current curve

        // ----- Small helpers -----
        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        private fun radiusFor(pressure: Float): Float = max(0.1f, params.size * (if (pressure > 0f) pressure else params.pressure) * 0.5f)

        private fun stamp(canvas: Canvas, c: Offset, r: Float): Rect {
            canvas.drawCircle(c, r, paint)
            return Rect(c.x - r, c.y - r, c.x + r, c.y + r)
        }

        // Quadratic Bezier event: (1-t)^2 * a + 2(1-t)t * b + t^2 * c
        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            val x = one * one * a.x + 2f * one * t * b.x + t * t * c.x
            val y = one * one * a.y + 2f * one * t * b.y + t * t * c.y
            return Offset(x, y)
        }

        /**
         * Walk a quadratic Bezier from 'a' (current) to 'c' with control 'b',
         * placing stamps every ~stepPx using residual carry-over between calls.
         * Returns the union of updated dirty rects.
         */
        private fun walkQuadratic(
            canvas: Canvas,
            a: Offset,
            b: Offset,
            c: Offset,
            pStart: Float,
            pEnd: Float
        ): DirtyRect {
            // Subdivide adaptively based on chord length to keep spacing stable.
            val approxLen = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c)) // rough but fast
            val subdivisions = max(8, ceil(approxLen / 6f).toInt()) // finer for long curves
            val dt = 1f / subdivisions

            var dirty: DirtyRect = null
            var prev = a
            var tAcc = 0f
            var acc = residual

            var t = dt
            while (t <= 1f + 1e-4f) {
                val cur = qPoint(a, b, c, t.coerceAtMost(1f))
                val seg = dist(prev, cur)

                var remain = seg
                // place stamps every stepPx along the polyline approximation
                while (acc + remain >= stepPx && remain > 0f) {
                    val need = stepPx - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)                      // uses your Offset.lerp
                    val tt = (t - dt) + dt * f                      // local t estimate for pressure lerp
                    val press = pStart + (pEnd - pStart) * tt
                    dirty = dirty.union(stamp(canvas, hit, radiusFor(press)))
                    // advance along the polyline to the hit event
                    prev = hit
                    remain -= need
                    acc = 0f
                }

                // advance to end of this small segment
                acc += remain
                prev = cur
                tAcc = t
                t += dt
            }

            residual = acc
            return dirty
        }

        // ----- StrokeSession API -----

        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            residual = 0f
            lastPressure = event.pressure ?: params.pressure

            // Initial dot so a tap leaves a mark.
            val r = radiusFor(lastPressure)
            path.reset()
            path.moveTo(event.position.x, event.position.y)
            return stamp(canvas, event.position, r)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure

            var dirty: DirtyRect = null
            when {
                p0 == null -> {
                    // shouldn't happen, but be safe
                    dirty = start(event)
                }

                p1 == null -> {
                    // First real segment: p0 .. newP. Use (p0 -> m01) quad.
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
                    // Typical case: we have p0, p1; incoming newP = p2
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
            // Ensure the final tail (lastMid -> last anchor) is rendered.
            val endPos = event.position
            val endPressure = event.pressure ?: params.pressure

            // Run through move() to make sure state is up-to-date to this final event.
            val moved = move(event)

            var dirty: DirtyRect = moved
            val a = lastMid
            val ctrl = p1
            if (a != null && ctrl != null) {
                // Append last segment from lastMid to last anchor using ctrl=anchor (acts like a line).
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

            // Final cap to guarantee a solid end.
            dirty = dirty.union(stamp(canvas, endPos, radiusFor(endPressure)))
            return dirty
        }
    }
}