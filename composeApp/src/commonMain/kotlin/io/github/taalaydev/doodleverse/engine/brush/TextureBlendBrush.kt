package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.TextureBrush
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlin.random.Random

/**
 * Configuration for a texture layer in the blend brush
 */
data class TextureLayer(
    val texture: ImageBitmap,
    val blendMode: BlendMode,
    val opacity: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val colorTint: Color? = null
)

/**
 * Advanced texture blend brush that combines multiple texture layers with different blend modes,
 * opacities, and transformations to create complex artistic effects.
 * Perfect for creating rich, layered textures like fabric, stone, wood grain, or abstract art.
 */
class TextureBlendBrush(
    private val textureLayers: List<TextureLayer>,
    private val layerCycling: Boolean = false, // Whether to cycle through layers over time
    private val pressureAffectsLayers: Boolean = true, // Whether pressure affects layer visibility
    private val velocityAffectsBlending: Boolean = false // Whether stroke speed affects blending
) : TextureBrush() {

    override val texture: ImageBitmap = textureLayers.firstOrNull()?.texture
        ?: throw IllegalArgumentException("At least one texture layer is required")

    override val id = ToolId("texture_blend")
    override val name: String = "Texture Blend"

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = TextureBlendStrokeSession(
        canvas, textureLayers, params, layerCycling,
        pressureAffectsLayers, velocityAffectsBlending
    )

    private class TextureBlendStrokeSession(
        private val canvas: Canvas,
        private val textureLayers: List<TextureLayer>,
        params: BrushParams,
        private val layerCycling: Boolean,
        private val pressureAffectsLayers: Boolean,
        private val velocityAffectsBlending: Boolean
    ) : StrokeSession(params) {

        private val stepPx: Float = params.size * 0.25f
        private val path = Path()

        // Random for variation effects
        private val random: Random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

        // State tracking
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f
        private var strokeTime: Long = 0L
        private var layerCycleIndex: Float = 0f

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float = max(1f, params.size * pressure * 0.5f)

        /**
         * Create a temporary bitmap for blending multiple texture layers
         */
        private fun createBlendedTexture(
            center: Offset,
            size: Float,
            pressure: Float,
            velocity: Float
        ): ImageBitmap? {
            if (textureLayers.isEmpty()) return null

            val bitmapSize = (size * 1.2f).toInt().coerceAtLeast(4)
            val blendBitmap = ImageBitmap(bitmapSize, bitmapSize)
            val blendCanvas = Canvas(blendBitmap)

            // Clear with transparent
            blendCanvas.drawRect(
                Rect(0f, 0f, bitmapSize.toFloat(), bitmapSize.toFloat()),
                Paint().apply { color = Color.Transparent; blendMode = BlendMode.Clear }
            )

            textureLayers.forEachIndexed { index, layer ->
                // Calculate layer visibility based on various factors
                var layerOpacity = layer.opacity

                if (pressureAffectsLayers) {
                    layerOpacity *= pressure
                }

                if (velocityAffectsBlending) {
                    val velocityFactor = (1f - velocity * 0.0001f).coerceIn(0.3f, 1f)
                    layerOpacity *= velocityFactor
                }

                if (layerCycling) {
                    val cyclePhase = (layerCycleIndex + index) % textureLayers.size
                    val cycleFactor = sin(cyclePhase * PI.toFloat() / textureLayers.size).coerceAtLeast(0f)
                    layerOpacity *= cycleFactor
                }

                if (layerOpacity < 0.01f) return@forEachIndexed

                // Create paint for this layer
                val layerPaint = Paint().apply {
                    isAntiAlias = true
                    alpha = layerOpacity
                    blendMode = layer.blendMode

                    // Apply color tint if specified
                    layer.colorTint?.let { tintColor ->
                        colorFilter = ColorFilter.tint(tintColor, BlendMode.Modulate)
                    }
                }

                // Calculate layer transformations
                val layerSize = bitmapSize * layer.scale
                val layerRotation = layer.rotation + (if (layerCycling) layerCycleIndex * 2f else 0f)

                blendCanvas.save()
                blendCanvas.translate(bitmapSize * 0.5f, bitmapSize * 0.5f)
                blendCanvas.rotate(layerRotation)

                val halfSize = layerSize * 0.5f
                val destRect = Rect(-halfSize, -halfSize, halfSize, halfSize)

                blendCanvas.drawImageRect(
                    image = layer.texture,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(layer.texture.width, layer.texture.height),
                    dstOffset = IntOffset(destRect.left.toInt(), destRect.top.toInt()),
                    dstSize = IntSize(destRect.width.toInt(), destRect.height.toInt()),
                    paint = layerPaint
                )

                blendCanvas.restore()
            }

            return blendBitmap
        }

        private fun drawBlendedStamp(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            pressure: Float,
            velocity: Float
        ): DirtyRect {
            val stampSize = radius * 2f

            // Create blended texture for this stamp
            val blendedTexture = createBlendedTexture(center, stampSize, pressure, velocity)
                ?: return Rect(center.x, center.y, center.x, center.y)

            // Create main paint with brush color influence
            val mainPaint = Paint().apply {
                isAntiAlias = true
                blendMode = params.blendMode
                alpha = params.color.alpha * pressure

                // Subtle color influence from brush color
                colorFilter = ColorFilter.tint(
                    params.color.copy(alpha = 0.3f),
                    BlendMode.Overlay
                )
            }

            // Draw the blended texture
            val halfSize = stampSize * 0.5f
            val destRect = Rect(
                center.x - halfSize,
                center.y - halfSize,
                center.x + halfSize,
                center.y + halfSize
            )

            canvas.drawImageRect(
                image = blendedTexture,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(blendedTexture.width, blendedTexture.height),
                dstOffset = IntOffset(destRect.left.toInt(), destRect.top.toInt()),
                dstSize = IntSize(destRect.width.toInt(), destRect.height.toInt()),
                paint = mainPaint
            )

            // Add subtle edge effects for more realistic blending
            if (pressure > 0.6f) {
                val edgePaint = Paint().apply {
                    isAntiAlias = true
                    blendMode = BlendMode.Multiply
                    alpha = (pressure - 0.6f) * 0.3f
                    color = params.color.copy(alpha = alpha)
                }

                // Draw soft edge circle
                canvas.drawCircle(center, radius * 1.1f, edgePaint)
            }

            // Update layer cycling
            if (layerCycling) {
                layerCycleIndex += 0.5f
            }

            return Rect(
                center.x - halfSize - 2f,
                center.y - halfSize - 2f,
                center.x + halfSize + 2f,
                center.y + halfSize + 2f
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
            pEnd: Float,
            velocityStart: Float = 0f,
            velocityEnd: Float = 0f
        ): DirtyRect {
            val approxLen = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            val subdivisions = max(8, ceil(approxLen / 5f).toInt())
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
                    val vel = velocityStart + (velocityEnd - velocityStart) * tt

                    dirty = dirty.union(drawBlendedStamp(canvas, hit, radiusFor(press), press, vel))
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
            strokeTime = Clock.System.now().toEpochMilliseconds()
            layerCycleIndex = 0f

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return drawBlendedStamp(
                canvas, event.position, radiusFor(lastPressure), lastPressure, 0f
            )
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure
            val newVelocity = event.velocity ?: 0f

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
                            pEnd = newPressure,
                            velocityEnd = newVelocity
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
                            pEnd = newPressure,
                            velocityEnd = newVelocity
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
            val endVelocity = event.velocity ?: 0f
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
                        pEnd = endPressure,
                        velocityEnd = endVelocity
                    )
                )
            }

            // Final stamp with enhanced blending
            dirty = dirty.union(drawBlendedStamp(canvas, endPos, radiusFor(endPressure) * 1.1f, endPressure, endVelocity))
            return dirty
        }
    }
}