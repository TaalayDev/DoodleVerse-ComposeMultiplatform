package io.github.taalaydev.doodleverse.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

data class Viewport(val scale: Float, val offset: Offset) // offset = where the bitmap starts inside the Canvas

fun viewToImage(p: Offset, vp: Viewport, imgW: Int, imgH: Int): Offset {
    val x = (p.x - vp.offset.x) / vp.scale
    val y = (p.y - vp.offset.y) / vp.scale
    // Clamp to bitmap bounds to avoid drawing outside
    return Offset(
        x.coerceIn(0f, imgW - 1f),
        y.coerceIn(0f, imgH - 1f)
    )
}

fun Viewport.viewToImage(p: Offset, imgW: Int, imgH: Int): Offset = viewToImage(p, this, imgW, imgH)
fun Viewport.viewToImageX(x: Float, imgW: Int): Float = ((x - offset.x) / scale).coerceIn(0f, imgW - 1f)
fun Viewport.viewToImageY(y: Float, imgH: Int): Float = ((y - offset.y) / scale).coerceIn(0f, imgH - 1f)
fun Viewport.viewToImage(p: Offset, size: IntSize) = viewToImage(p, this, size.width, size.height)