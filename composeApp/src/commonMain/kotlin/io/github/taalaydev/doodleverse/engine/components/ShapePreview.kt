package io.github.taalaydev.doodleverse.engine.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import io.github.taalaydev.doodleverse.engine.tool.shape.ShapeConstraints
import io.github.taalaydev.doodleverse.engine.tool.shape.ShapeType

/**
 * Small visual preview for a shape, drawn as a stroked outline.
 *
 * The preview auto-computes padding and stroke width based on the canvas size,
 * so it remains crisp at any dimension.
 */
@Composable
fun ShapePreview(
    shape: ShapeType,
    constraints: ShapeConstraints = ShapeConstraints(),
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    strokeColor: Color = MaterialTheme.colorScheme.onSurface,
    cornerRadius: Dp = 10.dp
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
    ) {
        val w = size.height
        val h = size.height
        val minDim = minOf(w, h)

        // Padding so strokes don’t hug the edges
        val pad = minDim * 0.18f
        val start = Offset(pad, pad)
        val end   = Offset(w - pad, h - pad)

        // Build the same path that your ShapeTool will use later for actual drawing
        val path = shape.createPath(start, end, constraints)

        // Stroke thickness scales with tile size
        val strokeWidth = minDim * 0.08f

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}