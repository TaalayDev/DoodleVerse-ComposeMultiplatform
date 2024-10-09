package io.github.taalaydev.doodleverse.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath

object DrawPainter {
    internal fun drawPath(
        canvas: Canvas,
        drawingPath: DrawingPath,
        brush: BrushData,
        paint: Paint,
        size: Size,
    ) {
        if (brush.customPainter != null) {
            brush.customPainter.invoke(
                canvas,
                size,
                drawingPath,
            )
        } else {
            canvas.drawPath(
                drawingPath.path,
                paint
            )
        }
    }

    internal fun drawBrushStampsBetweenPoints(
        canvas: Canvas,
        start: Offset,
        end: Offset,
        paint: Paint,
        drawingPath: DrawingPath,
    ) {
        val brushImage = drawingPath.brush.brush ?: return
        val brushSize = drawingPath.size
        val densityOffset = drawingPath.brush.densityOffset.toFloat()
        val useBrushWidthDensity = drawingPath.brush.useBrushWidthDensity

        val path = drawingPath.path
        val measure = PathMeasure().apply { setPath(path, false) }
        val length = measure.length
        val delta = Offset(end.x - start.x, end.y - start.y)

        val halfSize = brushSize / 8
        var i = 0f
        var currentPoint = start

        while (i < length) {
            val point = measure.getPosition(i)
            canvas.drawImageRect(
                brushImage,
                dstOffset = IntOffset(
                    (point.x - halfSize).toInt(),
                    (point.y - halfSize).toInt()
                ),
                dstSize = IntSize(brushSize.toInt(), brushSize.toInt()),
                paint = paint
            )
            currentPoint += delta
            i += halfSize
        }
    }
}