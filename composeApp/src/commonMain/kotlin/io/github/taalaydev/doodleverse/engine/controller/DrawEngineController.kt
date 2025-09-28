package io.github.taalaydev.doodleverse.engine.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import io.github.taalaydev.doodleverse.core.rendering.DrawRenderer
import io.github.taalaydev.doodleverse.core.toIntOffset
import io.github.taalaydev.doodleverse.core.toIntSize
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.data.models.ToolsData
import io.github.taalaydev.doodleverse.engine.BitmapCache
import io.github.taalaydev.doodleverse.engine.BitmapCacheSnapshot
import io.github.taalaydev.doodleverse.engine.DrawTool
import io.github.taalaydev.doodleverse.engine.DrawingCanvasState
import io.github.taalaydev.doodleverse.engine.DrawingState
import io.github.taalaydev.doodleverse.engine.DrawingStateWithCache
import io.github.taalaydev.doodleverse.engine.Viewport
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.brush.PenBrush
import io.github.taalaydev.doodleverse.engine.tool.ShapeBrush
import io.github.taalaydev.doodleverse.engine.copy
import io.github.taalaydev.doodleverse.engine.viewToImage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DrawEngineController(
    val imageSize: IntSize = IntSize(512, 512),
    val initialBrushSize: Float = 10f,
    private val operations: DrawOperations,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _tools = MutableStateFlow<ToolsData?>(null)
    val tools: StateFlow<ToolsData?> = _tools.asStateFlow()

    private var _currentTool: MutableStateFlow<DrawTool> = MutableStateFlow(DrawTool.BrushTool(PenBrush()))
    val currentTool: StateFlow<DrawTool> = _currentTool.asStateFlow()

    val brushParams = MutableStateFlow(BrushParams(color = Color(0xFF333333), size = initialBrushSize))

    val currentBrush: Flow<Brush> = currentTool.map { tool ->
        when (tool) {
            is DrawTool.BrushTool -> tool.brush
            is DrawTool.Eraser -> tool.brush
            is DrawTool.Shape -> tool.brush
            is DrawTool.Curve -> tool.brush
            else -> PenBrush()
        }
    }

    private val bitmapCache = BitmapCache()
    private val undoRedoManager = UndoRedoManager<DrawingStateWithCache>()
    private val selectionManager = SelectionManager()

    private val layerManager = LayerManager(bitmapCache) { layer, bitmap ->
        scope.launch(dispatcher) {
            operations.updateLayer(layer, bitmap)
        }
    }

    private var _lastBrush: Brush = PenBrush()

    private val _state = MutableStateFlow(
        DrawingState(
            canvasSize = imageSize,
            currentFrame = FrameModel(
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
    val state: StateFlow<DrawingState> = _state.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var drawingCanvasState by mutableStateOf<DrawingCanvasState?>(null)

    val selectionState: SelectionState get() = selectionManager.selectionState

    fun setBrush() {
        applySelection()
        _currentTool.value = DrawTool.BrushTool(_lastBrush)
    }

    fun setBrush(brush: Brush) {
        applySelection()
        when {
            _currentTool.value.isCurve -> {
                _lastBrush = brush
                _currentTool.value = DrawTool.Curve(brush)
            }
            else -> {
                _lastBrush = brush
                _currentTool.value = DrawTool.BrushTool(brush)
            }
        }
    }

    fun setBrushParams(params: BrushParams) {
        brushParams.value = params
    }

    fun setTool(tool: DrawTool) {
        applySelection()
        _currentTool.value = tool
    }

    fun setColor(color: Color) {
        applySelection()
        brushParams.value = brushParams.value.copy(color = color)
    }

    fun setBrushSize(size: Float) {
        applySelection()
        brushParams.value = brushParams.value.copy(size = size)
    }

    fun addLayer(name: String) {
        scope.launch {
            val currentState = _state.value
            saveStateToHistory(currentState)

            val newLayer = LayerModel(
                id = 0,
                frameId = currentState.currentFrame.id,
                name = name,
                paths = emptyList()
            )

            val layerId = operations.addLayer(newLayer)

            val emptyBitmap = createEmptyBitmap()
            bitmapCache.put(layerId, emptyBitmap)

            val newFrame = currentState.currentFrame.copy(
                layers = currentState.currentFrame.layers + newLayer.copy(id = layerId)
            )

            _state.value = currentState.copy(
                currentFrame = newFrame,
                currentLayerIndex = newFrame.layers.size - 1,
                isModified = true
            )

            updateUndoRedoState()
        }
    }

    fun selectLayer(index: Int) {
        val currentState = _state.value
        if (index < 0 || index >= currentState.layers.size) return

        saveStateToHistory(currentState)
        _state.value = currentState.copy(currentLayerIndex = index)
        updateUndoRedoState()
    }

    fun deleteLayer(index: Int) {
        val currentState = _state.value
        if (currentState.layers.size <= 1) return

        saveStateToHistory(currentState)

        val layerToDelete = currentState.layers[index]
        val newFrame = layerManager.deleteLayer(currentState.currentFrame, index)
        val newLayerIndex = (index - 1).coerceAtLeast(0)

        _state.value = currentState.copy(
            currentFrame = newFrame,
            currentLayerIndex = newLayerIndex,
            isModified = true
        )

        scope.launch(dispatcher) {
            operations.deleteLayer(layerToDelete)
        }

        updateUndoRedoState()
    }

    fun updateLayerVisibility(index: Int, isVisible: Boolean) {
        val currentState = _state.value
        saveStateToHistory(currentState)

        val newFrame = layerManager.updateLayerVisibility(currentState.currentFrame, index, isVisible)
        _state.value = currentState.copy(currentFrame = newFrame, isModified = true)
        updateUndoRedoState()
    }

    fun changeLayerOpacity(index: Int, opacity: Float) {
        applySelection()
        updateLayerOpacity(index, opacity)
    }

    fun updateLayerOpacity(index: Int, opacity: Float) {
        val currentState = _state.value
        saveStateToHistory(currentState)

        val newFrame = layerManager.updateLayerOpacity(currentState.currentFrame, index, opacity)
        _state.value = currentState.copy(currentFrame = newFrame, isModified = true)
        updateUndoRedoState()
    }

    fun reorderLayers(fromIndex: Int, toIndex: Int) {
        val currentState = _state.value
        saveStateToHistory(currentState)

        val newFrame = layerManager.reorderLayers(currentState.currentFrame, fromIndex, toIndex)
        _state.value = currentState.copy(
            currentFrame = newFrame,
            currentLayerIndex = toIndex,
            isModified = true
        )
        updateUndoRedoState()
    }

    fun isLayerEmpty(layerId: Long): Boolean {
        val bitmap = bitmapCache.get(layerId) ?: return true
        return bitmap.width == 0 || bitmap.height == 0
    }

    fun undo() {
        val currentState = _state.value
        val currentSnapshot = createStateSnapshot(currentState)

        undoRedoManager.undo(currentSnapshot)?.let { previousSnapshot ->
            restoreFromSnapshot(previousSnapshot)
        }
        updateUndoRedoState()
    }

    fun redo() {
        val currentState = _state.value
        val currentSnapshot = createStateSnapshot(currentState)

        undoRedoManager.redo(currentSnapshot)?.let { nextSnapshot ->
            restoreFromSnapshot(nextSnapshot)
        }
        updateUndoRedoState()
    }

    fun clearUndoRedoStack() {
        undoRedoManager.clear()
        updateUndoRedoState()
    }

    fun startSelection(offset: Offset) {
        if (selectionState.isActive) {
            applySelection()
        }
        selectionManager.startSelection(offset)
    }

    fun updateSelection(offset: Offset) {
        selectionManager.updateSelection(offset)
    }

    fun updateSelectionState(state: SelectionState) {
        selectionManager.updateSelectionState(state)
    }

    fun endSelection(viewport: Viewport): Rect? {
        val bounds = selectionManager.endSelection()
        if (bounds != null && bounds.width > 1 && bounds.height > 1) {
            captureSelection(bounds, viewport)
        }
        return bounds
    }

    fun applySelection() {
        if (selectionState.isActive && selectionState.transformedBitmap != null) {
            saveStateToHistory(_state.value)
            applySelectionToLayer(selectionState)
            selectionManager.clearSelection()
            updateUndoRedoState()
        }
    }

    private fun captureSelection(bounds: Rect, viewport: Viewport) {
        val currentState = state.value
        val layerBitmap = getLayerBitmap(currentState.currentLayer.id) ?: return

        val imageTopLeft = viewToImage(bounds.topLeft, viewport, imageSize.width, imageSize.height)
        val imageBottomRight = viewToImage(bounds.bottomRight, viewport, imageSize.width, imageSize.height)
        val imageBounds = Rect(imageTopLeft, imageBottomRight)

        if (imageBounds.width <= 0 || imageBounds.height <= 0) {
            clearSelection()
            return
        }

        val selectionBitmap = ImageBitmap(imageBounds.width.toInt(), imageBounds.height.toInt())
        val canvas = Canvas(selectionBitmap)

        canvas.drawImageRect(
            image = layerBitmap,
            srcOffset = imageBounds.topLeft.toIntOffset(),
            srcSize = imageBounds.size.toIntSize(),
            dstOffset = IntOffset.Zero,
            dstSize = bounds.size.toIntSize(),
            paint = Paint()
        )

        val layerCanvas = Canvas(layerBitmap)
        layerCanvas.drawRect(
            imageBounds,
            Paint().apply { blendMode = BlendMode.Clear }
        )

        updateSelectionState(
            SelectionState(
                bounds = bounds,
                imageBounds = imageBounds,
                originalBitmap = selectionBitmap,
                transformedBitmap = selectionBitmap,
                isActive = true
            )
        )
    }

    fun updateSelectionTransform(pan: Offset) {
        updateTransform(pan)
    }

    fun clearSelection() {
        selectionManager.clearSelection()
    }

    fun loadFrame(frame: FrameModel, layerBitmaps: Map<Long, ImageBitmap>) {
        _state.value = DrawingState(
            currentFrame = frame,
            currentLayerIndex = (frame.layers.size - 1).coerceAtLeast(0),
            canvasSize = imageSize
        )

        layerBitmaps.forEach { (layerId, bitmap) ->
            bitmapCache.put(layerId, bitmap)
        }

        undoRedoManager.clear()
        updateUndoRedoState()
    }

    fun floodFill(x: Int, y: Int, color: Color) {
        val currentState = _state.value
        val existingBitmap = bitmapCache.get(currentState.currentLayer.id) ?: return

        saveStateToHistory(currentState)

        val modifiedBitmap = existingBitmap.copy()
        val canvas = Canvas(modifiedBitmap)

        DrawRenderer.floodFill(canvas, modifiedBitmap, x, y, color.toArgb())

        bitmapCache.put(currentState.currentLayer.id, modifiedBitmap)

        val newFrame = layerManager.updateLayerBitmap(
            currentState.currentFrame,
            currentState.currentLayerIndex,
            modifiedBitmap
        )

        _state.value = currentState.copy(
            currentFrame = newFrame,
            isModified = true
        )

        updateUndoRedoState()
    }

    fun startTransform(transform: SelectionTransform) {
        selectionManager.startTransform(transform)
    }

    fun updateTransform(pan: Offset) {
        selectionManager.updateTransform(pan)
    }

    fun cleanup() {
        bitmapCache.clear()
        undoRedoManager.clear()
        selectionManager.clearSelection()
        drawingCanvasState?.clear()
        drawingCanvasState = null
    }

    private fun saveStateToHistory(state: DrawingState) {
        val snapshot = createStateSnapshot(state)
        undoRedoManager.saveState(snapshot)
    }

    private fun createStateSnapshot(state: DrawingState): DrawingStateWithCache {
        return DrawingStateWithCache(
            drawingState = state,
            bitmapCache = BitmapCacheSnapshot.fromCache(bitmapCache)
        )
    }

    private fun restoreFromSnapshot(snapshot: DrawingStateWithCache) {
        _state.value = snapshot.drawingState
        snapshot.bitmapCache.restoreToCache(bitmapCache)
    }

    private fun updateUndoRedoState() {
        _canUndo.value = undoRedoManager.canUndo
        _canRedo.value = undoRedoManager.canRedo
    }

    private fun applySelectionToLayer(selectionState: SelectionState) {
        val currentState = _state.value
        val bounds = selectionState.imageBounds
        val bitmap = selectionState.transformedBitmap ?: return

        val layerBitmap = bitmapCache.get(currentState.currentLayer.id)?.copy()
            ?: createEmptyBitmap()

        val canvas = Canvas(layerBitmap)

        canvas.withSave {
            val offset = selectionState.offset
            canvas.translate(offset.x + bounds.center.x, offset.y + bounds.center.y)
            canvas.rotate(selectionState.rotation)
            canvas.scale(selectionState.scale)
            canvas.translate(-bounds.center.x, -bounds.center.y)

            canvas.drawImage(bitmap, bounds.topLeft, Paint())
        }

        bitmapCache.put(currentState.currentLayer.id, layerBitmap)

        val newFrame = layerManager.updateLayerBitmap(
            currentState.currentFrame,
            currentState.currentLayerIndex,
            layerBitmap
        )

        _state.value = currentState.copy(
            currentFrame = newFrame,
            isModified = true
        )
    }

    fun updateLayerBitmap(finalBitmap: ImageBitmap) {
        val currentState = _state.value
        saveStateToHistory(currentState)

        bitmapCache.put(currentState.currentLayer.id, finalBitmap)

        val newFrame = layerManager.updateLayerBitmap(
            currentState.currentFrame,
            currentState.currentLayerIndex,
            finalBitmap
        )

        _state.value = currentState.copy(
            currentFrame = newFrame,
            isModified = true
        )

        updateUndoRedoState()
    }

    fun getLayerBitmap(layerId: Long): ImageBitmap? = bitmapCache.get(layerId)

    /// Get layer bitmap or create an empty one if it doesn't exist
    fun getOrCreateLayerBitmap(layerId: Long): ImageBitmap {
        return bitmapCache.get(layerId) ?: createEmptyBitmap().also {
            bitmapCache.put(layerId, it)
        }
    }

    private fun createEmptyBitmap(): ImageBitmap {
        return ImageBitmap(imageSize.width, imageSize.height)
    }

    fun getCombinedBitmap(): ImageBitmap? {
        val currentState = _state.value
        return bitmapCache.getCombinedBitmap(currentState.layers, imageSize.toSize())
    }
}