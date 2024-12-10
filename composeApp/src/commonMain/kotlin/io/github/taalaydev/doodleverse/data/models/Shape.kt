package io.github.taalaydev.doodleverse.data.models

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tan

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

    val gridShape = BrushData(
        id = 150,
        name = "Grid",
        stroke = "grid_shape",
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
                strokeWidth = 1f
                style = PaintingStyle.Stroke
            }

            // Draw outer rectangle
            canvas.drawRect(rect, paint)

            // Calculate grid divisions based on size
            val width = rect.width
            val height = rect.height
            val cellSize = minOf(width, height) / 8f

            // Draw vertical lines
            var x = rect.left + cellSize
            while (x < rect.right) {
                canvas.drawLine(
                    Offset(x, rect.top),
                    Offset(x, rect.bottom),
                    paint
                )
                x += cellSize
            }

            // Draw horizontal lines
            var y = rect.top + cellSize
            while (y < rect.bottom) {
                canvas.drawLine(
                    Offset(rect.left, y),
                    Offset(rect.right, y),
                    paint
                )
                y += cellSize
            }
        }
    )

    val perspectiveGridShape = BrushData(
        id = 151,
        name = "Perspective Grid",
        stroke = "perspective_grid_shape",
        isShape = true,
        customPainter = { canvas, size, drawingPath ->
            val startPoint = drawingPath.startPoint
            val endPoint = drawingPath.endPoint
            if (startPoint.isUnspecified || endPoint.isUnspecified) return@BrushData

            val vanishingPoint = startPoint
            val bounds = Rect(
                left = minOf(startPoint.x, endPoint.x),
                top = minOf(startPoint.y, endPoint.y),
                right = maxOf(startPoint.x, endPoint.x),
                bottom = maxOf(startPoint.y, endPoint.y)
            )

            val paint = Paint().apply {
                color = drawingPath.color
                strokeWidth = 1f
                style = PaintingStyle.Stroke
            }

            // Draw horizon line
            canvas.drawLine(
                Offset(bounds.left, vanishingPoint.y),
                Offset(bounds.right, vanishingPoint.y),
                paint
            )

            // Draw perspective lines
            val divisions = 8
            for (i in 0..divisions) {
                // Vertical divisions
                val x = bounds.left + (bounds.width * i / divisions)
                canvas.drawLine(
                    Offset(x, bounds.bottom),
                    vanishingPoint,
                    paint
                )

                // Horizontal divisions
                val y = bounds.top + (bounds.height * i / divisions)
                val leftPoint = Offset(bounds.left, y)
                val rightPoint = Offset(bounds.right, y)
                canvas.drawLine(leftPoint, rightPoint, paint)
            }
        }
    )

    val symmetryShape = BrushData(
        id = 152,
        name = "Symmetry Guide",
        stroke = "symmetry_guide_shape",
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
                strokeWidth = 1f
                style = PaintingStyle.Stroke
            }

            // Draw outer rectangle
            canvas.drawRect(rect, paint)

            // Draw center lines
            val centerX = (rect.left + rect.right) / 2
            val centerY = (rect.top + rect.bottom) / 2

            // Vertical center line
            canvas.drawLine(
                Offset(centerX, rect.top),
                Offset(centerX, rect.bottom),
                paint.apply { strokeWidth = 2f }
            )

            // Horizontal center line
            canvas.drawLine(
                Offset(rect.left, centerY),
                Offset(rect.right, centerY),
                paint
            )

            // Draw diagonal guides
            canvas.drawLine(
                Offset(rect.left, rect.top),
                Offset(rect.right, rect.bottom),
                paint.apply { strokeWidth = 1f }
            )
            canvas.drawLine(
                Offset(rect.right, rect.top),
                Offset(rect.left, rect.bottom),
                paint
            )
        }
    )

    val ruleOfThirdsShape = BrushData(
        id = 154,
        name = "Rule of Thirds",
        stroke = "rule_of_thirds_shape",
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
                strokeWidth = 1f
                style = PaintingStyle.Stroke
            }

            // Draw outer rectangle
            canvas.drawRect(rect, paint)

            // Calculate third points
            val thirdWidth = rect.width / 3f
            val thirdHeight = rect.height / 3f

            // Draw vertical lines
            for (i in 1..2) {
                val x = rect.left + thirdWidth * i
                canvas.drawLine(
                    Offset(x, rect.top),
                    Offset(x, rect.bottom),
                    paint
                )
            }

            // Draw horizontal lines
            for (i in 1..2) {
                val y = rect.top + thirdHeight * i
                canvas.drawLine(
                    Offset(rect.left, y),
                    Offset(rect.right, y),
                    paint
                )
            }

            // Draw intersection points
            val pointRadius = 4f
            for (i in 1..2) {
                for (j in 1..2) {
                    val x = rect.left + thirdWidth * i
                    val y = rect.top + thirdHeight * j
                    canvas.drawCircle(
                        Offset(x, y),
                        pointRadius,
                        paint.apply { style = PaintingStyle.Fill }
                    )
                }
            }
        }
    )

    val all = listOf(
        rectangleTool,
        circleTool,
        lineTool,
        arrowTool,
        ellipseTool,
        polygonTool,
        gridShape,
        perspectiveGridShape,
        symmetryShape,
        ruleOfThirdsShape,
    )
}