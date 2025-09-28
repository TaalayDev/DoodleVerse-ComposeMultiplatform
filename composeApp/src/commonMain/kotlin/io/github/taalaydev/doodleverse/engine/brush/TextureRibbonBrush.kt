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

class TextureRibbonBrush(
    override val texture: ImageBitmap,
    private val colorize: Color = Color.Unspecified,
    private val tileOverlap: Float = 0.15f,          // avoid visible seams
    private val followRotationRandomness: Float = 4f, // tiny wobble to break mechanical look
    override val id: ToolId = ToolId("texture_ribbon"),
    override val name: String = "Ribbon (Textured)"
) : TextureBrush() {

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        Session(canvas, texture, colorize, tileOverlap, followRotationRandomness, params)

    private class Session(
        private val canvas: Canvas,
        private val texture: ImageBitmap,
        colorize: Color,
        private val tileOverlap: Float,
        private val rotJitter: Float,
        params: BrushParams
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            blendMode = params.blendMode
            colorFilter = if (colorize != Color.Unspecified) ColorFilter.tint(colorize, BlendMode.SrcIn) else null
        }
        private val rng = Random(params.hashCode() xor Clock.System.now().toEpochMilliseconds().toInt())

        private fun tileLengthFor(thickness: Float): Float {
            val aspect = texture.width.toFloat() / max(1, texture.height).toFloat()
            return max(4f, thickness * aspect * (1f - tileOverlap))
        }

        private var lastPos = Offset.Zero
        private var residual = 0f
        private var lastAngle = 0f

        override fun start(event: GestureEvent): DirtyRect {
            lastPos = event.position
            residual = 0f
            lastAngle = 0f
            return drawTileAt(event.position, lastAngle, event.pressure ?: params.pressure)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val cur = event.position
            val prev = lastPos
            if (cur == prev) return null

            val dx = cur.x - prev.x
            val dy = cur.y - prev.y
            var angle = toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (!angle.isFinite()) angle = lastAngle
            val dist = prev.distanceTo(cur)

            val pressure = event.pressure ?: params.pressure
            val thickness = max(1f, params.size * pressure.coerceAtLeast(0.1f))
            val step = tileLengthFor(thickness).coerceAtLeast(1f)

            var dirty: DirtyRect = null
            var remaining = dist
            var p = prev
            var acc = residual

            while (acc + remaining >= step && remaining > 0f) {
                val need = step - acc
                val t = (need / remaining).coerceIn(0f, 1f)
                val hit = Offset.lerp(p, cur, t)
                val jitter = (rng.nextFloat() - 0.5f) * rotJitter
                dirty = dirty.union(drawTileAt(hit, angle + jitter, pressure))
                p = hit
                remaining -= need
                acc = 0f
            }

            residual = acc + remaining
            lastPos = cur
            lastAngle = angle
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val moved = move(event)
            val extra = drawTileAt(event.position, lastAngle, event.pressure ?: params.pressure)
            return moved.union(extra)
        }

        private fun drawTileAt(center: Offset, angleDeg: Float, pressure: Float): Rect {
            val thickness = max(1f, params.size * pressure.coerceAtLeast(0.1f))
            val tileLen = tileLengthFor(thickness)
            val hw = tileLen * 0.5f
            val hh = thickness * 0.5f

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
