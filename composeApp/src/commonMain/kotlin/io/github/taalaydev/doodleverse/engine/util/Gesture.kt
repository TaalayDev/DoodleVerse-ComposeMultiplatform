package io.github.taalaydev.doodleverse.engine.util

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import io.github.taalaydev.doodleverse.engine.DragState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

internal suspend fun PointerInputScope.handleDrawing(
    isActive: Boolean = true,
    onStart: (Offset, Float) -> Unit = { _, _ -> },
    onDrag: (Offset, Offset, Float) -> Unit = { _, _, _ -> },
    onEnd: (Offset, Float) -> Unit = { _, _ ->},
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
                onEnd(down.position, down.pressure)
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

        onEnd(down.position, down.pressure)
    }
}

internal suspend fun PointerInputScope.handleSmoothDrawing(
    isActive: Boolean = true,
    onStart: (Offset, Float) -> Unit = { _, _ -> },
    onDrag: (Offset, Float) -> Unit = { _, _ -> },
    onEnd: () -> Unit = {},
    interpolationDensity: Float = 2.0f
) {
    if (!isActive) return

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastPosition = down.position
        var isDrawing = false

        onStart(down.position, down.pressure)
        down.consume()
        isDrawing = true

        var drag: PointerInputChange?
        do {
            val event = awaitPointerEvent()
            drag = event.changes.firstOrNull { it.id == down.id }

            drag?.let { change ->
                if (change.positionChanged()) {
                    // Use optimized interpolation for pixel art
                    val distance = (change.position - lastPosition).getDistance()

                    if (distance > interpolationDensity) {
                        // Interpolate points between last and current position
                        val steps = (distance / interpolationDensity).toInt().coerceAtLeast(1)

                        for (i in 1..steps) {
                            val t = i.toFloat() / steps
                            val interpolatedPoint =
                                androidx.compose.ui.geometry.lerp(lastPosition, change.position, t)
                            onDrag(interpolatedPoint, change.pressure)
                        }
                    } else {
                        onDrag(change.position, change.pressure)
                    }

                    lastPosition = change.position
                    change.consume()
                }
            }
        } while (drag != null && drag.pressed)

        if (isDrawing) {
            onEnd()
        }
    }
}

private fun interpolatePoints(
    start: Offset,
    end: Offset,
    density: Float,
    onPoint: (Offset) -> Unit
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val distance = sqrt(dx * dx + dy * dy)

    if (distance <= density) {
        onPoint(end)
        return
    }

    val steps = (distance / density).toInt().coerceAtLeast(1)
    val stepX = dx / steps
    val stepY = dy / steps

    for (i in 1..steps) {
        val x = start.x + stepX * i
        val y = start.y + stepY * i
        onPoint(Offset(x, y))
    }
}

/**
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

suspend fun PointerInputScope.handleDragAndZoomGestures(
    dragState: MutableState<DragState>,
    zoomSensitivity: Float = 0.05f,
    minZoom: Float = 0.8f,
    maxZoom: Float = 2.5f,
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
                if (currentEvent.changes.size < 2) {
                    return@drag
                }

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
                    val newZoom = dragState.value.zoom * scaleChange
                    dragState.value = (dragState.value.copy(
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
                    dragState.value = (dragState.value.copy(
                        draggedTo = dragState.value.draggedTo + dragChange
                    ))
                }

                prevCenter = currentCenter
                it.consume()
            }
        }
    }
}

suspend fun PointerInputScope.modDetectTransformGestures(
    panZoomLock: Boolean = false,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.modCalculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })
    }
}

fun PointerEvent.modCalculatePan(): Offset {
    if (changes.size < 2) {
        return Offset.Zero
    }

    val currentCentroid = calculateCentroid(useCurrent = true)
    if (currentCentroid == Offset.Unspecified) {
        return Offset.Zero
    }
    val previousCentroid = calculateCentroid(useCurrent = false)
    return currentCentroid - previousCentroid
}