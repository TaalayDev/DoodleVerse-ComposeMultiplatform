package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.TextureBrush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlin.random.Random

/**
 * Watercolor brush that simulates the natural behavior of watercolor paint with paper texture:
 * - Translucent, layered strokes with color accumulation
 * - Wet-on-wet blending effects
 * - Edge darkening from pigment pooling
 * - Natural paper texture through texture bitmap
 * - Flow variation based on pressure
 * - Granulation and texture effects
 */
class WatercolorBrush(
    override val texture: ImageBitmap,        // Paper texture for realistic grain
    private val wetness: Float = 0.7f,        // 0-1, how wet the brush is (affects blending)
    private val pigmentLoad: Float = 0.5f,    // 0-1, amount of pigment (affects opacity)
    private val textureStrength: Float = 0.4f, // 0-1, how much the texture shows through
    private val edgeIntensity: Float = 1.3f   // Edge darkening multiplier
) : TextureBrush() {

    override val id = ToolId("watercolor_textured")
    override val name: String = "Watercolor"

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = WatercolorStrokeSession(
        canvas, texture, params, wetness, pigmentLoad, textureStrength, edgeIntensity
    )

    private class WatercolorStrokeSession(
        private val canvas: Canvas,
        private val texture: ImageBitmap,
        params: BrushParams,
        private val wetness: Float,
        private val pigmentLoad: Float,
        private val textureStrength: Float,
        private val edgeIntensity: Float
    ) : StrokeSession(params) {

        private val random = Random(Clock.System.now().toEpochMilliseconds())

        // Base paint for main wash
        private val washPaint = Paint().apply {
            isAntiAlias = true
            blendMode = BlendMode.Multiply  // Natural color mixing
        }

        // Paint for edge effects
        private val edgePaint = Paint().apply {
            isAntiAlias = true
            blendMode = BlendMode.Multiply
        }

        // Paint for texture overlay
        private val texturePaint = Paint().apply {
            isAntiAlias = true
            colorFilter = ColorFilter.tint(params.color, BlendMode.SrcIn)
            blendMode = BlendMode.Multiply
        }

        // Paint for granulation spots
        private val grainPaint = Paint().apply {
            isAntiAlias = true
            blendMode = BlendMode.Multiply
        }

        // Dynamic opacity based on wetness and pigment
        private val baseOpacity = (params.color.alpha * pigmentLoad * (1f - wetness * 0.5f)).coerceIn(0.05f, 0.4f)

        // Spacing control - watercolor has more overlap for blending
        private val stepPx: Float = params.size * 0.15f

        // Path tracking for smooth strokes
        private val path = Path()
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f

        // Track recent positions for flow simulation
        private val recentPositions = mutableListOf<Offset>()
        private val maxRecentPositions = 10

        // Texture tiling offset for variation
        private var textureOffset = Offset(
            random.nextFloat() * texture.width,
            random.nextFloat() * texture.height
        )

        // Helper functions
        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float {
            // Watercolor spreads more with less pressure (more water)
            val inversePressure = 1f - pressure * 0.3f
            return max(1f, params.size * inversePressure * 0.5f)
        }

        private fun drawWatercolorStamp(canvas: Canvas, center: Offset, radius: Float, pressure: Float): Rect {
            // Track position for flow effects
            recentPositions.add(center)
            if (recentPositions.size > maxRecentPositions) {
                recentPositions.removeAt(0)
            }

            // Calculate flow direction from recent positions
            val flowOffset = if (recentPositions.size >= 2) {
                val flow = recentPositions.last() - recentPositions[recentPositions.size - 2]
                flow * wetness * 0.2f
            } else {
                Offset.Zero
            }

            val actualCenter = center + flowOffset
            val maxRadius = radius * (1f + wetness * 0.3f)

            // 1. Main wash - multiple translucent layers
            val washLayers = 3
            for (i in 0 until washLayers) {
                val layerRadius = radius * (1f - i * 0.1f) + random.nextFloat() * radius * 0.1f
                val layerOpacity = baseOpacity * (1f - i * 0.2f)

                // Add random offset for natural variation
                val offset = Offset(
                    (random.nextFloat() - 0.5f) * radius * 0.2f,
                    (random.nextFloat() - 0.5f) * radius * 0.2f
                )

                washPaint.color = params.color.copy(alpha = layerOpacity)

                // Draw with soft edges using radial gradient
                val shader = RadialGradientShader(
                    center = actualCenter + offset,
                    radius = layerRadius,
                    colors = listOf(
                        params.color.copy(alpha = layerOpacity),
                        params.color.copy(alpha = layerOpacity * 0.3f),
                        Color.Transparent
                    ),
                    colorStops = listOf(0f, 0.7f, 1f)
                )
                washPaint.shader = shader

                canvas.drawCircle(actualCenter + offset, layerRadius * 1.2f, washPaint)
                washPaint.shader = null
            }

            // 2. Paper texture overlay - multiple small texture stamps for grain
            if (textureStrength > 0.1f) {
                val textureStamps = 5
                for (i in 0 until textureStamps) {
                    val stampRadius = radius * (0.3f + random.nextFloat() * 0.7f)
                    val stampAngle = random.nextFloat() * 2f * PI
                    val stampDistance = random.nextFloat() * radius * 0.8f
                    val stampOffset = Offset(
                        cos(stampAngle.toFloat()) * stampDistance,
                        sin(stampAngle.toFloat()) * stampDistance
                    )
                    val stampCenter = actualCenter + stampOffset

                    // Calculate texture source region with tiling
                    val srcX = ((stampCenter.x + textureOffset.x) % texture.width).toInt().coerceIn(0, texture.width - 1)
                    val srcY = ((stampCenter.y + textureOffset.y) % texture.height).toInt().coerceIn(0, texture.height - 1)
                    val srcSize = (stampRadius * 2f).toInt().coerceAtLeast(1)

                    val srcLeft = (srcX - srcSize/2).coerceIn(0, texture.width - srcSize)
                    val srcTop = (srcY - srcSize/2).coerceIn(0, texture.height - srcSize)

                    texturePaint.alpha = baseOpacity * textureStrength * (0.5f + random.nextFloat() * 0.5f)

                    // Apply texture with radial fade
                    canvas.save()
                    val clipPath = Path().apply {
                        addOval(Rect(
                            stampCenter.x - stampRadius,
                            stampCenter.y - stampRadius,
                            stampCenter.x + stampRadius,
                            stampCenter.y + stampRadius
                        ))
                    }
                    canvas.clipPath(clipPath)

                    val dstRect = Rect(
                        stampCenter.x - stampRadius,
                        stampCenter.y - stampRadius,
                        stampCenter.x + stampRadius,
                        stampCenter.y + stampRadius
                    )

                    canvas.drawImageRect(
                        image = texture,
                        srcOffset = IntOffset(srcLeft, srcTop),
                        srcSize = IntSize(srcSize.coerceAtMost(texture.width - srcLeft),
                            srcSize.coerceAtMost(texture.height - srcTop)),
                        dstOffset = IntOffset(dstRect.left.toInt(), dstRect.top.toInt()),
                        dstSize = IntSize(dstRect.width.toInt(), dstRect.height.toInt()),
                        paint = texturePaint
                    )
                    canvas.restore()
                }
            }

            // 3. Edge darkening - pigment accumulation
            if (edgeIntensity > 1f) {
                val edgeRadius = radius * 0.95f
                val edgeOpacity = baseOpacity * edgeIntensity * 0.3f

                val edgeShader = RadialGradientShader(
                    center = actualCenter,
                    radius = edgeRadius,
                    colors = listOf(
                        Color.Transparent,
                        params.color.copy(alpha = edgeOpacity * 0.5f),
                        params.color.copy(alpha = edgeOpacity)
                    ),
                    colorStops = listOf(0f, 0.6f, 0.95f)
                )
                edgePaint.shader = edgeShader
                canvas.drawCircle(actualCenter, edgeRadius, edgePaint)
                edgePaint.shader = null
            }

            // 4. Granulation effects - small pigment spots
            val grainCount = (8 * textureStrength * pressure).toInt()
            for (j in 0 until grainCount) {
                val grainRadius = radius * random.nextFloat() * 0.8f
                val angle = random.nextFloat() * 2f * PI
                val grainOffset = Offset(
                    cos(angle.toFloat()) * grainRadius,
                    sin(angle.toFloat()) * grainRadius
                )

                val grainSize = radius * 0.05f * random.nextFloat()
                val grainOpacity = baseOpacity * 0.8f * random.nextFloat()

                grainPaint.color = params.color.copy(alpha = grainOpacity)
                canvas.drawCircle(actualCenter + grainOffset, grainSize, grainPaint)
            }

            // 5. Wet bleeding effect - occasional larger, very translucent stamps
            if (wetness > 0.5f && random.nextFloat() < wetness * 0.2f) {
                val bleedRadius = radius * (1f + random.nextFloat() * wetness)
                val bleedOpacity = baseOpacity * 0.1f

                val bleedShader = RadialGradientShader(
                    center = actualCenter,
                    radius = bleedRadius,
                    colors = listOf(
                        params.color.copy(alpha = bleedOpacity),
                        Color.Transparent
                    ),
                    colorStops = listOf(0f, 1f)
                )
                washPaint.shader = bleedShader
                canvas.drawCircle(actualCenter, bleedRadius, washPaint)
                washPaint.shader = null

                // Add subtle texture to bleed area
                if (textureStrength > 0.2f) {
                    val bleedTexOpacity = bleedOpacity * textureStrength * 0.5f
                    texturePaint.alpha = bleedTexOpacity

                    val bleedDst = Rect(
                        actualCenter.x - bleedRadius,
                        actualCenter.y - bleedRadius,
                        actualCenter.x + bleedRadius,
                        actualCenter.y + bleedRadius
                    )

                    canvas.save()
                    val bleedClip = Path().apply {
                        addOval(bleedDst)
                    }
                    canvas.clipPath(bleedClip)

                    // Draw texture with tiling for large bleeds
                    canvas.drawImageRect(
                        image = texture,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(texture.width, texture.height),
                        dstOffset = IntOffset(bleedDst.left.toInt(), bleedDst.top.toInt()),
                        dstSize = IntSize(bleedDst.width.toInt(), bleedDst.height.toInt()),
                        paint = texturePaint
                    )
                    canvas.restore()
                }
            }

            return Rect(
                actualCenter.x - maxRadius,
                actualCenter.y - maxRadius,
                actualCenter.x + maxRadius,
                actualCenter.y + maxRadius
            )
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
            val subdivisions = max(8, ceil(approxLen / 4f).toInt())
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

                    // Slightly vary texture offset as we draw
                    textureOffset = textureOffset + Offset(
                        random.nextFloat() * 2f - 1f,
                        random.nextFloat() * 2f - 1f
                    )

                    dirty = dirty.union(drawWatercolorStamp(canvas, hit, radiusFor(press), press))

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
            recentPositions.clear()

            // Randomize texture offset for each stroke
            textureOffset = Offset(
                random.nextFloat() * texture.width,
                random.nextFloat() * texture.height
            )

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            // Initial wet spot
            return drawWatercolorStamp(canvas, event.position, radiusFor(lastPressure), lastPressure)
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

            // Final pooling effect - slightly larger, darker spot with extra texture
            val poolRadius = radiusFor(endPressure) * 1.1f
            dirty = dirty.union(drawWatercolorStamp(canvas, endPos, poolRadius, endPressure))

            return dirty
        }
    }
}