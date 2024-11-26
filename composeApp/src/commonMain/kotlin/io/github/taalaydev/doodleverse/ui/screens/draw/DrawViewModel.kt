package io.github.taalaydev.doodleverse.ui.screens.draw

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.ImageFormat
import io.github.taalaydev.doodleverse.core.SelectionHitTestResult
import io.github.taalaydev.doodleverse.core.SelectionState
import io.github.taalaydev.doodleverse.core.SelectionTransform
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.copy
import io.github.taalaydev.doodleverse.core.hitTestSelectionHandles
import io.github.taalaydev.doodleverse.core.resize
import io.github.taalaydev.doodleverse.core.toIntOffset
import io.github.taalaydev.doodleverse.core.toIntSize
import io.github.taalaydev.doodleverse.core.withBackground
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.AnimationStateModel
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class DrawState(
    // val layers: List<LayerModel> = emptyList(),
    val dirtyLayers: List<Int> = emptyList(),
    val caches: Map<Long, ImageBitmap> = emptyMap(),
    val currentLayerIndex: Int = 0,
    val states: List<AnimationStateModel> = emptyList(),
    val currentStateIndex: Int = 0,
    val currentFrameIndex: Int = 0
)

val DrawState.frames: List<FrameModel> get() = states[currentStateIndex].frames
val DrawState.currentLayer: LayerModel get() = layers[currentLayerIndex]
fun DrawState.currentLayerImage(): ImageBitmap? = caches[currentLayer.id]
val DrawState.layers: List<LayerModel> get() = frames[currentFrameIndex].layers
val DrawState.currentFrame: FrameModel get() = frames[currentFrameIndex]

fun DrawState.copyFrames(
    frames: List<FrameModel> = this.frames,
    caches: Map<Long, ImageBitmap> = this.caches,
    currentLayerIndex: Int = this.currentLayerIndex,
): DrawState {
    return DrawState(
        states = states.mapIndexed { index, state ->
            if (index == currentStateIndex) {
                state.copy(frames = frames)
            } else {
                state
            }
        },
        dirtyLayers = dirtyLayers,
        caches = caches,
        currentLayerIndex = currentLayerIndex,
        currentStateIndex = currentStateIndex,
        currentFrameIndex = currentFrameIndex
    )
}

interface DrawProvider {
    val selectionState: SelectionState
    fun undo()
    fun redo()
    fun updateCurrentTool(tool: Tool)
    fun setColor(color: Color)
    fun startSelection(offset: Offset)
    fun updateSelection(offset: Offset)
    fun updateSelection(state: SelectionState)
    fun endSelection()
    fun applySelection()
    fun startTransform(transform: SelectionTransform, point: Offset)
    fun updateSelectionTransform(pan: Offset)
    fun updateSelectionTransform(centroid: Offset, pan: Offset, zoom: Float, rotation: Float)
    fun startMove(offset: Offset)
    fun updateMove(offset: Offset)
    fun endMove()

    suspend fun addLayer(layer: LayerModel): Long
    suspend fun deleteLayer(layer: LayerModel)
    suspend fun updateLayer(layer: LayerModel, cache: ImageBitmap?)
    suspend fun updateProject()
}

class DrawingController(
    // private val projectRepo: ProjectRepository,
    private val provider: DrawProvider,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    val currentPath = mutableStateOf<DrawingPath?>(null)

    private val _undoStack = mutableListOf<DrawState>()
    private val _redoStack = mutableListOf<DrawState>()

    var state = mutableStateOf(DrawState(
        states = listOf(
            AnimationStateModel(
                id = 0,
                name = "Animation 1",
                duration = 1000,
                projectId = 0,
                frames = listOf(
                    FrameModel(
                        id = 1L,
                        animationId = 0,
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
            )
        ),
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

    private var moveOffset = Offset.Zero
    private var tempMoveBitmap: ImageBitmap? = null

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
        val newState = state.value.copyFrames(
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
        val newState = state.value.copyFrames(
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
            val id = provider.addLayer(newLayer)

            scope.launch(Dispatchers.Main) {
                val newState = state.value.copyFrames(
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
        val newState = state.value.copyFrames(
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
            provider.deleteLayer(deletedLayer)
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
        val newState = state.value.copyFrames(
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
            provider.updateLayer(layer, cache)

            provider.updateProject()
        }
    }

    fun moveLayer(from: Int, to: Int) {
        val newLayers = state.value.layers.toMutableList()
        val layer = newLayers.removeAt(from)
        newLayers.add(to, layer)

        val newState = state.value.copyFrames(
            frames = state.value.frames.mapIndexed { index, frame ->
                if (index == state.value.currentFrameIndex) {
                    frame.copy(layers = newLayers)
                } else {
                    frame
                }
            },
            currentLayerIndex = to
        )
        state.value = newState
    }

    fun changeLayerOpacity(index: Int, opacity: Float) {
        val newLayers = state.value.layers.toMutableList()
        newLayers[index] = newLayers[index].copy(opacity = opacity.toDouble())
        val newState = state.value.copyFrames(
            frames = state.value.frames.mapIndexed { index, frame ->
                if (index == state.value.currentFrameIndex) {
                    frame.copy(layers = newLayers)
                } else {
                    frame
                }
            }
        )
        state.value = newState
        updateLayer(newLayers[index], caches[newLayers[index].id])
    }

    fun moveCurrentLayer(deltaX: Float, deltaY: Float) {
        moveOffset = Offset(deltaX, deltaY)

        // Create temporary bitmap for move preview if not exists
        if (tempMoveBitmap == null) {
            val currentCache = state.value.caches[currentLayer.id] ?: return
            tempMoveBitmap = currentCache.copy()
        }

        // Create new bitmap for moved content
        val movedBitmap = ImageBitmap(
            canvasSize.value.width.toInt(),
            canvasSize.value.height.toInt()
        )
        val canvas = Canvas(movedBitmap)

        // Draw moved content
        tempMoveBitmap?.let {
            canvas.drawImage(
                it,
                Offset(moveOffset.x, moveOffset.y),
                Paint()
            )
        }

        // Update display
        val newCaches = state.value.caches + (currentLayer.id to movedBitmap)
        state.value = state.value.copy(caches = newCaches)
        restoreImage.value = movedBitmap
        isDirty.value = true
    }

    fun commitLayerMove() {
        if (moveOffset != Offset.Zero && tempMoveBitmap != null) {
            // Create final moved bitmap
            val finalBitmap = ImageBitmap(
                canvasSize.value.width.toInt(),
                canvasSize.value.height.toInt()
            )
            val canvas = Canvas(finalBitmap)

            canvas.drawImage(
                tempMoveBitmap!!,
                moveOffset,
                Paint()
            )

            // Add to undo stack
            addState(
                DrawingPath(
                    brush = BrushData.solid,
                    color = Color.Black,
                    size = 1f,
                    path = Path(),
                    startPoint = Offset.Zero,
                    endPoint = moveOffset
                ),
                finalBitmap
            )
        }

        // Reset move state
        moveOffset = Offset.Zero
        tempMoveBitmap = null
    }

    fun captureSelection(bounds: Rect) {
        val layerBitmap = state.value.caches[currentLayer.id]?.copy() ?: return

        val selectionBitmap = ImageBitmap(bounds.width.toInt(), bounds.height.toInt())
        val canvas = Canvas(selectionBitmap)

        // Copy selected region
        canvas.drawImageRect(
            image = layerBitmap,
            srcOffset = bounds.topLeft.toIntOffset(),
            srcSize = bounds.size.toIntSize(),
            dstSize = bounds.size.toIntSize(),
            paint = Paint()
        )

        // Clear selected region from layer
        val layerCanvas = Canvas(layerBitmap)
        layerCanvas.drawImage(
            selectionBitmap,
            bounds.topLeft,
            Paint().apply {
                blendMode = BlendMode.Clear
            }
        )

        // Update layer with cleared content
        state.value = state.value.copy(
            caches = caches + (currentLayer.id to layerBitmap),
            dirtyLayers = state.value.dirtyLayers + currentLayer.id.toInt(),
        )
        selectLayer(state.value.currentLayerIndex)

        // Update state with the selection
        provider.updateSelection(SelectionState(
            bounds = bounds,
            originalBitmap = selectionBitmap,
            transformedBitmap = selectionBitmap,
            isActive = true
        ))
    }

    fun applySelectionTransform(selectionState: SelectionState) {
        val bounds = selectionState.bounds
        val bitmap = selectionState.transformedBitmap ?: return
        val layerBitmap = state.value.caches[currentLayer.id]?.copy() ?: ImageBitmap(
            canvasSize.value.width.toInt(),
            canvasSize.value.height.toInt()
        )

        val transformed = ImageBitmap(
            canvasSize.value.width.toInt(),
            canvasSize.value.height.toInt()
        )
        val canvas = Canvas(transformed)

        canvas.withSave {
            val offset = selectionState.offset
            canvas.translate(offset.x + bounds.center.x, offset.y +  bounds.center.y)
            canvas.rotate(selectionState.rotation)
            canvas.scale(selectionState.scale)
            canvas.translate(-bounds.center.x, -bounds.center.y)

            canvas.drawImage(
                bitmap,
                bounds.topLeft,
                Paint()
            )
        }

        val layerCanvas = Canvas(layerBitmap)
        layerCanvas.drawImage(
            transformed,
            Offset.Zero,
            Paint()
        )

        state.value = state.value.copy(
            caches = caches + (currentLayer.id to layerBitmap),
            dirtyLayers = state.value.dirtyLayers + currentLayer.id.toInt(),
        )
        selectLayer(state.value.currentLayerIndex)
        updateLayer(currentLayer, layerBitmap)
    }

    fun clearUndoRedoStack() {
        _undoStack.clear()
        _redoStack.clear()
        updateUndoRedo()
    }
}

// ViewModel for the drawing screen
class DrawViewModel(
    private val projectRepo: ProjectRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel(), DrawProvider {
    private val _project = MutableStateFlow<ProjectModel?>(null)
    val project: StateFlow<ProjectModel?> = _project.asStateFlow()

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

        viewModelScope.launch {
            projectRepo.updateProject(project.toEntity().copy(
                lastModified = Clock.System.now().toEpochMilliseconds(),
                thumb = imageBitmapByteArray(
                    drawingController.getCombinedImageBitmap() ?:
                    return@launch, ImageFormat.PNG
                ),
                animationStates = state.value.states.map {
                    it.toEntity().copy(
                        frames = state.value.frames.map { it.toEntity() }
                    )
                }
            ))
        }
    }

    override suspend fun updateProject() {
        val project = _project.value ?: return

        projectRepo.updateProject(
            projectRepo.getProjectById(project.id).copy(
                lastModified = Clock.System.now().toEpochMilliseconds(),
                thumb = imageBitmapByteArray(
                    drawingController.getCombinedImageBitmap() ?: return,
                    ImageFormat.PNG
                )
            )
        )
    }

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

    override suspend fun addLayer(layer: LayerModel): Long {
        return projectRepo.insertLayer(layer.toEntity())
    }

    fun deleteLayer(index: Int) {
        applySelection()
        drawingController.deleteLayer(index)
    }

    override suspend fun deleteLayer(layer: LayerModel) {
        projectRepo.deleteLayerById(layer.id)
    }

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

    override suspend fun updateLayer(layer: LayerModel, cache: ImageBitmap?) {
        projectRepo.updateLayer(layer.toEntity().copy(
            pixels = imageBitmapByteArray(cache ?: return, ImageFormat.PNG),
            width = cache.width,
            height = cache.height
        ))
    }

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
        println("Applying selection")
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

    // Image handling functions

    fun saveAsPng() {
        val image = drawingController.getCombinedImageBitmap() ?: return
        val bytes = imageBitmapByteArray(image.withBackground(Color.White), ImageFormat.PNG)

        viewModelScope.launch {
            FileKit.saveFile(bytes, "${project.value?.name}", "png")
        }
    }

    fun importImage(bytes: ByteArray, width: Int, height: Int) {
        var bitmap = imageBitmapFromByteArray(bytes, width, height)
        // Create a new layer for the imported image
        val layerName = "Image ${state.value.layers.size + 1}"

        viewModelScope.launch {
            drawingController.addLayer(layerName)

            // Need to wait for layer to be added before we can draw on it
            delay(100)

            _currentTool.value = Tool.Selection
            _selectionState = SelectionState(
                bounds = Rect(Offset.Zero, Size(width.toFloat(), height.toFloat())),
                originalBitmap = bitmap,
                transformedBitmap = bitmap,
                isActive = true
            )

            val state = drawingController.state.value
            val size = drawingController.canvasSize.value
            drawingController.state.value = state.copy(
                caches = state.caches + (state.currentLayer.id to ImageBitmap(size.width.toInt(), size.height.toInt())),
                dirtyLayers = state.dirtyLayers + state.currentLayer.id.toInt()
            )
            drawingController.isDirty.value = true
            drawingController.selectLayer(state.currentLayerIndex)
        }
    }
}