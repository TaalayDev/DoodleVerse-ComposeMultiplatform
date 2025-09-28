package io.github.taalaydev.doodleverse.engine.tool

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import kotlinx.datetime.Clock

class CurveParams(
    val brush: Brush,
    val brushParams: BrushParams,
    val imageSize: IntSize,
    val curveSteps: Int = 2,
    val anchorPointRadius: Float = 10f,
    val previewAlpha: Float = 0.7f
)

class CurveTool(
    params: CurveParams
) : Tool {
    override val id: ToolId = ToolId("curve_tool")
    override val name: String = "Curve Tool"

    var curvePoints by mutableStateOf<List<Offset>>(emptyList())
    private var livePreviewPosition by mutableStateOf<Offset?>(null)
    private var params by mutableStateOf(params)

    private val paint = Paint().apply {
        color = Color.Black
        style = PaintingStyle.Fill
        isAntiAlias = true
    }

    fun updateParams(
        brush: Brush = params.brush,
        brushParams: BrushParams = params.brushParams,
        imageSize: IntSize = params.imageSize,
        curveSteps: Int = params.curveSteps,
        anchorPointRadius: Float = params.anchorPointRadius,
        previewAlpha: Float = params.previewAlpha
    ) {
        params = CurveParams(
            brush = brush,
            brushParams = brushParams,
            imageSize = imageSize,
            curveSteps = curveSteps,
            anchorPointRadius = anchorPointRadius,
            previewAlpha = previewAlpha
        )
    }

    fun isComplete(): Boolean = curvePoints.size == params.curveSteps

    fun handleTap(canvas: Canvas, point: Offset) {
        if (curvePoints.size < params.curveSteps) {
            curvePoints = curvePoints + point
            drawPreview(canvas)
        } else {
            clearCanvas(canvas)
            finalizeStroke(canvas, point)
            clear()
        }
    }

    fun handleDrag(canvas: Canvas, point: Offset?) {
        livePreviewPosition = point
        clearCanvas(canvas)
        drawPreview(canvas)
    }

    private fun clearCanvas(canvas: Canvas) {
        canvas.drawRect(
            Rect(0f, 0f, params.imageSize.width.toFloat(), params.imageSize.height.toFloat()),
            Paint().apply {
                color = Color.Transparent
                blendMode = BlendMode.Clear
            }
        )
    }

    private fun drawPreview(canvas: Canvas) {
        // Draw circles for the anchor points
        curvePoints.forEach { point ->
            paint.color = Color.Black
            canvas.drawCircle(radius = params.imageSize.width * 0.008f, center = point, paint = paint)
            paint.color = Color.White
            canvas.drawCircle(radius = params.imageSize.width * 0.006f, center = point, paint = paint)
        }

        // Draw the live preview curve
        if (curvePoints.size == 2 && livePreviewPosition != null) {
            val p0 = curvePoints[0]
            val p2 = curvePoints[1]
            val p1 = livePreviewPosition!!

            val previewPath = Path().apply {
                moveTo(p0.x, p0.y)
                quadraticBezierTo(p1.x, p1.y, p2.x, p2.y)
            }

            canvas.drawPath(
                path = previewPath,
                paint = Paint().apply {
                    color = params.brushParams.color.copy(alpha = params.previewAlpha)
                    strokeWidth = params.brushParams.size
                    style = PaintingStyle.Stroke
                    isAntiAlias = true
                    alpha = params.brushParams.color.alpha * params.previewAlpha
                },
            )
        }
    }

    private fun finalizeStroke(canvas: Canvas, point: Offset) {
        val p0 = curvePoints[0]
        val p2 = curvePoints[1]
        val p1 = point

        val session = params.brush.startSession(canvas, params.brushParams)
        session.start(GestureEvent(
            position = p0,
            timeMillis = currentTimeMillis(),
            pressure = 1f
        ))
        val steps = 100
        for (i in 1..steps) {
            val t = i / steps.toFloat()
            val x = (1 - t) * (1 - t) * p0.x + 2 * (1 - t) * t * p1.x + t * t * p2.x
            val y = (1 - t) * (1 - t) * p0.y + 2 * (1 - t) * t * p1.y + t * t * p2.y
            session.move(GestureEvent(
                position = Offset(x, y),
                timeMillis = currentTimeMillis(),
                pressure = 1f
            ))
        }
        session.end(GestureEvent(
            position = p2,
            timeMillis = currentTimeMillis(),
            pressure = 1f
        ))
    }

    private fun currentTimeMillis() = Clock.System.now().toEpochMilliseconds()

    fun clear() {
        curvePoints = emptyList()
        livePreviewPosition = null
    }
}