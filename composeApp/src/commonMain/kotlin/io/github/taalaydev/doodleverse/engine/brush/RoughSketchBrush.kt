package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.ProceduralBrush
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import io.github.taalaydev.doodleverse.engine.util.DirtyRect
import io.github.taalaydev.doodleverse.engine.util.currentTimeMillis
import io.github.taalaydev.doodleverse.engine.util.distanceTo
import io.github.taalaydev.doodleverse.engine.util.expand
import io.github.taalaydev.doodleverse.engine.util.lerp
import io.github.taalaydev.doodleverse.engine.util.nextGaussian
import io.github.taalaydev.doodleverse.engine.util.union
import kotlinx.datetime.Clock
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A procedural brush that creates rough, sketch-like strokes with texture and randomness.
 *
 * Features:
 * - Randomized stroke thickness for organic feel
 * - Multiple parallel strokes for rough texture
 * - Opacity variation along the stroke
 * - Directional texture based on stroke velocity
 * - Configurable roughness and density
 *
 * @property roughness Controls how much variation is applied to strokes (0.0 - 1.0)
 * @property density Number of parallel strokes to create texture (1-10)
 * @property opacityVariation Amount of opacity variation (0.0 - 1.0)
 */
class RoughSketchBrush(
    private val roughness: Float = 0.3f,
    private val density: Int = 3,
    private val opacityVariation: Float = 0.2f
) : ProceduralBrush() {

    override val id = ToolId("rough_sketch_pencil")
    override val name = "Rough Sketch Pencil"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession {
        return RoughSketchSession(
            canvas = canvas,
            params = params,
            roughness = roughness,
            density = density,
            opacityVariation = opacityVariation
        )
    }

    private class RoughSketchSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val roughness: Float,
        private val density: Int,
        private val opacityVariation: Float
    ) : StrokeSession(params) {

        private var lastPoint: Offset? = null
        private var lastVelocity: Float = 0f
        private val random = Random(Clock.currentTimeMillis())

        // Pre-generate offset patterns for consistent texture
        private val offsetPatterns = List(density) {
            Pair(
                random.nextGaussian().toFloat() * roughness,
                random.nextGaussian().toFloat() * roughness
            )
        }

        // Paints for each density layer
        private val paints = List(density) { index ->
            Paint().apply {
                color = params.color.copy(
                    alpha = params.color.alpha * (1f - opacityVariation * (index.toFloat() / density))
                )
                strokeWidth = params.size * (0.6f + 0.4f * (index.toFloat() / density))
                strokeCap = StrokeCap.Round
                style = PaintingStyle.Stroke
                isAntiAlias = true
                blendMode = params.blendMode
            }
        }

        override fun start(event: GestureEvent): DirtyRect {
            lastPoint = event.position
            lastVelocity = 0f

            // Draw initial point as a cluster of dots
            drawPoint(event.position, params.size)

            return createDirtyRect(event.position, params.size)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val currentPoint = event.position
            val prevPoint = lastPoint ?: currentPoint

            // Calculate velocity for dynamic effects
            val distance = prevPoint.distanceTo(currentPoint)
            val velocity = event.velocity ?: (distance / max(event.timeMillis - (lastPoint?.let { 16L } ?: 0L), 1L))
            lastVelocity = lerp(lastVelocity, velocity, 0.3f)

            // Skip if points are too close
            if (distance < 0.5f) {
                return null
            }

            // Draw the stroke with texture
            val dirtyRect = drawTexturedStroke(prevPoint, currentPoint, velocity)

            lastPoint = currentPoint
            return dirtyRect
        }

        override fun end(event: GestureEvent): DirtyRect {
            // Final touch-up point for smooth ending
            val rect = lastPoint?.let { point ->
                drawPoint(point, params.size * 0.8f)
                createDirtyRect(point, params.size)
            }

            lastPoint = null
            return rect
        }

        /**
         * Draws a textured stroke between two points using multiple parallel lines
         */
        private fun drawTexturedStroke(from: Offset, to: Offset, velocity: Float): DirtyRect {
            var combinedRect: DirtyRect = null

            // Adjust density based on velocity (more strokes when moving slowly)
            val velocityFactor = 1f / (1f + velocity * 0.1f)
            val activeDensity = max(1, (density * velocityFactor).toInt())

            for (i in 0 until activeDensity) {
                val paint = paints[i]
                val (offsetX, offsetY) = offsetPatterns[i]

                // Apply roughness offset
                val roughnessScale = params.size * roughness * 0.5f
                val fromOffset = Offset(
                    from.x + offsetX * roughnessScale + random.nextGaussian().toFloat() * roughness * 0.5f,
                    from.y + offsetY * roughnessScale + random.nextGaussian().toFloat() * roughness * 0.5f
                )
                val toOffset = Offset(
                    to.x + offsetX * roughnessScale + random.nextGaussian().toFloat() * roughness * 0.5f,
                    to.y + offsetY * roughnessScale + random.nextGaussian().toFloat() * roughness * 0.5f
                )

                // Vary stroke width slightly
                val widthVariation = 1f + (random.nextFloat() - 0.5f) * roughness * 0.3f
                paint.strokeWidth = params.size * (0.6f + 0.4f * (i.toFloat() / density)) * widthVariation

                // Draw the stroke segment
                canvas.drawLine(fromOffset, toOffset, paint)

                // Accumulate dirty rectangle
                val rect = createDirtyRect(fromOffset, toOffset, paint.strokeWidth)
                combinedRect = combinedRect.union(rect)
            }

            return combinedRect
        }

        /**
         * Draws a point as a cluster for texture
         */
        private fun drawPoint(point: Offset, size: Float) {
            for (i in 0 until min(density, 3)) {
                val paint = paints[i]
                val (offsetX, offsetY) = offsetPatterns[i]
                val roughnessScale = size * roughness * 0.3f

                val offset = Offset(
                    point.x + offsetX * roughnessScale,
                    point.y + offsetY * roughnessScale
                )

                canvas.drawCircle(offset, size * 0.3f * (1f - i * 0.2f / density), paint)
            }
        }

        /**
         * Creates a dirty rectangle for a single point
         */
        private fun createDirtyRect(point: Offset, size: Float): Rect {
            val padding = size * 2f
            return Rect(
                left = point.x - padding,
                top = point.y - padding,
                right = point.x + padding,
                bottom = point.y + padding
            )
        }

        /**
         * Creates a dirty rectangle for a line segment
         */
        private fun createDirtyRect(from: Offset, to: Offset, strokeWidth: Float): Rect {
            val padding = strokeWidth + params.size * roughness * 2f
            return Rect(
                left = min(from.x, to.x) - padding,
                top = min(from.y, to.y) - padding,
                right = max(from.x, to.x) + padding,
                bottom = max(from.y, to.y) + padding
            )
        }
    }
}