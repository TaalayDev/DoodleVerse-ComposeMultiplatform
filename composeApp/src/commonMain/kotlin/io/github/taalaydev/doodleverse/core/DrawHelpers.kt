package io.github.taalaydev.doodleverse.core

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.runtime.MutableState
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

internal fun distanceBetween(a: Offset, b: Offset) = sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
internal fun centerOf(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2)

data class DragState(
    val zoom: Float = 1f,
    val draggedTo: Offset = Offset.Zero,
)

suspend fun PointerInputScope.handleDragAndZoomGestures(
    dragState: DragState,
    zoomSensitivity: Float = 0.05f,
    minZoom: Float = 0.8f,
    maxZoom: Float = 2.5f,
    onChange: (DragState) -> Unit = {},
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var drag: PointerInputChange?
        var startDistance: Float? = null
        var startCenter: Offset? = null

        do {
            drag = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                if (currentEvent.changes.size >= 2) {
                    if (startDistance == null) {
                        startDistance = distanceBetween(
                            currentEvent.changes[0].position,
                            currentEvent.changes[1].position
                        )
                    }
                    if (startCenter == null) {
                        startCenter = centerOf(
                            currentEvent.changes[0].position,
                            currentEvent.changes[1].position
                        )
                    }
                    change.consume()
                }
            }
        } while (drag != null && !drag.isConsumed)

        var prevCenter: Offset? = startCenter
        if (drag != null) {
            drag(drag.id) {
                if (currentEvent.changes.size >= 2) {
                    /**
                     * Zoom
                     */
                    val currentDistance = distanceBetween(
                        currentEvent.changes[0].position,
                        currentEvent.changes[1].position
                    )

                    if (startDistance != null) {
                        val rawScaleChange = currentDistance / startDistance!!
                        val scaleChange = 1 + (rawScaleChange - 1) * zoomSensitivity
                        val newZoom = dragState.zoom * scaleChange
                        onChange(dragState.copy(
                            zoom = newZoom.coerceIn(minZoom, maxZoom)
                        ))
                    }

                    /**
                     * Drag
                     */
                    val currentCenter = centerOf(
                        currentEvent.changes[0].position,
                        currentEvent.changes[1].position
                    )

                    if (prevCenter != null) {
                        val dragChange = currentCenter - prevCenter!!
                        onChange(dragState.copy(
                            draggedTo = dragState.draggedTo + dragChange
                        ))
                    }

                    prevCenter = currentCenter
                    it.consume()
                }
            }
        }
    }
}

internal suspend fun PointerInputScope.handleDrawing(
    onStart: (Offset) -> Unit = {},
    onDrag: (Offset, Offset) -> Unit = { _, _ -> },
    onEnd: () -> Unit = {},
    fingersCount: Int = 1,
    touchAccuracy: Float = 1.0f,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onStart(down.position)
        down.consume()

        var drag: PointerInputChange?
        do {
            drag = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            drag?.let {
                if (it.positionChanged()) {
                    onDrag(it.previousPosition, it.position)
                    it.consume()
                }
            }
        } while (drag != null && drag.pressed)

        onEnd()
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
