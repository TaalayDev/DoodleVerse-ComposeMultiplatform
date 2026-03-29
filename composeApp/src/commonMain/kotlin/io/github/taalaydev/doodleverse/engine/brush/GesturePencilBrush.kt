package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.ProceduralBrush
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import io.github.taalaydev.doodleverse.engine.util.DirtyRect
import io.github.taalaydev.doodleverse.engine.util.distanceTo
import io.github.taalaydev.doodleverse.engine.util.expand
import io.github.taalaydev.doodleverse.engine.util.lerp
import io.github.taalaydev.doodleverse.engine.util.union
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * A natural, pressure and velocity-sensitive pencil brush with smooth interpolation.
 *
 * Features:
 * - Dynamic stroke width based on pressure input
 * - Velocity-based width modulation (thinner at high speeds)
 * - Smooth Bezier curve interpolation for organic feel
 * - Opacity modulation based on pressure
 * - Adaptive sampling for consistent appearance at any speed
 * - Low-latency drawing with predictive smoothing
 *
 * @property pressureSensitivity How much pressure affects stroke width (0.0 - 1.0)
 * @property velocitySensitivity How much velocity affects stroke width (0.0 - 1.0)
 * @property minWidthRatio Minimum width as ratio of base size (0.0 - 1.0)
 * @property smoothingFactor Smoothing factor for pressure/velocity (0.0 - 1.0)
 * @property opacityModulation Whether to modulate opacity with pressure
 */
class GesturePencilBrush(
    private val pressureSensitivity: Float = 0.7f,
    private val velocitySensitivity: Float = 0.3f,
    private val minWidthRatio: Float = 0.2f,
    private val smoothingFactor: Float = 0.3f,
    private val opacityModulation: Boolean = true
) : ProceduralBrush() {

    override val id = ToolId("gesture_pencil")
    override val name = "Gesture Pencil"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession {
        return GesturePencilSession(
            canvas = canvas,
            params = params,
            pressureSensitivity = pressureSensitivity,
            velocitySensitivity = velocitySensitivity,
            minWidthRatio = minWidthRatio,
            smoothingFactor = smoothingFactor,
            opacityModulation = opacityModulation
        )
    }

    private class GesturePencilSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val pressureSensitivity: Float,
        private val velocitySensitivity: Float,
        private val minWidthRatio: Float,
        private val smoothingFactor: Float,
        private val opacityModulation: Boolean
    ) : StrokeSession(params) {

        // Stroke state
        private val points = mutableListOf<StrokePoint>()
        private var lastPoint: StrokePoint? = null
        private var smoothedPressure: Float = 1f
        private var smoothedVelocity: Float = 0f

        // Path for smooth curves
        private val path = Path()
        private var lastDrawnIndex = 0

        // Paint
        private val paint = Paint().apply {
            color = params.color
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Round
            style = PaintingStyle.Stroke
            isAntiAlias = true
            blendMode = params.blendMode
        }

        private data class StrokePoint(
            val position: Offset,
            val pressure: Float,
            val velocity: Float,
            val width: Float,
            val opacity: Float,
            val timeMillis: Long
        )

        override fun start(event: GestureEvent): DirtyRect {
            val pressure = event.pressure ?: 1f
            smoothedPressure = pressure
            smoothedVelocity = 0f

            val width = calculateWidth(pressure, 0f)
            val opacity = if (opacityModulation) calculateOpacity(pressure) else params.color.alpha

            val point = StrokePoint(
                position = event.position,
                pressure = pressure,
                velocity = 0f,
                width = width,
                opacity = opacity,
                timeMillis = event.timeMillis
            )

            points.add(point)
            lastPoint = point
            lastDrawnIndex = 0

            // Draw initial point
            paint.strokeWidth = width
            paint.alpha = opacity
            canvas.drawCircle(event.position, width / 2f, paint)

            return Rect(
                left = event.position.x - width,
                top = event.position.y - width,
                right = event.position.x + width,
                bottom = event.position.y + width
            )
        }

        override fun move(event: GestureEvent): DirtyRect {
            val currentPos = event.position
            val prevPoint = lastPoint ?: return start(event)

            // Calculate velocity
            val distance = prevPoint.position.distanceTo(currentPos)
            val timeDelta = max(event.timeMillis - prevPoint.timeMillis, 1L)
            val velocity = distance / timeDelta.toFloat()

            // Get pressure
            val pressure = event.pressure ?: params.pressure

            // Smooth pressure and velocity
            smoothedPressure = lerp(smoothedPressure, pressure, smoothingFactor)
            smoothedVelocity = lerp(smoothedVelocity, velocity, smoothingFactor)

            // Calculate width and opacity
            val width = calculateWidth(smoothedPressure, smoothedVelocity)
            val opacity = if (opacityModulation) {
                calculateOpacity(smoothedPressure)
            } else {
                params.color.alpha
            }

            // Add point
            val point = StrokePoint(
                position = currentPos,
                pressure = smoothedPressure,
                velocity = smoothedVelocity,
                width = width,
                opacity = opacity,
                timeMillis = event.timeMillis
            )
            points.add(point)

            // Draw stroke segment
            val dirtyRect = drawStrokeSegment()

            lastPoint = point
            return dirtyRect
        }

        override fun end(event: GestureEvent): DirtyRect {
            // Draw any remaining segments
            val dirtyRect = drawStrokeSegment(finalSegment = true)

            // Clean up
            points.clear()
            lastPoint = null
            lastDrawnIndex = 0
            path.reset()

            return dirtyRect
        }

        /**
         * Calculates stroke width based on pressure and velocity
         */
        private fun calculateWidth(pressure: Float, velocity: Float): Float {
            // Base width from pressure
            val pressureWidth = lerp(
                minWidthRatio,
                1f,
                pressure.pow(1f - pressureSensitivity)
            )

            // Velocity modulation (thinner at high speeds)
            val velocityFactor = 1f - (velocity * velocitySensitivity).coerceIn(0f, 0.5f)

            return params.size * pressureWidth * velocityFactor
        }

        /**
         * Calculates opacity based on pressure
         */
        private fun calculateOpacity(pressure: Float): Float {
            val minOpacity = 0.3f
            val opacityRange = params.color.alpha - minOpacity
            return minOpacity + opacityRange * pressure.pow(0.8f)
        }

        /**
         * Draws a smooth stroke segment using Bezier curves
         */
        private fun drawStrokeSegment(finalSegment: Boolean = false): DirtyRect {
            if (points.size < 2) return null

            var dirtyRect: DirtyRect = null

            // Draw from last drawn index to current
            val startIdx = max(0, lastDrawnIndex - 1)
            val endIdx = if (finalSegment) points.size else points.size - 1

            for (i in startIdx until endIdx) {
                if (i + 1 >= points.size) break

                val p0 = points[max(0, i - 1)]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points.getOrNull(i + 2) ?: p2

                // Use Catmull-Rom for smooth curves
                val rect = drawCatmullRomSegment(p0, p1, p2, p3)
                dirtyRect = dirtyRect.union(rect)
            }

            lastDrawnIndex = endIdx
            return dirtyRect
        }

        /**
         * Draws a smooth segment using Catmull-Rom spline interpolation
         */
        private fun drawCatmullRomSegment(
            p0: StrokePoint,
            p1: StrokePoint,
            p2: StrokePoint,
            p3: StrokePoint
        ): Rect {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            var maxWidth = 0f

            // Number of interpolation steps based on distance
            val distance = p1.position.distanceTo(p2.position)
            val steps = max(2, (distance / 2f).toInt())

            for (i in 0..steps) {
                val t = i.toFloat() / steps

                // Catmull-Rom interpolation
                val pos = catmullRom(p0.position, p1.position, p2.position, p3.position, t)
                val width = catmullRomScalar(p0.width, p1.width, p2.width, p3.width, t)
                val opacity = catmullRomScalar(p0.opacity, p1.opacity, p2.opacity, p3.opacity, t)

                // Update paint
                paint.strokeWidth = width
                paint.alpha = opacity

                // Draw point
                if (i > 0) {
                    val prevT = (i - 1).toFloat() / steps
                    val prevPos = catmullRom(p0.position, p1.position, p2.position, p3.position, prevT)
                    canvas.drawLine(prevPos, pos, paint)
                }

                // Track bounds
                minX = min(minX, pos.x)
                minY = min(minY, pos.y)
                maxX = max(maxX, pos.x)
                maxY = max(maxY, pos.y)
                maxWidth = max(maxWidth, width)
            }

            val padding = maxWidth + 2f
            return Rect(
                left = minX - padding,
                top = minY - padding,
                right = maxX + padding,
                bottom = maxY + padding
            )
        }

        /**
         * Catmull-Rom interpolation for positions
         */
        private fun catmullRom(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
            val t2 = t * t
            val t3 = t2 * t

            val x = 0.5f * (
                    (2f * p1.x) +
                            (-p0.x + p2.x) * t +
                            (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                            (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3
                    )

            val y = 0.5f * (
                    (2f * p1.y) +
                            (-p0.y + p2.y) * t +
                            (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                            (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
                    )

            return Offset(x, y)
        }

        /**
         * Catmull-Rom interpolation for scalar values
         */
        private fun catmullRomScalar(v0: Float, v1: Float, v2: Float, v3: Float, t: Float): Float {
            val t2 = t * t
            val t3 = t2 * t

            return 0.5f * (
                    (2f * v1) +
                            (-v0 + v2) * t +
                            (2f * v0 - 5f * v1 + 4f * v2 - v3) * t2 +
                            (-v0 + 3f * v1 - 3f * v2 + v3) * t3
                    )
        }
    }
}