package io.github.taalaydev.doodleverse.core.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import io.github.taalaydev.doodleverse.engine.copy
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class BrushModifier {
    Stroke,
    Fill,
    Mirror,
}

object DrawRenderer {
    internal fun calcOpacity(alpha: Float, brushOpacity: Float): Float {
        return (max(alpha, brushOpacity) - min(alpha, brushOpacity))
            .coerceAtLeast(0f)
    }

    private fun toDegrees(radians: Double): Double {
        return radians * (180.0 / PI)
    }

    fun blendPathBitmap(
        pathBitmap: ImageBitmap,
        targetBitmap: ImageBitmap,
        blendMode: androidx.compose.ui.graphics.BlendMode = androidx.compose.ui.graphics.BlendMode.SrcOver,
        isEraser: Boolean = false
    ): ImageBitmap {
        if (isEraser || blendMode == androidx.compose.ui.graphics.BlendMode.Clear) {
            return pathBitmap
        }

        val resultBitmap = targetBitmap.copy()
        val canvas = Canvas(resultBitmap)

        canvas.drawImage(
            pathBitmap,
            Offset.Zero,
            Paint().apply {
                this.blendMode = blendMode
            }
        )

        return resultBitmap
    }

    private fun calculateTaperFactor(progress: Float, useSmoothing: Boolean = true): Float {
        return if (useSmoothing) {
            when {
                progress < 0.15f -> {
                    val normalizedProgress = progress / 0.15f
                    kotlin.math.sin(normalizedProgress * kotlin.math.PI / 2).toFloat()
                }
                progress > 0.85f -> {
                    val normalizedProgress = (1f - progress) / 0.15f
                    kotlin.math.sin(normalizedProgress * kotlin.math.PI / 2).toFloat()
                }
                else -> 1f
            }
        } else {
            when {
                progress < 0.1f -> progress * 10
                progress > 0.9f -> (1 - progress) * 10
                else -> 1f
            }
        }
    }

    fun floodFill(
        canvas: Canvas,
        imageBitmap: ImageBitmap,
        x: Int,
        y: Int,
        replacement: Int,
        borderPath: Path? = null,
    ) {
        if (x < 0 || y < 0 || x >= imageBitmap.width || y >= imageBitmap.height) return

        val width = imageBitmap.width
        val height = imageBitmap.height
        val pixelMap = imageBitmap.toPixelMap()
        val targetColor = pixelMap[x, y].toArgb()

        if (targetColor == replacement) return

        val visited = Array(height) { BooleanArray(width) }

        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.add(Pair(x, y))

        while (stack.isNotEmpty()) {
            val (x, y) = stack.removeLast()

            if (x < 0 || x >= width || y < 0 || y >= height || visited[y][x]) continue

            if (pixelMap[x, y].toArgb() != targetColor) continue

            visited[y][x] = true

            canvas.nativeCanvas.drawPoint(
                x.toFloat(),
                y.toFloat(),
                Paint().apply {
                    color = Color(replacement)
                    style = PaintingStyle.Fill
                }.asFrameworkPaint()
            )

            stack.add(Pair(x + 1, y))
            stack.add(Pair(x - 1, y))
            stack.add(Pair(x, y + 1))
            stack.add(Pair(x, y - 1))
        }
    }

    fun createSmoothPath(points: List<Offset>): Path {
        val path = Path()

        if (points.isEmpty()) return path

        path.moveTo(points[0].x, points[0].y)

        when (points.size) {
            1 -> {
                path.addOval(
                    androidx.compose.ui.geometry.Rect(
                        points[0].x - 1f,
                        points[0].y - 1f,
                        points[0].x + 1f,
                        points[0].y + 1f
                    )
                )
            }
            2 -> {
                path.lineTo(points[1].x, points[1].y)
            }
            else -> {
                var i = 1
                while (i < points.size) {
                    if (i == points.size - 1) {
                        path.lineTo(points[i].x, points[i].y)
                    } else {
                        val controlPoint = points[i]
                        val endPoint = Offset(
                            (points[i].x + points[i + 1].x) / 2f,
                            (points[i].y + points[i + 1].y) / 2f
                        )
                        path.quadraticBezierTo(
                            controlPoint.x, controlPoint.y,
                            endPoint.x, endPoint.y
                        )
                    }
                    i++
                }
            }
        }

        return path
    }

    private fun getDistance(point1: Offset, point2: Offset): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        return sqrt(dx * dx + dy * dy)
    }
}

private fun Paint.copy(): Paint {
    return Paint().apply {
        alpha = this@copy.alpha
        isAntiAlias = this@copy.isAntiAlias
        color = this@copy.color
        blendMode = this@copy.blendMode
        style = this@copy.style
        strokeWidth = this@copy.strokeWidth
        strokeCap = this@copy.strokeCap
        strokeJoin = this@copy.strokeJoin
        pathEffect = this@copy.pathEffect
        colorFilter = this@copy.colorFilter
    }
}