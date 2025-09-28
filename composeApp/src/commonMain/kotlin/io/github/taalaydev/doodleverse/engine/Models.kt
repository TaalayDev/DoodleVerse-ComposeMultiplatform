package io.github.taalaydev.doodleverse.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.shape.ShapeType

sealed class DrawTool {
    data class BrushTool(
        val brush: Brush
    ) : DrawTool()

    data class Eraser(
        val brush: Brush
    ) : DrawTool()

    data class Shape(
        val shape: ShapeType,
        val brush: Brush
    ) : DrawTool()

    data class TextTool(
        val size: Float,
        val color: Int
    ) : DrawTool()

    data class Curve(
        val brush: Brush
    ) : DrawTool()

    data object Zoom : DrawTool()
    data object Drag : DrawTool()
    data object Fill : DrawTool()
    data object Eyedropper : DrawTool()
    data object Selection : DrawTool()

    val isBrush: Boolean get() = this is BrushTool
    val isEraser: Boolean get() = this is Eraser
    val isShape: Boolean get() = this is Shape
    val isTextTool: Boolean get() = this is TextTool
    val isCurve: Boolean get() = this is Curve
    val isZoom: Boolean get() = this is Zoom
    val isDrag: Boolean get() = this is Drag
    val isFill: Boolean get() = this is Fill
    val isEyedropper: Boolean get() = this is Eyedropper
    val isSelection: Boolean get() = this is Selection
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

data class DrawingState(
    val currentFrame: FrameModel,
    val currentLayerIndex: Int = 0,
    val canvasSize: IntSize = IntSize.Zero,
    val isModified: Boolean = false
) {
    val currentLayer: LayerModel
        get() = currentFrame.layers.getOrNull(currentLayerIndex) ?: currentFrame.layers.first()

    val layers: List<LayerModel>
        get() = currentFrame.layers
}

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


data class DragState(
    val zoom: Float = 1f,
    val draggedTo: Offset = Offset.Zero,
    val rotation: Float = 0f,
)