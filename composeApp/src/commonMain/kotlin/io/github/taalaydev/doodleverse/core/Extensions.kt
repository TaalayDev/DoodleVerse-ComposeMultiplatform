package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

fun Offset.toIntOffset(): IntOffset {
    return IntOffset(x.toInt(), y.toInt())
}

fun Size.toIntSize(): androidx.compose.ui.unit.IntSize {
    return androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt())
}
fun Color.toHex(includeAlpha: Boolean = true): String {
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    val a = (alpha * 255).roundToInt()

    fun toHexByte(value: Int) =
        value.coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()

    return buildString {
        append("#")
        if (includeAlpha) append(toHexByte(a))
        append(toHexByte(r))
        append(toHexByte(g))
        append(toHexByte(b))
    }
}