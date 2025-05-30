package io.github.taalaydev.doodleverse.ui.screens.lesson

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import io.github.taalaydev.doodleverse.core.DrawingOperations
import io.github.taalaydev.doodleverse.core.SelectionState
import io.github.taalaydev.doodleverse.core.SelectionTransform
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.data.models.AnimationStateModel
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.toEntity
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

    // Drawing operations implementation for lesson mode
    @OptIn(ExperimentalTime::class)
    private val drawingOperations = object : DrawingOperations {
        override suspend fun addLayer(layer: LayerModel): Long {
            // For lesson mode, we can create temporary IDs
            return kotlin.time.Clock.System.now().toEpochMilliseconds()
        }

        override suspend fun deleteLayer(layer: LayerModel) {
            // In lesson mode, layers are temporary - no persistence needed
        }

        override suspend fun updateLayer(layer: LayerModel, bitmap: ImageBitmap?) {
            // In lesson mode, layers are temporary - no persistence needed
        }

        override suspend fun saveProject() {
            // Handled by createProject function
        }
    }

    // Drawing controller with focused responsibilities
    val drawingController = DrawingController(
        operations = drawingOperations,
        scope = viewModelScope,
        dispatcher = dispatcher
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
            else -> BrushData.solid
        }
    }

    // Delegate properties to controller
    val state: StateFlow<io.github.taalaydev.doodleverse.core.DrawingState> = drawingController.state
    val canUndo: StateFlow<Boolean> = drawingController.canUndo
    val canRedo: StateFlow<Boolean> = drawingController.canRedo
    override val selectionState: SelectionState get() = drawingController.selectionState

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
                val combinedBitmap = drawingController.getCombinedBitmap()
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
        drawingController.undo()
    }

    override fun redo() {
        applySelection()
        drawingController.redo()
    }

    override fun addLayer(layer: LayerModel) {
        applySelection()
        viewModelScope.launch {
            try {
                val layerId = drawingOperations.addLayer(layer)
                val currentState = drawingController.state.value

                val newFrame = currentState.currentFrame.copy(
                    layers = currentState.currentFrame.layers + layer.copy(id = layerId)
                )

                // Update controller state directly
                drawingController.loadFrame(newFrame, emptyMap())

            } catch (e: Exception) {
                println("Error adding layer: ${e.message}")
            }
        }
    }

    // Layer operations - delegate to controller
    fun addLayer(name: String = "Layer ${state.value.layers.size + 1}") {
        applySelection()
        drawingController.addLayer(name)
    }

    fun deleteLayer(index: Int) {
        applySelection()
        val currentState = drawingController.state.value
        if (currentState.layers.size > 1) { // Keep at least one layer
            drawingController.deleteLayer(index)
        }
    }

    override fun deleteLayer(layerId: Long) {
        applySelection()
        val currentState = drawingController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            deleteLayer(layerIndex)
        }
    }

    override fun updateLayer(layerId: Long, updater: (LayerModel) -> LayerModel) {}

    fun selectLayer(index: Int) {
        applySelection()
        drawingController.selectLayer(index)
    }

    fun layerVisibilityChanged(index: Int, isVisible: Boolean) {
        applySelection()
        drawingController.updateLayerVisibility(index, isVisible)
    }

    fun reorderLayers(from: Int, to: Int) {
        applySelection()
        drawingController.reorderLayers(from, to)
    }

    fun changeLayerOpacity(index: Int, opacity: Float) {
        applySelection()
        drawingController.updateLayerOpacity(index, opacity)
    }

    // Additional layer utility functions for lessons
    fun getCurrentLayer(): LayerModel {
        return drawingController.state.value.currentLayer
    }

    fun getCurrentLayerIndex(): Int {
        return drawingController.state.value.currentLayerIndex
    }

    fun getTotalLayers(): Int {
        return drawingController.state.value.layers.size
    }

    fun getLayerBitmap(layerId: Long): ImageBitmap? {
        return drawingController.getLayerBitmap(layerId)
    }

    fun getCurrentLayerBitmap(): ImageBitmap? {
        val currentLayer = getCurrentLayer()
        return drawingController.getLayerBitmap(currentLayer.id)
    }

    fun getCombinedBitmap(): ImageBitmap? {
        return drawingController.getCombinedBitmap()
    }

    // Selection operations - delegate to controller
    override fun startSelection(offset: Offset) {
        if (selectionState.isActive) {
            applySelection()
        }
        drawingController.startSelection(offset)
    }

    override fun updateSelection(offset: Offset) {
        drawingController.updateSelection(offset)
    }

    override fun updateSelection(state: SelectionState) {
        drawingController.updateSelectionState(state)
    }

    override fun endSelection() {
        val bounds = drawingController.endSelection()
        if (bounds != null && bounds.width > 1 && bounds.height > 1) {
            // Capture selection area if valid
            captureSelection(bounds)
        }
    }

    override fun applySelection() {
        drawingController.applySelection()
    }

    override fun startTransform(transform: SelectionTransform, point: Offset) {
        // For lesson mode, we can use a simplified transform approach
        drawingController.startTransform(transform)
    }

    override fun updateSelectionTransform(pan: Offset) {
        drawingController.updateTransform(pan)
    }

    override fun updateSelectionTransform(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
        // Handle multi-touch transform if needed
        updateSelectionTransform(pan)
    }

    private fun captureSelection(bounds: androidx.compose.ui.geometry.Rect) {
        val currentState = drawingController.state.value
        val layerBitmap = drawingController.getLayerBitmap(currentState.currentLayer.id) ?: return

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
        drawingController.updateSelectionState(
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
            drawingController.clearSelection()
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
        drawingController.floodFill(x, y, _currentColor.value)
    }

    // Legacy interface methods - kept for compatibility
    override fun updateCurrentTool(tool: Tool) = setTool(tool)

    // Lesson-specific utility functions
    fun clearCanvas() {
        // Clear all layers
        val currentState = drawingController.state.value
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
            drawingController.loadFrame(emptyFrame, emptyMap())
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
        drawingController.cleanup()
    }

    fun isCanvasEmpty(): Boolean {
        val currentState = drawingController.state.value
        return currentState.layers.all { layer ->
            layer.paths.isEmpty()
        }
    }

    fun getCanvasPreview(): ImageBitmap? {
        return getCombinedBitmap()
    }

    override fun onCleared() {
        super.onCleared()
        drawingController.cleanup()
    }
}