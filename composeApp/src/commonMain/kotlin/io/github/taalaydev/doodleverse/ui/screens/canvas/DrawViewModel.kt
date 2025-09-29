package io.github.taalaydev.doodleverse.ui.screens.canvas

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.taalaydev.doodleverse.ImageFormat
import io.github.taalaydev.doodleverse.core.toIntOffset
import io.github.taalaydev.doodleverse.core.toIntSize
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
import io.github.taalaydev.doodleverse.data.models.ToolsData
import io.github.taalaydev.doodleverse.data.models.toEntity
import io.github.taalaydev.doodleverse.data.models.toModel
import io.github.taalaydev.doodleverse.engine.DrawTool
import io.github.taalaydev.doodleverse.engine.DrawingState
import io.github.taalaydev.doodleverse.engine.Viewport
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.controller.DrawEngineController
import io.github.taalaydev.doodleverse.engine.controller.DrawOperations
import io.github.taalaydev.doodleverse.engine.controller.SelectionState
import io.github.taalaydev.doodleverse.engine.controller.SelectionTransform
import io.github.taalaydev.doodleverse.engine.copy
import io.github.taalaydev.doodleverse.engine.withBackground
import io.github.taalaydev.doodleverse.engine.resize
import io.github.taalaydev.doodleverse.imageBitmapByteArray
import io.github.taalaydev.doodleverse.imageBitmapFromByteArray
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class DrawViewModel(
    private val projectRepo: ProjectRepository,
    private val dispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _project = MutableStateFlow<ProjectModel?>(null)
    val project: StateFlow<ProjectModel?> = _project.asStateFlow()

    private val _tools = MutableStateFlow<ToolsData?>(null)
    val tools: StateFlow<ToolsData?> = _tools.asStateFlow()

    private var _projectSize = mutableStateOf(IntSize(1080, 1080))

    private val drawOperations = object : DrawOperations {
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

    val drawController: DrawEngineController by lazy {
        DrawEngineController(
            operations = drawOperations,
            imageSize = _projectSize.value,
            scope = viewModelScope,
            dispatcher = dispatcher
        )
    }

    val currentTool: StateFlow<DrawTool> get() = drawController.currentTool
    val currentBrush: Flow<Brush> get() = drawController.currentBrush

    val state: StateFlow<DrawingState> get() = drawController.state
    val canUndo: StateFlow<Boolean> get() = drawController.canUndo
    val canRedo: StateFlow<Boolean> get() = drawController.canRedo

    val selectionState: SelectionState get() = drawController.selectionState

    // Movement state
    private var moveStartPoint = Offset.Zero

    fun loadProject(id: Long) {
        viewModelScope.launch {
            try {
                val project = projectRepo.getProjectById(id)

                val frames = projectRepo.getAllFrames(id)
                val firstFrame = frames.firstOrNull()?.toModel() ?: return@launch

                _projectSize.value = getFixedSize(Size(project.width, project.height))

                // Load layer bitmaps
                val layerBitmaps = mutableMapOf<Long, ImageBitmap>()
                frames.forEach { frame ->
                    frame.layers.forEach { layer ->
                        if (layer.pixels.isNotEmpty() && layer.width > 0 && layer.height > 0) {
                            val bitmap = imageBitmapFromByteArray(layer.pixels, layer.width, layer.height)
                            layerBitmaps[layer.id] = bitmap.resize(_projectSize.value.width, _projectSize.value.height)
                        }
                    }
                }

                drawController.loadFrame(firstFrame, layerBitmaps)
                _project.value = project.toModel()
            } catch (e: Exception) {
                // Handle error
                println("Error loading project: ${e.message}")
            }
        }
    }

    private fun getFixedSize(size: Size): IntSize {
        val replaceSizes = mapOf(
            "1x1" to IntSize(1080, 1080),
            "16x9" to IntSize(1920, 1080),
            "4x3" to IntSize(1440, 1080)
        )

        return replaceSizes["${size.width.toInt()}x${size.height.toInt()}"]
            ?: IntSize(size.width.toInt(), size.height.toInt())
    }

    fun saveProject() {
        val project = _project.value ?: return
        val currentState = drawController.state.value

        viewModelScope.launch(dispatcher) {
            try {
                val combinedBitmap = drawController.getCombinedBitmap()
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

    suspend fun updateProject() {
        val project = _project.value ?: return

        return projectRepo.updateProject(
            projectRepo.getProjectById(project.id).copy(
                lastModified = Clock.System.now().toEpochMilliseconds(),
                thumb = imageBitmapByteArray(
                    drawController.getCombinedBitmap() ?: return,
                    ImageFormat.PNG
                )
            )
        )
    }

    fun setBrush() = drawController.setBrush()
    fun setBrush(brush: Brush) = drawController.setBrush(brush)
    fun setTool(tool: DrawTool) = drawController.setTool(tool)
    fun setColor(color: Color) = drawController.setColor(color)
    fun setBrushSize(size: Float) = drawController.setBrushSize(size)

    fun undo() {
        applySelection()
        drawController.undo()
    }

    fun redo() {
        applySelection()
        drawController.redo()
    }

    fun startSelection(offset: Offset) {
        if (selectionState.isActive) {
            applySelection()
        }
        drawController.startSelection(offset)
    }

    fun updateSelection(offset: Offset) {
        drawController.updateSelection(offset)
    }

    fun updateSelection(state: SelectionState) {
        drawController.updateSelectionState(state)
    }

    fun endSelection() {
        val bounds = drawController.endSelection()
        if (bounds != null && bounds.width > 1 && bounds.height > 1) {
            captureSelection(bounds)
        }
    }

    fun applySelection() {
        drawController.applySelection()
    }

    fun startTransform(transform: SelectionTransform, point: Offset) {
        drawController.startTransform(transform)
    }

    fun updateSelectionTransform(pan: Offset) {
        drawController.updateTransform(pan)
    }

    fun updateSelectionTransform(centroid: Offset, pan: Offset, zoom: Float, rotation: Float) {
        updateSelectionTransform(pan)
    }

    private fun captureSelection(bounds: Rect) {
        val currentState = drawController.state.value
        val layerBitmap = drawController.getLayerBitmap(currentState.currentLayer.id) ?: return

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

        drawController.updateSelectionState(
            SelectionState(
                bounds = bounds,
                originalBitmap = selectionBitmap,
                transformedBitmap = selectionBitmap,
                isActive = true
            )
        )
    }

    fun addLayer(name: String = "Layer ${state.value.layers.size + 1}") {
        applySelection()
        drawController.addLayer(name)
    }

    fun addLayer(layer: LayerModel) {
        applySelection()
        viewModelScope.launch {
            try {
                val layerId = drawOperations.addLayer(layer)
                val currentState = drawController.state.value

                val newFrame = currentState.currentFrame.copy(
                    layers = currentState.currentFrame.layers + layer.copy(id = layerId)
                )

                drawController.loadFrame(newFrame, emptyMap())

            } catch (e: Exception) {
                println("Error adding layer: ${e.message}")
            }
        }
    }

    fun deleteLayer(index: Int) {
        applySelection()
        val currentState = drawController.state.value
        if (currentState.layers.size <= 1) {
            println("Cannot delete the last layer")
            return
        }
        drawController.deleteLayer(index)
    }

    fun deleteLayer(layerId: Long) {
        applySelection()
        val currentState = drawController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            deleteLayer(layerIndex)
        }
    }

    fun updateLayer(index: Int, updater: (LayerModel) -> LayerModel) {
        applySelection()
        val currentState = drawController.state.value
        if (index < 0 || index >= currentState.layers.size) return

        viewModelScope.launch {
            try {
                val currentLayer = currentState.layers[index]
                val updatedLayer = updater(currentLayer)

                val bitmap = drawController.getLayerBitmap(updatedLayer.id)
                drawOperations.updateLayer(updatedLayer, bitmap)

                val newLayers = currentState.layers.toMutableList()
                newLayers[index] = updatedLayer

                val newFrame = currentState.currentFrame.copy(layers = newLayers)
                drawController.loadFrame(newFrame, buildMap {
                    currentState.layers.forEach { layer ->
                        drawController.getLayerBitmap(layer.id)?.let { bitmap ->
                            put(layer.id, bitmap)
                        }
                    }
                })

            } catch (e: Exception) {
                println("Error updating layer: ${e.message}")
            }
        }
    }

    fun updateLayer(layerId: Long, updater: (LayerModel) -> LayerModel) {
        val currentState = drawController.state.value
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
        drawController.selectLayer(index)
    }

    fun selectLayer(layerId: Long) {
        val currentState = drawController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            selectLayer(layerIndex)
        }
    }

    fun layerVisibilityChanged(index: Int, isVisible: Boolean) {
        applySelection()
        drawController.updateLayerVisibility(index, isVisible)
    }

    fun layerVisibilityChanged(layerId: Long, isVisible: Boolean) {
        val currentState = drawController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            layerVisibilityChanged(layerIndex, isVisible)
        }
    }

    fun reorderLayers(from: Int, to: Int) {
        applySelection()
        drawController.reorderLayers(from, to)
    }

    fun changeLayerOpacity(index: Int, opacity: Float) {
        applySelection()
        drawController.updateLayerOpacity(index, opacity)
    }

    fun changeLayerOpacity(layerId: Long, opacity: Float) {
        val currentState = drawController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            changeLayerOpacity(layerIndex, opacity)
        }
    }

    fun duplicateLayer(index: Int) {
        applySelection()
        val currentState = drawController.state.value
        if (index < 0 || index >= currentState.layers.size) return

        val layerToDuplicate = currentState.layers[index]
        val duplicatedLayer = layerToDuplicate.copy(
            id = 0,
            name = "${layerToDuplicate.name} Copy"
        )

        viewModelScope.launch {
            try {
                val originalBitmap = drawController.getLayerBitmap(layerToDuplicate.id)

                val layerId = drawOperations.addLayer(duplicatedLayer)
                val newLayer = duplicatedLayer.copy(id = layerId)

                val newFrame = currentState.currentFrame.copy(
                    layers = currentState.layers.toMutableList().apply {
                        add(index + 1, newLayer)
                    }
                )

                val layerBitmaps = buildMap {
                    currentState.layers.forEach { layer ->
                        drawController.getLayerBitmap(layer.id)?.let { bitmap ->
                            put(layer.id, bitmap)
                        }
                    }
                    originalBitmap?.let { put(layerId, it.copy()) }
                }

                drawController.loadFrame(newFrame, layerBitmaps)

                originalBitmap?.let { bitmap ->
                    drawOperations.updateLayer(newLayer, bitmap)
                }

            } catch (e: Exception) {
                println("Error duplicating layer: ${e.message}")
            }
        }
    }

    fun duplicateLayer(layerId: Long) {
        val currentState = drawController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            duplicateLayer(layerIndex)
        }
    }

    fun mergeLayerDown(index: Int) {
        applySelection()
        val currentState = drawController.state.value
        if (index <= 0 || index >= currentState.layers.size) return

        val upperLayer = currentState.layers[index]
        val lowerLayer = currentState.layers[index - 1]

        viewModelScope.launch {
            try {
                val upperBitmap = drawController.getLayerBitmap(upperLayer.id)
                val lowerBitmap = drawController.getLayerBitmap(lowerLayer.id)

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
                                drawController.getLayerBitmap(layer.id)?.let { bitmap ->
                                    put(layer.id, bitmap)
                                }
                            }
                        }
                    }

                    drawController.loadFrame(newFrame, layerBitmaps)

                    drawOperations.deleteLayer(upperLayer)
                    drawOperations.updateLayer(mergedLayer, mergedBitmap)
                }

            } catch (e: Exception) {
                println("Error merging layers: ${e.message}")
            }
        }
    }

    fun clearLayer(index: Int) {
        applySelection()
        val currentState = drawController.state.value
        if (index < 0 || index >= currentState.layers.size) return

        viewModelScope.launch {
            try {
                val currentLayer = currentState.layers[index]
                val clearedLayer = currentLayer.copy(paths = emptyList())

                val emptyBitmap = ImageBitmap(
                    drawController.imageSize.width,
                    drawController.imageSize.height
                )

                updateLayer(index) { clearedLayer }

                val layerBitmaps = buildMap {
                    currentState.layers.forEachIndexed { i, layer ->
                        if (i == index) {
                            put(layer.id, emptyBitmap)
                        } else {
                            drawController.getLayerBitmap(layer.id)?.let { bitmap ->
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

                drawController.loadFrame(newFrame, layerBitmaps)

            } catch (e: Exception) {
                println("Error clearing layer: ${e.message}")
            }
        }
    }

    fun clearLayer(layerId: Long) {
        val currentState = drawController.state.value
        val layerIndex = currentState.layers.indexOfFirst { it.id == layerId }
        if (layerIndex >= 0) {
            clearLayer(layerIndex)
        }
    }

    fun saveAsPng() {
        val image = drawController.getCombinedBitmap() ?: return
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
        val layerName = "Image ${drawController.state.value.layers.size + 1}"

        viewModelScope.launch {
            drawController.addLayer(layerName)

            delay(100)

            val bounds = Rect(Offset.Zero, Size(width.toFloat(), height.toFloat()))
            val imageBounds = drawController.getImageBoundsInView(bounds)
            drawController.updateSelectionState(
                SelectionState(
                    bounds = bounds,
                    imageBounds = bounds,
                    originalBitmap = bitmap,
                    isActive = true
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        drawController.cleanup()
    }

}