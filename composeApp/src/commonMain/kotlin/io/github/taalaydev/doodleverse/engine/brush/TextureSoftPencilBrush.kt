package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import io.github.taalaydev.doodleverse.engine.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.TextureBrush
import io.github.taalaydev.doodleverse.engine.tool.ToolId

/**
 * Texture-based soft pencil:
 *  - Builds a smoothed path (quadratic midevent smoothing).
 *  - Walks it with PathMeasure using delta = min(size/3, 10).
 *  - At each step, places 3 jittered *textured strips* (length ~10px) aligned with the path.
 *  - Texture is tinted to the brush color with low alpha.
 */
class TextureSoftPencilBrush(
    override val texture: ImageBitmap,
    override val id: ToolId = ToolId("texture_soft_pencil"),
    override val name: String = "Soft Pencil (Texture)"
) : TextureBrush() {

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = Session(canvas, texture, params)


    private class Session(
        private val canvas: Canvas,
        private val texture: ImageBitmap,
        params: BrushParams
    ) : StrokeSession(params) {

        // --- Path building (quadratic midevent smoothing) ---
        private val path = Path()
        private val measure = PathMeasure()
        private var lastMeasuredLen = 0f

        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null

        // Jitter step & segment length
        private val delta: Float = min(params.size / 3f, 1f)
        private val lookahead: Float = 10f

        private val opacityDiff: Float = 0.5f

        // Texture paint: tint + soft alpha
        private val texPaint = Paint().apply {
            isAntiAlias = true
            // Approx of calcOpacity(color.alpha, opacityDiff) with gentle base softness
            alpha = (params.color.alpha * 0.3f * (1f - opacityDiff)).coerceIn(0f, 1f)
            colorFilter = ColorFilter.tint(params.color, BlendMode.SrcIn)
            blendMode = params.blendMode
        }

        // Per-stroke RNG seed so jitter is stable during the stroke
        private val seed: Float = (12627262 xor params.hashCode()).toFloat() * 0.019f

        // --- Utilities ---

        private fun midevent(a: Offset, b: Offset) =
            Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun rand01(a: Float, b: Float, c: Float, d: Int, e: Int): Float {
            val x = a * 12.9898f + b * 78.233f + c * 37.719f + d * 0.123f + e * 0.517f + seed
            val s = sin(x.toDouble())
            val u = abs(s * 43758.5453123)
            return (u - floor(u)).toFloat()
        }

        private fun drawInitialStamp(canvas: Canvas, p: Offset): DirtyRect {
            // Tiny centered textured square so taps leave a mark
            val h = (params.size * 0.25f).coerceAtLeast(1f)
            val dst = Rect(p.x - h, p.y - h, p.x + h, p.y + h)
            canvas.drawImageRect(
                image = texture,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(texture.width, texture.height),
                dstOffset = IntOffset(dst.left.toInt(), dst.top.toInt()),
                dstSize = IntSize(dst.width.toInt(), dst.height.toInt()),
                paint = texPaint
            )
            return dst
        }

        /**
         * Render only the newly added tail of the path:
         *   - iterate i from lastMeasuredLen to total by 'delta'
         *   - at each i, place 3 jittered textured strips from i .. i+lookahead
         */
        private fun renderNewTail(canvas: Canvas): DirtyRect {
            measure.setPath(path, /*forceClosed*/ false)
            val total = measure.length
            if (total <= lastMeasuredLen || delta <= 0.01f) return null

            var dirty: DirtyRect = null
            var i = lastMeasuredLen
            while (i < total) {
                val p = measure.getPosition(i)
                val nextAt = min(total, i + lookahead)
                val q = measure.getPosition(nextAt)

                val dx = q.x - p.x
                val dy = q.y - p.y
                val angleDeg = toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                // Three soft “graphite” lanes with stable jitter around the centerline
                for (j in 0..2) {
                    val rx = rand01(i, p.x, p.y, j, 1)
                    val ry = rand01(i, p.x, p.y, j, 2)

                    val jitter = params.size * 0.5f
                    val off = Offset(
                        (rx - 0.5f) * jitter,
                        (ry - 0.5f) * jitter
                    )

                    // Destination quad: a short strip (length = nextAt - i, ~10px) by height = size*0.5
                    val length = (nextAt - i).coerceAtLeast(1f)
                    val halfLen = length * 0.5f
                    val halfHeight = (params.size * 0.5f) * 0.5f

                    val center = p + off

                    // Draw rotated textured strip aligned to path direction
                    canvas.save()
                    canvas.translate(center.x, center.y)
                    canvas.rotate(angleDeg)
                    val dst = Rect(-halfLen, -halfHeight, halfLen, halfHeight)

                    canvas.drawImageRect(
                        image = texture,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(texture.width, texture.height),
                        dstOffset = IntOffset(dst.left.toInt(), dst.top.toInt()),
                        dstSize = IntSize(dst.width.toInt(), dst.height.toInt()),
                        paint = texPaint
                    )
                    canvas.restore()

                    // Conservative AABB dirty rect around the strip
                    val ax = center.x - halfLen
                    val ay = center.y - halfHeight
                    val bx = center.x + halfLen
                    val by = center.y + halfHeight
                    dirty = dirty.union(Rect(min(ax, bx), min(ay, by), kotlin.math.max(ax, bx), kotlin.math.max(ay, by)))
                }

                i += delta
            }

            lastMeasuredLen = total
            return dirty
        }

        // --- StrokeSession lifecycle ---

        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            lastMeasuredLen = 0f

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return drawInitialStamp(canvas, event.position)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
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
                    dirty = dirty.union(renderNewTail(canvas))
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
                    dirty = dirty.union(renderNewTail(canvas))

                    lastMid = m2
                    p0 = a1
                    p1 = a2
                }
            }
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val moved = move(event)      // ensure final anchors are included
            val rest = renderNewTail(canvas)     // draw any remaining tail
            return moved.union(rest)
        }
    }


}

