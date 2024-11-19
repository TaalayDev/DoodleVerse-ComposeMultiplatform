package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset

fun Offset.toIntOffset(): IntOffset {
    return IntOffset(x.toInt(), y.toInt())
}

fun Size.toIntSize(): androidx.compose.ui.unit.IntSize {
    return androidx.compose.ui.unit.IntSize(width.toInt(), height.toInt())
}