package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.createScaledBitmap
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.TextureBrush
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import io.github.taalaydev.doodleverse.engine.util.*
import kotlinx.datetime.Clock
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A brush pen that simulates a calligraphic ink brush with a textured stroke.
 * Uses an ImageBitmap texture for the stroke, with pressure-sensitive width and opacity.
 * Spacing is in destination pixels. If spacing <= 0f, a default of 0.35 * size is used.
 */
class BrushPen(
    override val texture: ImageBitmap
) : TextureBrush() {

    override val id = ToolId("brush_pen")
    override val name: String = "Brush Pen"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession {
        return BrushPenStrokeSession(canvas, params, texture)
    }

    private class BrushPenStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val texture: ImageBitmap
    ) : StrokeSession(params) {

        // ----- Style / state -----
        private val paint = Paint().also {
            it.isAntiAlias = true
            it.color = params.color
            it.blendMode = params.blendMode
            it.alpha = 0.75f // Base opacity for brush pen effect
            it.colorFilter = ColorFilter.tint(params.color)
        }
        private val stepPx: Float = params.size * 0.35f
        private val random = Random(Clock.currentTimeMillis())

        // Current working path for geometry
        private val path = Path()

        // Anchor history for smoothing
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f

        // ----- Helpers -----
        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)

        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun widthFor(pressure: Float): Float {
            val baseWidth = params.size
            return max(0.3f, baseWidth * (if (pressure > 0f) pressure else params.pressure))
        }

        private fun opacityFor(pressure: Float): Float {
            val baseOpacity = 0.75f
            val pressureAdjusted = if (pressure > 0f) pressure else params.pressure
            // Vary opacity between 0.5 and 0.85 based on pressure
            return min(0.85f, baseOpacity * (0.6f + 0.4f * pressureAdjusted))
        }

        private fun stamp(canvas: Canvas, center: Offset, width: Float, pressure: Float): Rect {
            val textureAlpha = opacityFor(pressure) * (0.95f + random.nextFloat() * 0.05f) // Slight texture variation
            val tempPaint = paint.copy().also { it.alpha = textureAlpha }
            val halfWidth = width * 0.5f
            val textureSize = IntSize(
                (width * 1.2f).toInt().coerceAtLeast(1), // Slightly larger for texture
                (width * 0.6f).toInt().coerceAtLeast(1)
            )
            val scaledTexture = ImageBitmap.createScaledBitmap(texture, textureSize.width, textureSize.height)
            val rect = Rect(
                center.x - halfWidth,
                center.y - halfWidth * 0.3f,
                center.x + halfWidth,
                center.y + halfWidth * 0.3f
            )
            canvas.drawImageRect(
                image = scaledTexture,
                dstOffset = IntOffset(rect.left.toInt(), rect.top.toInt()),
                dstSize = IntSize(rect.width.toInt(), rect.height.toInt()),
                paint = tempPaint
            )
            return rect
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
            val subdivisions = max(8, ceil(approxLen / 6f).toInt())
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
                    dirty = dirty.union(stamp(canvas, hit, widthFor(press), press))
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

        // ----- StrokeSession API -----
        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            residual = 0f
            lastPressure = event.pressure ?: params.pressure

            path.reset()
            path.moveTo(event.position.x, event.position.y)
            return stamp(canvas, event.position, widthFor(lastPressure), lastPressure)
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

            dirty = dirty.union(stamp(canvas, endPos, widthFor(endPressure), endPressure))
            return dirty
        }
    }
}