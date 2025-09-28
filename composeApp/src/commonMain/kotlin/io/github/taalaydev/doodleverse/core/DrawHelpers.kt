package io.github.taalaydev.doodleverse.core

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import io.github.taalaydev.doodleverse.data.models.BrushData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

sealed class Tool {
    data class Brush(
        val brush: BrushData
    ) : Tool()
    data class Eraser(
        val brush: BrushData
    ) : Tool()
    data class Shape(
        val brush: BrushData
    ) : Tool()
    data class TextTool(
        val size: Float,
        val color: Int
    ) : Tool()
    data class Curve(
        val brush: BrushData
    ) : Tool()
    data object Zoom : Tool()
    data object Drag : Tool()
    data object Fill : Tool()
    data object Eyedropper : Tool()
    data object Selection: Tool()

    val isBrush: Boolean get() = this is Brush
    val isEraser: Boolean get() = this is Eraser
    val isShape: Boolean get() = this is Shape
    val isTextTool: Boolean get() = this is TextTool
    val isCurve: Boolean get() = this is Curve
    val isZoom: Boolean get() = this is Zoom
    val isDrag: Boolean get() = this is Drag
    val isFill: Boolean get() = this is Fill
    val isEyedropper: Boolean get() = this is Eyedropper
    val isSelection: Boolean get() = this is Selection
}

