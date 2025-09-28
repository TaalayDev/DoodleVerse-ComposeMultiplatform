package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.ProceduralBrush
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A pressure/velocity-aware sketch pencil that simulates graphite by layering
 * multiple jittered "hair" strokes with subtle grain dots along a smoothed path.
 *
 * Features
 * - Quadratic mid-point smoothing (like PenBrush)
 * - Residual spacing so density is stable regardless of framerate
 * - Pressure controls width; velocity slightly lightens the mark
 * - Jittered parallel hairs + micro-dots to emulate paper/graphite texture
 */
class SketchPencilBrush : ProceduralBrush() {

    override val id: ToolId = ToolId("sketch_pencil")
    override val name: String = "Sketch Pencil"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        PencilStrokeSession(canvas, params)

    // ------------------------------------------------------------
    private class PencilStrokeSession(
        private val canvas: Canvas,
        params: BrushParams
    ) : StrokeSession(params) {

        // ---- Style knobs (tweak to taste) ----
        private val baseStep = params.size * 0.35f
        private val hairs = max(3, (2 + params.size.toInt() / 4))            // count of parallel strands
        private val jitterAmp = params.size * 0.28f                           // normal jitter amplitude
        private val hairLenMul = 0.9f                                         // relative to local segment step
        private val minHairWidth = max(0.6f, params.size * 0.06f)
        private val maxHairWidth = max(minHairWidth, params.size * 0.22f)
        private val dotChance = 0.25f                                         // probability of grain dot per stamp
        private val dotRadiusRange = params.size * 0.04f..params.size * 0.18f

        // ---- Paint (reused) ----
        private val stroke = Paint().also { p ->
            p.isAntiAlias = true
            p.style = PaintingStyle.Stroke
            p.strokeCap = StrokeCap.Round
            p.blendMode = params.blendMode
            p.color = params.color
        }
        private val fill = Paint().also { p ->
            p.isAntiAlias = true
            p.style = PaintingStyle.Fill
            p.blendMode = params.blendMode
            p.color = params.color
        }

        // ---- Path smoothing like PenBrush ----
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f

        // Deterministic-ish random per stroke
        private val seed = params.hashCode() * 1664525 + 1013904223
        private fun rngFor(i: Int): Random = Random(seed xor (i * 974_711))

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun mid(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        // Value-ish noise in [0,1]
        private fun vnoise(ix: Int, iy: Int, channel: Int): Float {
            var h = ix * 374761393 + iy * 668265263 + channel * 1442695040888963407L.toInt() + seed
            h = (h xor (h ushr 13)) * 1274126177
            h = h xor (h ushr 16)
            val u = (h ushr 1) and 0x7FFFFFFF
            return (u.toFloat() / Int.MAX_VALUE.toFloat()).coerceIn(0f, 1f)
        }

        private fun normal(a: Offset, b: Offset): Offset {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = max(1e-4f, hypot(dx, dy))
            // left-hand normal
            return Offset(-dy / len, dx / len)
        }

        private fun tangent(a: Offset, b: Offset): Offset {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = max(1e-4f, hypot(dx, dy))
            return Offset(dx / len, dy / len)
        }

        private fun radiusFor(pressure: Float): Float {
            val p = if (pressure > 0f) pressure else params.pressure
            return (params.size * (0.35f + 0.65f * p)) * 0.5f
        }

        private fun lineBounds(a: Offset, b: Offset, width: Float): Rect {
            val minX = min(a.x, b.x) - width
            val minY = min(a.y, b.y) - width
            val maxX = max(a.x, b.x) + width
            val maxY = max(a.y, b.y) + width
            return Rect(minX, minY, maxX, maxY)
        }

        private fun stampBundle(
            prev: Offset,
            cur: Offset,
            center: Offset,
            pressure: Float,
            localIdx: Int,
        ): DirtyRect {
            val n = normal(prev, cur)
            val t = tangent(prev, cur)

            // Velocity softens mark a bit (faster -> lighter)
            val velSoft = (1f - (params.velocity.coerceAtLeast(0f) / 4000f)).coerceIn(0.75f, 1f)
            val baseAlpha = (params.color.alpha * 0.9f * velSoft).coerceIn(0.04f, 1f)

            var dirty: DirtyRect = null

            val stepLen = max(1f, baseStep)
            val hairLen = stepLen * hairLenMul

            // Draw parallel "hairs"
            repeat(hairs) { i ->
                val rr = rngFor(localIdx * 131 + i)
                val sign = if (rr.nextBoolean()) 1f else -1f
                val jitter = (rr.nextFloat() - 0.5f) * 2f * jitterAmp
                val lateral = n * (jitter + sign * 0.15f * params.size)
                val start = center + lateral
                val end = start + t * hairLen

                val width = min(
                    maxHairWidth,
                    max(minHairWidth, radiusFor(pressure) * (0.55f + rr.nextFloat() * 0.75f))
                )

                stroke.strokeWidth = width
                stroke.color = params.color.copy(alpha = baseAlpha * (0.45f + 0.55f * rr.nextFloat()))
                canvas.drawLine(start, end, stroke)

                dirty = dirty.union(lineBounds(start, end, width * 0.6f))
            }

            // Occasional grain dots (graphite specks)
            val ix = center.x.toInt()
            val iy = center.y.toInt()
            val noise = vnoise(ix, iy, localIdx)
            if (noise < dotChance) {
                val r = dotRadiusRange.start + (dotRadiusRange.endInclusive - dotRadiusRange.start) * (noise / dotChance)
                val alpha = baseAlpha * 0.25f * (0.6f + 0.4f * noise)
                fill.color = params.color.copy(alpha = alpha)
                canvas.drawCircle(center, r, fill)
                val rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
                dirty = dirty.union(rect)
            }

            return dirty
        }

        // Quadratic Bezier helper like in PenBrush
        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            val x = one * one * a.x + 2f * one * t * b.x + t * t * c.x
            val y = one * one * a.y + 2f * one * t * b.y + t * t * c.y
            return Offset(x, y)
        }

        /**
         * Walk a quadratic curve placing pencil bundles roughly every baseStep pixels.
         */
        private fun walkQuadratic(
            a: Offset,
            b: Offset,
            c: Offset,
            pStart: Float,
            pEnd: Float,
            localBase: Int,
        ): DirtyRect {
            // Subdivide reasonably (arc-length-ish heuristic)
            val approxLen = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            val subdivisions = max(8, ceil(approxLen / 6f).toInt())
            val dt = 1f / subdivisions

            var dirty: DirtyRect = null
            var prev = a
            var acc = residual
            var t = dt
            var idx = localBase

            while (t <= 1f + 1e-4f) {
                val cur = qPoint(a, b, c, t.coerceAtMost(1f))
                val seg = dist(prev, cur)
                var remain = seg

                while (acc + remain >= baseStep && remain > 0f) {
                    val need = baseStep - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)
                    val tt = (t - dt) + dt * f
                    val press = pStart + (pEnd - pStart) * tt

                    dirty = dirty.union(stampBundle(prev, cur, hit, press, idx))

                    prev = hit
                    remain -= need
                    acc = 0f
                    idx += 1
                }

                acc += remain
                prev = cur
                t += dt
            }

            residual = acc
            return dirty
        }

        // ---- StrokeSession API ----
        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            residual = 0f
            lastPressure = event.pressure ?: params.pressure

            // A tiny scribble so a tap leaves a pencil dot/mark
            val a = event.position + Offset(-0.5f, -0.5f)
            val c = event.position + Offset(0.5f, 0.5f)
            return stampBundle(a, c, event.position, lastPressure, 0)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure

            var dirty: DirtyRect = null
            when {
                p0 == null -> dirty = start(event)
                p1 == null -> {
                    // First segment
                    p1 = newP
                    val a = p0!!
                    val m01 = mid(a, newP)
                    if (lastMid == null) lastMid = a
                    dirty = dirty.union(
                        walkQuadratic(
                            a = lastMid!!,
                            b = a,
                            c = m01,
                            pStart = lastPressure,
                            pEnd = newPressure,
                            localBase = 0,
                        )
                    )
                    lastMid = m01
                }
                else -> {
                    val a0 = p0!!
                    val a1 = p1!!
                    val a2 = newP
                    val m1 = mid(a0, a1)
                    val m2 = mid(a1, a2)
                    if (lastMid == null) lastMid = m1
                    dirty = dirty.union(
                        walkQuadratic(
                            a = lastMid!!,
                            b = a1,
                            c = m2,
                            pStart = lastPressure,
                            pEnd = newPressure,
                            localBase = 97,
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
            // Ensure we advance to the last position
            val moved = move(event)
            var dirty: DirtyRect = moved

            // Final short pass to ensure a solid tail
            val endPos = event.position
            val p = event.pressure ?: params.pressure
            val a = endPos + Offset(-0.3f, 0.3f)
            val c = endPos + Offset(0.3f, -0.3f)
            dirty = dirty.union(stampBundle(a, c, endPos, p, 10_000))
            return dirty
        }
    }
}
