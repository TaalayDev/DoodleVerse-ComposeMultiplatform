package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.TextureBrush
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import io.github.taalaydev.doodleverse.engine.util.*
import kotlinx.datetime.Clock
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random

/**
 * A pen that draws with a repeating texture.
 *
 * The key feature is that stroke velocity controls the spacing of the texture stamps.
 * Faster strokes pull the stamps further apart, creating a "skipping" or "dashed" effect.
 * Pressure controls the size of the stamp.
 */
class TexturePenBrush(
    override val texture: ImageBitmap,
    override val id: ToolId = ToolId("texture_pen"),
    override val name: String = "Texture Pen"
) : TextureBrush() {

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession {
        return TexturePenStrokeSession(canvas, params, texture)
    }

    private class TexturePenStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val texture: ImageBitmap,
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            colorFilter = ColorFilter.tint(params.color, BlendMode.SrcIn)
        }
        // The base spacing is the width of the texture stamp itself.
        private val baseStepPx: Float = params.size * 0.1f
        private val path = Path()
        private val random = Random(Clock.currentTimeMillis())

        // Path smoothing state
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var lastVelocity: Float = params.velocity
        private var residual: Float = 0f

        // --- Helpers ---

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun widthFor(pressure: Float): Float {
            // Pressure subtly affects size.
            return params.size * (0.7f + pressure * 0.6f)
        }

        private fun stepFactorFor(velocity: Float): Float {
            // Velocity controls spacing. A factor of 1.0 is normal spacing.
            // A factor of 3.0 means stamps are 3x further apart.
            // We map a velocity range of [0, 2] to a spacing factor of [1, 3].
            val velocityFactor = velocity.coerceIn(0f, 2f) / 2f
            return 1.0f + velocityFactor * 2.0f
        }

        private fun stamp(canvas: Canvas, center: Offset, size: Float, tangent: Offset): Rect {
            val tempPaint = paint.copy().apply {
                alpha = (0.8f + random.nextFloat() * 0.2f) * params.color.alpha
            }

            val angle = atan2(tangent.y, tangent.x) * (180f / PI.toFloat())

            canvas.withSave {
                canvas.translate(center.x, center.y)
                canvas.rotate(angle)

                val dstRect = Rect(-size / 2f, -size / 2f, size / 2f, size / 2f)
                canvas.drawImageRect(
                    image = texture,
                    srcOffset = IntOffset.Zero, srcSize = IntSize(texture.width, texture.height),
                    dstOffset = IntOffset(dstRect.left.toInt(), dstRect.top.toInt()),
                    dstSize = IntSize(dstRect.width.toInt(), dstRect.height.toInt()),
                    paint = tempPaint
                )
            }

            val maxDim = size * 1.5f
            return Rect(center.x - maxDim / 2f, center.y - maxDim / 2f, center.x + maxDim / 2f, center.y + maxDim / 2f)
        }

        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            val x = one * one * a.x + 2f * one * t * b.x + t * t * c.x
            val y = one * one * a.y + 2f * one * t * b.y + t * t * c.y
            return Offset(x, y)
        }

        private fun walkQuadratic(
            canvas: Canvas, a: Offset, b: Offset, c: Offset,
            pStart: Float, pEnd: Float, vStart: Float, vEnd: Float
        ): DirtyRect {
            val approxLen = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            val subdivisions = max(1, ceil(approxLen / (baseStepPx * 0.5f)).toInt())
            val dt = 1f / subdivisions

            var dirty: DirtyRect = null
            var prev = a
            var acc = residual

            var t = dt
            while (t <= 1f + 1e-4f) {
                val cur = qPoint(a, b, c, t.coerceAtMost(1f))
                val segLen = dist(prev, cur)
                var remain = segLen

                // Interpolate pressure and velocity to the start of the current segment
                val tt = (t - dt) + dt * (1f - (remain / segLen).coerceIn(0f, 1f))
                val press = pStart + (pEnd - pStart) * tt
                val vel = vStart + (vEnd - vStart) * tt

                // Calculate the step distance based on velocity
                val stepPx = baseStepPx * stepFactorFor(vel)

                while (acc + remain >= stepPx && remain > 0f) {
                    val need = stepPx - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)

                    val tangent = (cur - prev).let { if (it.getDistanceSquared() > 0f) it / it.getDistance() else Offset(1f, 0f) }
                    val size = widthFor(press)
                    dirty = dirty.union(stamp(canvas, hit, size, tangent))

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

        // --- StrokeSession API ---

        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            residual = 0f
            lastPressure = event.pressure ?: params.pressure
            lastVelocity = event.velocity ?: params.velocity

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            val size = widthFor(lastPressure)
            return stamp(canvas, event.position, size, Offset(1f, 0f))
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
                    val m01 = midevent(a, newP)
                    if (lastMid == null) {
                        path.moveTo(a.x, a.y)
                        lastMid = a
                    }
                    path.quadraticBezierTo(a.x, a.y, m01.x, m01.y)
                    dirty = dirty.union(
                        walkQuadratic(canvas, lastMid!!, a, m01, lastPressure, newPressure, lastVelocity, newVelocity)
                    )
                    lastMid = m01
                }
                else -> {
                    val a0 = p0!!; val a1 = p1!!; val a2 = newP
                    val m1 = midevent(a0, a1); val m2 = midevent(a1, a2)
                    if (lastMid == null) {
                        path.moveTo(m1.x, m1.y)
                        lastMid = m1
                    }
                    path.quadraticBezierTo(a1.x, a1.y, m2.x, m2.y)
                    dirty = dirty.union(
                        walkQuadratic(canvas, lastMid!!, a1, m2, lastPressure, newPressure, lastVelocity, newVelocity)
                    )
                    lastMid = m2; p0 = a1; p1 = a2
                }
            }
            lastPressure = newPressure
            lastVelocity = newVelocity
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val moved = move(event)
            val endPressure = event.pressure ?: params.pressure
            val size = widthFor(endPressure)

            // Draw one final stamp at the end position to make sure the line feels complete.
            return moved.union(stamp(canvas, event.position, size, p1?.let { event.position - it } ?: Offset(1f, 0f)))
        }
    }
}