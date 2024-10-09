package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
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