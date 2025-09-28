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

class TextureSprayBrush(
    override val texture: ImageBitmap,
    private val radius: Float,
    private val ratePerSecond: Float = 260f,
    private val colorize: Color = Color.Unspecified
) : TextureBrush() {

    override val id = ToolId("texture_spray")
    override val name = "Spray (Textured)"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        Session(canvas, texture, radius, ratePerSecond, colorize, params)

    private class Session(
        private val canvas: Canvas,
        private val texture: ImageBitmap,
        private val radius: Float,
        private val rate: Float,
        colorize: Color,
        params: BrushParams
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            blendMode = params.blendMode
            alpha = params.color.alpha * 0.22f
            colorFilter = if (colorize != Color.Unspecified) ColorFilter.tint(colorize, BlendMode.SrcIn) else null
        }

        private val rng = Random(params.hashCode() xor Clock.System.now().toEpochMilliseconds().toInt())
        private var lastTime = 0L
        private var lastPos = Offset.Zero

        private val minDot = max(1f, params.size * 0.06f)
        private val maxDot = max(minDot + 0.5f, params.size * 0.22f)

        override fun start(event: GestureEvent): DirtyRect {
            lastTime = event.timeMillis
            lastPos = event.position
            return spray(event.position, (rate * 0.02f).toInt().coerceAtLeast(8))
        }

        override fun move(event: GestureEvent): DirtyRect {
            val now = event.timeMillis
            val dt = ((now - lastTime).coerceAtLeast(0)).toFloat() / 1000f
            lastTime = now

            val d = lastPos.distanceTo(event.position)
            lastPos = event.position

            val boost = (d / (radius.coerceAtLeast(1f))).coerceIn(0f, 3f)
            val count = ((rate * (1f + 0.6f * boost)) * dt).toInt()
            return if (count > 0) spray(event.position, count) else null
        }

        override fun end(event: GestureEvent): DirtyRect {
            return spray(event.position, (rate * 0.015f).toInt().coerceAtLeast(6))
        }

        private fun spray(center: Offset, count: Int): Rect {
            var dirty: DirtyRect = null
            repeat(count) {
                val a = rng.nextFloat() * (2f * PI).toFloat()
                val r = sqrt(rng.nextFloat()) * radius
                val x = center.x + cos(a) * r
                val y = center.y + sin(a) * r
                val t = rng.nextFloat(); val dot = minDot + (maxDot - minDot) * t

                canvas.save()
                canvas.translate(x, y)
                val s = dot / max(1, texture.height).toFloat() // texture height -> dot diameter
                canvas.scale(s, s)
                canvas.drawImageRect(
                    image = texture,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(texture.width, texture.height),
                    dstOffset = IntOffset(-(texture.width / 2), -(texture.height / 2)),
                    dstSize = IntSize(texture.width, texture.height),
                    paint = paint
                )
                canvas.restore()

                val drect = Rect(x - dot * 0.5f, y - dot * 0.5f, x + dot * 0.5f, y + dot * 0.5f)
                dirty = dirty.union(drect)
            }
            val pad = radius + maxDot
            dirty = dirty.union(Rect(center.x - pad, center.y - pad, center.x + pad, center.y + pad))
            return dirty!!
        }
    }
}
