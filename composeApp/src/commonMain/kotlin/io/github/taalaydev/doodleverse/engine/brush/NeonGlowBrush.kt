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
import kotlin.math.*

/**
 * Neon glow brush that creates bright, glowing strokes with multiple layers
 * to simulate neon lighting effects. Features inner bright core and outer glow.
 */
class NeonGlowBrush(
    private val glowIntensity: Float = 0.8f, // 0-1, intensity of the glow effect
    private val glowSize: Float = 3f, // Multiplier for glow radius
    private val coreIntensity: Float = 1.2f, // Brightness multiplier for core
    override val id: ToolId = ToolId("neon_glow"),
    override val name: String = "Neon Glow"
) : Brush() {

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = NeonGlowStrokeSession(canvas, params, glowIntensity, glowSize, coreIntensity)

    private class NeonGlowStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val glowIntensity: Float,
        private val glowSize: Float,
        private val coreIntensity: Float
    ) : StrokeSession(params) {

        // Multiple paint layers for glow effect
        private val glowPaint = Paint().apply {
            isAntiAlias = true
            blendMode = BlendMode.Plus // Additive blending for glow
        }

        private val corePaint = Paint().apply {
            isAntiAlias = true
            blendMode = BlendMode.Plus
        }

        private val stepPx: Float = params.size * 0.2f
        private val path = Path()

        // State tracking
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float = max(0.5f, params.size * pressure * 0.5f)

        private fun drawGlowStamp(canvas: Canvas, center: Offset, radius: Float): Rect {
            val maxRadius = radius * glowSize

            // Draw glow layers from outside to inside for proper blending
            val layers = 5
            for (i in layers downTo 1) {
                val layerRadius = maxRadius * (i.toFloat() / layers)
                val layerAlpha = (glowIntensity * params.color.alpha * (1f - i.toFloat() / layers)).coerceIn(0f, 1f)

                // Create color for this glow layer
                val glowColor = params.color.copy(alpha = layerAlpha)
                glowPaint.color = glowColor

                canvas.drawCircle(center, layerRadius, glowPaint)
            }

            // Draw bright inner core
            val coreColor = params.color.copy(
                red = min(1f, params.color.red * coreIntensity),
                green = min(1f, params.color.green * coreIntensity),
                blue = min(1f, params.color.blue * coreIntensity),
                alpha = params.color.alpha
            )
            corePaint.color = coreColor
            canvas.drawCircle(center, radius * 0.3f, corePaint)

            return Rect(
                center.x - maxRadius,
                center.y - maxRadius,
                center.x + maxRadius,
                center.y + maxRadius
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
            val subdivisions = max(8, ceil(approxLen / 3f).toInt())
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

                    dirty = dirty.union(drawGlowStamp(canvas, hit, radiusFor(press)))
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

            return drawGlowStamp(canvas, event.position, radiusFor(lastPressure))
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

            // Final bright cap
            dirty = dirty.union(drawGlowStamp(canvas, endPos, radiusFor(endPressure)))
            return dirty
        }
    }
}