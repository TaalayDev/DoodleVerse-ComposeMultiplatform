package io.github.taalaydev.doodleverse.ui.screens.lesson

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.core.SelectionHitTestResult
import io.github.taalaydev.doodleverse.core.SelectionState
import io.github.taalaydev.doodleverse.core.SelectionTransform
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.hitTestSelectionHandles
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ToolsData
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawProvider
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawState
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawingController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class LessonDrawViewModel(
    dispatcher: CoroutineDispatcher
) : ViewModel(), DrawProvider {
    private val _tools = MutableStateFlow<ToolsData?>(null)
    val tools: StateFlow<ToolsData?> = _tools.asStateFlow()

    private var _startPoint: MutableStateFlow<Offset> = MutableStateFlow(Offset.Zero)
    private var _endPoint: MutableStateFlow<Offset> = MutableStateFlow(Offset.Zero)

    private var _selectionState by mutableStateOf(SelectionState())
    override val selectionState: SelectionState get() = _selectionState

    val drawingController = DrawingController(
        provider = this,
        viewModelScope,
        dispatcher
    )

    private var _currentTool: MutableStateFlow<Tool> = MutableStateFlow(Tool.Brush(BrushData.solid))
    val currentTool: StateFlow<Tool> = _currentTool.asStateFlow()

    val currentBrush: Flow<BrushData>
        get() = currentTool.map {
            when (it) {
                is Tool.Brush -> it.brush
                is Tool.Eraser -> it.brush
                is Tool.Shape -> it.brush
                else -> BrushData.solid
            }
        }

    private var _currentColor = MutableStateFlow(Color(0xFF333333))
    val currentColor: StateFlow<Color> = _currentColor.asStateFlow()

    private var _brushSize = MutableStateFlow(10f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    val canUndo: StateFlow<Boolean> get() = drawingController.canUndo
    val canRedo: StateFlow<Boolean> get() = drawingController.canRedo

    val state: MutableState<DrawState> get() = drawingController.state

    fun setBrush(brush: BrushData) {
        applySelection()
        if (brush.isShape) {
            _currentTool.value = Tool.Shape(brush)
        } else if (brush.blendMode == BlendMode.Clear) {
            _currentTool.value = Tool.Eraser(brush)
        } else {
            _currentTool.value = Tool.Brush(brush)
        }
    }

    fun setTool(tool: Tool) {
        applySelection()
        _currentTool.value = tool
    }

    override fun setColor(color: Color) {
        applySelection()
        _currentColor.value = color
    }

    fun setBrushSize(size: Float) {
        endSelection()
        _brushSize.value = size
    }

    override fun undo() {
        endSelection()
        drawingController.undo()
    }

    override fun redo() {
        applySelection()
        drawingController.redo()
    }

    fun addLayer(name: String) {
        applySelection()
        drawingController.addLayer(name)
    }

    override suspend fun addLayer(layer: LayerModel): Long = Random.nextLong()

    fun deleteLayer(index: Int) {
        applySelection()
        drawingController.deleteLayer(index)
    }

    override suspend fun deleteLayer(layer: LayerModel) {}

    fun selectLayer(index: Int) {
        applySelection()
        drawingController.selectLayer(index)
    }

    fun layerVisibilityChanged(index: Int, isVisible: Boolean) {
        applySelection()
        drawingController.layerVisibilityChanged(index, isVisible)
    }

    fun reorderLayers(from: Int, to: Int) {
        applySelection()
        drawingController.moveLayer(from, to)
    }

    fun changeLayerOpacity(index: Int, opacity: Float) {
        applySelection()
        drawingController.changeLayerOpacity(index, opacity)
    }

    override suspend fun updateLayer(layer: LayerModel, cache: ImageBitmap?) {}
    override suspend fun updateProject() {}

    override fun updateCurrentTool(tool: Tool) {
        applySelection()
        _currentTool.value = tool
    }

    // Layer movement functions

    override fun startMove(offset: Offset) {
        val currentTool = _currentTool.value
        if (currentTool is Tool.Drag) {
            _startPoint.value = offset
        }
    }

    override fun updateMove(offset: Offset) {
        val currentTool = _currentTool.value
        if (currentTool is Tool.Drag && _startPoint.value != Offset.Zero) {
            _endPoint.value = offset

            // Calculate movement delta
            val deltaX = offset.x - (_startPoint.value.x)
            val deltaY = offset.y - (_startPoint.value.y)

            drawingController.moveCurrentLayer(deltaX, deltaY)
        }
    }

    override fun endMove() {
        val currentTool = _currentTool.value
        if (currentTool is Tool.Drag) {
            _startPoint.value = Offset.Zero
            _endPoint.value = Offset.Zero
            drawingController.commitLayerMove()
        }
    }

    // Selection functions

    private var selectionStartPoint: Offset? = null

    override fun startSelection(offset: Offset) {
        if (_selectionState.isActive) {
            applySelection()
        }

        _selectionState = SelectionState(
            bounds = Rect(offset, 0f),
            isActive = true
        )
        selectionStartPoint = offset
    }

    override fun updateSelection(offset: Offset) {
        if (selectionStartPoint == null) {
            selectionStartPoint = offset
            _selectionState = selectionState.copy(bounds = Rect(offset, 0f))
        }

        val startPoint = selectionStartPoint ?: return

        // Calculate selection rectangle bounds
        val minX = minOf(startPoint.x, offset.x)
        val minY = minOf(startPoint.y, offset.y)
        val maxX = maxOf(startPoint.x, offset.x)
        val maxY = maxOf(startPoint.y, offset.y)

        val newBounds = Rect(
            left = minX,
            top = minY,
            right = maxX,
            bottom = maxY
        )

        // Update selection state with new bounds
        _selectionState = _selectionState.copy(
            bounds = newBounds
        )
    }

    override fun endSelection() {
        val bounds = _selectionState.bounds

        // Only capture if we have a valid selection area
        if (bounds.width > 1 && bounds.height > 1) {
            // Capture the selection area into a bitmap
            drawingController.captureSelection(bounds)
        } else {
            // Reset selection if area is too small
            _selectionState = SelectionState()
        }

        selectionStartPoint = null
    }

    override fun updateSelection(state: SelectionState) {
        _selectionState = state
    }

    override fun startTransform(transform: SelectionTransform, point: Offset) {
        _selectionState = _selectionState.copy(
            transform = transform,
        )
    }

    override fun updateSelectionTransform(pan: Offset) {
        val state = _selectionState

        // Apply transformations based on the current transform type
        _selectionState = when (state.transform) {
            SelectionTransform.Move -> {
                state.copy(offset = state.offset + pan)
            }
            SelectionTransform.Rotate -> {
                state.copy(rotation = state.rotation + pan.x)
            }
            is SelectionTransform.Resize -> {
                state.copy(scale = (state.scale * (1 + pan.y / 100)).coerceIn(0.1f, 5f))
            }
            SelectionTransform.None -> state
        }
    }

    override fun updateSelectionTransform(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
        val state = _selectionState

        // Apply transformations based on the current transform type
        _selectionState = state.copy(
            offset = state.offset + pan,
            scale = (state.scale * zoom).coerceIn(0.1f, 5f),
            rotation = state.rotation + rotation
        )
    }

    override fun applySelection() {
        if (_selectionState.isActive && _selectionState.transformedBitmap != null) {
            // Apply the transformed selection back to the layer
            drawingController.applySelectionTransform(_selectionState)

            // Reset selection state
            _selectionState = SelectionState()
        }
    }

    fun cancelSelection() {
        if (_selectionState.isActive) {
            // Reset selection state without applying changes
            _selectionState = SelectionState()
            selectionStartPoint = null
        }
    }

    fun hitTestSelection(point: Offset): SelectionHitTestResult {
        return if (_selectionState.isActive) {
            hitTestSelectionHandles(point, _selectionState)
        } else {
            SelectionHitTestResult.Outside
        }
    }
}