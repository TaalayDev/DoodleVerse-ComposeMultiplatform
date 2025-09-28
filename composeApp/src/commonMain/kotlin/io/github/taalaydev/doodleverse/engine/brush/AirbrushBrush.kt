package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.ProceduralBrush
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlin.math.*
import kotlin.random.Random
import kotlinx.datetime.Clock

class Airbrush(
    private val ratePerSecond: Float = 220f // dots per second at rest
) : ProceduralBrush() {

    override val id = ToolId("airbrush")
    override val name = "Airbrush"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        Session(canvas, ratePerSecond, params)

    private class Session(
        private val canvas: Canvas,
        private val ratePerSecond: Float = 220f,
        params: BrushParams
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            color = params.color
            alpha = params.color.alpha * 0.18f
            blendMode = params.blendMode
        }

        private val rng = Random(Clock.currentTimeMillis().toInt() xor params.hashCode())
        private val radius = params.size * 0.5f
        private val minDot = (params.size * 0.05f).coerceAtLeast(0.6f)
        private val maxDot = (params.size * 0.18f).coerceAtLeast(minDot + 0.4f)

        private var lastTime = 0L
        private var lastPos = Offset.Zero

        private fun spray(center: Offset, count: Int): Rect {
            var dirty: DirtyRect = null
            repeat(count) {
                val a = rng.nextFloat() * (2f * PI).toFloat()
                val r = sqrt(rng.nextFloat()) * radius // denser at center
                val x = center.x + cos(a) * r
                val y = center.y + sin(a) * r
                val dotR = lerp(minDot, maxDot, rng.nextFloat())
                canvas.drawCircle(Offset(x, y), dotR, paint)
                val d = Rect(x - dotR, y - dotR, x + dotR, y + dotR)
                dirty = dirty.union(d)
            }
            // whole cloud area (safe)
            val pad = radius + maxDot
            dirty = dirty.union(Rect(center.x - pad, center.y - pad, center.x + pad, center.y + pad))
            return dirty!!
        }

        override fun start(event: GestureEvent): DirtyRect {
            lastTime = event.timeMillis
            lastPos = event.position
            // small initial puff
            return spray(event.position, (ratePerSecond * 0.02f).toInt().coerceAtLeast(6))
        }

        override fun move(event: GestureEvent): DirtyRect {
            val now = event.timeMillis
            val dt = ((now - lastTime).coerceAtLeast(0)).toFloat() / 1000f
            lastTime = now

            // add velocity = more emission
            val d = lastPos.distanceTo(event.position)
            lastPos = event.position
            val boost = (d / (radius.coerceAtLeast(1f))).coerceIn(0f, 3f)

            val count = ((ratePerSecond * (1f + 0.6f * boost)) * dt).toInt()
            return if (count > 0) spray(event.position, count) else null
        }

        override fun end(event: GestureEvent): DirtyRect {
            // gentle tail puff
            return spray(event.position, (ratePerSecond * 0.015f).toInt().coerceAtLeast(4))
        }
    }
}
