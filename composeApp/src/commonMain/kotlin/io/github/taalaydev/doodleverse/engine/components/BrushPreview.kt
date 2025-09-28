package io.github.taalaydev.doodleverse.engine.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.util.currentTimeMillis

@Composable
fun BrushPreview(
    brush: Brush,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    previewColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .border(borderWidth, borderColor, shape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(backgroundColor)
                .drawWithCache {
                    val w = size.width.toInt().coerceAtLeast(1)
                    val h = size.height.toInt().coerceAtLeast(1)

                    val bitmap = ImageBitmap(w, h)
                    val canvas = Canvas(bitmap)

                    fun renderPreview() {
                        val p0 = Offset(size.width * 0.1f,  size.height * 0.2f)  // Start point
                        val p1 = Offset(size.width * 0.2f,  size.height * 0.9f)  // First control point
                        val p2 = Offset(size.width * 0.8f,  size.height * 0.1f)  // Second control point
                        val p3 = Offset(size.width * 0.9f,  size.height * 0.8f)  // End point

                        val brushParams = BrushParams(
                            color = previewColor,
                            size  = (size.height * 0.30f).coerceAtLeast(1f)
                        )

                        val steps = 36
                        val session = brush.startSession(canvas, brushParams)
                        session.start(GestureEvent(position = p0, timeMillis = currentTimeMillis(), pressure = 1f))

                        for (i in 1..steps) {
                            val t = i / steps.toFloat()

                            val oneMinusT = 1f - t
                            val tSquared = t * t

                            val c0 = oneMinusT * oneMinusT * oneMinusT
                            val c1 = 3 * oneMinusT * oneMinusT * t
                            val c2 = 3 * oneMinusT * tSquared
                            val c3 = tSquared * t

                            val x = c0 * p0.x + c1 * p1.x + c2 * p2.x + c3 * p3.x
                            val y = c0 * p0.y + c1 * p1.y + c2 * p2.y + c3 * p3.y

                            session.move(GestureEvent(position = Offset(x, y), timeMillis = currentTimeMillis(), pressure = 1f))
                        }
                        session.end(GestureEvent(position = p3, timeMillis = currentTimeMillis(), pressure = 1f))
                    }

                    renderPreview()

                    onDrawBehind {
                        drawImage(bitmap)
                    }
                }
        )

        // Brush Name
        Text(
            text = brush.name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 4.dp)
        )
    }
}

/**
 * Draws a quadratic Bézier curve onto a canvas using a specified brush.
 * The curve is approximated by a series of small line segments.
 *
 * @param canvas The canvas to draw on.
 * @param p0 The starting point of the curve.
 * @param p1 The control point.
 * @param p2 The end point of the curve.
 * @param brush The brush to use for drawing.
 * @param brushParams The parameters for the brush.
 */
fun drawQuadraticBezier(
    canvas: Canvas,
    p0: Offset,
    p1: Offset,
    p2: Offset,
    brush: Brush,
    brushParams: BrushParams
) {
    val session = brush.startSession(canvas, brushParams)
    session.start(
        GestureEvent(
            position = p0,
            timeMillis = currentTimeMillis(),
            pressure = 1.0f
        )
    )

    // Approximate the curve by interpolating points along it
    val steps = 100
    for (i in 1..steps) {
        val t = i / steps.toFloat()
        val x = (1 - t) * (1 - t) * p0.x + 2 * (1 - t) * t * p1.x + t * t * p2.x
        val y = (1 - t) * (1 - t) * p0.y + 2 * (1 - t) * t * p1.y + t * t * p2.y
        val point = GestureEvent(
            position = Offset(x, y),
            timeMillis = currentTimeMillis(),
            pressure = 1.0f
        )
        session.move(point)
    }

    session.end(
        GestureEvent(
            position = p2,
            timeMillis = currentTimeMillis(),
            pressure = 1.0f
        )
    )
}

/**
 * Draws a preview of a brush stroke within a DrawScope.
 * This is useful for UI elements that show what a brush looks like.
 *
 * @param brush The brush to preview.
 * @param color The color of the preview stroke.
 * @param canvasSize The size of the canvas area for the preview.
 */
fun DrawScope.drawBrushPreview(
    brush: Brush,
    color: Color,
    canvasSize: Size
) {
    val p0 = Offset(canvasSize.width * 0.1f, canvasSize.height * 0.25f)
    val p1 = Offset(canvasSize.width * 0.5f, canvasSize.height * 0.9f)
    val p2 = Offset(canvasSize.width * 0.9f, canvasSize.height * 0.25f)

    val brushParams = BrushParams(
        color = color,
        size = (canvasSize.height * 0.3f).coerceAtLeast(1f)
    )

    drawQuadraticBezier(
        canvas = drawContext.canvas,
        p0 = p0,
        p1 = p1,
        p2 = p2,
        brush = brush,
        brushParams = brushParams
    )
}


private fun calculateOptimalBrushSize(brush: Brush, canvasSize: Size): Float {
    val baseSize = canvasSize.height * 0.08f
    return baseSize.coerceIn(2f, 20f)
}