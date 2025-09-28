package io.github.taalaydev.doodleverse

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin

actual class PlatformBitmap internal constructor(internal val bmp: Bitmap) {
    actual val width: Int get() = bmp.width
    actual val height: Int get() = bmp.height
    actual fun asImageBitmap(): ImageBitmap = bmp.asComposeImageBitmap()
}

actual class PlatformCanvas internal constructor(internal val canvas: Canvas)

actual fun createPlatformBitmap(width: Int, height: Int, clear: Boolean): PlatformBitmap {
    val b = Bitmap().apply {
        allocPixels(org.jetbrains.skia.ImageInfo.makeS32(width, height, org.jetbrains.skia.ColorAlphaType.PREMUL))
        if (clear) erase(0x00000000.toInt())
    }
    return PlatformBitmap(b)
}

actual fun PlatformBitmap.withCanvas(block: (PlatformCanvas) -> Unit) {
    val canvas = Canvas(this.bmp)
    block(PlatformCanvas(canvas))
}

actual fun PlatformCanvas.drawPath(path: Path, color: Color, stroke: Stroke) {
    val skPaint = Paint().apply {
        isAntiAlias = true
        mode = PaintMode.STROKE
        strokeWidth = stroke.width
        strokeCap = when (stroke.cap) {
            StrokeCap.Butt -> PaintStrokeCap.BUTT
            StrokeCap.Round -> PaintStrokeCap.ROUND
            StrokeCap.Square -> PaintStrokeCap.SQUARE
            else -> PaintStrokeCap.ROUND
        }
        strokeJoin = when (stroke.join) {
            StrokeJoin.Miter -> PaintStrokeJoin.MITER
            StrokeJoin.Round -> PaintStrokeJoin.ROUND
            StrokeJoin.Bevel -> PaintStrokeJoin.BEVEL
            else -> PaintStrokeJoin.ROUND
        }
        color4f = Color4f(color.red, color.green, color.blue, color.alpha)
    }
    val skPath = org.jetbrains.skia.Path().apply { addPath(path.asSkiaPath()) }
    canvas.drawPath(skPath, skPaint)
}

actual fun PlatformCanvas.clear(rect: Rect, color: Color) {
    val paint = Paint().apply {
        mode = PaintMode.FILL
        color4f = Color4f(0f,0f,0f,0f)
    }
    canvas.save()
    canvas.clipRect(rect.toSkiaRect())
    canvas.clear(0x00000000.toInt())
    if (color.alpha > 0f) {
        val fill = Paint().apply {
            mode = PaintMode.FILL
            color4f = Color4f(color.red, color.green, color.blue, color.alpha)
        }
        canvas.drawRect(rect.toSkiaRect(), fill)
    }
    canvas.restore()
}