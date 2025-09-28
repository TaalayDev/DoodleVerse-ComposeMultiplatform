
package io.github.taalaydev.doodleverse.engine

import androidx.compose.ui.graphics.ImageBitmap
import io.github.taalaydev.doodleverse.PixelBuffer

fun floodFillFast(
    target: ImageBitmap,
    x: Int,
    y: Int,
    replacementArgb: Int,
) {
    if (x < 0 || y < 0 || x >= target.width || y >= target.height) return
    val buffer = PixelBuffer(target)
    val targetColor = buffer.get(x, y)
    if (targetColor == replacementArgb) return

    // Non-recursive stack-based fill (avoids recursion and allocations)
    val stack = ArrayDeque<Int>()
    stack.add((y shl 16) or x)

    while (stack.isNotEmpty()) {
        val v = stack.removeLast()
        val cx = v and 0xFFFF
        val cy = v shr 16

        if (cx < 0 || cy < 0 || cx >= buffer.width || cy >= buffer.height) continue
        if (buffer.get(cx, cy) != targetColor) continue

        // Expand left & right in one scanline (scanline fill)
        var lx = cx
        var rx = cx
        while (lx - 1 >= 0 && buffer.get(lx - 1, cy) == targetColor) lx--
        while (rx + 1 < buffer.width && buffer.get(rx + 1, cy) == targetColor) rx++

        for (ix in lx..rx) buffer.set(ix, cy, replacementArgb)

        // push neighbors above and below
        if (cy - 1 >= 0) {
            var i = lx
            while (i <= rx) {
                if (buffer.get(i, cy - 1) == targetColor) stack.add(((cy - 1) shl 16) or i)
                i++
            }
        }
        if (cy + 1 < buffer.height) {
            var i = lx
            while (i <= rx) {
                if (buffer.get(i, cy + 1) == targetColor) stack.add(((cy + 1) shl 16) or i)
                i++
            }
        }
    }

    buffer.flushTo(target)
}
