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
 * GraphitePencilBrush — tighter, denser graphite than SketchPencil.
 *
 * Goals
 * - Crisp core line with darker edges (graphite edge accumulation)
 * - Subtle paper grain + micro‑hatching
 * - Pressure controls width & darkness; velocity slightly lightens
 * - Mid‑point quadratic smoothing + residual spacing (frame‑rate independent)
 * - Optional hardness knob (HB/2B/etc.) via constructor
 */
class GraphitePencilBrush(
    /** 0 = very soft (6B), 1 = hard (2H). */
    private val hardness: Float = 0.45f
) : ProceduralBrush() {

    override val id: ToolId = ToolId("graphite_pencil")
    override val name: String = "Graphite Pencil"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        GraphiteStrokeSession(canvas, params, hardness.coerceIn(0f, 1f))

    // ------------------------------------------------------------
    private class GraphiteStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val hardness: Float,
    ) : StrokeSession(params) {

        // ---- Tunables ----
        private val baseStep = max(1.5f, params.size * 0.33f)
        private val fibers = max(4, (3 + params.size.toInt() / 3))
        private val jitterAmp = params.size * (0.18f * (1f - 0.35f * hardness))
        private val edgeOffsetMul = 0.42f // how far dark edges sit from center (× width)
        private val grainChance = 0.18f
        private val grainRadiusRange = params.size * 0.03f..params.size * 0.14f
        private val hatchChance = 0.10f
        private val hatchLen = max(3f, params.size * 0.65f)

        // ---- Paints ----
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

        // ---- Smoothing state ----
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var residual: Float = 0f
        private var lastPressure: Float = params.pressure
        private var lastVelocity: Float = params.velocity

        // Per‑stroke RNG seed
        private val seed = params.hashCode() * 1664525 + 1013904223
        private fun rngFor(i: Int): Random = Random(seed xor (i * 1_234_567))

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun mid(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        private fun normal(a: Offset, b: Offset): Offset {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = max(1e-4f, hypot(dx, dy))
            return Offset(-dy / len, dx / len)
        }
        private fun tangent(a: Offset, b: Offset): Offset {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = max(1e-4f, hypot(dx, dy))
            return Offset(dx / len, dy / len)
        }

        private fun baseWidth(pressure: Float, velocity: Float): Float {
            val p = if (pressure > 0f) pressure else params.pressure
            val v = max(0f, velocity)
            val softness = 1f - hardness // softer pencils draw thicker lines
            val width = params.size * (0.35f + 0.55f * p) * (0.75f + 0.45f * softness)
            val speedThin = (1f - (v / 3600f)).coerceIn(0.86f, 1f)
            return width * speedThin
        }

        private fun coreAlpha(pressure: Float, velocity: Float): Float {
            val p = if (pressure > 0f) pressure else params.pressure
            val v = max(0f, velocity)
            val softness = 1f - hardness
            val base = params.color.alpha * (0.58f + 0.34f * p) * (0.70f + 0.60f * softness)
            val speedFade = (1f - (v / 4200f)).coerceIn(0.78f, 1f)
            return (base * speedFade).coerceIn(0.04f, 1f)
        }

        private fun edgeAlpha(core: Float): Float = (core * 1.15f).coerceIn(0.04f, 1f)

        private fun lineBounds(a: Offset, b: Offset, width: Float): Rect {
            val minX = min(a.x, b.x) - width
            val minY = min(a.y, b.y) - width
            val maxX = max(a.x, b.x) + width
            val maxY = max(a.y, b.y) + width
            return Rect(minX, minY, maxX, maxY)
        }

        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            val x = one * one * a.x + 2f * one * t * b.x + t * t * c.x
            val y = one * one * a.y + 2f * one * t * b.y + t * t * c.y
            return Offset(x, y)
        }

        private fun stampBundle(
            prev: Offset,
            cur: Offset,
            center: Offset,
            press: Float,
            veloc: Float,
            localIdx: Int,
        ): DirtyRect {
            var dirty: DirtyRect = null

            val n = normal(prev, cur)
            val t = tangent(prev, cur)
            val bw = baseWidth(press, veloc)
            val alpha = coreAlpha(press, veloc)

            // Core graphite pass (slight randomness in stroke width)
            run {
                val rr = rngFor(localIdx * 17 + 3)
                val half = max(5f, baseStep * 0.9f)
                val a = center - t * half
                val b = center + t * half
                val w = bw * (0.85f + rr.nextFloat() * 0.35f)
                stroke.strokeWidth = w
                stroke.color = params.color.copy(alpha = alpha)
                canvas.drawLine(a, b, stroke)
                dirty = dirty.union(lineBounds(a, b, w * 0.6f))
            }

            // Darker edges — graphite tends to catch on edges
            run {
                val rr = rngFor(localIdx * 19 + 5)
                val offset = n * (bw * edgeOffsetMul)
                val half = max(4f, baseStep * 0.8f)
                val a1 = center - t * half + offset
                val b1 = center + t * half + offset
                val a2 = center - t * half - offset
                val b2 = center + t * half - offset

                stroke.strokeWidth = bw * (0.45f + 0.20f * rr.nextFloat())
                stroke.color = params.color.copy(alpha = edgeAlpha(alpha) * (0.70f + 0.30f * rr.nextFloat()))
                canvas.drawLine(a1, b1, stroke)
                dirty = dirty.union(lineBounds(a1, b1, stroke.strokeWidth * 0.6f))

                stroke.strokeWidth = bw * (0.35f + 0.20f * rr.nextFloat())
                stroke.color = params.color.copy(alpha = edgeAlpha(alpha) * (0.55f + 0.35f * rr.nextFloat()))
                canvas.drawLine(a2, b2, stroke)
                dirty = dirty.union(lineBounds(a2, b2, stroke.strokeWidth * 0.6f))
            }

            // Fine fibers along the stroke for texture
            repeat(fibers) { i ->
                val rr = rngFor(localIdx * 131 + i)
                val k = (rr.nextFloat() - 0.5f) * 2f
                val lateral = n * (k * jitterAmp)
                val half = max(3f, baseStep * 0.7f)
                val a = center - t * half + lateral
                val b = center + t * half + lateral
                val w = (bw * 0.25f) * (0.6f + 0.8f * rr.nextFloat())
                val aMul = alpha * (0.25f + 0.35f * rr.nextFloat())

                stroke.strokeWidth = max(0.6f, w)
                stroke.color = params.color.copy(alpha = aMul)
                canvas.drawLine(a, b, stroke)
                dirty = dirty.union(lineBounds(a, b, stroke.strokeWidth * 0.6f))
            }

            // Paper grain specks
            run {
                val rr = rngFor(localIdx * 977)
                if (rr.nextFloat() < grainChance) {
                    val r = grainRadiusRange.start + rr.nextFloat() * (grainRadiusRange.endInclusive - grainRadiusRange.start)
                    val aMul = alpha * (0.18f + rr.nextFloat() * 0.22f)
                    fill.color = params.color.copy(alpha = aMul)
                    canvas.drawCircle(center, r, fill)
                    val rect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
                    dirty = dirty.union(rect)
                }
            }

            // Micro hatching (very faint short perpendicular stubs)
            run {
                val rr = rngFor(localIdx * 487)
                if (rr.nextFloat() < hatchChance) {
                    val m = (rr.nextFloat() - 0.5f) * 2f
                    val a = center + n * (-hatchLen * 0.5f)
                    val b = center + n * (hatchLen * 0.5f)
                    stroke.strokeWidth = max(0.6f, bw * 0.18f)
                    stroke.color = params.color.copy(alpha = alpha * 0.18f * (0.6f + 0.4f * m))
                    canvas.drawLine(a, b, stroke)
                    dirty = dirty.union(lineBounds(a, b, stroke.strokeWidth * 0.6f))
                }
            }

            return dirty
        }

        private fun walkQuadratic(
            a: Offset,
            b: Offset,
            c: Offset,
            pStart: Float,
            vStart: Float,
            pEnd: Float,
            vEnd: Float,
            localBase: Int,
        ): DirtyRect {
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
                    val vel = vStart + (vEnd - vStart) * tt

                    dirty = dirty.union(stampBundle(prev, cur, hit, press, vel, idx))

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
            lastVelocity = event.velocity ?: params.velocity

            // tap leaves a small graphite mark
            val a = event.position + Offset(-0.5f, -0.5f)
            val c = event.position + Offset(0.5f, 0.5f)
            return stampBundle(a, c, event.position, lastPressure, lastVelocity, 0)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure
            val newVelocity = event.velocity ?: params.velocity

            var dirty: DirtyRect = null
            when {
                p0 == null -> dirty = start(event)
                p1 == null -> {
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
                            vStart = lastVelocity,
                            pEnd = newPressure,
                            vEnd = newVelocity,
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
                            vStart = lastVelocity,
                            pEnd = newPressure,
                            vEnd = newVelocity,
                            localBase = 777,
                        )
                    )
                    lastMid = m2
                    p0 = a1
                    p1 = a2
                }
            }

            lastPressure = newPressure
            lastVelocity = newVelocity
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val moved = move(event)
            var dirty: DirtyRect = moved

            // Final graphite dab
            val endPos = event.position
            val p = event.pressure ?: params.pressure
            val v = event.velocity ?: params.velocity
            val a = endPos + Offset(-0.3f, 0.3f)
            val c = endPos + Offset(0.3f, -0.3f)
            dirty = dirty.union(stampBundle(a, c, endPos, p, v, 10_000))
            return dirty
        }
    }
}
