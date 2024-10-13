package io.github.taalaydev.doodleverse.ui.screens.draw

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.copy
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.ToolsData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

data class DrawState(
    val layers: List<LayerModel> = emptyList(),
    val dirtyLayers: List<Int> = emptyList(),
    val caches: Map<Long, ImageBitmap> = emptyMap(),
    val currentLayerIndex: Int = 0,
)

val DrawState.currentLayer: LayerModel get() = layers[currentLayerIndex]
fun DrawState.currentLayerImage(): ImageBitmap? = caches[currentLayer.id]

class DrawingController {
    val currentPath = mutableStateOf<DrawingPath?>(null)

    private val _undoStack = mutableListOf<DrawState>()
    private val _redoStack = mutableListOf<DrawState>()

    var state = mutableStateOf(DrawState(
        layers = listOf(
            LayerModel(
                id = 1L,
                name = "Layer 1",
                paths = emptyList()
            )
        )
    ))
    private val layers: List<LayerModel> get() = state.value.layers
    private val currentLayer: LayerModel get() = layers[state.value.currentLayerIndex]
    private val caches: Map<Long, ImageBitmap> get() = state.value.caches

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    val bitmap = mutableStateOf<ImageBitmap?>(null)
    val imageCanvas = mutableStateOf<Canvas?>(null)
    val canvasSize = mutableStateOf(Size.Zero)
    val restoreImage = mutableStateOf<ImageBitmap?>(null)
    val isDirty = mutableStateOf(false)

    fun addState(path: DrawingPath, image: ImageBitmap) {
        val newState = state.value.copy(
            layers = layers.mapIndexed { index, layer ->
                if (index == state.value.currentLayerIndex) {
                    layer.copy(
                        paths = layer.paths + path
                    )
                } else {
                    layer
                }
            },
            caches = caches + (currentLayer.id to image.copy())
        )
        _undoStack.add(state.value)
        _redoStack.clear()
        state.value = newState

        updateUndoRedo()
    }

    fun undo() {
        if (!canUndo.value) return

        val lastState = _undoStack.removeLast()
        _redoStack.add(state.value)
        state.value = lastState
        restoreImage.value = state.value.caches[currentLayer.id]
        isDirty.value = true

        updateUndoRedo()
    }

    fun redo() {
        if (!canRedo.value) return

        val lastState = _redoStack.removeLast()
        _undoStack.add(state.value)
        state.value = lastState
        restoreImage.value = state.value.currentLayerImage()
        isDirty.value = true

        updateUndoRedo()
    }

    private fun updateUndoRedo() {
        _canUndo.value = _undoStack.isNotEmpty()
        _canRedo.value = _redoStack.isNotEmpty()
    }

    fun loadProject(project: ProjectModel) {

    }

    fun addLayer(name: String) {
        val newLayer = LayerModel(
            id = Clock.System.now().toEpochMilliseconds(),
            name = name,
            paths = emptyList()
        )
        val newState = state.value.copy(
            layers = layers + newLayer,
            currentLayerIndex = layers.size
        )
        _undoStack.add(state.value)
        _redoStack.clear()
        state.value = newState
        restoreImage.value = null
        isDirty.value = true

        updateUndoRedo()
    }

    fun deleteLayer(index: Int) {
        val newLayers = layers.toMutableList()
        newLayers.removeAt(index)
        val newState = state.value.copy(
            layers = newLayers,
            caches = caches.filterKeys { it != layers[index].id },
            currentLayerIndex = state.value.currentLayerIndex.coerceAtMost(newLayers.size - 1)
        )
        _undoStack.add(state.value)
        _redoStack.clear()
        state.value = newState

        updateUndoRedo()
    }

    fun selectLayer(index: Int) {
        val newState = state.value.copy(
            currentLayerIndex = index
        )
        _undoStack.add(state.value)
        _redoStack.clear()
        state.value = newState
        restoreImage.value = state.value.currentLayerImage()
        isDirty.value = true

        updateUndoRedo()
    }

    fun layerVisibilityChanged(index: Int, isVisible: Boolean) {
        val newLayers = layers.toMutableList()
        newLayers[index] = newLayers[index].copy(isVisible = isVisible)
        val newState = state.value.copy(
            layers = newLayers
        )
        _undoStack.add(state.value)
        _redoStack.clear()
        state.value = newState

        updateUndoRedo()
    }
}

// ViewModel for the drawing screen
class DrawViewModel : ViewModel() {
    private val _project = MutableStateFlow<ProjectModel?>(null)
    val project: StateFlow<ProjectModel?> = _project.asStateFlow()

    private val _tools = MutableStateFlow<ToolsData?>(null)
    val tools: StateFlow<ToolsData?> = _tools.asStateFlow()

    val drawingController = DrawingController()

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

    fun loadProject() {

    }

    fun saveProject() {

    }

    fun setBrush(brush: BrushData) {
        if (brush.isShape) {
            _currentTool.value = Tool.Shape(brush)
        } else if (brush.blendMode == BlendMode.Clear) {
            _currentTool.value = Tool.Eraser(brush)
        } else {
            _currentTool.value = Tool.Brush(brush)
        }
    }

    fun setTool(tool: Tool) {
        _currentTool.value = tool
    }

    fun setColor(color: Color) {
        _currentColor.value = color
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }

    fun undo() {
        drawingController.undo()
    }

    fun redo() {
        drawingController.redo()
    }

    fun addLayer(name: String) {
        drawingController.addLayer(name)
    }

    fun deleteLayer(index: Int) {
        drawingController.deleteLayer(index)
    }

    fun selectLayer(index: Int) {
        drawingController.selectLayer(index)
    }

    fun layerVisibilityChanged(index: Int, isVisible: Boolean) {
        drawingController.layerVisibilityChanged(index, isVisible)
    }
}