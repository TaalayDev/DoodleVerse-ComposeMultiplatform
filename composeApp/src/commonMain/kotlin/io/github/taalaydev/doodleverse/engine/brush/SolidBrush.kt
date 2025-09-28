package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import io.github.taalaydev.doodleverse.engine.render.PathRenderer
import io.github.taalaydev.doodleverse.engine.render.PathRendererArgs
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.ShapeBrush
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Solid round brush using PathRenderer for all drawing.
 * Interprets BrushParams.size as stroke width (px).
 */
class SolidBrush : ShapeBrush() {

    override val id = ToolId("shape.solid.path")
    override val name: String = "Solid (Path)"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession {
        return Session(canvas, params)
    }

    private class Session(
        private val canvas: Canvas,
        params: BrushParams,
    ) : StrokeSession(params) {

        private val pathRenderer = PathRenderer()

        // We draw incremental segments using this reusable path.
        private val segPath = Path()

        // For optional “start/end” dot we reuse this path to add a tiny oval.
        private val dotPath = Path()

        private var last: Offset = Offset.Zero

        override fun start(event: GestureEvent): DirtyRect {
            last = event.position
            // Draw a small round "dot" at start to ensure visible tap with round look.
            return renderDot(center = last, strokeWidth = strokeWidth(event))
        }

        override fun move(event: GestureEvent): DirtyRect {
            val current = event.position
            // Avoid zero-length segments (prevents eventless work and NaNs).
            if ((current - last).getLength() < 0.001f) return null

            // Build a tiny path segment from last -> current
            segPath.reset()
            segPath.moveTo(last.x, last.y)
            segPath.quadraticBezierTo(
                (last.x + current.x) * 0.5f,
                (last.y + current.y) * 0.5f,
                current.x,
                current.y
            )

            val dirty = pathRenderer.render(
                canvas,
                PathRendererArgs(
                    path = segPath,
                    color = params.color,
                    style = PaintingStyle.Stroke,
                    strokeWidth = strokeWidth(event),
                    strokeCap = StrokeCap.Round,
                    strokeJoin = StrokeJoin.Round,
                    blendMode = params.blendMode,
                    alpha = 1f // use color’s alpha
                )
            )

            last = current
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            // Finalize with a “cap” dot at the end for crisp termination.
            val d1 = renderDot(center = event.position, strokeWidth = strokeWidth(event))
            return d1
        }

        /** Stroke width in px; respects pressure if provided. */
        private fun strokeWidth(event: GestureEvent): Float {
            val p = (event.pressure ?: params.pressure).coerceAtLeast(0.01f)
            return max(0.25f, params.size * p)
        }

        /**
         * Renders a circular dot via PathRenderer (Fill). Using fill avoids edge cases where
         * a zero-length stroked segment might be discarded by the rasterizer.
         */
        private fun renderDot(center: Offset, strokeWidth: Float): Rect? {
            val r = max(0.25f, strokeWidth * 0.5f)
            dotPath.reset()
            dotPath.addOval(
                Rect(
                    left = center.x - r,
                    top = center.y - r,
                    right = center.x + r,
                    bottom = center.y + r,
                ),
            )
            return pathRenderer.render(
                canvas,
                PathRendererArgs(
                    path = dotPath,
                    color = params.color,
                    style = PaintingStyle.Fill,
                    strokeWidth = 0f, // ignored for Fill
                    strokeCap = StrokeCap.Round,
                    strokeJoin = StrokeJoin.Round,
                    blendMode = params.blendMode,
                    alpha = 1f
                )
            )
        }

        // Small helper for vector length to avoid extra allocs
        private fun Offset.getLength(): Float = sqrt(x * x + y * y)
    }
}
