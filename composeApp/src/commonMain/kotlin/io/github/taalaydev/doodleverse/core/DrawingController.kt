package io.github.taalaydev.doodleverse.core

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.withSave
import io.github.taalaydev.doodleverse.core.rendering.DrawRenderer
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DrawingState(
    val currentFrame: FrameModel,
    val currentLayerIndex: Int = 0,
    val canvasSize: Size = Size.Zero,
    val isModified: Boolean = false
) {
    val currentLayer: LayerModel
        get() = currentFrame.layers.getOrNull(currentLayerIndex) ?: currentFrame.layers.first()

    val layers: List<LayerModel>
        get() = currentFrame.layers
}

data class BitmapCacheSnapshot(
    val bitmaps: Map<Long, ImageBitmap>
) {
    companion object {
        fun fromCache(cache: BitmapCache): BitmapCacheSnapshot {
            return BitmapCacheSnapshot(cache.getAllBitmaps())
        }
    }

    fun restoreToCache(cache: BitmapCache) {
        cache.clear()
        bitmaps.forEach { (layerId, bitmap) ->
            cache.put(layerId, bitmap.copy())
        }
    }
}

data class DrawingStateWithCache(
    val drawingState: DrawingState,
    val bitmapCache: BitmapCacheSnapshot
)

data class DrawingCanvasState(
    val bitmap: ImageBitmap,
    val canvas: Canvas,
    val isActive: Boolean = false
) {
    fun clear() {
        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = bitmap.width.toFloat(),
            bottom = bitmap.height.toFloat(),
            paint = Paint().apply {
                color = Color.Transparent
                blendMode = BlendMode.Clear
            }
        )
    }
}

class DrawingController(
    private val operations: DrawingOperations,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        private const val MAX_UNDO_REDO = 20
    }

    private val bitmapCache = BitmapCache()
    // FIXED: Use enhanced undo/redo manager that includes bitmap state
    private val undoRedoManager = UndoRedoManager<DrawingStateWithCache>()
    private val selectionManager = SelectionManager()

    private val layerManager = LayerManager(bitmapCache) { layer, bitmap ->
        scope.launch(dispatcher) {
            operations.updateLayer(layer, bitmap)
        }
    }

    private val _state = MutableStateFlow(
        DrawingState(
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

    var currentPath by mutableStateOf<DrawingPath?>(null)
    var canvasSize by mutableStateOf(Size.Zero)

    private var drawingCanvasState by mutableStateOf<DrawingCanvasState?>(null)

    val selectionState: SelectionState get() = selectionManager.selectionState

    fun getOrCreateDrawingCanvas(): DrawingCanvasState {
        val currentState = drawingCanvasState
        return if (currentState != null &&
            currentState.bitmap.width == canvasSize.width.toInt() &&
            currentState.bitmap.height == canvasSize.height.toInt()) {
            currentState
        } else {
            val bitmap = ImageBitmap(
                canvasSize.width.toInt().coerceAtLeast(1),
                canvasSize.height.toInt().coerceAtLeast(1)
            )
            val canvas = Canvas(bitmap)
            val newState = DrawingCanvasState(bitmap, canvas, false)
            drawingCanvasState = newState
            newState
        }
    }

    fun startDrawingSession(): DrawingCanvasState {
        val canvasState = getOrCreateDrawingCanvas()
        canvasState.clear() // Clear any previous drawing
        drawingCanvasState = canvasState.copy(isActive = true)
        return drawingCanvasState!!
    }

    fun endDrawingSession(commit: Boolean = true): ImageBitmap? {
        val canvasState = drawingCanvasState ?: return null
        val resultBitmap = if (commit) canvasState.bitmap.copy() else null

        drawingCanvasState = canvasState.copy(isActive = false)
        canvasState.clear()

        return resultBitmap
    }

    fun addDrawingPath(path: DrawingPath, finalBitmap: ImageBitmap) {
        val currentState = _state.value
        saveStateToHistory(currentState)

        bitmapCache.put(currentState.currentLayer.id, finalBitmap)

        val newFrame = layerManager.addPathToLayer(
            currentState.currentFrame,
            currentState.currentLayerIndex,
            path,
            finalBitmap
        )

        _state.value = currentState.copy(
            currentFrame = newFrame,
            isModified = true
        )

        updateUndoRedoState()
    }

    fun commitDrawingBitmap(drawingBitmap: ImageBitmap, drawingPath: DrawingPath) {
        val currentState = _state.value
        saveStateToHistory(currentState)

        val existingBitmap = bitmapCache.get(currentState.currentLayer.id)
            ?: ImageBitmap(canvasSize.width.toInt(), canvasSize.height.toInt())

        val finalBitmap = combineDrawingBitmapWithLayer(
            drawingBitmap = drawingBitmap,
            layerBitmap = existingBitmap,
            blendMode = drawingPath.brush.blendMode
        )

        bitmapCache.put(currentState.currentLayer.id, finalBitmap)

        val newFrame = layerManager.addPathToLayer(
            currentState.currentFrame,
            currentState.currentLayerIndex,
            drawingPath,
            finalBitmap
        )

        _state.value = currentState.copy(
            currentFrame = newFrame,
            isModified = true
        )

        updateUndoRedoState()
    }

    private fun combineDrawingBitmapWithLayer(
        drawingBitmap: ImageBitmap,
        layerBitmap: ImageBitmap,
        blendMode: BlendMode = BlendMode.SrcOver
    ): ImageBitmap {
        val resultBitmap = layerBitmap.copy()
        val canvas = Canvas(resultBitmap)

        val paint = Paint().apply {
            this.blendMode = blendMode
        }

        canvas.drawImage(drawingBitmap, Offset.Zero, paint)
        return resultBitmap
    }

    fun createEmptyBitmap(): ImageBitmap {
        return ImageBitmap(
            canvasSize.width.toInt().coerceAtLeast(1),
            canvasSize.height.toInt().coerceAtLeast(1)
        )
    }

    fun renderPathToBitmap(path: DrawingPath): ImageBitmap {
        val existingBitmap = bitmapCache.get(_state.value.currentLayer.id)
        return DrawRenderer.renderPathToBitmap(path, canvasSize, existingBitmap)
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
        selectionManager.startSelection(offset)
    }

    fun updateSelection(offset: Offset) {
        selectionManager.updateSelection(offset)
    }

    fun updateSelectionState(state: SelectionState) {
        selectionManager.updateSelectionState(state)
    }

    fun endSelection(): Rect? {
        return selectionManager.endSelection()
    }

    fun applySelection() {
        if (selectionState.isActive && selectionState.transformedBitmap != null) {
            saveStateToHistory(_state.value)
            applySelectionToLayer(selectionState)
            selectionManager.clearSelection()
            updateUndoRedoState()
        }
    }

    fun clearSelection() {
        selectionManager.clearSelection()
    }

    fun getLayerBitmap(layerId: Long): ImageBitmap? = bitmapCache.get(layerId)

    fun getCombinedBitmap(): ImageBitmap? {
        val currentState = _state.value
        return bitmapCache.getCombinedBitmap(currentState.layers, canvasSize)
    }

    fun isLayerEmpty(layerId: Long): Boolean {
        val bitmap = bitmapCache.get(layerId) ?: return true
        return bitmap.width == 0 || bitmap.height == 0
    }

    fun loadFrame(frame: FrameModel, layerBitmaps: Map<Long, ImageBitmap>) {
        _state.value = DrawingState(
            currentFrame = frame,
            currentLayerIndex = (frame.layers.size - 1).coerceAtLeast(0),
            canvasSize = canvasSize
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

        val fillPath = DrawingPath(
            brush = BrushData.solid,
            color = color,
            size = 1f,
            path = Path(),
            startPoint = Offset(x.toFloat(), y.toFloat()),
            endPoint = Offset(x.toFloat(), y.toFloat())
        )

        val newFrame = layerManager.addPathToLayer(
            currentState.currentFrame,
            currentState.currentLayerIndex,
            fillPath,
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
        val bounds = selectionState.bounds
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

        val selectionPath = DrawingPath(
            brush = BrushData.solid,
            color = Color.Black,
            size = 1f,
            path = Path(),
            startPoint = bounds.topLeft,
            endPoint = bounds.bottomRight
        )

        val newFrame = layerManager.addPathToLayer(
            currentState.currentFrame,
            currentState.currentLayerIndex,
            selectionPath,
            layerBitmap
        )

        _state.value = currentState.copy(
            currentFrame = newFrame,
            isModified = true
        )
    }
}

fun DrawingState.withFrame(frame: FrameModel): DrawingState =
    copy(currentFrame = frame, isModified = true)

fun DrawingState.withLayerIndex(index: Int): DrawingState =
    copy(currentLayerIndex = index.coerceIn(0, currentFrame.layers.size - 1))

fun DrawingState.withCanvasSize(size: Size): DrawingState =
    copy(canvasSize = size)