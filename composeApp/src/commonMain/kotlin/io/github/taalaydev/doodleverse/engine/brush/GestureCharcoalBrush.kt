package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.ProceduralBrush
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import io.github.taalaydev.doodleverse.engine.util.DirtyRect
import io.github.taalaydev.doodleverse.engine.util.lerp
import io.github.taalaydev.doodleverse.engine.util.union
import kotlin.math.*
import kotlin.random.Random

class GestureCharcoalBrush : ProceduralBrush() {

    override val id: ToolId = ToolId("gesture_charcoal")
    override val name: String = "Gesture Charcoal"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        GestureSession(canvas, params)

    private class GestureSession(
        private val canvas: Canvas,
        params: BrushParams
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
            blendMode = params.blendMode
            color = params.color
        }

        // Settings
        private val minSize = params.size * 0.5f
        private val maxSize = params.size * 3.0f // Can get very wide when slow
        private val textureDensity = 0.5f

        // State
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure = params.pressure
        private var lastVelocity = 0f

        // Random
        private val seed = params.hashCode() * 777
        private fun rng(i: Int) = Random(seed + i)

        private fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        private fun dist(a: Offset, b: Offset) = hypot(b.x - a.x, b.y - a.y)
        private fun normal(a: Offset, b: Offset): Offset {
            val d = dist(a, b)
            return if (d < 0.001f) Offset(0f, 1f) else Offset(-(b.y - a.y) / d, (b.x - a.x) / d)
        }

        // Draw a single "slice" of the charcoal stick
        private fun stamp(
            prev: Offset,
            curr: Offset,
            pressure: Float,
            velocity: Float,
            idx: Int
        ): DirtyRect {
            var dirty: DirtyRect = null

            val n = normal(prev, curr)
            val rand = rng(idx)

            // --- THE GESTURE PHYSICS ---

            // Velocity Factor: 0.0 (stopped) to 1.0 (very fast, >3000px/s)
            val vFactor = (velocity / 3000f).coerceIn(0f, 1f)

            // Width: Fast = Thin (Tip), Slow = Wide (Broadside)
            val targetWidth = lerp(maxSize, minSize, vFactor)
            val width = targetWidth * (0.8f + 0.2f * pressure) // Pressure has minor effect on width

            // Alpha: Fast = Dark/Solid, Slow = Light/Grainy
            val targetAlpha = lerp(0.15f, 1.0f, vFactor) // Slow is very faint
            val alpha = (params.color.alpha * targetAlpha * pressure).coerceIn(0.01f, 1f)

            // Texture/Grain:
            // When slow (wide), we draw multiple lines with gaps to simulate paper texture caught on charcoal
            // When fast (thin), we draw a solid line

            paint.color = params.color.copy(alpha = alpha)

            if (vFactor > 0.6f) {
                // FAST MODE: Solid line
                paint.strokeWidth = width
                canvas.drawLine(prev, curr, paint)
                dirty = dirty.union(Rect(prev, curr).inflate(width))
            } else {
                // SLOW/SHADING MODE: Textured broad stroke
                // We simulate this by drawing multiple parallel lines perpendicular to travel
                val slices = (width / 2f).toInt().coerceAtLeast(3)

                // Draw perpendicular "fibers" or accumulation
                repeat(slices) { i ->
                    // Scatter the lines along the normal
                    val spread = (rand.nextFloat() - 0.5f) * width
                    val offset = n * spread

                    val start = prev + offset
                    val end = curr + offset

                    // Graininess: random breaks
                    if (rand.nextFloat() > (0.2f + 0.3f * vFactor)) {
                        paint.strokeWidth = max(1f, width * 0.1f) // fine grain
                        canvas.drawLine(start, end, paint)
                        dirty = dirty.union(Rect(start, end).inflate(paint.strokeWidth))
                    }
                }
            }

            return dirty
        }

        private fun walkQuadratic(
            a: Offset, b: Offset, c: Offset,
            p1: Float, p2: Float,
            v1: Float, v2: Float,
            baseIdx: Int
        ): DirtyRect {
            val len = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            // High density stepping for smooth texture
            val steps = max(5, (len / 3f).toInt())
            val dt = 1f / steps
            var t = 0f
            var prevPt = a
            var dirty: DirtyRect = null
            var i = baseIdx

            while (t <= 1f) {
                val one = 1f - t
                val curPt = Offset(
                    one * one * a.x + 2 * one * t * b.x + t * t * c.x,
                    one * one * a.y + 2 * one * t * b.y + t * t * c.y
                )

                val p = lerp(p1, p2, t)
                val v = lerp(v1, v2, t)

                dirty = dirty.union(stamp(prevPt, curPt, p, v, i++))
                prevPt = curPt
                t += dt
            }
            return dirty
        }

        // --- Lifecycle --- (Standard ProceduralBrush boilerplate)
        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            lastPressure = event.pressure ?: 1f
            lastVelocity = 0f
            return stamp(event.position, event.position + Offset(0.1f, 0.1f), lastPressure, 0f, 0)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val curP = event.position
            val curPressure = event.pressure ?: 1f
            val curVelocity = event.velocity ?: 0f

            var dirty: DirtyRect = null

            when {
                p0 == null -> dirty = start(event)
                p1 == null -> {
                    p1 = curP
                    val mid = mid(p0!!, curP)
                    lastMid = p0
                    dirty = walkQuadratic(p0!!, p0!!, mid, lastPressure, curPressure, lastVelocity, curVelocity, 0)
                    lastMid = mid
                }
                else -> {
                    val mid = mid(p1!!, curP)
                    dirty = walkQuadratic(lastMid!!, p1!!, mid, lastPressure, curPressure, lastVelocity, curVelocity, (curP.x).toInt())
                    lastMid = mid
                    p0 = p1
                    p1 = curP
                }
            }
            lastPressure = curPressure
            lastVelocity = curVelocity
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val moved = move(event)
            val endP = event.position
            val mid = mid(lastMid ?: p0!!, endP)
            val final = walkQuadratic(lastMid!!, mid, endP, lastPressure, lastPressure, 0f, 0f, 9999)
            return moved.union(final)
        }
    }
}