package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
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
import kotlin.math.pow
import kotlin.random.Random

class OilPaintBrush(
    override val texture: ImageBitmap
) : TextureBrush() {
    override val id = ToolId("oil_paint")
    override val name: String = "Oil Paint"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession {
        return OilPaintStrokeSession(canvas, params, texture)
    }

    private class OilPaintStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val texture: ImageBitmap
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            color = params.color
            blendMode = params.blendMode
            colorFilter = ColorFilter.tint(params.color)
        }
        private val stepPx: Float = params.size * 0.15f
        private val random = Random(Clock.currentTimeMillis())
        private val path = Path()

        // Anchor & stroke history
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var lastVelocity: Float = params.velocity // Track velocity
        private var residual: Float = 0f

        // Oil paint specific state
        private var paintLoad: Float = 1.0f
        private val paintFadeRate = 0.002f
        private val baseHsl = rgbToHsl(params.color)

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun widthFor(pressure: Float, velocity: Float): Float {
            // Faster strokes are thinner as the thick paint is spread out.
            val velocityFactor = 1.0f - (velocity.coerceIn(0f, 2f) / 2f) * 0.5f
            return params.size * (0.5f + pressure.pow(1.5f)) * velocityFactor
        }

        private fun opacityFor(pressure: Float, velocity: Float): Float {
            val baseOpacity = 0.6f
            val pressureAdjusted = 0.4f + pressure * 0.6f
            // Faster strokes are more transparent/streaky.
            val velocityFactor = 1.0f - (velocity.coerceIn(0f, 2f) / 2f) * 0.4f
            return (baseOpacity * pressureAdjusted * paintLoad * velocityFactor).coerceIn(0.1f, 0.8f)
        }

        private fun jitterColor(): Color {
            val hue = baseHsl[0]
            val sat = (baseHsl[1] + (random.nextFloat() - 0.5f) * 0.1f).coerceIn(0f, 1f)
            val light = (baseHsl[2] + (random.nextFloat() - 0.5f) * 0.15f).coerceIn(0f, 1f)
            return hslToRgb(floatArrayOf(hue, sat, light))
        }

        private fun stamp(canvas: Canvas, center: Offset, width: Float, pressure: Float, velocity: Float, tangent: Offset): Rect {
            val stampWidth = width * (0.8f + random.nextFloat() * 0.4f)
            val stampHeight = stampWidth * (texture.height.toFloat() / texture.width.toFloat())

            val tempPaint = paint.copy().apply {
                alpha = opacityFor(pressure, velocity) * (0.8f + random.nextFloat() * 0.2f)
                color = jitterColor()
            }

            val angle = atan2(tangent.y, tangent.x) * (180f / PI.toFloat())

            canvas.withSave {
                canvas.translate(center.x, center.y)
                canvas.rotate(angle)
                val dstRect = Rect(-stampWidth / 2f, -stampHeight / 2f, stampWidth / 2f, stampHeight / 2f)
                canvas.drawImageRect(
                    image = texture,
                    srcOffset = IntOffset.Zero, srcSize = IntSize(texture.width, texture.height),
                    dstOffset = IntOffset(dstRect.left.toInt(), dstRect.top.toInt()),
                    dstSize = IntSize(dstRect.width.toInt(), dstRect.height.toInt()),
                    paint = tempPaint
                )
            }

            val maxDim = hypot(stampWidth, stampHeight)
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
            val subdivisions = max(1, ceil(approxLen / (stepPx * 0.5f)).toInt())
            val dt = 1f / subdivisions
            var dirty: DirtyRect = null
            var prev = a
            var acc = residual

            var t = dt
            while (t <= 1f + 1e-4f) {
                val cur = qPoint(a, b, c, t.coerceAtMost(1f))
                val segLen = dist(prev, cur)
                var remain = segLen
                while (acc + remain >= stepPx && remain > 0f) {
                    val need = stepPx - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)
                    val tt = (t - dt) + dt * f

                    val press = pStart + (pEnd - pStart) * tt
                    val vel = vStart + (vEnd - vStart) * tt

                    val tangent = (cur - prev).let { if (it.getDistanceSquared() > 0f) it / it.getDistance() else Offset(1f, 0f) }

                    dirty = dirty.union(stamp(canvas, hit, widthFor(press, vel), press, vel, tangent))

                    // Faster strokes use up paint more quickly over the same distance.
                    val velocityFadeFactor = 1.0f + vel.coerceIn(0f, 2f) * 0.5f
                    paintLoad = (paintLoad - stepPx * paintFadeRate * velocityFadeFactor).coerceAtLeast(0f)

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
            lastVelocity = event.velocity ?: params.velocity
            paintLoad = 1.0f

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return stamp(canvas, event.position, widthFor(lastPressure, lastVelocity), lastPressure, lastVelocity, Offset(1f, 0f))
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
            val endPos = event.position
            val endPressure = event.pressure ?: params.pressure
            val endVelocity = event.velocity ?: params.velocity
            var dirty: DirtyRect = move(event)

            val a = lastMid; val ctrl = p1
            if (a != null && ctrl != null) {
                path.quadraticBezierTo(ctrl.x, ctrl.y, endPos.x, endPos.y)
                dirty = dirty.union(
                    walkQuadratic(canvas, a, ctrl, endPos, lastPressure, endPressure, lastVelocity, endVelocity)
                )
            }

            return dirty
        }
    }
}