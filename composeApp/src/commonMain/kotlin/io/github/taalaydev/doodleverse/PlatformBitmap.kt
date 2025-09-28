package io.github.taalaydev.doodleverse

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect

expect class PlatformBitmap {
    val width: Int
    val height: Int
    fun asImageBitmap(): ImageBitmap
}

expect class PlatformCanvas

expect fun createPlatformBitmap(width: Int, height: Int, clear: Boolean = false): PlatformBitmap

expect fun PlatformBitmap.withCanvas(block: (PlatformCanvas) -> Unit)

expect fun PlatformCanvas.drawPath(path: Path, color: Color, stroke: Stroke)

expect fun PlatformCanvas.clear(rect: Rect, color: Color = Color.Transparent)