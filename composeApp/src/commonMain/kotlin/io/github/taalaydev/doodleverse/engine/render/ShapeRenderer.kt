package io.github.taalaydev.doodleverse.engine.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.taalaydev.doodleverse.engine.util.DirtyRect

data class RectRendererArgs(
    val rect: Rect,
    val color: Color = Color.Black,
    val style: PaintingStyle = PaintingStyle.Stroke,
    val strokeWidth: Float = 1f,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val alpha: Float = 1f
) : RendererArgs

class RectRenderer : Renderer<RectRendererArgs>() {
    private val paint = Paint()
    override fun render(canvas: Canvas, args: RectRendererArgs): DirtyRect {
        paint.apply {
            isAntiAlias = true
            color = args.color
            style = args.style
            strokeWidth = args.strokeWidth
            blendMode = args.blendMode
            alpha = args.alpha.coerceIn(0f, 1f)
        }
        if (args.style == PaintingStyle.Fill) {
            canvas.drawRect(args.rect, paint)
            return args.rect
        } else {
            canvas.drawRect(args.rect, paint)
            val pad = args.strokeWidth * 0.5f
            val r = args.rect
            return Rect(r.left - pad, r.top - pad, r.right + pad, r.bottom + pad)
        }
    }
}

data class CircleRendererArgs(
    val center: Offset,
    val radius: Float,
    val color: Color = Color.Black,
    val style: PaintingStyle = PaintingStyle.Fill,
    val strokeWidth: Float = 1f,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val alpha: Float = 1f
) : RendererArgs

class CircleRenderer : Renderer<CircleRendererArgs>() {
    private val paint = Paint()
    override fun render(canvas: Canvas, args: CircleRendererArgs): DirtyRect {
        paint.apply {
            isAntiAlias = true
            color = args.color
            style = args.style
            strokeWidth = args.strokeWidth
            blendMode = args.blendMode
            alpha = args.alpha.coerceIn(0f, 1f)
        }
        canvas.drawCircle(args.center, args.radius, paint)
        val pad = if (args.style == PaintingStyle.Stroke) args.strokeWidth * 0.5f else 0f
        val r = args.radius + pad
        return Rect(args.center.x - r, args.center.y - r, args.center.x + r, args.center.y + r)
    }
}
