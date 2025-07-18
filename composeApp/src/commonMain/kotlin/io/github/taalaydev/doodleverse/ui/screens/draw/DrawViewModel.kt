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
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.ImageFormat
import io.github.taalaydev.doodleverse.core.DrawProvider
import io.github.taalaydev.doodleverse.core.DrawingController
import io.github.taalaydev.doodleverse.core.DrawRenderer
import io.github.taalaydev.doodleverse.core.DrawingOperations
import io.github.taalaydev.doodleverse.core.DrawingState
import io.github.taalaydev.doodleverse.core.SelectionHitTestResult
import io.github.taalaydev.doodleverse.core.SelectionState
import io.github.taalaydev.doodleverse.core.SelectionTransform
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.core.copy
import io.github.taalaydev.doodleverse.core.hitTestSelectionHandles
import io.github.taalaydev.doodleverse.core.toIntOffset
import io.github.taalaydev.doodleverse.core.toIntSize
import io.github.taalaydev.doodleverse.core.withBackground
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.ToolsData
import io.github.taalaydev.doodleverse.data.models.toEntity
import io.github.taalaydev.doodleverse.data.models.toModel
import io.github.taalaydev.doodleverse.imageBitmapByteArray
import io.github.taalaydev.doodleverse.imageBitmapFromByteArray
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class DrawViewModel(
    private val projectRepo: ProjectRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel(), DrawProvider {
    private val _project = MutableStateFlow<ProjectModel?>(null)
    val project: StateFlow<ProjectModel?> = _project.asStateFlow()

    private val _tools = MutableStateFlow<ToolsData?>(null)
    val tools: StateFlow<ToolsData?> = _tools.asStateFlow()

    private val drawingOperations = object : DrawingOperations {
        override suspend fun addLayer(layer: LayerModel): Long {
            return projectRepo.insertLayer(layer.toEntity())
        }

        override suspend fun deleteLayer(layer: LayerModel) {
            projectRepo.deleteLayerById(layer.id)
        }

        override suspend fun updateLayer(layer: LayerModel, bitmap: ImageBitmap?) {
            if (bitmap == null) return

            val layerEntity = layer.toEntity().copy(
                pixels = imageBitmapByteArray(bitmap, ImageFormat.PNG),
                width = bitmap.width,
                height = bitmap.height
            )
            projectRepo.updateLayer(layerEntity)
        }

        override suspend fun saveProject() {
            this@DrawViewModel.saveProject()
        }
    }

    val drawingController = DrawingController(
        operations = drawingOperations,
        viewModelScope,
        dispatcher
    )

    private var _currentTool: MutableStateFlow<Tool> = MutableStateFlow(Tool.Brush(BrushData.solid))
    val currentTool: StateFlow<Tool> = _currentTool.asStateFlow()

    private var _currentColor = MutableStateFlow(Color(0xFF333333))
    val currentColor: StateFlow<Color> = _currentColor.asStateFlow()

    private var _brushSize = MutableStateFlow(10f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    val currentBrush: Flow<BrushData> = currentTool.map { tool ->
        when (tool) {
            is Tool.Brush -> tool.brush
            is Tool.Eraser -> tool.brush
            is Tool.Shape -> tool.brush
            else -> BrushData.solid
        }
    }

    val state: StateFlow<DrawingState> = drawingController.state
    val canUndo: StateFlow<Boolean> = drawingController.canUndo
    val canRedo: StateFlow<Boolean> = drawingController.canRedo

    override val selectionState: SelectionState get() = drawingController.selectionState

    private var _lastBrush: BrushData = BrushData.solid

    // Movement state
    private var moveStartPoint = Offset.Zero

    fun loadProject(id: Long) {
        viewModelScope.launch {
            try {
                val project = projectRepo.getProjectById(id)
                _project.value = project.toModel()

                val frames = projectRepo.getAllFrames(id)
                val firstFrame = frames.firstOrNull()?.toModel() ?: return@launch

                // Load layer bitmaps
                val layerBitmaps = mutableMapOf<Long, ImageBitmap>()
                frames.forEach { frame ->
                    frame.layers.forEach { layer ->
                        if (layer.pixels.isNotEmpty() && layer.width > 0 && layer.height > 0) {
                            val bitmap = imageBitmapFromByteArray(layer.pixels, layer.width, layer.height)
                            layerBitmaps[layer.id] = bitmap
                        }
                    }
                }

                drawingController.loadFrame(firstFrame, layerBitmaps)

            } catch (e: Exception) {
                // Handle error
                println("Error loading project: ${e.message}")
            }
        }
    }

    fun saveProject() {
        val project = _project.value ?: return
        val currentState = drawingController.state.value

        viewModelScope.launch(dispatcher) {
            try {
                val combinedBitmap = drawingController.getCombinedBitmap()
                val thumbnail = combinedBitmap?.let {
                    imageBitmapByteArray(it, ImageFormat.PNG)
                }

                val updatedProject = project.toEntity().copy(
                    lastModified = Clock.System.now().toEpochMilliseconds(),
                    thumb = thumbnail
                )

                projectRepo.updateProject(updatedProject)

            } catch (e: Exception) {
                println("Error saving project: ${e.message}")
            }
        }
    }

    override suspend fun updateProject() {
        val project = _project.value ?: return

        return projectRepo.updateProject(
            projectRepo.getProjectById(project.id).copy(
                lastModified = Clock.System.now().toEpochMilliseconds(),
                thumb = imageBitmapByteArray(
                    drawingController.getCombinedBitmap() ?: return,
                    ImageFormat.PNG
                )
            )
        )
    }

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

    override fun undo() {
        endSelection()
        drawingController.undo()
    }

    override fun redo() {
        applySelection()
        drawingController.redo()
    }

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
            captureSelection(bounds)
        }
    }

    override fun applySelection() {
        drawingController.applySelection()
    }

    override fun startTransform(transform: SelectionTransform, point: Offset) {
        drawingController.startTransform(transform)
    }

    override fun updateSelectionTransform(pan: Offset) {
        drawingController.updateTransform(pan)
    }

    override fun updateSelectionTransform(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
        updateSelectionTransform(pan)
    }

    private fun captureSelection(bounds: Rect) {
        val currentState = drawingController.state.value
        val layerBitmap = drawingController.getLayerBitmap(currentState.currentLayer.id) ?: return

        val selectionBitmap = ImageBitmap(bounds.width.toInt(), bounds.height.toInt())
        val canvas = Canvas(selectionBitmap)

        canvas.drawImageRect(
            image = layerBitmap,
            srcOffset = bounds.topLeft.toIntOffset(),
            srcSize = bounds.size.toIntSize(),
            dstSize = bounds.size.toIntSize(),
            paint = Paint()
        )

        val layerCanvas = Canvas(layerBitmap)
        layerCanvas.drawRect(
            bounds,
            Paint().apply { blendMode = BlendMode.Clear }
        )

        drawingController.updateSelectionState(
            SelectionState(
                bounds = bounds,
                originalBitmap = selectionBitmap,
                transformedBitmap = selectionBitmap,
                isActive = true
            )
        )
    }

    override fun startMove(offset: Offset) {
        val currentTool = _currentTool.value
        if (currentTool is Tool.Drag) {
            moveStartPoint = offset
        }
    }

    override fun updateMove(offset: Offset) {
        val currentTool = _currentTool.value
        if (currentTool is Tool.Drag && moveStartPoint != Offset.Zero) {
            val deltaX = offset.x - moveStartPoint.x
            val deltaY = offset.y - moveStartPoint.y

            moveStartPoint = offset
        }
    }

    override fun endMove() {
        if (_currentTool.value is Tool.Drag) {
            moveStartPoint = Offset.Zero
        }
    }

    override fun floodFill(x: Int, y: Int) {
        drawingController.floodFill(x, y, _currentColor.value)
    }

    fun addLayer(name: String = "Layer ${state.value.layers.size + 1}") {
        applySelection()
        drawingController.addLayer(name)
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

                drawingController.loadFrame(newFrame, emptyMap())

            } catch (e: Exception) {
                println("Error adding layer: ${e.message}")
            }
        }
    }

    fun deleteLayer(index: Int) {
        applySelection()
        val currentState = drawingController.state.value
        if (currentState.layers.size <= 1) {
            println("Cannot delete the last layer")
            return
        }
        drawingController.deleteLayer(index)
    }

    override fun deleteLayer(layerId: Long) {
        applySelection()
        val currentState = drawingController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            deleteLayer(layerIndex)
        }
    }

    fun updateLayer(index: Int, updater: (LayerModel) -> LayerModel) {
        applySelection()
        val currentState = drawingController.state.value
        if (index < 0 || index >= currentState.layers.size) return

        viewModelScope.launch {
            try {
                val currentLayer = currentState.layers[index]
                val updatedLayer = updater(currentLayer)

                val bitmap = drawingController.getLayerBitmap(updatedLayer.id)
                drawingOperations.updateLayer(updatedLayer, bitmap)

                val newLayers = currentState.layers.toMutableList()
                newLayers[index] = updatedLayer

                val newFrame = currentState.currentFrame.copy(layers = newLayers)
                drawingController.loadFrame(newFrame, buildMap {
                    currentState.layers.forEach { layer ->
                        drawingController.getLayerBitmap(layer.id)?.let { bitmap ->
                            put(layer.id, bitmap)
                        }
                    }
                })

            } catch (e: Exception) {
                println("Error updating layer: ${e.message}")
            }
        }
    }

    override fun updateLayer(layerId: Long, updater: (LayerModel) -> LayerModel) {
        val currentState = drawingController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            updateLayer(layerIndex, updater)
        }
    }

    fun updateLayerName(index: Int, name: String) {
        updateLayer(index) { it.copy(name = name) }
    }

    fun updateLayerName(layerId: Long, name: String) {
        updateLayer(layerId) { it.copy(name = name) }
    }

    fun selectLayer(index: Int) {
        applySelection()
        drawingController.selectLayer(index)
    }

    fun selectLayer(layerId: Long) {
        val currentState = drawingController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            selectLayer(layerIndex)
        }
    }

    fun layerVisibilityChanged(index: Int, isVisible: Boolean) {
        applySelection()
        drawingController.updateLayerVisibility(index, isVisible)
    }

    fun layerVisibilityChanged(layerId: Long, isVisible: Boolean) {
        val currentState = drawingController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            layerVisibilityChanged(layerIndex, isVisible)
        }
    }

    fun reorderLayers(from: Int, to: Int) {
        applySelection()
        drawingController.reorderLayers(from, to)
    }

    fun changeLayerOpacity(index: Int, opacity: Float) {
        applySelection()
        drawingController.updateLayerOpacity(index, opacity)
    }

    fun changeLayerOpacity(layerId: Long, opacity: Float) {
        val currentState = drawingController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            changeLayerOpacity(layerIndex, opacity)
        }
    }

    fun duplicateLayer(index: Int) {
        applySelection()
        val currentState = drawingController.state.value
        if (index < 0 || index >= currentState.layers.size) return

        val layerToDuplicate = currentState.layers[index]
        val duplicatedLayer = layerToDuplicate.copy(
            id = 0,
            name = "${layerToDuplicate.name} Copy"
        )

        viewModelScope.launch {
            try {
                val originalBitmap = drawingController.getLayerBitmap(layerToDuplicate.id)

                val layerId = drawingOperations.addLayer(duplicatedLayer)
                val newLayer = duplicatedLayer.copy(id = layerId)

                val newFrame = currentState.currentFrame.copy(
                    layers = currentState.layers.toMutableList().apply {
                        add(index + 1, newLayer)
                    }
                )

                val layerBitmaps = buildMap {
                    currentState.layers.forEach { layer ->
                        drawingController.getLayerBitmap(layer.id)?.let { bitmap ->
                            put(layer.id, bitmap)
                        }
                    }
                    originalBitmap?.let { put(layerId, it.copy()) }
                }

                drawingController.loadFrame(newFrame, layerBitmaps)

                originalBitmap?.let { bitmap ->
                    drawingOperations.updateLayer(newLayer, bitmap)
                }

            } catch (e: Exception) {
                println("Error duplicating layer: ${e.message}")
            }
        }
    }

    fun duplicateLayer(layerId: Long) {
        val currentState = drawingController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            duplicateLayer(layerIndex)
        }
    }

    fun mergeLayerDown(index: Int) {
        applySelection()
        val currentState = drawingController.state.value
        if (index <= 0 || index >= currentState.layers.size) return

        val upperLayer = currentState.layers[index]
        val lowerLayer = currentState.layers[index - 1]

        viewModelScope.launch {
            try {
                val upperBitmap = drawingController.getLayerBitmap(upperLayer.id)
                val lowerBitmap = drawingController.getLayerBitmap(lowerLayer.id)

                if (upperBitmap != null && lowerBitmap != null) {
                    val mergedBitmap = lowerBitmap.copy()
                    val canvas = Canvas(mergedBitmap)
                    canvas.drawImage(
                        upperBitmap,
                        Offset.Zero,
                        Paint().apply { alpha = upperLayer.opacity.toFloat() }
                    )

                    val mergedLayer = lowerLayer.copy(
                        name = "${lowerLayer.name} + ${upperLayer.name}",
                        opacity = 1.0
                    )

                    val newLayers = currentState.layers.toMutableList()
                    newLayers.removeAt(index)
                    newLayers[index - 1] = mergedLayer

                    val newFrame = currentState.currentFrame.copy(layers = newLayers)

                    val layerBitmaps = buildMap {
                        newLayers.forEach { layer ->
                            if (layer.id == mergedLayer.id) {
                                put(layer.id, mergedBitmap)
                            } else {
                                drawingController.getLayerBitmap(layer.id)?.let { bitmap ->
                                    put(layer.id, bitmap)
                                }
                            }
                        }
                    }

                    drawingController.loadFrame(newFrame, layerBitmaps)

                    drawingOperations.deleteLayer(upperLayer)
                    drawingOperations.updateLayer(mergedLayer, mergedBitmap)
                }

            } catch (e: Exception) {
                println("Error merging layers: ${e.message}")
            }
        }
    }

    fun clearLayer(index: Int) {
        applySelection()
        val currentState = drawingController.state.value
        if (index < 0 || index >= currentState.layers.size) return

        viewModelScope.launch {
            try {
                val currentLayer = currentState.layers[index]
                val clearedLayer = currentLayer.copy(paths = emptyList())

                val emptyBitmap = ImageBitmap(
                    drawingController.canvasSize.width.toInt(),
                    drawingController.canvasSize.height.toInt()
                )

                updateLayer(index) { clearedLayer }

                val layerBitmaps = buildMap {
                    currentState.layers.forEachIndexed { i, layer ->
                        if (i == index) {
                            put(layer.id, emptyBitmap)
                        } else {
                            drawingController.getLayerBitmap(layer.id)?.let { bitmap ->
                                put(layer.id, bitmap)
                            }
                        }
                    }
                }

                val newFrame = currentState.currentFrame.copy(
                    layers = currentState.layers.toMutableList().apply {
                        set(index, clearedLayer)
                    }
                )

                drawingController.loadFrame(newFrame, layerBitmaps)

            } catch (e: Exception) {
                println("Error clearing layer: ${e.message}")
            }
        }
    }

    fun clearLayer(layerId: Long) {
        val currentState = drawingController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            clearLayer(layerIndex)
        }
    }

    fun saveAsPng() {
        val image = drawingController.getCombinedBitmap() ?: return
        val bytes = imageBitmapByteArray(image.withBackground(Color.White), ImageFormat.PNG)

        viewModelScope.launch {
            try {
                FileKit.saveFile(bytes, "${project.value?.name}", "png")
            } catch (e: Exception) {
                println("Error saving image: ${e.message}")
            }
        }
    }

    fun importImage() {
        viewModelScope.launch {
            try {
                val result = FileKit.pickFile(PickerType.Image, mode = PickerMode.Single)
                if (result != null) {
                    val bytes = result.readBytes()
                    val bitmap = imageBitmapFromByteArray(bytes, 0, 0)
                    importImage(bytes, bitmap.width, bitmap.height)
                }
            } catch (e: Exception) {
                println("Error importing image: ${e.message}")
            }
        }
    }

    fun importImage(bytes: ByteArray, width: Int, height: Int) {
        val bitmap = imageBitmapFromByteArray(bytes, width, height)
        val layerName = "Image ${drawingController.state.value.layers.size + 1}"

        viewModelScope.launch {
            drawingController.addLayer(layerName)

            delay(100)

            drawingController.updateSelectionState(
                SelectionState(
                    bounds = Rect(Offset.Zero, Size(width.toFloat(), height.toFloat())),
                    originalBitmap = bitmap,
                    transformedBitmap = bitmap,
                    isActive = true
                )
            )
        }
    }

    override fun updateCurrentTool(tool: Tool) = setTool(tool)

    override fun onCleared() {
        super.onCleared()
        drawingController.cleanup()
    }

}