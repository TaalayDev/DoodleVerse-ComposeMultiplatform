package io.github.taalaydev.doodleverse.engine.tool.shape

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

sealed class ShapeType {
    abstract fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints = ShapeConstraints()): Path

    data object Rectangle : ShapeType() {
        override fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints): Path {
            val rect = if (constraints.maintainAspectRatio) {
                createSquare(start, end)
            } else {
                Rect(start, end)
            }

            return Path().apply {
                addRect(rect)
            }
        }
    }

    data object Circle : ShapeType() {
        override fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints): Path {
            val center = (start + end) / 2f
            val radius = start.distanceTo(end) / 2f

            return Path().apply {
                addOval(Rect(center - Offset(radius, radius), center + Offset(radius, radius)))
            }
        }
    }

    data object Ellipse : ShapeType() {
        override fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints): Path {
            return Path().apply {
                addOval(Rect(start, end))
            }
        }
    }

    data object Line : ShapeType() {
        override fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints): Path {
            val adjustedEnd = if (constraints.snapToAngles) {
                snapToAngle(start, end)
            } else {
                end
            }

            return Path().apply {
                moveTo(start.x, start.y)
                lineTo(adjustedEnd.x, adjustedEnd.y)
            }
        }
    }

    data object Triangle : ShapeType() {
        override fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints): Path {
            val topCenter = Offset((start.x + end.x) / 2f, start.y)
            val bottomLeft = Offset(start.x, end.y)
            val bottomRight = Offset(end.x, end.y)

            return Path().apply {
                moveTo(topCenter.x, topCenter.y)
                lineTo(bottomLeft.x, bottomLeft.y)
                lineTo(bottomRight.x, bottomRight.y)
                close()
            }
        }
    }

    data object Arrow : ShapeType() {
        override fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints): Path {
            val adjustedEnd = if (constraints.snapToAngles) {
                snapToAngle(start, end)
            } else {
                end
            }

            val angle = atan2((adjustedEnd.y - start.y).toDouble(), (adjustedEnd.x - start.x).toDouble())
            val arrowLength = min(start.distanceTo(adjustedEnd) * 0.3f, 50f)
            val arrowAngle = PI / 6 // 30 degrees

            val arrowPoint1 = Offset(
                (adjustedEnd.x - arrowLength * cos(angle - arrowAngle)).toFloat(),
                (adjustedEnd.y - arrowLength * sin(angle - arrowAngle)).toFloat()
            )

            val arrowPoint2 = Offset(
                (adjustedEnd.x - arrowLength * cos(angle + arrowAngle)).toFloat(),
                (adjustedEnd.y - arrowLength * sin(angle + arrowAngle)).toFloat()
            )

            return Path().apply {
                // Line
                moveTo(start.x, start.y)
                lineTo(adjustedEnd.x, adjustedEnd.y)

                // Arrow head
                moveTo(adjustedEnd.x, adjustedEnd.y)
                lineTo(arrowPoint1.x, arrowPoint1.y)

                moveTo(adjustedEnd.x, adjustedEnd.y)
                lineTo(arrowPoint2.x, arrowPoint2.y)
            }
        }
    }

    data object Star : ShapeType() {
        override fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints): Path {
            val center = (start + end) / 2f
            val outerRadius = start.distanceTo(end) / 2f
            val innerRadius = outerRadius * 0.4f
            val points = 5

            return Path().apply {
                for (i in 0 until points * 2) {
                    val angle = (PI * i / points).toFloat()
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val x = center.x + radius * cos(angle - PI.toFloat() / 2)
                    val y = center.y + radius * sin(angle - PI.toFloat() / 2)

                    if (i == 0) {
                        moveTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }
                close()
            }
        }
    }

    data object Polygon : ShapeType() {
        override fun createPath(start: Offset, end: Offset, constraints: ShapeConstraints): Path {
            val sides = constraints.polygonSides
            val center = (start + end) / 2f
            val radius = start.distanceTo(end) / 2f

            return Path().apply {
                for (i in 0 until sides) {
                    val angle = (2 * PI * i / sides - PI / 2).toFloat()
                    val x = center.x + radius * cos(angle)
                    val y = center.y + radius * sin(angle)

                    if (i == 0) {
                        moveTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }
                close()
            }
        }
    }

    companion object {
        fun values() = listOf(Rectangle, Circle, Ellipse, Line, Triangle, Arrow, Star, Polygon)
    }
}

data class ShapeConstraints(
    val maintainAspectRatio: Boolean = false,
    val snapToAngles: Boolean = false,
    val polygonSides: Int = 6
)

// Helper functions
private fun createSquare(start: Offset, end: Offset): Rect {
    val width = abs(end.x - start.x)
    val height = abs(end.y - start.y)
    val size = min(width, height)

    val left = if (end.x > start.x) start.x else start.x - size
    val top = if (end.y > start.y) start.y else start.y - size

    return Rect(left, top, left + size, top + size)
}

private fun snapToAngle(start: Offset, end: Offset): Offset {
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val snapAngles = listOf(0.0, PI/4, PI/2, 3*PI/4, PI, -3*PI/4, -PI/2, -PI/4)

    val snappedAngle = snapAngles.minByOrNull { abs(angle - it) } ?: angle
    val distance = start.distanceTo(end)

    return Offset(
        (start.x + distance * cos(snappedAngle)).toFloat(),
        (start.y + distance * sin(snappedAngle)).toFloat()
    )
}

private operator fun Offset.plus(other: Offset) = Offset(x + other.x, y + other.y)
private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)
private operator fun Offset.div(scalar: Float) = Offset(x / scalar, y / scalar)
private fun Offset.distanceTo(other: Offset): Float {
    val dx = other.x - x
    val dy = other.y - y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}