package io.github.taalaydev.doodleverse

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap

actual class PlatformBitmap internal constructor(internal val bmp: Bitmap) {
    actual val width: Int get() = bmp.width
    actual val height: Int get() = bmp.height
    actual fun asImageBitmap(): ImageBitmap = bmp.asImageBitmap()
}

actual class PlatformCanvas internal constructor(internal val canvas: Canvas)

actual fun createPlatformBitmap(width: Int, height: Int, clear: Boolean): PlatformBitmap {
    val b = createBitmap(width, height)
    if (clear) {
        b.eraseColor(android.graphics.Color.TRANSPARENT)
    }
    return PlatformBitmap(b)
}

actual fun PlatformBitmap.withCanvas(block: (PlatformCanvas) -> Unit) {
    val c = Canvas(this.bmp)
    block(PlatformCanvas(c))
}

actual fun PlatformCanvas.drawPath(path: Path, color: Color, stroke: Stroke) {
    val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = stroke.width
        strokeCap = when (stroke.cap) {
            StrokeCap.Butt -> Paint.Cap.BUTT
            StrokeCap.Round -> Paint.Cap.ROUND
            StrokeCap.Square -> Paint.Cap.SQUARE
            else -> Paint.Cap.ROUND
        }
        strokeJoin = when (stroke.join) {
            StrokeJoin.Miter -> Paint.Join.MITER
            StrokeJoin.Round -> Paint.Join.ROUND
            StrokeJoin.Bevel -> Paint.Join.BEVEL
            else -> Paint.Join.ROUND
        }
        colorFilter = null
        colorInt = color.toArgb()
    }
    canvas.drawPath(path.asAndroidPath(), paint)
}

private var Paint.colorInt: Int
    get() = color
    set(value) { color = value }

actual fun PlatformCanvas.clear(rect: Rect, color: Color) {
    val paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint)
    if (color.alpha > 0f) {
        val fill = Paint().apply { this.color = color.toArgb() }
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, fill)
    }
}