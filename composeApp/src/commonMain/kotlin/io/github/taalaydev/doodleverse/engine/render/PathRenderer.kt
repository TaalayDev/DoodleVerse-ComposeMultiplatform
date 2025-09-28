package io.github.taalaydev.doodleverse.engine.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.taalaydev.doodleverse.engine.util.DirtyRect

data class PathRendererArgs(
    val path: Path,
    val color: Color = Color.Black,
    val style: PaintingStyle = PaintingStyle.Stroke,
    val strokeWidth: Float = 1f,
    val strokeCap: StrokeCap = StrokeCap.Butt,
    val strokeJoin: StrokeJoin = StrokeJoin.Miter,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val alpha: Float = 1f
) : RendererArgs

class PathRenderer : Renderer<PathRendererArgs>() {
    private val paint = Paint()

    override fun render(canvas: Canvas, args: PathRendererArgs): DirtyRect {
        paint.apply {
            isAntiAlias = true
            color = args.color
            style = args.style
            strokeWidth = args.strokeWidth
            strokeCap = args.strokeCap
            strokeJoin = args.strokeJoin
            blendMode = args.blendMode
            alpha = args.alpha.coerceIn(0f, 1f)
        }

        canvas.drawPath(args.path, paint)

        // Conservative dirty rect: path bounds inflated by half stroke width
        val b = args.path.getBounds()
        val pad = if (args.style == PaintingStyle.Stroke) args.strokeWidth * 0.5f else 0f
        return Rect(b.left - pad, b.top - pad, b.right + pad, b.bottom + pad)
    }
}
