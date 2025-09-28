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

class TextureBristleBrush(
    override val texture: ImageBitmap,
    private val strands: Int = 12,
    private val spread: Float = 0.6f,       // how far hairs spread across width
    private val lengthFactor: Float = 1.1f, // hair length relative to thickness
    private val randomAngle: Float = 6f,    // tiny angle wobble per hair (deg)
    private val colorize: Color = Color.Unspecified
) : TextureBrush() {

    override val id = ToolId("texture_bristle")
    override val name = "Bristle (Textured)"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        Session(canvas, texture, strands, spread, lengthFactor, randomAngle, colorize, params)

    private class Session(
        private val canvas: Canvas,
        private val texture: ImageBitmap,
        private val strands: Int,
        private val spread: Float,
        private val lengthFactor: Float,
        private val randAngle: Float,
        colorize: Color,
        params: BrushParams
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            blendMode = params.blendMode
            colorFilter = if (colorize != Color.Unspecified) ColorFilter.tint(colorize, BlendMode.SrcIn) else null
        }
        private val rng = Random(params.hashCode() xor Clock.System.now().toEpochMilliseconds().toInt())

        private var lastPos = Offset.Zero
        private var lastDir = 0f
        private var residual = 0f
        private val stepPx: Float = params.size * 0.6f

        override fun start(event: GestureEvent): DirtyRect {
            lastPos = event.position; lastDir = 0f; residual = 0f
            return stampGroup(event.position, lastDir, event.pressure ?: params.pressure)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val cur = event.position
            val prev = lastPos
            if (cur == prev) return null

            val dx = cur.x - prev.x
            val dy = cur.y - prev.y
            val angle = toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val dist = prev.distanceTo(cur)

            var dirty: DirtyRect = null
            var remaining = dist
            var p = prev
            var acc = residual
            val pressure = event.pressure ?: params.pressure

            while (acc + remaining >= stepPx && remaining > 0f) {
                val need = stepPx - acc
                val t = (need / remaining).coerceIn(0f, 1f)
                val hit = Offset.lerp(p, cur, t)
                dirty = dirty.union(stampGroup(hit, angle, pressure))
                p = hit
                remaining -= need
                acc = 0f
            }

            residual = acc + remaining
            lastPos = cur
            lastDir = angle
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val moved = move(event)
            val fin = stampGroup(event.position, lastDir, event.pressure ?: params.pressure)
            return moved.union(fin)
        }

        private fun stampGroup(center: Offset, directionDeg: Float, pressure: Float): Rect {
            val thickness = max(1f, params.size * pressure.coerceAtLeast(0.1f))
            val length = thickness * lengthFactor
            val half = thickness * 0.5f

            val rad = toRadians(directionDeg.toDouble())
            val cosA = cos(rad).toFloat()
            val sinA = sin(rad).toFloat()
            val nx = -sinA; val ny = cosA // normal

            var dirty: DirtyRect = null
            for (i in 0 until strands) {
                val f = if (strands == 1) 0f else (i / (strands - 1f)) // 0..1
                val offsetAcross = (f - 0.5f) * 2f * half * spread
                val jitterAcross = (rng.nextFloat() - 0.5f) * half * 0.2f
                val px = center.x + (offsetAcross + jitterAcross) * nx
                val py = center.y + (offsetAcross + jitterAcross) * ny
                val strandCenter = Offset(px, py)

                val strandLen = length * (0.9f + rng.nextFloat() * 0.2f)
                val strandWidth = (thickness * 0.12f) * (0.7f + rng.nextFloat() * 0.6f)
                val a = directionDeg + (rng.nextFloat() - 0.5f) * randAngle

                dirty = dirty.union(drawStrip(strandCenter, a, strandLen, strandWidth))
            }
            return dirty!!
        }

        private fun drawStrip(center: Offset, angleDeg: Float, length: Float, width: Float): Rect {
            val hw = length * 0.5f
            val hh = width * 0.5f

            canvas.save()
            canvas.translate(center.x, center.y)
            canvas.rotate(angleDeg)
            canvas.drawImageRect(
                image = texture,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(texture.width, texture.height),
                dstOffset = IntOffset((-hw).toInt(), (-hh).toInt()),
                dstSize = IntSize((hw * 2f).toInt(), (hh * 2f).toInt()),
                paint = paint
            )
            canvas.restore()

            return rotatedAabb(center, hw * 2f, hh * 2f, angleDeg)
        }

        private fun rotatedAabb(center: Offset, width: Float, height: Float, angleDeg: Float): Rect {
            val rad = toRadians(angleDeg.toDouble())
            val cos = cos(rad).toFloat(); val sin = sin(rad).toFloat()
            val hx = width * 0.5f; val hy = height * 0.5f
            val pts = arrayOf(Offset(-hx,-hy), Offset(hx,-hy), Offset(hx,hy), Offset(-hx,hy))
            var minX = Float.POSITIVE_INFINITY; var minY = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY
            for (p in pts) {
                val rx = p.x * cos - p.y * sin
                val ry = p.x * sin + p.y * cos
                val wx = center.x + rx; val wy = center.y + ry
                if (wx < minX) minX = wx; if (wy < minY) minY = wy
                if (wx > maxX) maxX = wx; if (wy > maxY) maxY = wy
            }
            return Rect(minX, minY, maxX, maxY)
        }
    }
}
