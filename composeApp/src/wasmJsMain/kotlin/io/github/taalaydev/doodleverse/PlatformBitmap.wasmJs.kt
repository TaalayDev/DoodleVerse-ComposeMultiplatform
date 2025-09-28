package io.github.taalaydev.doodleverse

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect

// NOTE: This is a simplified placeholder. You'll need to wire CGImage/CGContext.
actual class PlatformBitmap {
    actual val width: Int get() = _width
    actual val height: Int get() = _height
    private var _width: Int = 1
    private var _height: Int = 1
    actual fun asImageBitmap(): ImageBitmap = ImageBitmap(width, height)
}

actual class PlatformCanvas

actual fun createPlatformBitmap(width: Int, height: Int, clear: Boolean): PlatformBitmap {
    return PlatformBitmap()
}

actual fun PlatformBitmap.withCanvas(block: (PlatformCanvas) -> Unit) {
    block(PlatformCanvas())
}

actual fun PlatformCanvas.drawPath(path: Path, color: Color, stroke: Stroke) {
    // TODO: Use CoreGraphics to draw into CGBitmapContext
}

actual fun PlatformCanvas.clear(rect: Rect, color: Color) {
    // TODO
}