package io.github.taalaydev.doodleverse.ui.screens.lesson

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.ImageFormat
import io.github.taalaydev.doodleverse.core.DrawProvider
import io.github.taalaydev.doodleverse.core.DrawingController
import io.github.taalaydev.doodleverse.engine.controller.DrawOperations
import io.github.taalaydev.doodleverse.engine.controller.SelectionState
import io.github.taalaydev.doodleverse.engine.controller.SelectionTransform
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.data.models.AnimationStateModel
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.toEntity
import io.github.taalaydev.doodleverse.engine.DrawingState
import io.github.taalaydev.doodleverse.engine.controller.DrawEngineController
import io.github.taalaydev.doodleverse.imageBitmapByteArray
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

class LessonDrawViewModel(
    private val projectRepo: ProjectRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel(), DrawProvider {

    @OptIn(ExperimentalTime::class)
    private val drawingOperations = object : DrawOperations {
        override suspend fun addLayer(layer: LayerModel): Long {
            return kotlin.time.Clock.System.now().toEpochMilliseconds()
        }
        override suspend fun deleteLayer(layer: LayerModel) {}
        override suspend fun updateLayer(layer: LayerModel, bitmap: ImageBitmap?) {}
        override suspend fun saveProject() {}
    }

    // Drawing controller with focused responsibilities
    val drawController = DrawEngineController(
        operations = drawingOperations,
        scope = viewModelScope,
        dispatcher = dispatcher,
        initialBrushSize = 5f
    )

    // Tool and drawing state
    private val _currentTool = MutableStateFlow<Tool>(Tool.Brush(BrushData.solid))
    val currentTool: StateFlow<Tool> = _currentTool.asStateFlow()

    private val _currentColor = MutableStateFlow(Color(0xFF333333))
    val currentColor: StateFlow<Color> = _currentColor.asStateFlow()

    private val _brushSize = MutableStateFlow(10f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    // Derived states
    val currentBrush: Flow<BrushData> = currentTool.map { tool ->
        when (tool) {
            is Tool.Brush -> tool.brush
            is Tool.Eraser -> tool.brush
            is Tool.Shape -> tool.brush
            is Tool.Curve -> tool.brush
            else -> BrushData.solid
        }
    }

    // Delegate properties to controller
    val state: StateFlow<DrawingState> = drawController.state
    val canUndo: StateFlow<Boolean> = drawController.canUndo
    val canRedo: StateFlow<Boolean> = drawController.canRedo
    override val selectionState: SelectionState get() = drawController.selectionState

    private var _lastBrush: BrushData = BrushData.solid

    // Movement state
    private var moveStartPoint = Offset.Zero

    // Selection state
    private var selectionStartPoint: Offset? = null

    /**
     * Creates a project from the current lesson drawing
     */
    fun createProject(
        name: String,
        width: Int,
        height: Int,
        onProjectCreated: (ProjectModel) -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            try {
                // Get the current drawing bitmap
                val combinedBitmap = drawController.getCombinedBitmap()
                val thumbnailBytes = combinedBitmap?.let {
                    imageBitmapByteArray(it, ImageFormat.PNG)
                }

                // Create project structure
                val project = ProjectModel(
                    id = 0,
                    name = name,
                    animationStates = emptyList(),
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                    lastModified = Clock.System.now().toEpochMilliseconds(),
                    aspectRatio = Size(width.toFloat(), height.toFloat()),
                )

                val animationState = AnimationStateModel(
                    id = 0,
                    name = "Animation 1",
                    duration = 1000,
                    frames = emptyList(),
                    projectId = 0,
                )

                val frame = FrameModel(
                    id = 0,
                    animationId = 0,
                    name = "Frame 1",
                    layers = emptyList(),
                )

                val layer = LayerModel(
                    id = 0,
                    frameId = 0,
                    name = "Layer 1",
                    isVisible = true,
                    isLocked = false,
                    isBackground = false,
                    opacity = 1.0,
                    paths = emptyList(),
                )

                // Save to database
                val projectId = projectRepo.insertProject(
                    project.toEntity().copy(thumb = thumbnailBytes)
                )

                val animationStateId = projectRepo.insertAnimationState(
                    animationState.copy(projectId = projectId).toEntity()
                )

                val frameId = projectRepo.insertFrame(
                    frame.copy(animationId = animationStateId).toEntity()
                )

                val layerId = projectRepo.insertLayer(
                    layer.copy(frameId = frameId).toEntity().copy(
                        pixels = thumbnailBytes ?: byteArrayOf(),
                        width = width,
                        height = height
                    )
                )

                // Return the complete project
                viewModelScope.launch(Dispatchers.Main) {
                    onProjectCreated(
                        project.copy(
                            id = projectId,
                            animationStates = listOf(
                                animationState.copy(
                                    id = animationStateId,
                                    frames = listOf(
                                        frame.copy(
                                            id = frameId,
                                            layers = listOf(
                                                layer.copy(id = layerId)
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                }

            } catch (e: Exception) {
                println("Error creating project from lesson: ${e.message}")
            }
        }
    }

    override suspend fun updateProject() {}

    // Tool management
    fun setBrush() {
        applySelection()
        _currentTool.value = Tool.Brush(_lastBrush)
    }

    fun setBrush(brush: BrushData) {
        applySelection()
        when {
            brush.isShape -> _currentTool.value = Tool.Shape(brush)
            brush.blendMode == BlendMode.Clear -> _currentTool.value = Tool.Eraser(brush)
            else -> {
                _lastBrush = brush
                _currentTool.value = Tool.Brush(brush)
            }
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

    // Drawing operations
    override fun undo() {
        endSelection()
        drawController.undo()
    }

    override fun redo() {
        applySelection()
        drawController.redo()
    }

    override fun addLayer(layer: LayerModel) {
        applySelection()
        viewModelScope.launch {
            try {
                val layerId = drawingOperations.addLayer(layer)
                val currentState = drawController.state.value

                val newFrame = currentState.currentFrame.copy(
                    layers = currentState.currentFrame.layers + layer.copy(id = layerId)
                )

                // Update controller state directly
                drawController.loadFrame(newFrame, emptyMap())

            } catch (e: Exception) {
                println("Error adding layer: ${e.message}")
            }
        }
    }

    // Layer operations - delegate to controller
    fun addLayer(name: String = "Layer ${state.value.layers.size + 1}") {
        applySelection()
        drawController.addLayer(name)
    }

    fun deleteLayer(index: Int) {
        applySelection()
        val currentState = drawController.state.value
        if (currentState.layers.size > 1) { // Keep at least one layer
            drawController.deleteLayer(index)
        }
    }

    override fun deleteLayer(layerId: Long) {
        applySelection()
        val currentState = drawController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            deleteLayer(layerIndex)
        }
    }

    override fun updateLayer(layerId: Long, updater: (LayerModel) -> LayerModel) {}

    fun selectLayer(index: Int) {
        applySelection()
        drawController.selectLayer(index)
    }

    fun layerVisibilityChanged(index: Int, isVisible: Boolean) {
        applySelection()
        drawController.updateLayerVisibility(index, isVisible)
    }

    fun reorderLayers(from: Int, to: Int) {
        applySelection()
        drawController.reorderLayers(from, to)
    }

    fun changeLayerOpacity(index: Int, opacity: Float) {
        applySelection()
        drawController.updateLayerOpacity(index, opacity)
    }

    // Additional layer utility functions for lessons
    fun getCurrentLayer(): LayerModel {
        return drawController.state.value.currentLayer
    }

    fun getCurrentLayerIndex(): Int {
        return drawController.state.value.currentLayerIndex
    }

    fun getTotalLayers(): Int {
        return drawController.state.value.layers.size
    }

    fun getLayerBitmap(layerId: Long): ImageBitmap? {
        return drawController.getLayerBitmap(layerId)
    }

    fun getCurrentLayerBitmap(): ImageBitmap? {
        val currentLayer = getCurrentLayer()
        return drawController.getLayerBitmap(currentLayer.id)
    }

    fun getCombinedBitmap(): ImageBitmap? {
        return drawController.getCombinedBitmap()
    }

    // Selection operations - delegate to controller
    override fun startSelection(offset: Offset) {
        if (selectionState.isActive) {
            applySelection()
        }
        drawController.startSelection(offset)
    }

    override fun updateSelection(offset: Offset) {
        drawController.updateSelection(offset)
    }

    override fun updateSelection(state: SelectionState) {
        drawController.updateSelectionState(state)
    }

    override fun endSelection() {}

    override fun applySelection() {
        drawController.applySelection()
    }

    override fun startTransform(transform: SelectionTransform, point: Offset) {
        drawController.startTransform(transform)
    }

    override fun updateSelectionTransform(pan: Offset) {
        drawController.updateTransform(pan)
    }

    override fun updateSelectionTransform(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
        updateSelectionTransform(pan)
    }

    private fun captureSelection(bounds: androidx.compose.ui.geometry.Rect) {
        val currentState = drawController.state.value
        val layerBitmap = drawController.getLayerBitmap(currentState.currentLayer.id) ?: return

        // Create selection bitmap
        val selectionBitmap = ImageBitmap(bounds.width.toInt(), bounds.height.toInt())
        val canvas = androidx.compose.ui.graphics.Canvas(selectionBitmap)

        canvas.drawImageRect(
            image = layerBitmap,
            srcOffset = androidx.compose.ui.unit.IntOffset(bounds.left.toInt(), bounds.top.toInt()),
            srcSize = androidx.compose.ui.unit.IntSize(bounds.width.toInt(), bounds.height.toInt()),
            dstSize = androidx.compose.ui.unit.IntSize(bounds.width.toInt(), bounds.height.toInt()),
            paint = androidx.compose.ui.graphics.Paint()
        )

        // Update selection state
        drawController.updateSelectionState(
            SelectionState(
                bounds = bounds,
                originalBitmap = selectionBitmap,
                transformedBitmap = selectionBitmap,
                isActive = true
            )
        )
    }

    fun cancelSelection() {
        if (selectionState.isActive) {
            drawController.clearSelection()
            selectionStartPoint = null
        }
    }

    // Movement operations
    override fun startMove(offset: Offset) {
        if (_currentTool.value is Tool.Drag) {
            moveStartPoint = offset
        }
    }

    override fun updateMove(offset: Offset) {
        if (_currentTool.value is Tool.Drag && moveStartPoint != Offset.Zero) {
            val deltaX = offset.x - moveStartPoint.x
            val deltaY = offset.y - moveStartPoint.y

            // Handle layer movement - simplified for lesson mode
            moveStartPoint = offset
        }
    }

    override fun endMove() {
        if (_currentTool.value is Tool.Drag) {
            moveStartPoint = Offset.Zero
        }
    }

    // Fill operation
    override fun floodFill(x: Int, y: Int) {
        drawController.floodFill(x, y, _currentColor.value)
    }

    // Legacy interface methods - kept for compatibility
    override fun updateCurrentTool(tool: Tool) = setTool(tool)

    // Lesson-specific utility functions
    fun clearCanvas() {
        // Clear all layers
        val currentState = drawController.state.value
        currentState.layers.indices.reversed().forEach { index ->
            if (index > 0) { // Keep the first layer
                deleteLayer(index)
            }
        }

        // Clear the remaining layer
        if (currentState.layers.isNotEmpty()) {
            val emptyFrame = currentState.currentFrame.copy(
                layers = listOf(
                    currentState.layers.first().copy(paths = emptyList())
                )
            )
            drawController.loadFrame(emptyFrame, emptyMap())
        }
    }

    fun resetToInitialState() {
        // Reset all state
        _currentTool.value = Tool.Brush(BrushData.solid)
        _currentColor.value = Color(0xFF333333)
        _brushSize.value = 10f

        // Clear canvas
        clearCanvas()

        // Clear undo/redo history
        drawController.cleanup()
    }

    fun isCanvasEmpty(): Boolean {
        val currentState = drawController.state.value
        return currentState.layers.all { layer ->
            layer.paths.isEmpty()
        }
    }

    fun getCanvasPreview(): ImageBitmap? {
        return getCombinedBitmap()
    }

    override fun onCleared() {
        super.onCleared()
        drawController.cleanup()
    }
}