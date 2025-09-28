package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlin.random.Random

/**
 * Hatching brush that creates cross-hatching patterns for artistic shading.
 * Draws perpendicular lines across the stroke path to simulate traditional hatching techniques.
 */
class HatchingBrush(
    private val hatchSpacing: Float = 3f, // Distance between hatch lines
    private val hatchLength: Float = 8f, // Length of individual hatch marks
    private val hatchAngle: Float = 45f, // Angle of hatch lines in degrees
    private val doubleCross: Boolean = false, // Whether to add cross-hatching
    private val lineVariation: Float = 0.2f // 0-1, variation in line length and position
) : Brush() {

    override val id = ToolId("hatching")
    override val name: String = "Hatching"

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = HatchingStrokeSession(canvas, params, hatchSpacing, hatchLength, hatchAngle, doubleCross, lineVariation)

    private class HatchingStrokeSession(
        private val canvas: Canvas,
        params: BrushParams,
        private val hatchSpacing: Float,
        private val hatchLength: Float,
        private val hatchAngle: Float,
        private val doubleCross: Boolean,
        private val lineVariation: Float
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            color = params.color
            blendMode = params.blendMode
            strokeWidth = max(0.5f, params.size * 0.1f) // Thin lines for hatching
            strokeCap = StrokeCap.Round
        }

        private val stepPx: Float = hatchSpacing * 0.9f
        private val path = Path()

        // Random number generator with consistent seed per stroke
        private val random: Random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

        // State tracking
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f
        private var hatchIndex: Int = 0 // Counter for alternating hatch patterns

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float = max(1f, params.size * pressure * 0.5f)

        /**
         * Draws hatching lines at a given point perpendicular to the stroke direction
         */
        private fun drawHatchStamp(
            canvas: Canvas,
            center: Offset,
            strokeDirection: Float,
            radius: Float,
            pressure: Float
        ): Rect {
            val adjustedLength = hatchLength * pressure
            val adjustedRadius = radius * pressure

            // Calculate number of hatch lines to draw across the brush width
            val numLines = max(1, (adjustedRadius * 2f / hatchSpacing).toInt())
            val actualSpacing = adjustedRadius * 2f / numLines

            var dirty: Rect? = null

            // Draw primary hatch lines
            for (i in 0 until numLines) {
                val offsetFromCenter = (i - numLines / 2f) * actualSpacing
                val lineVariationFactor = 1f + (random.nextFloat() - 0.5f) * lineVariation
                val currentLength = adjustedLength * lineVariationFactor

                // Calculate position along the perpendicular to stroke direction
                val perpAngle = toRadians((strokeDirection + 90).toDouble()).toFloat()
                val lineCenter = Offset(
                    center.x + cos(perpAngle) * offsetFromCenter,
                    center.y + sin(perpAngle) * offsetFromCenter
                )

                // Draw hatch line
                val hatchLineAngle = toRadians(hatchAngle.toDouble()).toFloat()
                val halfLength = currentLength * 0.5f

                val start = Offset(
                    lineCenter.x - cos(hatchLineAngle) * halfLength,
                    lineCenter.y - sin(hatchLineAngle) * halfLength
                )
                val end = Offset(
                    lineCenter.x + cos(hatchLineAngle) * halfLength,
                    lineCenter.y + sin(hatchLineAngle) * halfLength
                )

                // Vary opacity based on pressure and position
                val lineOpacity = params.color.alpha * pressure * (0.7f + random.nextFloat() * 0.3f)
                val linePaint = paint.copy().apply {
                    color = params.color.copy(alpha = lineOpacity)
                }

                canvas.drawLine(start, end, linePaint)

                // Update dirty rect
                val lineRect = Rect(
                    min(start.x, end.x) - paint.strokeWidth,
                    min(start.y, end.y) - paint.strokeWidth,
                    max(start.x, end.x) + paint.strokeWidth,
                    max(start.y, end.y) + paint.strokeWidth
                )
                dirty = if (dirty == null) lineRect else dirty.union(lineRect)
            }

            // Draw cross-hatching if enabled
            if (doubleCross && pressure > 0.5f) { // Only cross-hatch with sufficient pressure
                val crossHatchAngle = toRadians((hatchAngle + 90).toDouble()).toFloat()

                for (i in 0 until numLines) {
                    val offsetFromCenter = (i - numLines / 2f) * actualSpacing
                    val lineVariationFactor = 1f + (random.nextFloat() - 0.5f) * lineVariation
                    val currentLength = adjustedLength * lineVariationFactor * 0.8f // Slightly shorter cross lines

                    val perpAngle = toRadians((strokeDirection + 90).toDouble()).toFloat()
                    val lineCenter = Offset(
                        center.x + cos(perpAngle) * offsetFromCenter,
                        center.y + sin(perpAngle) * offsetFromCenter
                    )

                    val halfLength = currentLength * 0.5f
                    val start = Offset(
                        lineCenter.x - cos(crossHatchAngle) * halfLength,
                        lineCenter.y - sin(crossHatchAngle) * halfLength
                    )
                    val end = Offset(
                        lineCenter.x + cos(crossHatchAngle) * halfLength,
                        lineCenter.y + sin(crossHatchAngle) * halfLength
                    )

                    val lineOpacity = params.color.alpha * pressure * 0.5f * (0.7f + random.nextFloat() * 0.3f)
                    val crossPaint = paint.copy().apply {
                        color = params.color.copy(alpha = lineOpacity)
                    }

                    canvas.drawLine(start, end, crossPaint)

                    val lineRect = Rect(
                        min(start.x, end.x) - paint.strokeWidth,
                        min(start.y, end.y) - paint.strokeWidth,
                        max(start.x, end.x) + paint.strokeWidth,
                        max(start.y, end.y) + paint.strokeWidth
                    )
                    dirty = dirty?.union(lineRect) ?: lineRect
                }
            }

            hatchIndex++
            return dirty ?: Rect(center.x, center.y, center.x, center.y)
        }

        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            val x = one * one * a.x + 2f * one * t * b.x + t * t * c.x
            val y = one * one * a.y + 2f * one * t * b.y + t * t * c.y
            return Offset(x, y)
        }

        private fun walkQuadratic(
            canvas: Canvas,
            a: Offset,
            b: Offset,
            c: Offset,
            pStart: Float,
            pEnd: Float
        ): DirtyRect {
            val approxLen = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            val subdivisions = max(8, ceil(approxLen / 8f).toInt())
            val dt = 1f / subdivisions

            var dirty: DirtyRect = null
            var prev = a
            var acc = residual

            var t = dt
            while (t <= 1f + 1e-4f) {
                val cur = qPoint(a, b, c, t.coerceAtMost(1f))
                val seg = dist(prev, cur)

                var remain = seg
                while (acc + remain >= stepPx && remain > 0f) {
                    val need = stepPx - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)
                    val tt = (t - dt) + dt * f
                    val press = pStart + (pEnd - pStart) * tt

                    // Calculate stroke direction for perpendicular hatching
                    val dx = cur.x - prev.x
                    val dy = cur.y - prev.y
                    val strokeDirection = if (dx != 0f || dy != 0f) {
                        toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    } else 0f

                    dirty = dirty.union(drawHatchStamp(canvas, hit, strokeDirection, radiusFor(press), press))
                    prev = hit
                    remain -= need
                    acc = 0f
                }

                acc += remain
                prev = cur
                t += dt
            }

            residual = acc
            return dirty
        }

        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            residual = 0f
            lastPressure = event.pressure ?: params.pressure
            hatchIndex = 0

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return drawHatchStamp(canvas, event.position, 0f, radiusFor(lastPressure), lastPressure)
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure

            var dirty: DirtyRect = null
            when {
                p0 == null -> {
                    dirty = start(event)
                }
                p1 == null -> {
                    p1 = newP
                    val a = p0!!
                    val m01 = midevent(a, newP)
                    if (lastMid == null) {
                        path.moveTo(a.x, a.y)
                        lastMid = a
                    }
                    path.quadraticBezierTo(a.x, a.y, m01.x, m01.y)
                    dirty = dirty.union(
                        walkQuadratic(
                            canvas = canvas,
                            a = lastMid!!,
                            b = a,
                            c = m01,
                            pStart = lastPressure,
                            pEnd = newPressure
                        )
                    )
                    lastMid = m01
                }
                else -> {
                    val a0 = p0!!
                    val a1 = p1!!
                    val a2 = newP
                    val m1 = midevent(a0, a1)
                    val m2 = midevent(a1, a2)

                    if (lastMid == null) {
                        path.moveTo(m1.x, m1.y)
                        lastMid = m1
                    }

                    path.quadraticBezierTo(a1.x, a1.y, m2.x, m2.y)
                    dirty = dirty.union(
                        walkQuadratic(
                            canvas = canvas,
                            a = lastMid!!,
                            b = a1,
                            c = m2,
                            pStart = lastPressure,
                            pEnd = newPressure
                        )
                    )

                    lastMid = m2
                    p0 = a1
                    p1 = a2
                }
            }

            lastPressure = newPressure
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val endPos = event.position
            val endPressure = event.pressure ?: params.pressure
            val moved = move(event)

            var dirty: DirtyRect = moved
            val a = lastMid
            val ctrl = p1
            if (a != null && ctrl != null) {
                path.quadraticBezierTo(ctrl.x, ctrl.y, ctrl.x, ctrl.y)
                dirty = dirty.union(
                    walkQuadratic(
                        canvas = canvas,
                        a = a,
                        b = ctrl,
                        c = ctrl,
                        pStart = lastPressure,
                        pEnd = endPressure
                    )
                )
            }

            // Final hatch at end position
            val dx = endPos.x - (lastMid?.x ?: endPos.x)
            val dy = endPos.y - (lastMid?.y ?: endPos.y)
            val finalDirection = if (dx != 0f || dy != 0f) {
                toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            } else 0f

            dirty = dirty.union(drawHatchStamp(canvas, endPos, finalDirection, radiusFor(endPressure), endPressure))
            return dirty
        }
    }
}