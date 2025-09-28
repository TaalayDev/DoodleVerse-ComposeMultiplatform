package io.github.taalaydev.doodleverse.engine.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.util.DirtyRect
import io.github.taalaydev.doodleverse.engine.util.toRadians
import kotlin.math.cos
import kotlin.math.sin

data class BitmapRendererArgs(
    val image: ImageBitmap,
    /** top-left destination position; if dstSize is null, draws at native size */
    val position: Offset = Offset.Zero,
    /** scale target; if null, use image's intrinsic size */
    val dstSize: IntSize? = null,
    /** source crop; null means full image */
    val srcOffset: IntOffset? = null,
    val srcSize: IntSize? = null,
    /** optional rotation around pivot (degrees) */
    val rotation: Float = 0f,
    /** pivot for rotation in destination space; null -> center of dest rect */
    val pivot: Offset? = null,
    /** paint params */
    val alpha: Float = 1f,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val colorFilter: ColorFilter? = null,
) : RendererArgs

class BitmapRenderer : Renderer<BitmapRendererArgs>() {
    private val paint = Paint()

    override fun render(canvas: Canvas, args: BitmapRendererArgs): DirtyRect {
        val img = args.image
        val dstSize = args.dstSize ?: IntSize(img.width, img.height)
        val dstRect = Rect(
            left = args.position.x,
            top = args.position.y,
            right = args.position.x + dstSize.width,
            bottom = args.position.y + dstSize.height
        )

        paint.apply {
            alpha = args.alpha.coerceIn(0f, 1f)
            blendMode = args.blendMode
            colorFilter = args.colorFilter
            isAntiAlias = true
        }

        val srcOffset = args.srcOffset ?: IntOffset.Zero
        val srcSize = args.srcSize ?: IntSize(img.width, img.height)

        val dirty = if (args.rotation == 0f) {
            // Fast path
            canvas.drawImageRect(
                image = img,
                srcOffset = srcOffset,
                srcSize = srcSize,
                dstOffset = IntOffset(dstRect.left.toInt(), dstRect.top.toInt()),
                dstSize = IntSize(dstRect.width.toInt(), dstRect.height.toInt()),
                paint = paint
            )
            dstRect
        } else {
            // Rotate around pivot
            val pivot = args.pivot ?: Offset(
                x = dstRect.left + dstRect.width * 0.5f,
                y = dstRect.top + dstRect.height * 0.5f
            )

            canvas.save()
            canvas.translate(pivot.x, pivot.y)
            canvas.rotate(args.rotation)
            canvas.translate(-pivot.x, -pivot.y)

            canvas.drawImageRect(
                image = img,
                srcOffset = srcOffset,
                srcSize = srcSize,
                dstOffset = IntOffset(dstRect.left.toInt(), dstRect.top.toInt()),
                dstSize = IntSize(dstRect.width.toInt(), dstRect.height.toInt()),
                paint = paint
            )
            canvas.restore()

            rotatedAabb(dstRect, pivot, args.rotation)
        }

        return dirty
    }

    private fun rotatedAabb(r: Rect, pivot: Offset, degrees: Float): Rect {
        if (degrees % 360f == 0f) return r
        val rad = toRadians(degrees.toDouble())
        val c = cos(rad); val s = sin(rad)

        fun rot(p: Offset): Offset {
            val x = p.x - pivot.x
            val y = p.y - pivot.y
            val rx = (x * c - y * s).toFloat() + pivot.x
            val ry = (x * s + y * c).toFloat() + pivot.y
            return Offset(rx, ry)
        }

        val pts = arrayOf(
            Offset(r.left, r.top),
            Offset(r.right, r.top),
            Offset(r.right, r.bottom),
            Offset(r.left, r.bottom)
        ).map(::rot)

        val minX = pts.minOf { it.x }
        val maxX = pts.maxOf { it.x }
        val minY = pts.minOf { it.y }
        val maxY = pts.maxOf { it.y }
        return Rect(minX, minY, maxX, maxY)
    }
}
