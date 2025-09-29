package io.github.taalaydev.doodleverse.core.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.charlietap.cachemap.CacheMap
import io.github.charlietap.cachemap.cacheMapOf
import io.github.taalaydev.doodleverse.data.models.LayerModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.math.min

data class TileCoordinate(val x: Int, val y: Int) {
    override fun toString(): String = "($x,$y)"
}

data class LayerTile(
    val layerId: Long,
    val coordinate: TileCoordinate,
    val bounds: Rect,
    var bitmap: ImageBitmap?,
    var isDirty: Boolean = true,
    var lastModified: Long = 0L,
    var version: Int = 0
) {
    fun markDirty() {
        isDirty = true
        lastModified = Clock.System.now().toEpochMilliseconds()
        version++
    }

    fun markClean() {
        isDirty = false
    }

    fun intersects(pathBounds: Rect): Boolean {
        return bounds.overlaps(pathBounds)
    }
}

data class CompositeTile(
    val coordinate: TileCoordinate,
    val bounds: Rect,
    var bitmap: ImageBitmap?,
    var isDirty: Boolean = true,
    var lastModified: Long = 0L,
    var layerVersions: Map<Long, Int> = emptyMap()
) {
    fun markDirty() {
        isDirty = true
        lastModified = Clock.System.now().toEpochMilliseconds()
    }

    fun markClean() {
        isDirty = false
    }

    fun needsUpdate(layerTiles: Map<Long, LayerTile>): Boolean {
        if (isDirty) return true

        return layerTiles.any { (layerId, tile) ->
            val lastVersion = layerVersions[layerId] ?: -1
            tile.version > lastVersion
        }
    }
}

data class ViewportInfo(
    val bounds: Rect,
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero
) {
    fun getVisibleTileRange(tileSize: Int, totalTilesX: Int, totalTilesY: Int): TileRange {
        val startX = ((bounds.left / tileSize).toInt() - 1).coerceAtLeast(0)
        val endX = ((bounds.right / tileSize).toInt() + 1).coerceAtMost(totalTilesX - 1)
        val startY = ((bounds.top / tileSize).toInt() - 1).coerceAtLeast(0)
        val endY = ((bounds.bottom / tileSize).toInt() + 1).coerceAtMost(totalTilesY - 1)

        return TileRange(startX, endX, startY, endY)
    }
}

data class TileRange(
    val startX: Int,
    val endX: Int,
    val startY: Int,
    val endY: Int
) {
    fun forEach(action: (TileCoordinate) -> Unit) {
        for (y in startY..endY) {
            for (x in startX..endX) {
                action(TileCoordinate(x, y))
            }
        }
    }

    fun count(): Int = (endX - startX + 1) * (endY - startY + 1)
}

class TileRenderer(
    private val canvasSize: Size,
    private val tileSize: Int = 512, // Larger tiles for better performance
    private val scope: CoroutineScope,
    private val maxConcurrentTiles: Int = 8 // Limit concurrent rendering
) {
    private val tilesX = (canvasSize.width / tileSize).toInt() + 1
    private val tilesY = (canvasSize.height / tileSize).toInt() + 1

    // Per-layer tile storage
    private val layerTiles = cacheMapOf<Long, HashMap<TileCoordinate, LayerTile>>()

    // Composite tiles (final rendered result)
    private val compositeTiles = cacheMapOf<TileCoordinate, CompositeTile>()

    // Dirty tracking
    private val dirtyLayerTiles = cacheMapOf<Long, MutableSet<TileCoordinate>>()
    private val dirtyCompositeTiles = mutableSetOf<TileCoordinate>()

    // Rendering control
    private val renderMutex = Mutex()
    private var currentRenderJob: Job? = null

    // Performance metrics
    private var lastRenderTime = 0L
    private var renderedTileCount = 0

    init {
        initializeTileStructure()
    }

    private fun initializeTileStructure() {
        for (y in 0 until tilesY) {
            for (x in 0 until tilesX) {
                val coordinate = TileCoordinate(x, y)
                val bounds = calculateTileBounds(coordinate)
                compositeTiles[coordinate] = CompositeTile(coordinate, bounds, null)
            }
        }
    }

    private fun calculateTileBounds(coordinate: TileCoordinate): Rect {
        return Rect(
            left = coordinate.x * tileSize.toFloat(),
            top = coordinate.y * tileSize.toFloat(),
            right = min((coordinate.x + 1) * tileSize.toFloat(), canvasSize.width),
            bottom = min((coordinate.y + 1) * tileSize.toFloat(), canvasSize.height)
        )
    }

    /**
     * Initialize tiles for a new layer
     */
    fun initializeLayer(layerId: Long) {
        layerTiles[layerId] = HashMap()
        dirtyLayerTiles[layerId] = mutableSetOf()

        for (y in 0 until tilesY) {
            for (x in 0 until tilesX) {
                val coordinate = TileCoordinate(x, y)
                val bounds = calculateTileBounds(coordinate)
                layerTiles[layerId]!![coordinate] = LayerTile(layerId, coordinate, bounds, null)
                dirtyLayerTiles[layerId]!!.add(coordinate)
            }
        }
    }

    /**
     * Remove tiles for a deleted layer
     */
    fun removeLayer(layerId: Long) {
        layerTiles.remove(layerId)
        dirtyLayerTiles.remove(layerId)

        // Mark all composite tiles as dirty since layer composition changed
        compositeTiles.values.forEach { it.markDirty() }
        dirtyCompositeTiles.addAll(compositeTiles.keys)
    }

    /**
     * Mark tiles dirty for a specific area
     */
    fun markAreaDirty(layerId: Long, bounds: Rect) {
        val affectedTiles = getTilesInBounds(bounds)

        layerTiles[layerId]?.let { tiles ->
            affectedTiles.forEach { coordinate ->
                tiles[coordinate]?.markDirty()
                dirtyLayerTiles.getOrPut(layerId) { mutableSetOf() }.add(coordinate)
            }
        }

        // Mark corresponding composite tiles dirty
        affectedTiles.forEach { coordinate ->
            compositeTiles[coordinate]?.markDirty()
            dirtyCompositeTiles.add(coordinate)
        }
    }

    private fun getTilesInBounds(bounds: Rect): List<TileCoordinate> {
        val startX = (bounds.left / tileSize).toInt().coerceAtLeast(0)
        val endX = (bounds.right / tileSize).toInt().coerceAtMost(tilesX - 1)
        val startY = (bounds.top / tileSize).toInt().coerceAtLeast(0)
        val endY = (bounds.bottom / tileSize).toInt().coerceAtMost(tilesY - 1)

        val tiles = mutableListOf<TileCoordinate>()
        for (y in startY..endY) {
            for (x in startX..endX) {
                tiles.add(TileCoordinate(x, y))
            }
        }
        return tiles
    }

    /**
     * Update layer bitmap and mark affected tiles dirty
     */
    suspend fun updateLayerBitmap(layerId: Long, bitmap: ImageBitmap, affectedBounds: Rect? = null) {
        val tiles = layerTiles.getOrPut(layerId) { HashMap() }
        val dirtyTiles = dirtyLayerTiles.getOrPut(layerId) { mutableSetOf() }

        val boundsToUpdate = affectedBounds ?: Rect(Offset.Zero, canvasSize)
        val affectedTileCoords = getTilesInBounds(boundsToUpdate)

        // Update affected layer tiles
        affectedTileCoords.forEach { coordinate ->
            val tile = tiles.getOrPut(coordinate) {
                LayerTile(layerId, coordinate, calculateTileBounds(coordinate), null)
            }

            // Extract tile portion from layer bitmap
            tile.bitmap = extractTileBitmap(bitmap, tile.bounds)
            tile.markDirty()
            dirtyTiles.add(coordinate)

            // Mark composite tile dirty
            compositeTiles[coordinate]?.markDirty()
            dirtyCompositeTiles.add(coordinate)
        }
    }

    private fun extractTileBitmap(sourceBitmap: ImageBitmap, tileBounds: Rect): ImageBitmap {
        val tileWidth = tileBounds.width.toInt()
        val tileHeight = tileBounds.height.toInt()

        if (tileWidth <= 0 || tileHeight <= 0) {
            return ImageBitmap(1, 1) // Minimal bitmap for edge cases
        }

        val tileBitmap = ImageBitmap(tileWidth, tileHeight)
        val canvas = Canvas(tileBitmap)

        canvas.drawImageRect(
            image = sourceBitmap,
            srcOffset = IntOffset(tileBounds.left.toInt(), tileBounds.top.toInt()),
            srcSize = IntSize(tileWidth, tileHeight),
            dstSize = IntSize(tileWidth, tileHeight),
            paint = Paint()
        )

        return tileBitmap
    }

    /**
     * Render dirty tiles with optimized batching and viewport awareness
     */
    suspend fun renderDirtyTiles(
        layers: List<LayerModel>,
        viewport: ViewportInfo? = null
    ) {
        // Cancel previous render job if still running
        currentRenderJob?.cancel()

        currentRenderJob = scope.launch {
            renderMutex.withLock {
                val startTime = Clock.System.now().toEpochMilliseconds()

                // Determine which tiles to render (prioritize viewport)
                val tilesToRender = if (viewport != null) {
                    val visibleRange = viewport.getVisibleTileRange(tileSize, tilesX, tilesY)
                    val visibleTiles = mutableSetOf<TileCoordinate>()
                    visibleRange.forEach { visibleTiles.add(it) }

                    // Prioritize visible dirty tiles, then non-visible
                    val visibleDirty = dirtyCompositeTiles.intersect(visibleTiles)
                    val nonVisibleDirty = dirtyCompositeTiles.subtract(visibleTiles)

                    visibleDirty.toList() + nonVisibleDirty.take(maxConcurrentTiles - visibleDirty.size)
                } else {
                    dirtyCompositeTiles.take(maxConcurrentTiles)
                }

                if (tilesToRender.isNotEmpty()) {
                    // Render tiles in parallel chunks
                    val chunks = tilesToRender.chunked(4)
                    chunks.forEach { chunk ->
                        val renderJobs = chunk.map { coordinate ->
                            async(Dispatchers.Default) {
                                renderCompositeTile(coordinate, layers)
                            }
                        }
                        renderJobs.awaitAll()
                    }

                    // Remove rendered tiles from dirty set
                    tilesToRender.forEach { dirtyCompositeTiles.remove(it) }
                }

                // Update metrics
                lastRenderTime = Clock.System.now().toEpochMilliseconds() - startTime
                renderedTileCount = tilesToRender.size
            }
        }
    }

    private suspend fun renderCompositeTile(coordinate: TileCoordinate, layers: List<LayerModel>) {
        val compositeTile = compositeTiles[coordinate] ?: return
        if (!compositeTile.isDirty) return

        val tileWidth = compositeTile.bounds.width.toInt()
        val tileHeight = compositeTile.bounds.height.toInt()

        if (tileWidth <= 0 || tileHeight <= 0) return

        // Create or reuse composite bitmap
        val compositeBitmap = compositeTile.bitmap?.takeIf {
            it.width == tileWidth && it.height == tileHeight
        } ?: ImageBitmap(tileWidth, tileHeight)

        val canvas = Canvas(compositeBitmap)

        // Clear tile
        canvas.drawRect(
            Rect(0f, 0f, tileWidth.toFloat(), tileHeight.toFloat()),
            Paint().apply {
                color = Color.Transparent
                blendMode = BlendMode.Clear
            }
        )

        // Composite visible layers
        val layerVersions = mutableMapOf<Long, Int>()
        layers.filter { it.isVisible && it.opacity > 0.0 }.forEach { layer ->
            val layerTile = layerTiles[layer.id]?.get(coordinate)
            layerTile?.bitmap?.let { layerBitmap ->
                canvas.drawImage(
                    image = layerBitmap,
                    topLeftOffset = Offset.Zero,
                    paint = Paint().apply {
                        alpha = layer.opacity.toFloat()
                        blendMode = BlendMode.SrcOver
                    }
                )
                layerVersions[layer.id] = layerTile.version
            }
        }

        compositeTile.bitmap = compositeBitmap
        compositeTile.layerVersions = layerVersions
        compositeTile.markClean()
    }

    /**
     * Draw tiles to canvas with viewport culling
     */
    fun drawToCanvas(canvas: Canvas, viewport: ViewportInfo) {
        val visibleRange = viewport.getVisibleTileRange(tileSize, tilesX, tilesY)

        visibleRange.forEach { coordinate ->
            val tile = compositeTiles[coordinate]
            tile?.bitmap?.let { bitmap ->
                canvas.drawImage(
                    image = bitmap,
                    topLeftOffset = tile.bounds.topLeft,
                    paint = Paint()
                )
            }
        }
    }

    /**
     * Draw all tiles to canvas (fallback for full rendering)
     */
    fun drawAllToCanvas(canvas: Canvas) {
        compositeTiles.values.forEach { tile ->
            tile.bitmap?.let { bitmap ->
                canvas.drawImage(
                    image = bitmap,
                    topLeftOffset = tile.bounds.topLeft,
                    paint = Paint()
                )
            }
        }
    }

    /**
     * Get performance statistics
     */
    fun getPerformanceStats(): TileRenderStats {
        return TileRenderStats(
            totalTiles = compositeTiles.size,
            dirtyTiles = dirtyCompositeTiles.size,
            lastRenderTime = lastRenderTime,
            renderedTileCount = renderedTileCount,
            memoryUsage = estimateMemoryUsage()
        )
    }

    private fun estimateMemoryUsage(): Long {
        var totalBytes = 0L
        compositeTiles.values.forEach { tile ->
            tile.bitmap?.let { bitmap ->
                totalBytes += (bitmap.width * bitmap.height * 4) // ARGB
            }
        }
        layerTiles.values.forEach { layerTileMap ->
            layerTileMap.values.forEach { tile ->
                tile.bitmap?.let { bitmap ->
                    totalBytes += (bitmap.width * bitmap.height * 4)
                }
            }
        }
        return totalBytes
    }

    /**
     * Force re-render of all tiles (for layer changes, etc.)
     */
    fun invalidateAll() {
        compositeTiles.values.forEach { it.markDirty() }
        dirtyCompositeTiles.addAll(compositeTiles.keys)

        layerTiles.values.forEach { layerTileMap ->
            layerTileMap.values.forEach { it.markDirty() }
        }
    }

    /**
     * Clear cache and reset
     */
    fun clear() {
        currentRenderJob?.cancel()
        compositeTiles.values.forEach { it.bitmap = null }
        layerTiles.values.forEach { layerTileMap ->
            layerTileMap.values.forEach { it.bitmap = null }
        }
        dirtyCompositeTiles.clear()
        dirtyLayerTiles.clear()
    }

    /**
     * Get a specific tile bitmap for debugging
     */
    fun getTileBitmap(coordinate: TileCoordinate): ImageBitmap? {
        return compositeTiles[coordinate]?.bitmap
    }
}

data class TileRenderStats(
    val totalTiles: Int,
    val dirtyTiles: Int,
    val lastRenderTime: Long,
    val renderedTileCount: Int,
    val memoryUsage: Long
)

fun <K, V> CacheMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    return this[key] ?: defaultValue().also { this[key] = it }
}