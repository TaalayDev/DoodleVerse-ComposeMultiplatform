package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.TextureBrush
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlin.random.Random

/**
 * Texture painting brush that overlays texture patterns onto brush strokes
 * to simulate real paint texture, canvas texture, or paper texture effects.
 * Supports multiple blend modes and dynamic texture scaling.
 */
class TexturePaintingBrush(
    override val texture: ImageBitmap,
    private val textureScale: Float = 1f, // Scale factor for texture
    private val textureOpacity: Float = 0.7f, // How visible the texture is
    private val paintFlow: Float = 0.8f, // How much paint flows (affects coverage)
    private val textureRotation: Float = 0f, // Fixed rotation for texture
    private val colorMixing: Boolean = true // Whether to mix paint color with texture
) : TextureBrush() {

    override val id = ToolId("texture_painting")
    override val name: String = "Texture Paint"

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = TexturePaintingStrokeSession(
        canvas, texture, params, textureScale, textureOpacity,
        paintFlow, textureRotation, colorMixing
    )

    private class TexturePaintingStrokeSession(
        private val canvas: Canvas,
        private val texture: ImageBitmap,
        params: BrushParams,
        private val textureScale: Float,
        private val textureOpacity: Float,
        private val paintFlow: Float,
        private val textureRotation: Float,
        private val colorMixing: Boolean
    ) : StrokeSession(params) {

        // Paint for base color layer
        private val basePaint = Paint().apply {
            isAntiAlias = true
            color = params.color.copy(alpha = params.color.alpha * paintFlow)
            blendMode = params.blendMode
        }

        // Paint for texture overlay
        private val texturePaint = Paint().apply {
            isAntiAlias = true
            alpha = textureOpacity
            blendMode = if (colorMixing) BlendMode.Multiply else BlendMode.Overlay
        }

        private val stepPx: Float = params.size * 0.3f
        private val path = Path()

        // Random for texture variation
        private val random: Random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

        // State tracking
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f
        private var paintAccumulation: Float = 0f // Simulates paint building up

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float = max(0.5f, params.size * pressure * 0.5f)

        private fun drawTexturedStamp(canvas: Canvas, center: Offset, radius: Float, pressure: Float): DirtyRect {
            val stampSize = radius * 2f
            val textureSize = stampSize * textureScale

            // Accumulate paint for realistic layering
            paintAccumulation += pressure * 0.1f
            val layerOpacity = min(1f, paintAccumulation * paintFlow)

            // Draw base paint layer
            val adjustedBasePaint = basePaint.copy().apply {
                alpha = layerOpacity * pressure
            }
            // canvas.drawCircle(center, radius, adjustedBasePaint)

            // Apply texture overlay
            if (textureOpacity > 0f && textureSize > 1f) {
                canvas.save()
                canvas.translate(center.x, center.y)
                canvas.rotate(textureRotation + random.nextFloat() * 10f - 5f) // Slight random rotation

                val halfSize = textureSize * 0.5f
                val textureRect = Rect(-halfSize, -halfSize, halfSize, halfSize)

                // Create color-tinted texture if color mixing is enabled
                val finalTexturePaint = if (colorMixing) {
                    texturePaint.copy().apply {
                        alpha = textureOpacity * pressure
                        colorFilter = ColorFilter.tint(params.color, BlendMode.Modulate)
                    }
                } else {
                    texturePaint.copy().apply { alpha = textureOpacity * pressure }
                }

                canvas.drawImageRect(
                    image = texture,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(texture.width, texture.height),
                    dstOffset = IntOffset(textureRect.left.toInt(), textureRect.top.toInt()),
                    dstSize = IntSize(textureRect.width.toInt(), textureRect.height.toInt()),
                    paint = finalTexturePaint
                )

                canvas.restore()
            }

            // Add subtle paint drips for realism (at high pressure)
            if (pressure > 0.8f && random.nextFloat() < 0.1f) {
                val dripLength = radius * (0.5f + random.nextFloat() * 0.5f)
                val dripAngle = random.nextFloat() * 2f * PI.toFloat()
                val dripEnd = Offset(
                    center.x + cos(dripAngle) * dripLength,
                    center.y + sin(dripAngle) * dripLength
                )

                val dripPaint = basePaint.copy().apply { alpha = alpha * 0.3f }
                canvas.drawLine(center, dripEnd, dripPaint)
            }

            // Return expanded dirty rect to include texture and effects
            val padding = max(radius, textureSize * 0.5f) + 5f
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

                    dirty = dirty.union(drawTexturedStamp(canvas, hit, radiusFor(press), press))
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
            paintAccumulation = 0f
            lastPressure = event.pressure ?: params.pressure

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return drawTexturedStamp(canvas, event.position, radiusFor(lastPressure), lastPressure)
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

            // Final stamp with extra paint accumulation for stroke end
            paintAccumulation += 0.2f
            dirty = dirty.union(drawTexturedStamp(canvas, endPos, radiusFor(endPressure), endPressure))
            return dirty
        }
    }
}