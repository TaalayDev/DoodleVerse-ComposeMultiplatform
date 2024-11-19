package io.github.taalaydev.doodleverse.core

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.runtime.MutableState
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import io.github.taalaydev.doodleverse.data.models.BrushData
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
    data object Zoom : Tool()
    data object Drag : Tool()
    data object Fill : Tool()
    data object Eyedropper : Tool()
    data object Selection: Tool()


    val isBrush: Boolean get() = this is Brush
    val isEraser: Boolean get() = this is Eraser
    val isShape: Boolean get() = this is Shape
    val isTextTool: Boolean get() = this is TextTool
    val isZoom: Boolean get() = this is Zoom
    val isDrag: Boolean get() = this is Drag
    val isFill: Boolean get() = this is Fill
    val isEyedropper: Boolean get() = this is Eyedropper
    val isSelection: Boolean get() = this is Selection
}

data class DragState(
    val zoom: Float = 1f,
    val draggedTo: Offset = Offset.Zero,
    val rotation: Float = 0f,
)

internal suspend fun PointerInputScope.handleDrawing(
    isActive: Boolean = true,
    onStart: (Offset, Float) -> Unit = { _, _ -> },
    onDrag: (Offset, Offset, Float) -> Unit = { _, _, _ -> },
    onEnd: () -> Unit = {},
    fingersCount: Int = 1,
    touchAccuracy: Float = 1.0f,
) {
    if (!isActive) return
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onStart(down.position, down.pressure)
        down.consume()

        var drag: PointerInputChange?
        do {
            val event = awaitPointerEvent()
            if (event.changes.size != fingersCount) {
                onEnd()
                break
            }
            drag = event.changes.firstOrNull { it.id == down.id }
            drag?.let {
                if (it.positionChanged()) {
                    onDrag(it.previousPosition, it.position, it.pressure)
                    it.consume()
                }
            }
        } while (drag != null && drag.pressed)

        onEnd()
    }
}

/*
    * Handle drag gesture with a single finger

 */
suspend fun PointerInputScope.detectAdvancedVerticalDragGestures(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onVerticalDrag: (event: PointerEvent, change: PointerInputChange, dragAmount: Float, pointerCount: Int) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        var overSlop = 0f
        val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
            change.consume()
            overSlop = over
        }

        if (drag != null) {
            onDragStart.invoke(drag.position)


            //Here's the block of code I added to return count
            //By the way, IM not sure which pointer returns the drag amount
            //but i think it returns the drag amount for the pointer
            //that had the first down event (that touched the screen first)
            val changes = currentEvent.changes.takeLast(2)
            val count: Int = if (changes.size == 1) {
                1
            } else if (changes.size == 2) {
                if (changes.last().id != changes.first().id) {
                    2
                } else {
                    1
                }
            } else {
                0
            }

            onVerticalDrag.invoke(currentEvent, drag, overSlop, count)
            if (verticalDrag(drag.id) {
                    onVerticalDrag(currentEvent, it, it.positionChange().y, count)
                    it.consume()
                }
            ) {
                onDragEnd()
            }
        }
    }
}


internal fun Offset.getDensityOffsetBetweenPoints(
    startPoint: Offset,
    density: Float = 10f,
    handleOffset: (Offset) -> Unit
) {
    val dx = this.x - startPoint.x
    val dy = this.y - startPoint.y

    // Use Manhattan distance for faster calculation
    val distance = abs(dx) + abs(dy)

    // Early exit if distance is too small
    if (distance < density) {
        handleOffset(this)
        return
    }

    val steps = (distance / density).toInt()
    val stepX = dx / steps
    val stepY = dy / steps

    var x = startPoint.x
    var y = startPoint.y

    repeat(steps) {
        handleOffset(Offset(x, y))
        x += stepX
        y += stepY
    }

    // Handle the last point
    handleOffset(this)
}

private fun quadraticBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val x = (1 - t).pow(2) * p0.x + 2 * (1 - t) * t * p1.x + t.pow(2) * p2.x
    val y = (1 - t).pow(2) * p0.y + 2 * (1 - t) * t * p1.y + t.pow(2) * p2.y
    return Offset(x, y)
}

internal fun Offset.distanceTo(other: Offset): Float {
    return sqrt((this.x - other.x).pow(2) + (this.y - other.y).pow(2))
}

sealed interface DrawState {
    data object Idle : DrawState
    data object Started : DrawState
    data object Drag : DrawState
    data object End : DrawState
}
