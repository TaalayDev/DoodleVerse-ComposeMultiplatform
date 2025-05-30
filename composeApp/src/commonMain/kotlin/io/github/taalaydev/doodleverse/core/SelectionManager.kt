package io.github.taalaydev.doodleverse.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

class SelectionManager {
    private var _selectionState by mutableStateOf(SelectionState())
    val selectionState: SelectionState get() = _selectionState

    private var selectionStartPoint: Offset? = null

    fun startSelection(offset: Offset) {
        _selectionState = SelectionState(
            bounds = Rect(offset, 0f),
            isActive = true
        )
        selectionStartPoint = offset
    }

    fun updateSelection(offset: Offset) {
        val startPoint = selectionStartPoint ?: run {
            selectionStartPoint = offset
            _selectionState = _selectionState.copy(bounds = Rect(offset, 0f))
            return
        }

        val bounds = Rect(
            left = minOf(startPoint.x, offset.x),
            top = minOf(startPoint.y, offset.y),
            right = maxOf(startPoint.x, offset.x),
            bottom = maxOf(startPoint.y, offset.y)
        )

        _selectionState = _selectionState.copy(bounds = bounds)
    }

    fun updateSelectionState(state: SelectionState) {
        _selectionState = state
    }

    fun endSelection(): Rect? {
        val bounds = _selectionState.bounds
        selectionStartPoint = null

        return if (bounds.width > 1 && bounds.height > 1) bounds else null
    }

    fun clearSelection() {
        _selectionState = SelectionState()
        selectionStartPoint = null
    }

    fun startTransform(transform: SelectionTransform) {
        _selectionState = _selectionState.copy(transform = transform)
    }

    fun updateTransform(pan: Offset) {
        _selectionState = when (_selectionState.transform) {
            SelectionTransform.Move -> _selectionState.copy(
                offset = _selectionState.offset + pan
            )
            SelectionTransform.Rotate -> _selectionState.copy(
                rotation = _selectionState.rotation + pan.x
            )
            SelectionTransform.Resize -> _selectionState.copy(
                scale = (_selectionState.scale * (1 + pan.y / 100)).coerceIn(0.1f, 5f)
            )
            SelectionTransform.None -> _selectionState
        }
    }
}