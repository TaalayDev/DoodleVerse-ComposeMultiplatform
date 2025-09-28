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
 * Types of distortion effects available
 */
enum class DistortionType {
    WAVE,           // Sine wave distortion
    RIPPLE,         // Ripple effect from center
    TWIST,          // Rotational twist distortion
    PINCH,          // Pinch/bulge effect
    NOISE,          // Random noise distortion
    STRETCH,        // Directional stretching
    FLOW            // Flow field distortion
}

/**
 * Configuration for distortion effects
 */
data class DistortionConfig(
    val type: DistortionType,
    val intensity: Float = 0.5f,        // 0-1, strength of distortion
    val frequency: Float = 1f,           // Frequency for wave-based distortions
    val animated: Boolean = false,       // Whether distortion changes over time
    val pressureAffects: Boolean = true  // Whether pressure affects distortion intensity
)

/**
 * Advanced texture distortion brush that applies various distortion effects to textures
 * while drawing. Creates dynamic, fluid effects perfect for abstract art, water effects,
 * smoke, flames, or surreal artistic styles.
 */
class TextureDistortionBrush(
    override val texture: ImageBitmap,
    private val distortionConfig: DistortionConfig,
    private val enableColorShift: Boolean = false, // Color shifting based on distortion
    private val trailLength: Float = 0.3f // How much previous stamps influence current ones
) : TextureBrush() {

    override val id = ToolId("texture_distortion")
    override val name: String = "Texture Distortion"

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = TextureDistortionStrokeSession(
        canvas, texture, params, distortionConfig, enableColorShift, trailLength
    )

    private class TextureDistortionStrokeSession(
        private val canvas: Canvas,
        private val sourceTexture: ImageBitmap,
        params: BrushParams,
        private val distortionConfig: DistortionConfig,
        private val enableColorShift: Boolean,
        private val trailLength: Float
    ) : StrokeSession(params) {

        private val paint = Paint().apply {
            isAntiAlias = true
            blendMode = params.blendMode
        }

        private val stepPx: Float = params.size * 0.2f
        private val path = Path()

        // Random for distortion variations
        private val random: Random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

        // State tracking
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var residual: Float = 0f
        private var animationTime: Float = 0f
        private var strokeVelocity: Offset = Offset.Zero

        // Trail system for flow effects
        private val trailPoints = mutableListOf<TrailPoint>()

        private data class TrailPoint(
            val position: Offset,
            val timestamp: Float,
            val intensity: Float
        )

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float = max(2f, params.size * pressure * 0.5f)

        /**
         * Apply distortion to a point based on the configured distortion type
         */
        private fun applyDistortion(
            point: Offset,
            center: Offset,
            intensity: Float,
            time: Float
        ): Offset {
            val dx = point.x - center.x
            val dy = point.y - center.y
            val distance = hypot(dx, dy)

            if (distance < 0.1f) return point

            return when (distortionConfig.type) {
                DistortionType.WAVE -> {
                    val waveOffset = sin((dx * 0.1f + time) * distortionConfig.frequency) * intensity
                    Offset(point.x, point.y + waveOffset)
                }

                DistortionType.RIPPLE -> {
                    val angle = atan2(dy, dx)
                    val ripple = sin(distance * 0.1f * distortionConfig.frequency + time) * intensity
                    Offset(
                        point.x + cos(angle) * ripple,
                        point.y + sin(angle) * ripple
                    )
                }

                DistortionType.TWIST -> {
                    val angle = atan2(dy, dx)
                    val twist = (distance * 0.01f * distortionConfig.frequency + time) * intensity
                    val newAngle = angle + twist
                    Offset(
                        center.x + cos(newAngle) * distance,
                        center.y + sin(newAngle) * distance
                    )
                }

                DistortionType.PINCH -> {
                    val scale = 1f + (intensity * 0.3f * sin(time + distance * 0.1f))
                    Offset(
                        center.x + dx * scale,
                        center.y + dy * scale
                    )
                }

                DistortionType.NOISE -> {
                    val noiseX = (random.nextFloat() - 0.5f) * intensity * 2f
                    val noiseY = (random.nextFloat() - 0.5f) * intensity * 2f
                    Offset(point.x + noiseX, point.y + noiseY)
                }

                DistortionType.STRETCH -> {
                    val stretchDir = strokeVelocity.getDistance()
                    if (stretchDir > 0.1f) {
                        val normalizedVel = Offset(strokeVelocity.x / stretchDir, strokeVelocity.y / stretchDir)
                        val stretch = intensity * 0.5f
                        Offset(
                            point.x + normalizedVel.x * dx * stretch,
                            point.y + normalizedVel.y * dy * stretch
                        )
                    } else point
                }

                DistortionType.FLOW -> {
                    var flowX = 0f
                    var flowY = 0f

                    // Calculate flow based on trail points
                    trailPoints.forEach { trailPoint ->
                        val trailDist = point.distanceTo(trailPoint.position)
                        if (trailDist < params.size) {
                            val influence = (1f - trailDist / params.size) * trailPoint.intensity
                            val trailAngle = atan2(
                                trailPoint.position.y - point.y,
                                trailPoint.position.x - point.x
                            )
                            flowX += cos(trailAngle) * influence * intensity
                            flowY += sin(trailAngle) * influence * intensity
                        }
                    }

                    Offset(point.x + flowX, point.y + flowY)
                }
            }
        }

        /**
         * Create a distorted version of the texture
         */
        private fun createDistortedTexture(
            center: Offset,
            size: Float,
            pressure: Float
        ): ImageBitmap {
            val textureSize = (size * 1.5f).toInt().coerceIn(4, 256) // Limit size for performance
            val distortedBitmap = ImageBitmap(textureSize, textureSize)
            val distortedCanvas = Canvas(distortedBitmap)

            // Clear with transparent
            distortedCanvas.drawRect(
                Rect(0f, 0f, textureSize.toFloat(), textureSize.toFloat()),
                Paint().apply {
                    color = Color.Transparent
                    blendMode = BlendMode.Clear
                }
            )

            val intensity = distortionConfig.intensity * (if (distortionConfig.pressureAffects) pressure else 1f)
            val currentTime = if (distortionConfig.animated) animationTime else 0f

            // Create distortion by sampling the source texture with distorted coordinates
            val gridSize = 8 // Lower resolution for performance
            val stepSize = textureSize.toFloat() / gridSize

            for (y in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    val localPos = Offset(x * stepSize, y * stepSize)
                    val centerOffset = Offset(textureSize * 0.5f, textureSize * 0.5f)

                    // Apply distortion to this grid point
                    val distortedPos = applyDistortion(localPos, centerOffset, intensity, currentTime)

                    // Calculate source coordinates in texture space
                    val srcX = ((distortedPos.x / textureSize) * sourceTexture.width).toInt()
                        .coerceIn(0, sourceTexture.width - 1)
                    val srcY = ((distortedPos.y / textureSize) * sourceTexture.height).toInt()
                        .coerceIn(0, sourceTexture.height - 1)

                    // Sample from source and draw to distorted texture
                    val srcRect = Rect(srcX.toFloat(), srcY.toFloat(), (srcX + 1).toFloat(), (srcY + 1).toFloat())
                    val dstRect = Rect(localPos.x, localPos.y, localPos.x + stepSize, localPos.y + stepSize)

                    val samplePaint = Paint().apply {
                        isAntiAlias = false // Faster for grid sampling

                        // Apply color shift based on distortion if enabled
                        if (enableColorShift) {
                            val shiftAmount = intensity * 0.3f
                            val hueShift = sin(currentTime + localPos.x * 0.01f) * shiftAmount
                            colorFilter = ColorFilter.tint(
                                Color.hsv(
                                    (hueShift * 360f).coerceIn(0f, 360f),
                                    0.2f, 1f,
                                    0.3f
                                ),
                                BlendMode.Overlay
                            )
                        }
                    }

                    distortedCanvas.drawImageRect(
                        image = sourceTexture,
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(1, 1),
                        dstOffset = IntOffset(localPos.x.toInt(), localPos.y.toInt()),
                        dstSize = IntSize(stepSize.toInt(), stepSize.toInt()),
                        paint = samplePaint
                    )
                }
            }

            return distortedBitmap
        }

        private fun drawDistortedStamp(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            pressure: Float
        ): DirtyRect {
            val stampSize = radius * 2f

            // Create distorted texture
            val distortedTexture = createDistortedTexture(center, stampSize, pressure)

            // Create main paint
            val mainPaint = paint.copy().apply {
                alpha = params.color.alpha * pressure
                colorFilter = ColorFilter.tint(params.color, BlendMode.Modulate)
            }

            // Draw the distorted texture
            val halfSize = stampSize * 0.5f
            canvas.drawImageRect(
                image = distortedTexture,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(distortedTexture.width, distortedTexture.height),
                dstOffset = IntOffset((center.x - halfSize).toInt(), (center.y - halfSize).toInt()),
                dstSize = IntSize(stampSize.toInt(), stampSize.toInt()),
                paint = mainPaint
            )

            // Update trail for flow effects
            val currentTime = Clock.System.now().toEpochMilliseconds().toFloat() * 0.001f
            trailPoints.add(TrailPoint(center, currentTime, pressure))

            // Clean up old trail points
            trailPoints.removeAll { currentTime - it.timestamp > trailLength }

            // Update animation time
            if (distortionConfig.animated) {
                animationTime += 0.1f
            }

            return Rect(
                center.x - halfSize - 5f,
                center.y - halfSize - 5f,
                center.x + halfSize + 5f,
                center.y + halfSize + 5f
            )
        }

        private fun updateStrokeVelocity(from: Offset, to: Offset) {
            val dx = to.x - from.x
            val dy = to.y - from.y
            strokeVelocity = Offset(dx, dy)
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
            val subdivisions = max(6, ceil(approxLen / 6f).toInt())
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

                    updateStrokeVelocity(prev, cur)
                    dirty = dirty.union(drawDistortedStamp(canvas, hit, radiusFor(press), press))
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
            animationTime = 0f
            strokeVelocity = Offset.Zero
            trailPoints.clear()

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return drawDistortedStamp(canvas, event.position, radiusFor(lastPressure), lastPressure)
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

            // Final enhanced stamp
            dirty = dirty.union(drawDistortedStamp(canvas, endPos, radiusFor(endPressure) * 1.2f, endPressure))
            return dirty
        }
    }
}