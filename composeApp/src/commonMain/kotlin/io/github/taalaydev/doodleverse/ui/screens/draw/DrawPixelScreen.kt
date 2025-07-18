package io.github.taalaydev.doodleverse.ui.screens.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.jvm.JvmInline
import kotlin.math.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@JvmInline
value class PackedCoord(val value: Long) {
    val x: Int get() = (value shr 32).toInt()
    val y: Int get() = value.toInt()

    companion object {
        fun pack(x: Int, y: Int): PackedCoord = PackedCoord((x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFF))
    }
}

data class PixelData(
    val coord: PackedCoord,
    val color: Color,
    val pressure: Float = 1f
)

// Tile-based rendering system
data class TileCoord(val x: Int, val y: Int) {
    companion object {
        fun fromPixel(pixelX: Int, pixelY: Int, tileSize: Int): TileCoord {
            return TileCoord(pixelX / tileSize, pixelY / tileSize)
        }
    }
}

data class Tile(
    val coord: TileCoord,
    val pixels: MutableMap<PackedCoord, PixelData> = mutableMapOf(),
    var isDirty: Boolean = true,
    var bitmap: ImageBitmap? = null,
    var canvas: Canvas? = null,
    val tileSize: Int = 128
) {
    fun addPixel(pixel: PixelData) {
        pixels[pixel.coord] = pixel
        isDirty = true
    }

    fun removePixel(coord: PackedCoord) {
        pixels.remove(coord)
        isDirty = true
    }

    fun getBounds(): Rect {
        return Rect(
            left = coord.x * tileSize.toFloat(),
            top = coord.y * tileSize.toFloat(),
            right = (coord.x + 1) * tileSize.toFloat(),
            bottom = (coord.y + 1) * tileSize.toFloat()
        )
    }
}

// Vertex-based rendering data
data class PixelVertex(
    val position: Offset,
    val color: Color
)

data class VertexBatch(
    val vertices: FloatArray,
    val colors: IntArray,
    val indices: ShortArray,
    val vertexCount: Int
) {
    companion object {
        fun fromPixels(pixels: Collection<PixelData>, pixelSize: Float): VertexBatch {
            val vertexCount = pixels.size * 4 // 4 vertices per pixel quad
            val vertices = FloatArray(vertexCount * 2) // x, y per vertex
            val colors = IntArray(vertexCount)
            val indices = ShortArray(pixels.size * 6) // 6 indices per pixel quad (2 triangles)

            var vertexIndex = 0
            var colorIndex = 0
            var indexIndex = 0
            var quadIndex = 0

            pixels.forEach { pixel ->
                val x = pixel.coord.x * pixelSize
                val y = pixel.coord.y * pixelSize
                val colorInt = pixel.color.toArgb()

                // Create quad vertices (top-left, top-right, bottom-right, bottom-left)
                // Top-left
                vertices[vertexIndex++] = x
                vertices[vertexIndex++] = y
                colors[colorIndex++] = colorInt

                // Top-right
                vertices[vertexIndex++] = x + pixelSize
                vertices[vertexIndex++] = y
                colors[colorIndex++] = colorInt

                // Bottom-right
                vertices[vertexIndex++] = x + pixelSize
                vertices[vertexIndex++] = y + pixelSize
                colors[colorIndex++] = colorInt

                // Bottom-left
                vertices[vertexIndex++] = x
                vertices[vertexIndex++] = y + pixelSize
                colors[colorIndex++] = colorInt

                // Create indices for two triangles
                val baseVertex = (quadIndex * 4).toShort()

                // First triangle (top-left, top-right, bottom-right)
                indices[indexIndex++] = baseVertex
                indices[indexIndex++] = (baseVertex + 1).toShort()
                indices[indexIndex++] = (baseVertex + 2).toShort()

                // Second triangle (top-left, bottom-right, bottom-left)
                indices[indexIndex++] = baseVertex
                indices[indexIndex++] = (baseVertex + 2).toShort()
                indices[indexIndex++] = (baseVertex + 3).toShort()

                quadIndex++
            }

            return VertexBatch(vertices, colors, indices, vertexCount)
        }
    }
}

data class BrushSettings(
    val size: Int = 1,
    val isPressureSensitive: Boolean = true,
    val pressureOpacity: Boolean = true,
    val pressureSize: Boolean = false,
    val minPressure: Float = 0.1f,
    val maxPressure: Float = 1f
)

data class ViewportState(
    val offset: Offset = Offset.Zero,
    val zoom: Float = 1f,
    val bounds: Rect = Rect.Zero
)

data class PixelArtState(
    val pixels: Map<PackedCoord, PixelData> = emptyMap(),
    val currentColor: Color = Color.Black,
    val isErasing: Boolean = false,
    val gridSize: Int = 64,
    val tileSize: Int = 128, // Configurable tile size
    val tiles: Map<TileCoord, Tile> = emptyMap(),
    val dirtyTiles: Set<TileCoord> = emptySet(),
    val previewPixels: Map<PackedCoord, PixelData> = emptyMap(),
    val isDirty: Boolean = false,
    val canvasSize: Size = Size.Zero,
    val pixelSize: Float = 0f,
    val brushSettings: BrushSettings = BrushSettings(),
    val viewport: ViewportState = ViewportState(),
    val lastDrawnPixels: Set<PackedCoord> = emptySet(),
    val vertexBatch: VertexBatch? = null,
    val previewVertexBatch: VertexBatch? = null,
    val offscreenBitmap: ImageBitmap? = null,
    val offscreenCanvas: Canvas? = null,
    val renderStats: RenderStats = RenderStats()
)

data class RenderStats(
    val totalPixels: Int = 0,
    val visiblePixels: Int = 0,
    val activeTiles: Int = 0,
    val dirtyTiles: Int = 0,
    val lastRenderTime: Long = 0L,
    val tileUpdateTime: Long = 0L,
    val vertexBatchTime: Long = 0L
)

sealed class PixelArtAction {
    data class SetPixel(val x: Int, val y: Int, val pressure: Float = 1f) : PixelArtAction()
    data class StartDrawing(val x: Int, val y: Int, val pressure: Float = 1f) : PixelArtAction()
    data class ContinueDrawing(val x: Int, val y: Int, val pressure: Float = 1f) : PixelArtAction()
    data object EndDrawing : PixelArtAction()
    data class ChangeColor(val color: Color) : PixelArtAction()
    data class UpdateCanvasSize(val size: Size, val pixelSize: Float) : PixelArtAction()
    data class UpdateBrushSize(val size: Int) : PixelArtAction()
    data class UpdateBrushSettings(val settings: BrushSettings) : PixelArtAction()
    data class UpdateViewport(val offset: Offset, val zoom: Float) : PixelArtAction()
    data class UpdateTileSize(val tileSize: Int) : PixelArtAction()
    object ToggleEraseMode : PixelArtAction()
    object TogglePressureSensitivity : PixelArtAction()
    object ClearCanvas : PixelArtAction()
    object ExportArt : PixelArtAction()
    object RefreshTiles : PixelArtAction()
    object CommitDrawing : PixelArtAction()
}

class PixelArtViewModel {
    private var _state by mutableStateOf(PixelArtState())
    val state: PixelArtState get() = _state

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var lastDrawPosition: Pair<Int, Int>? = null
    private val brushCache = mutableMapOf<Int, List<Pair<Int, Int>>>()

    fun handleAction(action: PixelArtAction) {
        when (action) {
            is PixelArtAction.StartDrawing -> {
                lastDrawPosition = action.x to action.y
                drawWithBrush(action.x, action.y, action.pressure, isPreview = true)
            }

            is PixelArtAction.ContinueDrawing -> {
                val lastPos = lastDrawPosition
                if (lastPos != null) {
                    interpolateAndDraw(lastPos.first, lastPos.second, action.x, action.y, action.pressure, isPreview = true)
                } else {
                    drawWithBrush(action.x, action.y, action.pressure, isPreview = true)
                }
                lastDrawPosition = action.x to action.y
            }

            is PixelArtAction.EndDrawing -> {
                commitPreviewPixels()
                lastDrawPosition = null
            }

            is PixelArtAction.CommitDrawing -> {
                commitOffscreenCanvas()
            }

            is PixelArtAction.SetPixel -> {
                drawWithBrush(action.x, action.y, action.pressure, isPreview = false)
                updateTiles()
            }

            is PixelArtAction.ChangeColor -> {
                _state = _state.copy(currentColor = action.color)
            }

            is PixelArtAction.UpdateCanvasSize -> {
                initializeOffscreenCanvas(action.size, action.pixelSize)
                _state = _state.copy(
                    canvasSize = action.size,
                    pixelSize = action.pixelSize,
                    isDirty = true
                )
                updateTiles()
            }

            is PixelArtAction.UpdateTileSize -> {
                val newTileSize = action.tileSize.coerceIn(64, 512)
                _state = _state.copy(tileSize = newTileSize)
                rebuildTiles()
            }

            is PixelArtAction.UpdateBrushSize -> {
                val newSize = action.size.coerceIn(1, 50)
                _state = _state.copy(
                    brushSettings = _state.brushSettings.copy(size = newSize)
                )
                brushCache.remove(_state.brushSettings.size)
            }

            is PixelArtAction.UpdateBrushSettings -> {
                _state = _state.copy(brushSettings = action.settings)
                brushCache.clear()
            }

            is PixelArtAction.UpdateViewport -> {
                val newViewport = _state.viewport.copy(
                    offset = action.offset,
                    zoom = action.zoom.coerceIn(0.1f, 20f),
                    bounds = calculateViewportBounds(action.offset, action.zoom)
                )
                _state = _state.copy(viewport = newViewport)
                updateVertexBatches()
            }

            PixelArtAction.ToggleEraseMode -> {
                _state = _state.copy(isErasing = !_state.isErasing)
            }

            PixelArtAction.TogglePressureSensitivity -> {
                _state = _state.copy(
                    brushSettings = _state.brushSettings.copy(
                        isPressureSensitive = !_state.brushSettings.isPressureSensitive
                    )
                )
            }

            PixelArtAction.ClearCanvas -> {
                clearCanvas()
            }

            PixelArtAction.ExportArt -> {
                exportPixelArt(_state.pixels)
            }

            PixelArtAction.RefreshTiles -> {
                updateTiles()
            }
        }
    }

    private fun initializeOffscreenCanvas(size: Size, pixelSize: Float) {
        if (size.width > 0 && size.height > 0) {
            try {
                val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
                val canvas = Canvas(bitmap)

                // Clear the canvas with white background
                canvas.drawRect(
                    Rect(0f, 0f, size.width, size.height),
                    Paint().apply {
                        color = Color.White
                        style = PaintingStyle.Fill
                    }
                )

                _state = _state.copy(
                    offscreenBitmap = bitmap,
                    offscreenCanvas = canvas
                )
            } catch (e: Exception) {
                println("Failed to create offscreen canvas: ${e.message}")
            }
        }
    }

    private fun commitOffscreenCanvas() {
        // Apply the offscreen drawing to the main state
        commitPreviewPixels()
        updateTiles()
    }

    private fun calculateViewportBounds(offset: Offset, zoom: Float): Rect {
        val canvasSize = _state.canvasSize
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return Rect.Zero

        val viewWidth = canvasSize.width / zoom
        val viewHeight = canvasSize.height / zoom

        return Rect(
            left = -offset.x / zoom,
            top = -offset.y / zoom,
            right = (-offset.x + viewWidth) / zoom,
            bottom = (-offset.y + viewHeight) / zoom
        )
    }

    private fun getBrushShape(size: Int): List<Pair<Int, Int>> {
        return brushCache.getOrPut(size) {
            val radius = size / 2f
            val radiusSquared = radius * radius
            val coords = mutableListOf<Pair<Int, Int>>()

            for (x in -size..size) {
                for (y in -size..size) {
                    val distanceSquared = x * x + y * y
                    if (distanceSquared <= radiusSquared) {
                        coords.add(x to y)
                    }
                }
            }
            coords
        }
    }

    private fun drawWithBrush(x: Int, y: Int, pressure: Float, isPreview: Boolean) {
        val effectivePressure = if (_state.brushSettings.isPressureSensitive) {
            pressure.coerceIn(_state.brushSettings.minPressure, _state.brushSettings.maxPressure)
        } else {
            1f
        }

        val effectiveBrushSize = if (_state.brushSettings.isPressureSensitive && _state.brushSettings.pressureSize) {
            (_state.brushSettings.size * effectivePressure).roundToInt().coerceAtLeast(1)
        } else {
            _state.brushSettings.size
        }

        val effectiveColor = if (_state.brushSettings.isPressureSensitive && _state.brushSettings.pressureOpacity) {
            _state.currentColor.copy(alpha = effectivePressure)
        } else {
            _state.currentColor
        }

        val brushShape = getBrushShape(effectiveBrushSize)
        val pixelsToModify = mutableMapOf<PackedCoord, PixelData>()
        val newDrawnPixels = mutableSetOf<PackedCoord>()
        val affectedTiles = mutableSetOf<TileCoord>()

        // Apply brush shape and track affected tiles
        for ((dx, dy) in brushShape) {
            val pixelX = x + dx
            val pixelY = y + dy

            if (pixelX in 0 until _state.gridSize && pixelY in 0 until _state.gridSize) {
                val coord = PackedCoord.pack(pixelX, pixelY)
                val tileCoord = TileCoord.fromPixel(pixelX, pixelY, _state.tileSize)

                newDrawnPixels.add(coord)
                affectedTiles.add(tileCoord)

                if (!_state.isErasing) {
                    pixelsToModify[coord] = PixelData(coord, effectiveColor, effectivePressure)

                    // Draw to offscreen canvas immediately
                    _state.offscreenCanvas?.let { canvas ->
                        val pixelSize = _state.pixelSize
                        canvas.drawRect(
                            Rect(
                                left = pixelX * pixelSize,
                                top = pixelY * pixelSize,
                                right = (pixelX + 1) * pixelSize,
                                bottom = (pixelY + 1) * pixelSize
                            ),
                            Paint().apply {
                                color = effectiveColor
                                style = PaintingStyle.Fill
                            }
                        )
                    }
                }
            }
        }

        // Update state and tiles
        if (isPreview) {
            val newPreviewPixels = _state.previewPixels.toMutableMap()

            if (_state.isErasing) {
                newDrawnPixels.forEach { coord -> newPreviewPixels.remove(coord) }
            } else {
                newPreviewPixels.putAll(pixelsToModify)
            }

            _state = _state.copy(
                previewPixels = newPreviewPixels,
                lastDrawnPixels = newDrawnPixels
            )

            updatePreviewVertexBatch()
        } else {
            updatePixelsAndTiles(pixelsToModify, newDrawnPixels, affectedTiles)
        }
    }

    private fun updatePixelsAndTiles(
        pixelsToModify: Map<PackedCoord, PixelData>,
        newDrawnPixels: Set<PackedCoord>,
        affectedTiles: Set<TileCoord>
    ) {
        val newPixels = _state.pixels.toMutableMap()
        val newTiles = _state.tiles.toMutableMap()
        val newDirtyTiles = _state.dirtyTiles.toMutableSet()

        if (_state.isErasing) {
            newDrawnPixels.forEach { coord ->
                newPixels.remove(coord)
                val tileCoord = TileCoord.fromPixel(coord.x, coord.y, _state.tileSize)
                newTiles[tileCoord]?.removePixel(coord)
            }
        } else {
            newPixels.putAll(pixelsToModify)

            // Update affected tiles
            affectedTiles.forEach { tileCoord ->
                val tile = newTiles.getOrPut(tileCoord) {
                    Tile(tileCoord, tileSize = _state.tileSize)
                }

                pixelsToModify.values.forEach { pixel ->
                    val pixelTileCoord = TileCoord.fromPixel(pixel.coord.x, pixel.coord.y, _state.tileSize)
                    if (pixelTileCoord == tileCoord) {
                        tile.addPixel(pixel)
                    }
                }

                newDirtyTiles.add(tileCoord)
            }
        }

        _state = _state.copy(
            pixels = newPixels,
            tiles = newTiles,
            dirtyTiles = newDirtyTiles,
            lastDrawnPixels = newDrawnPixels,
            isDirty = true
        )
    }

    private fun interpolateAndDraw(x1: Int, y1: Int, x2: Int, y2: Int, pressure: Float, isPreview: Boolean) {
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
        val steps = maxOf(dx, dy, 1)

        val maxSteps = 20
        val stepSize = if (steps > maxSteps) steps / maxSteps else 1

        for (i in 0..steps step stepSize) {
            val t = if (steps == 0) 0f else i.toFloat() / steps
            val x = (x1 + t * (x2 - x1)).roundToInt()
            val y = (y1 + t * (y2 - y1)).roundToInt()

            val coord = PackedCoord.pack(x, y)
            if (coord !in _state.lastDrawnPixels) {
                drawWithBrush(x, y, pressure, isPreview)
            }
        }
    }

    private fun commitPreviewPixels() {
        val newPixels = _state.pixels.toMutableMap()
        val newTiles = _state.tiles.toMutableMap()
        val newDirtyTiles = _state.dirtyTiles.toMutableSet()

        if (_state.isErasing) {
            _state.previewPixels.keys.forEach { coord ->
                newPixels.remove(coord)
                val tileCoord = TileCoord.fromPixel(coord.x, coord.y, _state.tileSize)
                newTiles[tileCoord]?.removePixel(coord)
                newDirtyTiles.add(tileCoord)
            }
        } else {
            newPixels.putAll(_state.previewPixels)

            _state.previewPixels.values.forEach { pixel ->
                val tileCoord = TileCoord.fromPixel(pixel.coord.x, pixel.coord.y, _state.tileSize)
                val tile = newTiles.getOrPut(tileCoord) {
                    Tile(tileCoord, tileSize = _state.tileSize)
                }
                tile.addPixel(pixel)
                newDirtyTiles.add(tileCoord)
            }
        }

        _state = _state.copy(
            pixels = newPixels,
            tiles = newTiles,
            dirtyTiles = newDirtyTiles,
            previewPixels = emptyMap(),
            previewVertexBatch = null,
            lastDrawnPixels = emptySet(),
            isDirty = true
        )

        updateVertexBatches()
    }

    private fun rebuildTiles() {
        val newTiles = mutableMapOf<TileCoord, Tile>()
        val newDirtyTiles = mutableSetOf<TileCoord>()

        _state.pixels.values.forEach { pixel ->
            val tileCoord = TileCoord.fromPixel(pixel.coord.x, pixel.coord.y, _state.tileSize)
            val tile = newTiles.getOrPut(tileCoord) {
                Tile(tileCoord, tileSize = _state.tileSize)
            }
            tile.addPixel(pixel)
            newDirtyTiles.add(tileCoord)
        }

        _state = _state.copy(
            tiles = newTiles,
            dirtyTiles = newDirtyTiles,
            isDirty = true
        )

        updateVertexBatches()
    }

    @OptIn(ExperimentalTime::class)
    private fun updateTiles() {
        if (_state.pixelSize <= 0) return

        val startTime = Clock.System.now().toEpochMilliseconds()

        // Update only dirty tiles
        val updatedTiles = _state.tiles.toMutableMap()
        _state.dirtyTiles.forEach { tileCoord ->
            updatedTiles[tileCoord]?.let { tile ->
                // Mark tile as clean after processing
                tile.isDirty = false
            }
        }

        val tileTime = Clock.System.now().toEpochMilliseconds() - startTime

        // Update vertex batches
        updateVertexBatches()

        val vertexTime = Clock.System.now().toEpochMilliseconds() - startTime - tileTime

        val stats = _state.renderStats.copy(
            totalPixels = _state.pixels.size,
            activeTiles = _state.tiles.size,
            dirtyTiles = _state.dirtyTiles.size,
            tileUpdateTime = tileTime,
            vertexBatchTime = vertexTime
        )

        _state = _state.copy(
            tiles = updatedTiles,
            dirtyTiles = emptySet(),
            isDirty = false,
            renderStats = stats
        )
    }

    private fun updateVertexBatches() {
        if (_state.pixelSize <= 0) return

        // Filter visible pixels based on viewport
        val visiblePixels = if (_state.viewport.bounds.isEmpty) {
            _state.pixels.values
        } else {
            _state.pixels.values.filter { pixel ->
                val x = pixel.coord.x * _state.pixelSize
                val y = pixel.coord.y * _state.pixelSize
                x < _state.viewport.bounds.right && x + _state.pixelSize > _state.viewport.bounds.left &&
                        y < _state.viewport.bounds.bottom && y + _state.pixelSize > _state.viewport.bounds.top
            }
        }

        val vertexBatch = if (visiblePixels.isNotEmpty()) {
            VertexBatch.fromPixels(visiblePixels, _state.pixelSize)
        } else null

        _state = _state.copy(
            vertexBatch = vertexBatch,
            renderStats = _state.renderStats.copy(visiblePixels = visiblePixels.size)
        )
    }

    private fun updatePreviewVertexBatch() {
        if (_state.pixelSize <= 0 || _state.previewPixels.isEmpty()) {
            _state = _state.copy(previewVertexBatch = null)
            return
        }

        val batch = VertexBatch.fromPixels(_state.previewPixels.values, _state.pixelSize)
        _state = _state.copy(previewVertexBatch = batch)
    }

    private fun clearCanvas() {
        // Clear offscreen canvas
        _state.offscreenCanvas?.let { canvas ->
            canvas.drawRect(
                Rect(0f, 0f, _state.canvasSize.width, _state.canvasSize.height),
                Paint().apply {
                    color = Color.White
                    style = PaintingStyle.Fill
                }
            )
        }

        _state = _state.copy(
            pixels = emptyMap(),
            tiles = emptyMap(),
            dirtyTiles = emptySet(),
            previewPixels = emptyMap(),
            lastDrawnPixels = emptySet(),
            vertexBatch = null,
            previewVertexBatch = null,
            isDirty = true,
            renderStats = RenderStats()
        )
        brushCache.clear()
    }

    private fun exportPixelArt(pixels: Map<PackedCoord, PixelData>) {
        println("Exporting ${pixels.size} pixels with tile-based vertex rendering")
    }
}

@Composable
fun PixelArtDrawingScreen(
    viewModel: PixelArtViewModel = remember { PixelArtViewModel() }
) {
    val state = viewModel.state

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Text(
                    text = "Ultra-Optimized Pixel Art Studio",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Tile-Based Rendering • Vertex Batching • Off-screen Canvas • Smart Culling",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Toolbar
                    ToolbarPanel(
                        state = state,
                        onAction = viewModel::handleAction,
                        modifier = Modifier.width(320.dp)
                    )

                    // Canvas
                    PixelCanvas(
                        state = state,
                        onDrawStart = { x, y, pressure ->
                            viewModel.handleAction(PixelArtAction.StartDrawing(x, y, pressure))
                        },
                        onDrawContinue = { x, y, pressure ->
                            viewModel.handleAction(PixelArtAction.ContinueDrawing(x, y, pressure))
                        },
                        onDrawEnd = {
                            viewModel.handleAction(PixelArtAction.EndDrawing)
                        },
                        onCanvasSizeChanged = { size, pixelSize ->
                            viewModel.handleAction(PixelArtAction.UpdateCanvasSize(size, pixelSize))
                        },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
fun ToolbarPanel(
    state: PixelArtState,
    onAction: (PixelArtAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tools Section
            Text(
                text = "Tools",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Draw/Erase Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = {
                        if (state.isErasing) onAction(PixelArtAction.ToggleEraseMode)
                    },
                    label = { Text("Draw") },
                    selected = !state.isErasing,
                    leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    onClick = {
                        if (!state.isErasing) onAction(PixelArtAction.ToggleEraseMode)
                    },
                    label = { Text("Erase") },
                    selected = state.isErasing,
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Rendering Settings
            Text(
                text = "Rendering Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Tile Size Slider
            Column {
                Text(
                    text = "Tile Size: ${state.tileSize}px",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = state.tileSize.toFloat(),
                    onValueChange = { size ->
                        onAction(PixelArtAction.UpdateTileSize(size.roundToInt()))
                    },
                    valueRange = 64f..512f,
                    steps = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Smaller = more granular updates, Larger = fewer draw calls",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Brush Settings
            Text(
                text = "Brush Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Brush Size Slider
            Column {
                Text(
                    text = "Brush Size: ${state.brushSettings.size}px",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = state.brushSettings.size.toFloat(),
                    onValueChange = { size ->
                        onAction(PixelArtAction.UpdateBrushSize(size.roundToInt()))
                    },
                    valueRange = 1f..25f,
                    steps = 23,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Pressure Sensitivity Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pressure Sensitive",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = state.brushSettings.isPressureSensitive,
                    onCheckedChange = {
                        onAction(PixelArtAction.TogglePressureSensitivity)
                    }
                )
            }

            if (state.brushSettings.isPressureSensitive) {
                // Pressure Opacity Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pressure Opacity",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(
                        checked = state.brushSettings.pressureOpacity,
                        onCheckedChange = { enabled ->
                            onAction(PixelArtAction.UpdateBrushSettings(
                                state.brushSettings.copy(pressureOpacity = enabled)
                            ))
                        }
                    )
                }

                // Pressure Size Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Pressure Size",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Switch(
                        checked = state.brushSettings.pressureSize,
                        onCheckedChange = { enabled ->
                            onAction(PixelArtAction.UpdateBrushSettings(
                                state.brushSettings.copy(pressureSize = enabled)
                            ))
                        }
                    )
                }
            }

            // Current Color
            Text(
                text = "Current Color",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(state.currentColor, RoundedCornerShape(8.dp))
                        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                )

                Text(
                    text = "#${state.currentColor.toArgb().toString(16).uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // Color Palette
            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.labelLarge
            )

            ColorPalette(
                selectedColor = state.currentColor,
                onColorSelected = { color ->
                    onAction(PixelArtAction.ChangeColor(color))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            Button(
                onClick = { onAction(PixelArtAction.ClearCanvas) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Canvas")
            }

            OutlinedButton(
                onClick = { onAction(PixelArtAction.ExportArt) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Art")
            }

            if (state.isDirty) {
                OutlinedButton(
                    onClick = { onAction(PixelArtAction.RefreshTiles) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Cached, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh Tiles")
                }
            }

            // Performance Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Performance Stats",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Canvas: ${state.gridSize}×${state.gridSize}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Tile Size: ${state.tileSize}×${state.tileSize}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Total Pixels: ${state.renderStats.totalPixels}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Visible Pixels: ${state.renderStats.visiblePixels}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Active Tiles: ${state.renderStats.activeTiles}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Dirty Tiles: ${state.renderStats.dirtyTiles}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.renderStats.dirtyTiles > 0)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Preview: ${state.previewPixels.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (state.renderStats.tileUpdateTime > 0) {
                        Text(
                            text = "Tile Update: ${state.renderStats.tileUpdateTime}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.renderStats.tileUpdateTime < 16)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    if (state.renderStats.vertexBatchTime > 0) {
                        Text(
                            text = "Vertex Batch: ${state.renderStats.vertexBatchTime}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.renderStats.vertexBatchTime < 8)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    if (state.isDirty) {
                        Text(
                            text = "Status: Updating tiles...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        text = "Off-screen Canvas: ${if (state.offscreenBitmap != null) "Active" else "Inactive"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.offscreenBitmap != null)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color.Black, Color.White, Color.Red, Color.Green, Color.Blue, Color.Yellow,
        Color.Magenta, Color.Cyan, Color(0xFFFFA500), Color(0xFF800080),
        Color(0xFFFFC0CB), Color(0xFFA52A2A), Color.Gray, Color(0xFF008000),
        Color(0xFF000080), Color(0xFF800000), Color(0xFF808000), Color(0xFF008080),
        Color(0xFFC0C0C0), Color(0xFFFF6347), Color(0xFF4169E1), Color(0xFF32CD32),
        Color(0xFFFFD700), Color(0xFFDA70D6)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color, RoundedCornerShape(4.dp))
                    .border(
                        width = if (color == selectedColor) 3.dp else 1.dp,
                        color = if (color == selectedColor)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun PixelCanvas(
    state: PixelArtState,
    onDrawStart: (Int, Int, Float) -> Unit,
    onDrawContinue: (Int, Int, Float) -> Unit,
    onDrawEnd: () -> Unit,
    onCanvasSizeChanged: (Size, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Ultra-Optimized Drawing Canvas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(2.dp, MaterialTheme.colorScheme.outline)
                        .pointerInput(Unit) {
                            handleSmoothDrawing(
                                onStart = { offset, pressure ->
                                    val canvasSize = size
                                    val pixelSize = minOf(
                                        canvasSize.width.toFloat() / state.gridSize,
                                        canvasSize.height.toFloat() / state.gridSize
                                    )

                                    val newSize = Size(
                                        state.gridSize * pixelSize,
                                        state.gridSize * pixelSize
                                    )

                                    if (newSize != state.canvasSize || pixelSize != state.pixelSize) {
                                        onCanvasSizeChanged(newSize, pixelSize)
                                    }

                                    val x = (offset.x / pixelSize).toInt()
                                    val y = (offset.y / pixelSize).toInt()
                                    if (x in 0 until state.gridSize && y in 0 until state.gridSize) {
                                        onDrawStart(x, y, pressure)
                                    }
                                },
                                onDrag = { offset, pressure ->
                                    val pixelSize = if (state.pixelSize > 0) state.pixelSize else {
                                        minOf(
                                            size.width.toFloat() / state.gridSize,
                                            size.height.toFloat() / state.gridSize
                                        )
                                    }

                                    val x = (offset.x / pixelSize).toInt()
                                    val y = (offset.y / pixelSize).toInt()
                                    if (x in 0 until state.gridSize && y in 0 until state.gridSize) {
                                        onDrawContinue(x, y, pressure)
                                    }
                                },
                                onEnd = {
                                    onDrawEnd()
                                },
                                interpolationDensity = 0.8f
                            )
                        }
                ) {
                    val canvasSize = size
                    val pixelSize = minOf(
                        canvasSize.width / state.gridSize,
                        canvasSize.height / state.gridSize
                    )

                    // Draw background
                    drawRect(
                        color = Color.White,
                        size = Size(state.gridSize * pixelSize, state.gridSize * pixelSize)
                    )

                    // Draw from offscreen bitmap if available
                    state.offscreenBitmap?.let { bitmap ->
                        drawImage(
                            image = bitmap,
                            dstSize = IntSize(
                                (state.gridSize * pixelSize).toInt(),
                                (state.gridSize * pixelSize).toInt()
                            )
                        )
                    }

                    // Draw main pixels using vertex batching
                    state.vertexBatch?.let { batch ->
                        drawVertexBatch(batch)
                    }

                    // Draw preview pixels using vertex batching
                    state.previewVertexBatch?.let { batch ->
                        drawVertexBatch(batch, alpha = 0.8f)
                    }
                }
            }

            Text(
                text = "Tile-Based Vertex Rendering • ${state.renderStats.visiblePixels}/${state.renderStats.totalPixels} pixels • ${state.renderStats.activeTiles} tiles (${state.renderStats.dirtyTiles} dirty) • Off-screen: ${if (state.offscreenBitmap != null) "ON" else "OFF"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// Optimized vertex batch rendering
fun DrawScope.drawVertexBatch(batch: VertexBatch, alpha: Float = 1f) {
    // For now, we'll fall back to individual rectangles since Compose doesn't directly support
    // vertex arrays. In a more advanced implementation, this could use a custom painter
    // or native graphics calls.

    var i = 0
    while (i < batch.vertices.size) {
        val x = batch.vertices[i]
        val y = batch.vertices[i + 1]
        val colorInt = batch.colors[i / 2]
        val color = Color(colorInt)

        val adjustedColor = if (alpha < 1f) color.copy(alpha = color.alpha * alpha) else color

        // Each vertex represents a corner of a pixel quad
        // We advance by 8 (4 vertices * 2 coordinates) to get to the next pixel
        if (i % 8 == 0) { // Only draw once per pixel quad
            val pixelSize = batch.vertices[i + 2] - x // width from x to x+1
            drawRect(
                color = adjustedColor,
                topLeft = Offset(x, y),
                size = Size(pixelSize, pixelSize)
            )
        }

        i += 2
    }
}

// Simplified and efficient pointer handling (unchanged from original)
suspend fun PointerInputScope.handleSmoothDrawing(
    isActive: Boolean = true,
    onStart: (Offset, Float) -> Unit = { _, _ -> },
    onDrag: (Offset, Float) -> Unit = { _, _ -> },
    onEnd: () -> Unit = {},
    interpolationDensity: Float = 1.0f
) {
    if (!isActive) return

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastPosition = down.position

        onStart(down.position, down.pressure)
        down.consume()

        var drag: PointerInputChange?
        do {
            val event = awaitPointerEvent()
            drag = event.changes.firstOrNull { it.id == down.id }

            drag?.let { change ->
                if (change.positionChanged()) {
                    val distance = (change.position - lastPosition).getDistance()

                    if (distance > interpolationDensity) {
                        val steps = (distance / interpolationDensity).toInt().coerceIn(1, 5)
                        val stepSize = 1f / steps

                        for (i in 1..steps) {
                            val t = i * stepSize
                            val interpolatedPoint = Offset(
                                lastPosition.x + t * (change.position.x - lastPosition.x),
                                lastPosition.y + t * (change.position.y - lastPosition.y)
                            )
                            onDrag(interpolatedPoint, change.pressure)
                        }
                    } else {
                        onDrag(change.position, change.pressure)
                    }

                    lastPosition = change.position
                    change.consume()
                }
            }
        } while (drag != null && drag.pressed)

        onEnd()
    }
}