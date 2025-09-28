package io.github.taalaydev.doodleverse.engine.tool.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.util.DirtyRect
import io.github.taalaydev.doodleverse.engine.util.currentTimeMillis
import io.github.taalaydev.doodleverse.engine.util.expand
import kotlinx.datetime.Clock

class ShapeTool(
    private var shapeType: ShapeType,
    private var brush: Brush,
    private var params: BrushParams,
    private val imageSize: IntSize,
    private var constraints: ShapeConstraints = ShapeConstraints()
) {
    private var startPoint: Offset? = null
    private var currentPoint: Offset? = null
    private var isDrawing = false
    private var currentPath: Path? = null
    private val previewImage = androidx.compose.ui.graphics.ImageBitmap(imageSize.width, imageSize.height)
    private val canvas = Canvas(previewImage)

    fun updateParams(brush: Brush, params: BrushParams, shapeType: ShapeType, constraints: ShapeConstraints = ShapeConstraints()) {
        this.brush = brush
        this.params = params
        this.shapeType = shapeType
        this.constraints = constraints
    }

    fun handleStart(event: GestureEvent): DirtyRect {
        startPoint = event.position
        currentPoint = event.position
        isDrawing = true

        clearCanvas()

        currentPath = shapeType.createPath(event.position, event.position, constraints)

        return currentPath?.getBounds()?.expand(params.size)
    }

    fun handleMove(event: GestureEvent): DirtyRect {
        if (!isDrawing || startPoint == null) return null

        clearCanvas()
        currentPoint = event.position

        // Clear previous preview (if needed)
        val oldBounds = currentPath?.getBounds()

        // Create new path with current shape
        currentPath = shapeType.createPath(startPoint!!, event.position, constraints)

        // Draw preview
        drawShapePreview(canvas)

        val newBounds = currentPath?.getBounds()

        // Return combined dirty region
        return if (oldBounds != null && newBounds != null) {
            oldBounds.expandToInclude(newBounds).expand(params.size)
        } else {
            newBounds?.expand(params.size)
        }
    }

    fun handleEnd(event: GestureEvent): DirtyRect {
        if (!isDrawing || startPoint == null) return null

        currentPoint = event.position

        // Create final path
        val finalPath = shapeType.createPath(startPoint!!, event.position, constraints)

        // Stroke the path with the current brush
        strokePathWithBrush(canvas, finalPath)

        val dirtyRect = finalPath.getBounds().expand(params.size)

        // Reset state
        clear()

        return dirtyRect
    }

    private fun drawShapePreview(canvas: Canvas) {
        val path = currentPath ?: return

        // Draw a preview stroke (lighter/thinner than final)
        val previewPaint = androidx.compose.ui.graphics.Paint().apply {
            color = params.color.copy(alpha = 0.5f)
            strokeWidth = params.size * 0.5f
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            strokeJoin = androidx.compose.ui.graphics.StrokeJoin.Round
        }

        canvas.drawPath(path, previewPaint)
    }

    private fun strokePathWithBrush(canvas: Canvas, path: Path) {
        // Convert path to points and stroke with brush
        val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
        pathMeasure.setPath(path, false)

        val length = pathMeasure.length
        val step = params.size * 0.1f // Adjust density based on brush size

        var distance = 0f
        val session = brush.startSession(canvas, params)

        var isFirst = true
        while (distance <= length) {
            val position = pathMeasure.getPosition(distance)

            val event = GestureEvent(
                position = position,
                timeMillis = Clock.currentTimeMillis(),
                pressure = params.pressure
            )

            if (isFirst) {
                session.start(event)
                isFirst = false
            } else {
                session.move(event)
            }

            distance += step
        }

        // End the stroke
        val endPosition = pathMeasure.getPosition(length)
        session.end(
            GestureEvent(
                position = endPosition,
                timeMillis = Clock.currentTimeMillis(),
                pressure = params.pressure
            )
        )
    }

    fun clear() {
        startPoint = null
        currentPoint = null
        isDrawing = false
        currentPath = null
    }

    private fun clearCanvas() {
        canvas.drawRect(
            Rect(Offset.Zero, Offset(imageSize.width.toFloat(), imageSize.height.toFloat())),
            Paint().apply {
                color = androidx.compose.ui.graphics.Color.Transparent
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            }
        )
    }

    fun isActive() = isDrawing
    fun getCurrentPath() = currentPath
    fun getPreviewImage() = previewImage
}

// Helper to expand rectangles
private fun androidx.compose.ui.geometry.Rect.expandToInclude(other: androidx.compose.ui.geometry.Rect): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
        left = minOf(this.left, other.left),
        top = minOf(this.top, other.top),
        right = maxOf(this.right, other.right),
        bottom = maxOf(this.bottom, other.bottom)
    )
}