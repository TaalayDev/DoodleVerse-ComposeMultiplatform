package io.github.taalaydev.doodleverse.data.models

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
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

    val starTool = BrushData(
        id = 200,
        name = "Star",
        stroke = "star_tool",
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

            val center = rect.center
            val outerRadius = minOf(rect.width, rect.height) / 2
            val innerRadius = outerRadius * 0.4f  // 40% of outer radius for inner points
            val starPath = Path()

            // Create a 5-pointed star
            val points = 5
            for (i in 0 until points * 2) {
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                val angle = (PI / points * i - PI / 2).toFloat()  // Start at top (- PI/2)
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)

                if (i == 0) {
                    starPath.moveTo(x, y)
                } else {
                    starPath.lineTo(x, y)
                }
            }
            starPath.close()

            canvas.drawPath(starPath, paint)
        }
    )

    val triangleTool = BrushData(
        id = 202,
        name = "Triangle",
        stroke = "triangle_tool",
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

            val trianglePath = Path().apply {
                moveTo(rect.center.x, rect.top)  // Top center point
                lineTo(rect.right, rect.bottom)  // Bottom right point
                lineTo(rect.left, rect.bottom)   // Bottom left point
                close()
            }

            canvas.drawPath(trianglePath, paint)
        }
    )

    val hexagonTool = BrushData(
        id = 203,
        name = "Hexagon",
        stroke = "hexagon_tool",
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

            val center = rect.center
            val radius = minOf(rect.width / 2, rect.height / 2)
            val hexagonPath = Path()

            // Create a hexagon (6 sides)
            val sides = 6
            for (i in 0 until sides) {
                val angle = (2 * PI * i / sides - PI / 2).toFloat() // Start at top (- PI/2)
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)

                if (i == 0) {
                    hexagonPath.moveTo(x, y)
                } else {
                    hexagonPath.lineTo(x, y)
                }
            }
            hexagonPath.close()

            canvas.drawPath(hexagonPath, paint)
        }
    )

    val diamondTool = BrushData(
        id = 204,
        name = "Diamond",
        stroke = "diamond_tool",
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

            val diamondPath = Path().apply {
                moveTo(rect.center.x, rect.top)       // Top point
                lineTo(rect.right, rect.center.y)     // Right point
                lineTo(rect.center.x, rect.bottom)    // Bottom point
                lineTo(rect.left, rect.center.y)      // Left point
                close()
            }

            canvas.drawPath(diamondPath, paint)
        }
    )

    val speechBubbleTool = BrushData(
        id = 205,
        name = "Speech Bubble",
        stroke = "speech_bubble_tool",
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

            val bubblePath = Path()
            val cornerRadius = minOf(rect.width, rect.height) * 0.2f

            // Draw rounded rectangle for the bubble
            bubblePath.addRoundRect(
                RoundRect(
                    rect = Rect(
                        left = rect.left,
                        top = rect.top,
                        right = rect.right,
                        bottom = rect.bottom - rect.height * 0.2f
                    ),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )
            )

            // Add the pointer at the bottom
            bubblePath.moveTo(rect.center.x - rect.width * 0.1f, rect.bottom - rect.height * 0.2f)
            bubblePath.lineTo(rect.center.x, rect.bottom)
            bubblePath.lineTo(rect.center.x + rect.width * 0.1f, rect.bottom - rect.height * 0.2f)

            canvas.drawPath(bubblePath, paint)
        }
    )

    val pentagonTool = BrushData(
        id = 208,
        name = "Pentagon",
        stroke = "pentagon_tool",
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

            val center = rect.center
            val radius = minOf(rect.width / 2, rect.height / 2)
            val pentagonPath = Path()

            // Create a pentagon (5 sides)
            val sides = 5
            for (i in 0 until sides) {
                val angle = (2 * PI * i / sides - PI / 2).toFloat() // Start at top (- PI/2)
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)

                if (i == 0) {
                    pentagonPath.moveTo(x, y)
                } else {
                    pentagonPath.lineTo(x, y)
                }
            }
            pentagonPath.close()

            canvas.drawPath(pentagonPath, paint)
        }
    )

    val octagonTool = BrushData(
        id = 209,
        name = "Octagon",
        stroke = "octagon_tool",
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

            val center = rect.center
            val radius = minOf(rect.width / 2, rect.height / 2)
            val octagonPath = Path()

            // Create an octagon (8 sides)
            val sides = 8
            for (i in 0 until sides) {
                val angle = (2 * PI * i / sides).toFloat()
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)

                if (i == 0) {
                    octagonPath.moveTo(x, y)
                } else {
                    octagonPath.lineTo(x, y)
                }
            }
            octagonPath.close()

            canvas.drawPath(octagonPath, paint)
        }
    )

    val donutTool = BrushData(
        id = 210,
        name = "Donut",
        stroke = "donut_tool",
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

            val center = rect.center
            val outerRadius = minOf(rect.width / 2, rect.height / 2)
            val innerRadius = outerRadius * 0.6f

            // Draw outer circle
            canvas.drawCircle(center, outerRadius, paint)

            // Draw inner circle
            canvas.drawCircle(center, innerRadius, paint)
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

        starTool,
        triangleTool,
        hexagonTool,
        diamondTool,
        speechBubbleTool,
        pentagonTool,
        octagonTool,
        donutTool
    )
}