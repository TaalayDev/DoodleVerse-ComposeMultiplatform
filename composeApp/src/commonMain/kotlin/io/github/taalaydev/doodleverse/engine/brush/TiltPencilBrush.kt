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

/**
 * A realistic graphite pencil that responds to Stylus Tilt, Pressure, and Velocity.
 *
 * Mechanics:
 * - **Vertical Stylus**: Draws with the "tip" — sharp, dark, defined lines.
 * - **Tilted Stylus**: Draws with the "side" — broad, soft, grainy shading.
 * - **Speed**: Faster movement deposits less graphite (lighter stroke).
 */
class TiltPencilBrush : ProceduralBrush() {

    override val id: ToolId = ToolId("tilt_pencil")
    override val name: String = "Real Pencil (Tilt)"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        TiltPencilSession(canvas, params)

    private class TiltPencilSession(
        private val canvas: Canvas,
        params: BrushParams
    ) : StrokeSession(params) {

        // --- Configuration ---
        private val baseSize = params.size
        private val maxTiltSpread = params.size * 5.0f // Max width when fully tilted (shading)
        private val shadingThreshold = 0.2f // Radians where shading behavior starts (~11 degrees)

        // Physics constants
        private val friction = 0.85f // Graphite friction (higher = darker)
        private val grainScale = 0.5f // Scale of the paper grain simulation

        // --- State ---
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var lastTilt: Float = 0f
        private var lastVelocity: Float = 0f
        private var residual: Float = 0f

        // Reusable Paint objects to avoid allocation
        private val paint = Paint().apply {
            isAntiAlias = true
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
            blendMode = params.blendMode
            color = params.color
        }

        // Stable Randomness
        private val seed = params.hashCode() * 1664525 + 1013904223
        private fun rng(i: Int): Random = Random(seed + i * 73856093)

        // --- Helpers ---
        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun mid(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
        private fun normal(a: Offset, b: Offset): Offset {
            val d = dist(a, b)
            if (d < 0.001f) return Offset(0f, 1f)
            return Offset(-(b.y - a.y) / d, (b.x - a.x) / d)
        }

        // --- Rendering Core ---

        /**
         * Draws a single "step" of the pencil.
         * Instead of one solid circle/line, it draws a cluster of "fibers" (graphite particles).
         * The spread of these fibers is determined by the Tilt.
         */
        private fun stamp(
            prev: Offset,
            curr: Offset,
            center: Offset,
            pressure: Float,
            tilt: Float,
            velocity: Float,
            stepIdx: Int
        ): DirtyRect {
            var dirty: DirtyRect = null

            // 1. Calculate Tilt Factor (0.0 = vertical, 1.0 = flat)
            // We assume max effective tilt is around 1.0 radian (~57 degrees)
            val tiltFactor = (tilt / 1.0f).coerceIn(0f, 1f)

            // 2. Determine Stroke Properties based on Tilt
            // Width: Narrow at 0 tilt, Wide at high tilt
            val width = lerp(baseSize * 0.4f, maxTiltSpread, tiltFactor * tiltFactor) * (0.8f + 0.4f * pressure)

            // Opacity: Solid at 0 tilt, Transparent/Grainy at high tilt
            // Faster velocity = lighter stroke
            val velocityDim = (1f - (velocity / 5000f)).coerceIn(0.4f, 1f)
            val tiltDim = lerp(1.0f, 0.15f, tiltFactor) // Shading is much lighter
            val alpha = (params.color.alpha * pressure * friction * velocityDim * tiltDim).coerceIn(0.01f, 1f)

            // 3. Texture/Grain Density
            // Tip mode: dense fibers. Shading mode: sparse fibers (grainy).
            val baseFibers = 5
            val extraFibers = (width / 1.5f).toInt() // More fibers for wider strokes
            val totalFibers = (baseFibers + extraFibers).coerceAtMost(40)

            // Geometry
            val n = normal(prev, curr)
            val rand = rng(stepIdx)

            paint.color = params.color.copy(alpha = alpha)

            // Draw the fibers
            repeat(totalFibers) { i ->
                // Distribution: Gaussian-like distribution concentrates fibers in center for sharpness,
                // but spreads them out for shading.
                val distribution = (rand.nextFloat() + rand.nextFloat() - 1f) // -1 to 1 (approx bell curve)

                // Spread factor increases with tilt
                val spread = width * 0.1f * distribution

                // Jitter position slightly for paper grain effect
                val grainX = (rand.nextFloat() - 0.5f) * baseSize * grainScale
                val grainY = (rand.nextFloat() - 0.5f) * baseSize * grainScale

                val start = center + (n * spread) + Offset(grainX, grainY)

                // Fiber length: Shading has shorter, spotty fibers. Tip has longer, flowy fibers.
                val fiberLen = lerp(baseSize * 0.5f, baseSize * 0.1f, tiltFactor)
                val end = start + ((curr - prev) * fiberLen * 0.1f) // Oriented with stroke

                // Thickness of individual graphite fibers
                paint.strokeWidth = lerp(1.5f, 0.5f, tiltFactor) * (0.5f + rand.nextFloat())

                canvas.drawLine(start, end, paint)

                // Calculate dirty rect
                val r = paint.strokeWidth
                dirty = dirty.union(Rect(start.x - r, start.y - r, start.x + r, start.y + r))
            }

            return dirty
        }

        // --- Path Interpolation (Quadratic) ---

        private fun walkQuadratic(
            a: Offset, b: Offset, c: Offset,
            p1: Float, p2: Float, // Pressure start/end
            t1: Float, t2: Float, // Tilt start/end
            v1: Float, v2: Float, // Velocity start/end
            baseIdx: Int
        ): DirtyRect {
            val len = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            val steps = max(5, ceil(len / (baseSize * 0.25f)).toInt())
            val dt = 1f / steps

            var dirty: DirtyRect = null
            var t = 0f
            var prevPt = a
            var idx = baseIdx

            while (t <= 1f) {
                // Quadratic Bezier: B(t) = (1-t)^2*a + 2(1-t)t*b + t^2*c
                val one = 1f - t
                val curPt = Offset(
                    one * one * a.x + 2 * one * t * b.x + t * t * c.x,
                    one * one * a.y + 2 * one * t * b.y + t * t * c.y
                )

                // Interpolate Physics
                val p = lerp(p1, p2, t)
                val tilt = lerp(t1, t2, t)
                val v = lerp(v1, v2, t)

                dirty = dirty.union(stamp(prevPt, curPt, curPt, p, tilt, v, idx++))

                prevPt = curPt
                t += dt
            }
            return dirty
        }

        // --- Session Lifecycle ---

        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            lastPressure = event.pressure ?: 1.0f
            lastTilt = event.tilt ?: 0f
            lastVelocity = 0f

            // Initial dot
            return stamp(
                event.position - Offset(1f, 1f),
                event.position,
                event.position,
                lastPressure,
                lastTilt,
                0f,
                0
            )
        }

        override fun move(event: GestureEvent): DirtyRect {
            val curP = event.position
            val curPressure = event.pressure ?: 1.0f
            val curTilt = event.tilt ?: 0f // Default to 0 (vertical) if device doesn't support tilt
            val curVelocity = event.velocity ?: 0f

            var dirty: DirtyRect = null

            when {
                p0 == null -> dirty = start(event)
                p1 == null -> {
                    // First segment
                    p1 = curP
                    val mid = mid(p0!!, curP)
                    lastMid = p0

                    dirty = walkQuadratic(
                        p0!!, p0!!, mid,
                        lastPressure, curPressure,
                        lastTilt, curTilt,
                        lastVelocity, curVelocity,
                        0
                    )
                    lastMid = mid
                }
                else -> {
                    // Continuation
                    val mid = mid(p1!!, curP)
                    dirty = walkQuadratic(
                        lastMid!!, p1!!, mid,
                        lastPressure, curPressure,
                        lastTilt, curTilt,
                        lastVelocity, curVelocity,
                        (curP.x + curP.y).toInt() // Randomize seed base based on pos
                    )

                    lastMid = mid
                    p0 = p1
                    p1 = curP
                }
            }

            lastPressure = curPressure
            lastTilt = curTilt
            lastVelocity = curVelocity

            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val moved = move(event)
            // Draw final segment to the actual end point
            val endP = event.position
            val endMid = mid(lastMid ?: p0!!, endP)

            val final = walkQuadratic(
                lastMid!!, endMid, endP,
                lastPressure, event.pressure ?: lastPressure,
                lastTilt, event.tilt ?: lastTilt,
                lastVelocity, 0f,
                10000
            )
            return moved.union(final)
        }
    }
}