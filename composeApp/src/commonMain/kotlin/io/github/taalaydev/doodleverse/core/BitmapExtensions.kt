package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize

fun ImageBitmap.copy(): ImageBitmap {
    val image =  ImageBitmap(width, height)
    val canvas = Canvas(image)
    canvas.drawImage(this, Offset.Zero, Paint())
    return image
}

fun ImageBitmap.Companion.createScaledBitmap(
    source: ImageBitmap,
    newWidth: Int,
    newHeight: Int,
): ImageBitmap {
    val image = ImageBitmap(newWidth, newHeight)
    val canvas = Canvas(image)
    canvas.drawImageRect(
        image = source,
        dstSize = IntSize(newWidth, newHeight),
        paint = Paint(),
    )
    return image
}

fun ImageBitmap.withBackground(color: androidx.compose.ui.graphics.Color): ImageBitmap {
    val image = ImageBitmap(width, height)
    val canvas = Canvas(image)
    canvas.drawRect(
        Rect(0f, 0f, width.toFloat(), height.toFloat()),
        Paint().apply { this.color = color }
    )
    canvas.drawImage(this, Offset.Zero, Paint())
    return image
}

fun ImageBitmap.resize(newWidth: Int, newHeight: Int): ImageBitmap {
    return ImageBitmap.createScaledBitmap(this, newWidth, newHeight)
}