
package io.github.taalaydev.doodleverse.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

object DrawRenderer {

    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.Stroke
    }
    private val smoothStrokePaint = Paint().apply {
        isAntiAlias = true
        style = PaintingStyle.Stroke
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
        filterQuality = FilterQuality.Medium
    }
    private val stampPaint = Paint().apply {
        isAntiAlias = true
        filterQuality = FilterQuality.Medium
    }

    internal fun calcOpacity(alpha: Float, brushOpacity: Float): Float {
        return (max(alpha, brushOpacity) - min(alpha, brushOpacity)).coerceAtLeast(0f)
    }

    private fun configStrokePaint(dst: Paint, path: DrawingPath, roundCaps: Boolean): Paint {
        val b = path.brush
        dst.color = path.color
        dst.strokeWidth = path.size
        dst.strokeCap = if (roundCaps) StrokeCap.Round else b.strokeCap
        dst.strokeJoin = if (roundCaps) StrokeJoin.Round else b.strokeJoin
        dst.pathEffect = b.pathEffect?.invoke(path.size)
        dst.blendMode = b.blendMode
        dst.colorFilter = if (b.brush != null) ColorFilter.tint(path.color) else null
        dst.alpha = calcOpacity(path.color.alpha, b.opacityDiff)
        return dst
    }

    private fun configStampPaint(dst: Paint, path: DrawingPath, alphaMul: Float): Paint {
        val b = path.brush
        dst.color = path.color
        dst.blendMode = b.blendMode
        dst.colorFilter = ColorFilter.tint(path.color)
        dst.alpha = calcOpacity(path.color.alpha, b.opacityDiff) * alphaMul
        return dst
    }

    fun renderPathCanvas(
        canvas: Canvas,
        drawingPath: DrawingPath,
        canvasSize: Size,
        useSmoothing: Boolean = true,
        brushImage: ImageBitmap? = null
    ) {
        val brush = drawingPath.brush

        if (brush.customPainter != null) {
            brush.customPainter.invoke(canvas, canvasSize, drawingPath); return
        }

        if (brush.brush != null && brushImage != null) {
            // Bitmap stamp brush path
            drawBitmapStamps(canvas, drawingPath, brushImage)
            return
        }

        // Vector stroke
        val p = if (useSmoothing && !brush.isShape) configStrokePaint(smoothStrokePaint, drawingPath, true)
                else configStrokePaint(strokePaint, drawingPath, false)

        if (useSmoothing && drawingPath.points.size > 2 && !brush.isShape) {
            canvas.drawPath(drawingPath.path, p)
            // Subtle extra pass reduces "tooth"
            if (drawingPath.size > 1f) {
                val a = p.alpha
                val w = p.strokeWidth
                p.alpha = (a * 0.28f)
                p.strokeWidth = w * 0.85f
                canvas.drawPath(drawingPath.path, p)
                p.alpha = a; p.strokeWidth = w
            }
        } else {
            canvas.drawPath(drawingPath.path, p)
        }
    }

    private fun drawBitmapStamps(
        canvas: Canvas,
        path: DrawingPath,
        brushImage: ImageBitmap,
    ) {
        val pts = path.points
        if (pts.isEmpty()) return

        // Always stamp first & last
        val first = pts.first().toOffset()
        val last = pts.last().toOffset()
        val size = path.size
        val half = size / 2f

        val p = configStampPaint(stampPaint, path, 1f)

        // First
        canvas.drawImageRect(
            brushImage,
            dstOffset = IntOffset((first.x - half).toInt(), (first.y - half).toInt()),
            dstSize = IntSize(size.toInt(), size.toInt()),
            paint = p
        )

        var prev = first
        for (i in 1 until pts.size) {
            val cur = pts[i].toOffset()
            stampBetween(canvas, prev, cur, brushImage, path, p)
            prev = cur
        }

        // Last
        canvas.drawImageRect(
            brushImage,
            dstOffset = IntOffset((last.x - half).toInt(), (last.y - half).toInt()),
            dstSize = IntSize(size.toInt(), size.toInt()),
            paint = p
        )
    }

    private fun stampBetween(
        canvas: Canvas,
        start: Offset,
        end: Offset,
        img: ImageBitmap,
        path: DrawingPath,
        paint: Paint,
    ) {
        val size = path.size
        val half = size / 2f

        val dx = end.x - start.x
        val dy = end.y - start.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0.001f) return

        // Adaptive spacing: denser for slow, sparser for fast
        val base = (size * 0.33f).coerceIn(0.8f, 10f)
        val steps = (dist / base).toInt().coerceAtLeast(1)
        val stepX = dx / steps
        val stepY = dy / steps

        val rotBase = atan2(dy, dx)

        for (i in 1..steps) {
            val px = start.x + stepX * i
            val py = start.y + stepY * i

            val rotation = if (path.brush.rotationRandomness > 0f) {
                val randomRotation = (Random.nextFloat() - 0.5f) * 2f * path.brush.rotationRandomness
                rotBase + randomRotation
            } else rotBase

            val alphaMul = 1f // could fade with taper if desired

            // Fast path: avoid save/restore when no rotation
            if (rotation == 0f) {
                paint.alpha = configStampPaint(paint, path, alphaMul).alpha
                canvas.drawImageRect(
                    img,
                    dstOffset = IntOffset((px - half).toInt(), (py - half).toInt()),
                    dstSize = IntSize(size.toInt(), size.toInt()),
                    paint = paint
                )
            } else {
                canvas.save()
                canvas.translate(px, py)
                canvas.rotate((rotation * 180f / PI.toFloat()))
                canvas.translate(-half, -half)
                paint.alpha = configStampPaint(paint, path, alphaMul).alpha
                canvas.drawImageRect(
                    img,
                    dstOffset = IntOffset(0, 0),
                    dstSize = IntSize(size.toInt(), size.toInt()),
                    paint = paint
                )
                canvas.restore()
            }
        }
    }
}
