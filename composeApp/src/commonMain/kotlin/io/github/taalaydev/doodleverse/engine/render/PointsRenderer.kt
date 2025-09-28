package io.github.taalaydev.doodleverse.engine.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.taalaydev.doodleverse.engine.util.DirtyRect
import io.github.taalaydev.doodleverse.engine.util.union
import kotlin.math.max

data class PointsRendererArgs(
    val points: List<Offset>,
    val color: Color = Color.Black,
    /** radius of each point in px */
    val radius: Float = 2f,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val alpha: Float = 1f
) : RendererArgs

class PointsRenderer : Renderer<PointsRendererArgs>() {
    private val paint = Paint()

    override fun render(canvas: Canvas, args: PointsRendererArgs): DirtyRect {
        if (args.points.isEmpty()) return null

        paint.apply {
            isAntiAlias = true
            color = args.color
            blendMode = args.blendMode
            alpha = args.alpha.coerceIn(0f, 1f)
        }

        var dirty: Rect? = null
        val r = max(0.25f, args.radius)
        args.points.forEach { p ->
            canvas.drawCircle(p, r, paint)
            val dr = Rect(p.x - r, p.y - r, p.x + r, p.y + r)
            dirty = dirty.union(dr)
        }
        return dirty
    }
}
