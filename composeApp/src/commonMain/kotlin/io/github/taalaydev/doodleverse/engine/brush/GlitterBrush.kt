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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * A versatile brush that scatters one or more textures along a stroke path.
 *
 * Ideal for effects like glitter, sparkles, leaves, or confetti. It uses pressure
 * to control the density and spread of the scattered items, each of which has
 * randomized size, rotation, and opacity.
 *
 * @param textures A list of ImageBitmaps to be randomly scattered.
 */
class GlitterBrush(
    private val textures: List<ImageBitmap>
) : TextureBrush() {

    init {
        require(textures.isNotEmpty()) { "GlitterBrush requires at least one texture." }
    }

    // The TextureBrush base class requires a single texture property. We'll just provide the first one.
    override val texture: ImageBitmap
        get() = textures.first()

    override val id = ToolId("glitter_scatter")
    override val name: String = "Glitter"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession {
        return GlitterStrokeSession(canvas, params, textures)
    }

    private class GlitterStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val textures: List<ImageBitmap>
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            // Use the user's selected color to tint the textures.
            colorFilter = ColorFilter.tint(params.color, BlendMode.SrcIn)
        }
        private val baseStepPx: Float = params.size * 0.5f
        private val random = Random(Clock.currentTimeMillis())
        private val path = Path()

        // Path smoothing state
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f

        // --- Helpers ---

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        /**
         * The core drawing logic. Draws a single, randomly transformed texture.
         */
        private fun stamp(canvas: Canvas, pathCenter: Offset, pressure: Float): Rect {
            val textureToDraw = textures.random(random)

            // 1. Calculate random transformations
            val spread = (params.size * 2f) * pressure
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val radius = random.nextFloat() * spread
            val offset = Offset(cos(angle) * radius, sin(angle) * radius)
            val center = pathCenter + offset

            val rotation = random.nextFloat() * 360f
            val scale = (0.4f + random.nextFloat() * 0.8f).coerceAtLeast(0.1f)
            val stampSize = params.size * scale

            // 2. Apply transformations via canvas state
            canvas.withSave {
                val tempPaint = paint.copy().apply {
                    alpha = (0.6f + random.nextFloat() * 0.4f) * params.color.alpha
                }

                canvas.translate(center.x, center.y)
                canvas.rotate(rotation)

                val dstRect = Rect(-stampSize / 2f, -stampSize / 2f, stampSize / 2f, stampSize / 2f)
                canvas.drawImageRect(
                    image = textureToDraw,
                    srcOffset = IntOffset.Zero, srcSize = IntSize(textureToDraw.width, textureToDraw.height),
                    dstOffset = IntOffset(dstRect.left.toInt(), dstRect.top.toInt()),
                    dstSize = IntSize(dstRect.width.toInt(), dstRect.height.toInt()),
                    paint = tempPaint
                )
            }

            // 3. Return the dirty rect
            val maxDim = stampSize * 1.5f // A bit larger to be safe
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
            pStart: Float, pEnd: Float
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

                val tt = (t - dt) + dt * (1f - (remain / segLen).coerceIn(0f, 1f))
                val press = pStart + (pEnd - pStart) * tt

                // Higher pressure = smaller step = higher density
                val stepPx = baseStepPx * (1.2f - press.coerceIn(0.1f, 1f))

                while (acc + remain >= stepPx && remain > 0f) {
                    val need = stepPx - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)

                    dirty = dirty.union(stamp(canvas, hit, press))

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

            path.reset()
            path.moveTo(event.position.x, event.position.y)
            return stamp(canvas, event.position, lastPressure)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure
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
                        walkQuadratic(canvas, lastMid!!, a, m01, lastPressure, newPressure)
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
                        walkQuadratic(canvas, lastMid!!, a1, m2, lastPressure, newPressure)
                    )
                    lastMid = m2; p0 = a1; p1 = a2
                }
            }
            lastPressure = newPressure
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val endPressure = event.pressure ?: params.pressure
            return move(event).union(stamp(canvas, event.position, endPressure))
        }
    }
}