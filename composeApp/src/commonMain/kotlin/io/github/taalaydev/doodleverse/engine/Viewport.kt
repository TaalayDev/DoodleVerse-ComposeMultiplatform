package io.github.taalaydev.doodleverse.engine

import androidx.compose.ui.geometry.Offset

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