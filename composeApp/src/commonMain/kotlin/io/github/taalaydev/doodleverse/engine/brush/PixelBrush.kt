package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.ProceduralBrush
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlin.math.*

class PixelBrush : ProceduralBrush() {

    override val id = ToolId("pixel")
    override val name: String = "Pixel"

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession {
        return PixelStrokeSession(canvas, params)
    }

    private class PixelStrokeSession(
        private val canvas: Canvas,
        params: BrushParams
    ) : StrokeSession(params) {

        private val pixelPaint = Paint().apply {
            isAntiAlias = false // Important for pixel art!
            color = params.color
            blendMode = params.blendMode
        }

        private val pixelSize = max(1f, params.size)
        private var lastGridPos: Pair<Int, Int>? = null

        private fun worldToGrid(worldPos: Offset): Pair<Int, Int> {
            return Pair(
                (worldPos.x / pixelSize).toInt(),
                (worldPos.y / pixelSize).toInt()
            )
        }

        private fun gridToWorld(gridPos: Pair<Int, Int>): Offset {
            return Offset(
                gridPos.first * pixelSize,
                gridPos.second * pixelSize
            )
        }

        private fun drawPixel(gridPos: Pair<Int, Int>): DirtyRect {
            val worldPos = gridToWorld(gridPos)
            canvas.drawRect(
                worldPos.x,
                worldPos.y,
                worldPos.x + pixelSize,
                worldPos.y + pixelSize,
                pixelPaint
            )

            return Rect(
                worldPos.x,
                worldPos.y,
                worldPos.x + pixelSize,
                worldPos.y + pixelSize
            )
        }

        private fun drawLine(from: Pair<Int, Int>, to: Pair<Int, Int>): DirtyRect {
            // Bresenham's line algorithm for pixel-perfect lines
            val x0 = from.first
            val y0 = from.second
            val x1 = to.first
            val y1 = to.second

            var dirty: DirtyRect = null

            val dx = abs(x1 - x0)
            val dy = abs(y1 - y0)
            val sx = if (x0 < x1) 1 else -1
            val sy = if (y0 < y1) 1 else -1
            var err = dx - dy

            var x = x0
            var y = y0

            while (true) {
                dirty = dirty.union(drawPixel(Pair(x, y)))

                if (x == x1 && y == y1) break

                val e2 = 2 * err
                if (e2 > -dy) {
                    err -= dy
                    x += sx
                }
                if (e2 < dx) {
                    err += dx
                    y += sy
                }
            }

            return dirty
        }

        override fun start(event: GestureEvent): DirtyRect {
            val gridPos = worldToGrid(event.position)
            lastGridPos = gridPos
            return drawPixel(gridPos)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val currentGridPos = worldToGrid(event.position)
            val lastGrid = lastGridPos

            return if (lastGrid != null && lastGrid != currentGridPos) {
                val dirty = drawLine(lastGrid, currentGridPos)
                lastGridPos = currentGridPos
                dirty
            } else {
                null
            }
        }

        override fun end(event: GestureEvent): DirtyRect {
            return move(event)
        }
    }
}