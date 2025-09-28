package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlin.math.*

class EraserBrush : Brush() {

    override val id = ToolId("eraser")
    override val name = "Eraser"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        Session(canvas, params)

    private class Session(
        private val canvas: Canvas,
        params: BrushParams
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            blendMode = BlendMode.Clear
            alpha = 1f
        }
        private val stepPx = params.size * 0.4f

        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var residual = 0f

        private fun dist(a: Offset, b: Offset) = a.distanceTo(b)
        private fun midevent(a: Offset, b: Offset) = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun stamp(c: Offset, r: Float): Rect {
            canvas.drawCircle(c, r, paint)
            return Rect(c.x - r, c.y - r, c.x + r, c.y + r)
        }

        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            return Offset(
                one * one * a.x + 2f * one * t * b.x + t * t * c.x,
                one * one * a.y + 2f * one * t * b.y + t * t * c.y
            )
        }

        private fun walkQuadratic(a: Offset, b: Offset, c: Offset, radius: Float): DirtyRect {
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
                    dirty = dirty.union(stamp(hit, radius))
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
            p0 = event.position; p1 = null; lastMid = null; residual = 0f
            return stamp(event.position, params.size * 0.5f)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            var dirty: DirtyRect = null
            when {
                p0 == null -> dirty = start(event)
                p1 == null -> {
                    p1 = newP
                    val a = p0!!; val m01 = midevent(a, newP)
                    dirty = dirty.union(walkQuadratic(lastMid ?: a, a, m01, params.size * 0.5f))
                    lastMid = m01
                }
                else -> {
                    val a0 = p0!!; val a1 = p1!!; val a2 = newP
                    val m1 = midevent(a0, a1); val m2 = midevent(a1, a2)
                    if (lastMid == null) lastMid = m1
                    dirty = dirty.union(walkQuadratic(lastMid!!, a1, m2, params.size * 0.5f))
                    lastMid = m2; p0 = a1; p1 = a2
                }
            }
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val moved = move(event)
            return moved.union(stamp(event.position, params.size * 0.5f))
        }
    }
}
