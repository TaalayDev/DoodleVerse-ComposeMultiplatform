package io.github.taalaydev.doodleverse.engine.gesture

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import io.github.taalaydev.doodleverse.engine.util.DirtyRect

/**
 * A generic gesture event, to be adapted from platform-specific events.
 * All fields are in logical pixels.
 */
data class GestureEvent(
    val position: Offset,
    val timeMillis: Long,
    val pressure: Float? = null,
    val velocity: Float? = null,
    val pointerId: Long? = null,
    val tilt: Float? = null,     // radians [0..π/2], if available
    val azimuth: Float? = null,  // radians [0..2π], if available
)

/**
 * Base class for handling gestures.
 * Implementations should provide logic for start, move, and end events.
 */
abstract class GestureHandler<T> {
    abstract fun start(event: GestureEvent): T
    abstract fun move(event: GestureEvent): T
    abstract fun end(event: GestureEvent): T
}

/**
 * Per-stroke state holder created by a Brush.
 * Keep last point, residual spacing, paths, random seeds, etc. here.
 */
abstract class GestureSession<P>(
    val params: P
) : GestureHandler<DirtyRect>() {
    abstract override fun start(event: GestureEvent): DirtyRect
    abstract override fun move(event: GestureEvent): DirtyRect
    abstract override fun end(event: GestureEvent): DirtyRect
}

abstract class HandleCanvasGestureSession<P> {
    abstract fun startSession(canvas: Canvas, params: P): GestureSession<P>
}

abstract class HandleTapGesture {
    abstract fun handleTap(canvas: Canvas, point: Offset): DirtyRect
}