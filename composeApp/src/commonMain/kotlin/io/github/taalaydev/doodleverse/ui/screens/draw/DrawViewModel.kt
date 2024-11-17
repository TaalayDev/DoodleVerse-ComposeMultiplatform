package io.github.taalaydev.doodleverse.ui.screens.draw

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.ImageFormat
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.copy
import io.github.taalaydev.doodleverse.core.withBackground
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.ToolsData
import io.github.taalaydev.doodleverse.data.models.toEntity
import io.github.taalaydev.doodleverse.data.models.toModel
import io.github.taalaydev.doodleverse.imageBitmapByteArray
import io.github.taalaydev.doodleverse.imageBitmapFromByteArray
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.vinceglb.filekit.core.FileKit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class DrawState(
    // val layers: List<LayerModel> = emptyList(),
    val dirtyLayers: List<Int> = emptyList(),
    val caches: Map<Long, ImageBitmap> = emptyMap(),
    val currentLayerIndex: Int = 0,
    val frames: List<FrameModel> = emptyList(),
    val currentFrameIndex: Int = 0
)

val DrawState.currentLayer: LayerModel get() = layers[currentLayerIndex]
fun DrawState.currentLayerImage(): ImageBitmap? = caches[currentLayer.id]
val DrawState.layers: List<LayerModel> get() = frames[currentFrameIndex].layers
val DrawState.currentFrame: FrameModel get() = frames[currentFrameIndex]

class DrawingController(
    private val projectRepo: ProjectRepository,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    val currentPath = mutableStateOf<DrawingPath?>(null)

    private val _undoStack = mutableListOf<DrawState>()
    private val _redoStack = mutableListOf<DrawState>()

    var state = mutableStateOf(DrawState(
        frames = listOf(
            FrameModel(
                id = 1L,
                projectId = 0,
                name = "Frame 1",
                layers = listOf(
                    LayerModel(
                        id = 1L,
                        frameId = 1L,
                        name = "Layer 1",
                        paths = emptyList()
                    )
                )
            )
        )
    ))
    private val layers: List<LayerModel> get() = state.value.layers
    private val currentLayer: LayerModel get() = layers[state.value.currentLayerIndex]
    private val caches: Map<Long, ImageBitmap> get() = state.value.caches
    private val frames: List<FrameModel> get() = state.value.frames
    private val currentFrame: FrameModel get() = frames[state.value.currentFrameIndex]

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    val bitmap = mutableStateOf<ImageBitmap?>(null)
    val imageCanvas = mutableStateOf<Canvas?>(null)
    val canvasSize = mutableStateOf(Size.Zero)
    val restoreImage = mutableStateOf<ImageBitmap?>(null)
    val isDirty = mutableStateOf(false)

    fun getCombinedImageBitmap(): ImageBitmap? {
        val canvasWidth = canvasSize.value.width.toInt()
        val canvasHeight = canvasSize.value.height.toInt()

        if (canvasWidth <= 0 || canvasHeight <= 0) return null

        val combinedBitmap = ImageBitmap(canvasWidth, canvasHeight)
        val canvas = Canvas(combinedBitmap)

        // Draw all visible layers onto the combined bitmap
        for (layer in state.value.layers) {
            if (!layer.isVisible || layer.opacity == 0.0) continue
            val image = state.value.caches[layer.id]
            if (image != null) {
                canvas.drawImageRect(image, paint = Paint())
            }
        }

        return combinedBitmap
    }

    fun addState(path: DrawingPath, image: ImageBitmap) {
        val newState = state.value.copy(
            frames = state.value.frames.mapIndexed { index, frame ->
                if (index == state.value.currentFrameIndex) {
                    frame.copy(
                        layers = layers.mapIndexed { index, layer ->
                            if (index == state.value.currentLayerIndex) {
                                layer.copy(
                                    paths = layer.paths + path
                                )
                            } else {
                                layer
                            }
                        }
                    )
                } else {
                    frame
                }
            },
            caches = caches + (currentLayer.id to image.copy())
        )
        _undoStack.add(state.value)
        _redoStack.clear()
        state.value = newState

        updateUndoRedo()

        updateLayer(currentLayer, image)
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

    fun loadFrames(frames: List<FrameModel>) {
        val newState = state.value.copy(
            frames = frames.map { frame ->
                frame.copy(
                    layers = if (frame.layers.isNotEmpty()) {
                        frame.layers
                    } else {
                        listOf(
                            LayerModel(
                                id = 1L,
                                frameId = 0,
                                name = "Layer 1",
                                paths = emptyList()
                            )
                        )
                    }
                )
            },
        )
        state.value = newState.copy(
            currentFrameIndex = 0,
            currentLayerIndex = newState.frames[0].layers.size - 1
        )
    }

    fun loadLayers(layers: Map<Long, ImageBitmap>) {
        val newState = state.value.copy(
            dirtyLayers = state.value.layers.map { it.id.toInt() },
            caches = layers
        )
        state.value = newState
        selectLayer(state.value.currentLayerIndex)
    }

    fun addLayer(name: String) {
        val newLayer = LayerModel(
            id = 0,
            frameId = currentFrame.id,
            name = name,
            paths = emptyList()
        )

        scope.launch {
            val id = projectRepo.insertLayer(newLayer.toEntity())

            scope.launch(Dispatchers.Main) {
                val newState = state.value.copy(
                    frames = state.value.frames.mapIndexed { index, frame ->
                        if (index == state.value.currentFrameIndex) {
                            frame.copy(layers = layers + newLayer.copy(id = id))
                        } else {
                            frame
                        }
                    },
                    currentLayerIndex = layers.size
                )
                _undoStack.add(state.value)
                _redoStack.clear()
                state.value = newState
                restoreImage.value = null
                isDirty.value = true

                updateUndoRedo()
            }
        }
    }

    fun deleteLayer(index: Int) {
        val newLayers = layers.toMutableList()
        val deletedLayer = newLayers.removeAt(index)
        val newState = state.value.copy(
            frames = state.value.frames.mapIndexed { index, frame ->
                if (index == state.value.currentFrameIndex) {
                    frame.copy(layers = newLayers)
                } else {
                    frame
                }
            },
            caches = caches.filterKeys { it != layers[index].id },
            currentLayerIndex = state.value.currentLayerIndex.coerceAtMost(newLayers.size - 1)
        )
        _undoStack.add(state.value)
        _redoStack.clear()
        state.value = newState

        updateUndoRedo()

        scope.launch {
            projectRepo.deleteLayerById(deletedLayer.id)
        }
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
            frames = state.value.frames.mapIndexed { index, frame ->
                if (index == state.value.currentFrameIndex) {
                    frame.copy(
                        layers = newLayers
                    )
                } else {
                    frame
                }
            }
        )
        _undoStack.add(state.value)
        _redoStack.clear()
        state.value = newState

        updateUndoRedo()

        updateLayer(newLayers[index], caches[newLayers[index].id])
    }

    private fun updateLayer(layer: LayerModel, cache: ImageBitmap?) {
        scope.launch(dispatcher) {
            projectRepo.updateLayer(layer.toEntity().copy(
                pixels = imageBitmapByteArray(cache ?: return@launch, ImageFormat.PNG),
                width = cache.width,
                height = cache.height
            ))

            projectRepo.updateProject(
                projectRepo.getProjectById(currentFrame.projectId).copy(
                    lastModified = Clock.System.now().toEpochMilliseconds(),
                    thumb = imageBitmapByteArray(
                        getCombinedImageBitmap() ?:
                        return@launch, ImageFormat.PNG
                    )
                )
            )
        }
    }
}

// ViewModel for the drawing screen
class DrawViewModel(
    private val projectRepo: ProjectRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _project = MutableStateFlow<ProjectModel?>(null)
    val project: StateFlow<ProjectModel?> = _project.asStateFlow()

    private val _tools = MutableStateFlow<ToolsData?>(null)
    val tools: StateFlow<ToolsData?> = _tools.asStateFlow()

    val drawingController = DrawingController(
        projectRepo,
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

    fun loadProject(id: Long) {
        viewModelScope.launch {
            val project = projectRepo.getProjectById(id)
            _project.value = project.toModel()

            val frames = projectRepo.getAllFrames(id)
            drawingController.loadFrames(frames.map { it.toModel() })

            val layers = mutableMapOf<Long, ImageBitmap>()
            for (frame in frames) {
                for (layer in frame.layers) {
                    if (layer.pixels.isEmpty()) continue
                    if (layer.width <= 0 || layer.height <= 0) continue
                    val image = imageBitmapFromByteArray(layer.pixels, layer.width, layer.height)
                    layers[layer.id] = image
                }
            }
            drawingController.loadLayers(layers)
        }
    }

    fun saveProject() {
        val project = _project.value ?: return
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

    fun saveAsPng() {
        val image = drawingController.getCombinedImageBitmap() ?: return
        val bytes = imageBitmapByteArray(image.withBackground(Color.White), ImageFormat.PNG)

        viewModelScope.launch {
            FileKit.saveFile(bytes, "${project.value?.name}", "png")
        }
    }
}