package io.github.taalaydev.doodleverse.data.models

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object Shape {
    val rectangleTool = BrushData(
        id = 52,
        name = "Rectangle",
        stroke = "rectangle_tool",
        isShape = true,
        customPainter = { canvas, size, drawingPath ->
            val startPoint = drawingPath.startPoint
            val endPoint = drawingPath.endPoint
            if (startPoint.isUnspecified || endPoint.isUnspecified) return@BrushData

            val rect = Rect(
                left = minOf(startPoint.x, endPoint.x),
                top = minOf(startPoint.y, endPoint.y),
                right = maxOf(startPoint.x, endPoint.x),
                bottom = maxOf(startPoint.y, endPoint.y)
            )

            val paint = Paint().apply {
                color = drawingPath.color
                style = PaintingStyle.Stroke
                strokeWidth = drawingPath.size
            }

            canvas.drawRect(rect, paint)
        }
    )

    val circleTool = BrushData(
        id = 53,
        name = "Circle",
        stroke = "circle_tool",
        isShape = true,
        customPainter = { canvas, size, drawingPath ->
            val startPoint = drawingPath.startPoint
            val endPoint = drawingPath.endPoint
            if (startPoint.isUnspecified || endPoint.isUnspecified) return@BrushData

            val radius = hypot(endPoint.x - startPoint.x, endPoint.y - startPoint.y)

            val paint = Paint().apply {
                color = drawingPath.color
                style = PaintingStyle.Stroke
                strokeWidth = drawingPath.size
            }

            canvas.drawCircle(startPoint, radius, paint)
        }
    )

    val lineTool = BrushData(
        id = 54,
        name = "Line",
        stroke = "line_tool",
        isShape = true,
        customPainter = { canvas, size, drawingPath ->
            val startPoint = drawingPath.startPoint
            val endPoint = drawingPath.endPoint

            val paint = Paint().apply {
                color = drawingPath.color
                style = PaintingStyle.Stroke
                strokeWidth = drawingPath.size
                strokeCap = StrokeCap.Round  // Optional: for rounded line ends
            }

            canvas.drawLine(startPoint, endPoint, paint)
        }
    )

    val arrowTool = BrushData(
        id = 56,
        name = "Arrow",
        stroke = "arrow_tool",
        isShape = true,
        customPainter = { canvas, size, drawingPath ->
            val startPoint = drawingPath.startPoint
            val endPoint = drawingPath.endPoint
            if (startPoint.isUnspecified || endPoint.isUnspecified) return@BrushData

            val paint = Paint().apply {
                color = drawingPath.color
                style = PaintingStyle.Stroke
                strokeWidth = drawingPath.size
                strokeCap = StrokeCap.Round
            }

            // Draw the main line
            canvas.drawLine(startPoint, endPoint, paint)

            // Calculate arrow head
            val angle = atan2(
                endPoint.y - startPoint.y,
                endPoint.x - startPoint.x
            )
            val arrowLength = 20f
            val arrowAngle = PI.toFloat() / 6f // 30 degrees

            val x1 = endPoint.x - arrowLength * cos(angle - arrowAngle)
            val y1 = endPoint.y - arrowLength * sin(angle - arrowAngle)
            val x2 = endPoint.x - arrowLength * cos(angle + arrowAngle)
            val y2 = endPoint.y - arrowLength * sin(angle + arrowAngle)

            // Draw arrow head
            val arrowPath = Path().apply {
                moveTo(endPoint.x, endPoint.y)
                lineTo(x1, y1)
                moveTo(endPoint.x, endPoint.y)
                lineTo(x2, y2)
            }

            canvas.drawPath(arrowPath, paint)
        }
    )

    val ellipseTool = BrushData(
        id = 57,
        name = "Ellipse",
        stroke = "ellipse_tool",
        isShape = true,
        customPainter = { canvas, size, drawingPath ->
            val startPoint = drawingPath.startPoint
            val endPoint = drawingPath.endPoint
            if (startPoint.isUnspecified || endPoint.isUnspecified) return@BrushData

            val rect = Rect(
                left = minOf(startPoint.x, endPoint.x),
                top = minOf(startPoint.y, endPoint.y),
                right = maxOf(startPoint.x, endPoint.x),
                bottom = maxOf(startPoint.y, endPoint.y)
            )

            val paint = Paint().apply {
                color = drawingPath.color
                style = PaintingStyle.Stroke
                strokeWidth = drawingPath.size
            }

            canvas.drawOval(
                rect = rect,
                paint = paint
            )
        }
    )

    val polygonTool = BrushData(
        id = 58,
        name = "Polygon",
        stroke = "polygon_tool",
        isShape = true,
        customPainter = { canvas, size, drawingPath ->
            val startPoint = drawingPath.startPoint
            val endPoint = drawingPath.endPoint
            if (startPoint.isUnspecified || endPoint.isUnspecified) return@BrushData

            val paint = Paint().apply {
                color = drawingPath.color
                style = PaintingStyle.Stroke
                strokeWidth = drawingPath.size
            }

            val path = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                lineTo(endPoint.x, endPoint.y)
            }

            canvas.drawPath(path, paint)
        }
    )

    val all = listOf(
        rectangleTool,
        circleTool,
        lineTool,
        arrowTool,
        ellipseTool,
        polygonTool
    )
}