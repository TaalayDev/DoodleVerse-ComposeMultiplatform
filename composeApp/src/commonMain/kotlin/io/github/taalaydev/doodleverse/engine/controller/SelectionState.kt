package io.github.taalaydev.doodleverse.engine.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import io.github.taalaydev.doodleverse.core.rendering.FloodFillRenderer
import kotlin.math.PI
import kotlin.math.atan2

sealed class SelectionTransform {
    data object None : SelectionTransform()
    data object Move : SelectionTransform()
    data object Rotate : SelectionTransform()
    data object Resize : SelectionTransform()
}

sealed class SelectionHitTestResult {
    object Outside : SelectionHitTestResult()
    object Move : SelectionHitTestResult()
    object Rotate : SelectionHitTestResult()
    data object Resize : SelectionHitTestResult()
}

fun handleSelectionTransform(
    event: SelectionTransform,
    state: SelectionState,
    dragAmount: Offset,
    scaleFactor: Float = 1f,
    rotationDelta: Float = 0f
): SelectionState {
    val center = state.bounds.center

    return when (event) {
        SelectionTransform.Move -> {
            state.copy(offset = state.offset + dragAmount)
        }
        SelectionTransform.Rotate -> {
            // Calculate rotation around the center
            val angle = calculateRotationAngle(center, state.offset + dragAmount, rotationDelta)
            state.copy(rotation = state.rotation + angle)
        }
        SelectionTransform.Resize -> {
            // Calculate scale from center
            val newScale = (state.scale * scaleFactor).coerceIn(0.1f, 5f)
            val scaleDiff = newScale / state.scale

            // Adjust offset to maintain center position
            val newOffset = state.offset + (center - state.bounds.center) * (1 - scaleDiff)

            state.copy(
                scale = newScale,
                offset = newOffset
            )
        }
        SelectionTransform.None -> state
    }
}

fun calculateRotationAngle(center: Offset, currentPoint: Offset, baseDelta: Float): Float {
    // Calculate the angle between the previous point and the new point relative to center
    val previousAngle = atan2(currentPoint.y - center.y, currentPoint.x - center.x)
    val newAngle = atan2(
        (currentPoint.y + baseDelta) - center.y,
        (currentPoint.x + baseDelta) - center.x
    )

    // Return the difference in angles
    return (newAngle - previousAngle) * (180f / PI.toFloat())
}

fun SelectionState.getTransformedMatrix(fromCenter: Boolean = true): Matrix {
    val matrix = Matrix()
    val center = if (fromCenter) bounds.center else Offset.Zero

    matrix.translate(offset.x + center.x, offset.y + center.y)
    matrix.rotateZ(rotation)
    matrix.scale(scale, scale)
    matrix.translate(-center.x, -center.y)

    return matrix
}

fun SelectionState.getTransformedBounds(fromCenter: Boolean = true): Rect {
    val matrix = getTransformedMatrix(fromCenter)
    val path = Path().apply {
        addRect(bounds)
        transform(matrix)
    }
    return path.getBounds()
}

fun SelectionState.hitTest(position: Offset): SelectionHitTestResult {
    val handleSize = 24f
    val transformedBounds = getTransformedBounds()

    // Rotate handle (above the top center)
    val rotateHandlePosition = Offset(transformedBounds.center.x, transformedBounds.top - 40f)
    val rotateHandleRect = Rect(
        rotateHandlePosition - Offset(handleSize / 2, handleSize / 2),
        Size(handleSize, handleSize)
    )
    if (rotateHandleRect.contains(position)) {
        return SelectionHitTestResult.Rotate
    }

    // Move selection
    if (transformedBounds.contains(position)) {
        return SelectionHitTestResult.Move
    }

    // Outside selection
    return SelectionHitTestResult.Outside
}

data class TransformState(
    val initialOffset: Offset = Offset.Zero,
    val initialScale: Float = 1f,
    val initialRotation: Float = 0f,
    val initialCenter: Offset = Offset.Zero
)

class SelectionController(
    private val onStateChange: (SelectionState) -> Unit
) {
    var selectionState by mutableStateOf(SelectionState())
    private var initialTouchPoint: Offset? = null
    private var initialTransform: SelectionTransform = SelectionTransform.None

    fun startSelection(point: Offset) {
        initialTouchPoint = point
        selectionState = SelectionState(
            bounds = Rect(point, 0f),
            isActive = true,
            isTransforming = false
        )
        onStateChange(selectionState)
    }

    fun updateSelection(point: Offset) {
        if (initialTouchPoint == null) {
            initialTouchPoint = point
            selectionState = selectionState.copy(bounds = Rect(point, 0f))
        }

        initialTouchPoint?.let { start ->
            val minX = minOf(start.x, point.x)
            val minY = minOf(start.y, point.y)
            val maxX = maxOf(start.x, point.x)
            val maxY = maxOf(start.y, point.y)

            selectionState = selectionState.copy(
                bounds = Rect(
                    left = minX,
                    top = minY,
                    right = maxX,
                    bottom = maxY
                )
            )
            onStateChange(selectionState)
        }
    }

    fun endSelection() {
        initialTouchPoint = null
        selectionState = selectionState.copy(isTransforming = true)
        onStateChange(selectionState)
    }

    fun startTransform(transform: SelectionTransform, point: Offset) {
        initialTouchPoint = point
        initialTransform = transform
    }

    fun updateTransform(point: Offset) {
        initialTouchPoint?.let { start ->
            val delta = point - start
            val newState = when (selectionState.transform) {
                SelectionTransform.Move -> {
                    selectionState.copy(offset = selectionState.offset + delta)
                }
                SelectionTransform.Rotate -> {
                    selectionState.copy(rotation = selectionState.rotation + delta.x)
                }
                is SelectionTransform.Resize -> {
                    selectionState.copy(scale = (selectionState.scale * (1 + delta.y / 100)).coerceIn(0.1f, 5f))
                }
                SelectionTransform.None -> selectionState
            }
            selectionState = newState
            onStateChange(selectionState)
        }
    }

    fun endTransform() {
        initialTouchPoint = null
        initialTransform = SelectionTransform.None
    }
}

data class SelectionState(
    val bounds: Rect = Rect.Zero,
    val imageBounds: Rect = Rect.Zero,
    val originalBitmap: ImageBitmap? = null,
    val transformedBitmap: ImageBitmap? = null,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val transform: SelectionTransform = SelectionTransform.None,
    val isActive: Boolean = false,
    val isTransforming: Boolean = false
) {

    fun getTransformedBounds(): Rect {
        val matrix = getTransformedMatrix()
        val path = Path().apply {
            addRect(bounds)
            transform(matrix)
        }
        return path.getBounds()
    }
}

fun hitTestSelectionHandles(position: Offset, selectionState: SelectionState): SelectionHitTestResult {
    val handleSize = 24f
    val transformedBounds = selectionState.getTransformedBounds()


    // Rotate handle (above the top center)
    val rotateHandlePosition = Offset(transformedBounds.center.x, transformedBounds.top - 40f)
    val rotateHandleRect = Rect(
        rotateHandlePosition - Offset(handleSize / 2, handleSize / 2),
        Size(handleSize, handleSize)
    )
    if (rotateHandleRect.contains(position)) {
        return SelectionHitTestResult.Rotate
    }

    // Move selection
    if (transformedBounds.contains(position)) {
        return SelectionHitTestResult.Move
    }

    // Outside selection
    return SelectionHitTestResult.Outside
}
