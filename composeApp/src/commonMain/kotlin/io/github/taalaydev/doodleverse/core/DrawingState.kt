package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.util.lerp
import io.github.taalaydev.doodleverse.data.models.DrawingPath

enum class CanvasDrawState {
    Idle, Start, Drawing, Ended
}

data class CanvasDrawingState(
    val points: MutableList<Offset> = mutableListOf(),
    val path: Path = Path(),
    var prevPosition: Offset = Offset.Zero,
    var currentPosition: Offset = Offset.Zero,
    var lastDrawnIndex: Int = 0
) {
    fun addPoint(point: Offset): Boolean {
        if (points.isEmpty() || points.last().getDistance(point) > 5f) {
            points.add(point)
            return true
        }
        return false
    }

    fun reset() {
        points.clear()
        lastDrawnIndex = 0
    }

    fun pathReset() {
        path.reset()
        points.clear()
        lastDrawnIndex = 0
    }

    fun createIncrementalPath(callback: (Path) -> Unit = {}) {
        val lerpX = lerp(prevPosition.x, currentPosition.x, 0.5f)
        val lerpY = lerp(prevPosition.y, currentPosition.y, 0.5f)

        if (lastDrawnIndex == 0) {
            path.moveTo(prevPosition.x, prevPosition.y)
        }

        path.quadraticBezierTo(
            prevPosition.x,
            prevPosition.y,
            lerpX,
            lerpY
        )

        callback(path)

        path.reset()
        path.moveTo(lerpX, lerpY)

        prevPosition = currentPosition
        lastDrawnIndex = points.size - 1
    }
}

data class DrawingBitmapState(
    val bitmap: ImageBitmap,
    val canvas: Canvas,
    val currentPath: DrawingPath? = null,
    val drawingState: CanvasDrawingState = CanvasDrawingState(),
    val lastPoint: Offset? = null
) {
    fun updatePath(newPath: DrawingPath): DrawingBitmapState {
        return copy(currentPath = newPath)
    }

    fun addPoint(point: Offset): DrawingBitmapState {
        drawingState.addPoint(point)
        return copy(lastPoint = point)
    }

    fun reset() {
        drawingState.reset()
    }
}