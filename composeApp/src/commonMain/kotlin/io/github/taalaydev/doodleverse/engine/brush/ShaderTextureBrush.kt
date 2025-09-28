package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.taalaydev.doodleverse.engine.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.TextureBrush
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.util.toRadians
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlin.random.Random

/**
 * Configuration for shader-based texture brush behavior
 */
@Immutable
data class ShaderTextureConfig(
    val shaderMode: ShaderMode = ShaderMode.LINEAR_GRADIENT,
    val gradientColors: List<Color> = listOf(Color.Black, Color.Gray),
    val gradientStops: List<Float>? = null, // null means evenly distributed
    val animateShader: Boolean = false,
    val animationSpeed: Float = 1f,
    val scaleVariation: Float = 0.1f,
    val opacityVariation: Float = 0.15f,
    val rotationMode: RotationMode = RotationMode.STROKE_DIRECTION,
    val pressureAffectsShader: Boolean = true,
    val velocityAffectsShader: Boolean = false,
    val customShaderTileMode: TileMode = TileMode.Clamp,
    val shaderIntensity: Float = 1f, // 0-1, controls shader effect strength
    val blendWithTexture: Boolean = true // blend shader with base texture
)

enum class ShaderMode {
    LINEAR_GRADIENT,
    RADIAL_GRADIENT,
    SWEEP_GRADIENT,
    TEXTURE_SHADER, // Uses the texture as a shader pattern
    NOISE_SHADER,   // Procedural noise-based shader
    ANIMATED_GRADIENT,
    PRESSURE_GRADIENT, // Gradient based on pressure
    VELOCITY_GRADIENT  // Gradient based on velocity
}

/**
 * Advanced shader-based texture brush that combines texture stamping with shader effects.
 * Supports gradients, animated shaders, pressure-sensitive shader effects, and advanced blending.
 */
class ShaderTextureBrush(
    override val texture: ImageBitmap,
    private val additionalTextures: List<ImageBitmap> = emptyList(),
    private val shaderConfig: ShaderTextureConfig = ShaderTextureConfig(),
    private val baseConfig: TextureStampConfig = TextureStampConfig()
) : TextureBrush() {

    override val id = ToolId("shader_texture_brush")
    override val name: String = "Shader Texture Brush"

    private val allTextures = listOf(texture) + additionalTextures

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = ShaderTextureStrokeSession(
        canvas, allTextures, shaderConfig, baseConfig, params
    )
}

private class ShaderTextureStrokeSession(
    private val canvas: Canvas,
    private val textures: List<ImageBitmap>,
    private val shaderConfig: ShaderTextureConfig,
    private val baseConfig: TextureStampConfig,
    params: BrushParams
) : StrokeSession(params) {

    // Stroke tracking
    private var lastPosition: Offset = Offset.Zero
    private var currentPosition: Offset = Offset.Zero
    private var strokeDirection: Float = 0f
    private var strokeStartTime: Long = 0L
    private var strokeLength: Float = 0f

    // Spacing and stamping
    private val spacing: Float = params.size * 0.2f
    private var residualDistance: Float = 0f

    // Random generation
    private val random: Random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

    // Shader cache for performance
    private var cachedShader: Shader? = null
    private var lastShaderParams: ShaderParams? = null

    private data class ShaderParams(
        val mode: ShaderMode,
        val position: Offset,
        val size: Float,
        val pressure: Float,
        val velocity: Float,
        val time: Float,
        val direction: Float
    )

    // Paint for shader rendering
    private val shaderPaint = Paint().apply {
        isAntiAlias = true
        blendMode = params.blendMode
    }

    // Paint for texture base
    private val texturePaint = Paint().apply {
        isAntiAlias = true
        blendMode = BlendMode.SrcOver
        colorFilter = if (params.color != Color.Unspecified && !shaderConfig.blendWithTexture) {
            ColorFilter.tint(params.color, BlendMode.SrcIn)
        } else null
    }

    // --- Shader Generation ---

    private fun createShader(shaderParams: ShaderParams): Shader {
        val size = shaderParams.size
        val position = shaderParams.position
        val pressure = shaderParams.pressure
        val velocity = shaderParams.velocity
        val time = shaderParams.time

        return when (shaderConfig.shaderMode) {
            ShaderMode.LINEAR_GRADIENT -> {
                val angle = if (baseConfig.rotationMode == RotationMode.STROKE_DIRECTION) {
                    toRadians(shaderParams.direction.toDouble())
                } else 0.0

                val start = Offset(
                    position.x - cos(angle).toFloat() * size * 0.5f,
                    position.y - sin(angle).toFloat() * size * 0.5f
                )
                val end = Offset(
                    position.x + cos(angle).toFloat() * size * 0.5f,
                    position.y + sin(angle).toFloat() * size * 0.5f
                )

                val colors = if (shaderConfig.animateShader) {
                    createAnimatedColors(time)
                } else {
                    applyPressureToColors(shaderConfig.gradientColors, pressure)
                }
                
                LinearGradientShader(
                    colors = colors,
                    colorStops = shaderConfig.gradientStops,
                    from = start,
                    to = end,
                    tileMode = shaderConfig.customShaderTileMode
                )
            }

            ShaderMode.RADIAL_GRADIENT -> {
                val radius = size * 0.5f * (if (shaderConfig.pressureAffectsShader) pressure else 1f)
                val colors = applyPressureToColors(shaderConfig.gradientColors, pressure)

                RadialGradientShader(
                    colors = colors,
                    colorStops = shaderConfig.gradientStops,
                    center = position,
                    radius = radius,
                    tileMode = shaderConfig.customShaderTileMode
                )
            }

            ShaderMode.SWEEP_GRADIENT -> {
                val colors = if (shaderConfig.animateShader) {
                    createAnimatedColors(time)
                } else {
                    applyPressureToColors(shaderConfig.gradientColors, pressure)
                }

                SweepGradientShader(
                    colors = colors,
                    colorStops = shaderConfig.gradientStops,
                    center = position
                )
            }

            ShaderMode.TEXTURE_SHADER -> {
                val selectedTexture = selectTexture()
                ImageShader(
                    image = selectedTexture,
                    tileModeX = shaderConfig.customShaderTileMode,
                    tileModeY = shaderConfig.customShaderTileMode
                )
            }

            ShaderMode.NOISE_SHADER -> {
                createNoiseShader(position, size, time)
            }

            ShaderMode.ANIMATED_GRADIENT -> {
                createAnimatedGradient(position, size, time, shaderParams.direction)
            }

            ShaderMode.PRESSURE_GRADIENT -> {
                createPressureGradient(position, size, pressure)
            }

            ShaderMode.VELOCITY_GRADIENT -> {
                createVelocityGradient(position, size, velocity, shaderParams.direction)
            }
        }
    }

    private fun createAnimatedColors(time: Float): List<Color> {
        return shaderConfig.gradientColors.mapIndexed { index, color ->
            val phase = (time * shaderConfig.animationSpeed + index * 0.3f) % (2f * PI.toFloat())
            val intensity = (sin(phase.toDouble()).toFloat() + 1f) * 0.5f
            Color(
                red = (color.red * intensity).coerceIn(0f, 1f),
                green = (color.green * intensity).coerceIn(0f, 1f),
                blue = (color.blue * intensity).coerceIn(0f, 1f),
                alpha = color.alpha
            )
        }
    }

    private fun applyPressureToColors(colors: List<Color>, pressure: Float): List<Color> {
        if (!shaderConfig.pressureAffectsShader) return colors

        val intensity = pressure.coerceIn(0.1f, 1f)
        return colors.map { color ->
            Color(
                red = (color.red * intensity).coerceIn(0f, 1f),
                green = (color.green * intensity).coerceIn(0f, 1f),
                blue = (color.blue * intensity).coerceIn(0f, 1f),
                alpha = color.alpha
            )
        }
    }

    private fun createNoiseShader(position: Offset, size: Float, time: Float): Shader {
        // Create a simple procedural noise pattern using gradients
        val colors = listOf(
            Color.Black.copy(alpha = 0.1f),
            Color.Gray.copy(alpha = 0.3f),
            Color.White.copy(alpha = 0.1f)
        )

        val noiseScale = size * 0.1f
        val animatedOffset = if (shaderConfig.animateShader) {
            Offset(
                cos(time * shaderConfig.animationSpeed).toFloat() * noiseScale,
                sin(time * shaderConfig.animationSpeed * 1.3f).toFloat() * noiseScale
            )
        } else Offset.Zero

        return RadialGradientShader(
            colors = colors,
            center = position + animatedOffset,
            radius = size * 0.3f,
            tileMode = TileMode.Repeated
        )
    }

    private fun createAnimatedGradient(position: Offset, size: Float, time: Float, direction: Float): Shader {
        val phase = time * shaderConfig.animationSpeed
        val animatedAngle = toRadians((direction + phase * 30f).toDouble())

        val start = Offset(
            position.x - cos(animatedAngle).toFloat() * size * 0.5f,
            position.y - sin(animatedAngle).toFloat() * size * 0.5f
        )
        val end = Offset(
            position.x + cos(animatedAngle).toFloat() * size * 0.5f,
            position.y + sin(animatedAngle).toFloat() * size * 0.5f
        )

        return LinearGradientShader(
            colors = createAnimatedColors(time),
            from = start,
            to = end,
            tileMode = shaderConfig.customShaderTileMode
        )
    }

    private fun createPressureGradient(position: Offset, size: Float, pressure: Float): Shader {
        val radius = size * 0.5f * pressure.coerceIn(0.1f, 1f)
        val alpha = pressure.coerceIn(0.1f, 1f)

        val colors = listOf(
            params.color.copy(alpha = alpha),
            params.color.copy(alpha = alpha * 0.5f),
            params.color.copy(alpha = 0f)
        )

        return RadialGradientShader(
            colors = colors,
            center = position,
            radius = radius,
            tileMode = TileMode.Clamp
        )
    }

    private fun createVelocityGradient(position: Offset, size: Float, velocity: Float, direction: Float): Shader {
        val velocityFactor = (velocity * 0.01f).coerceIn(0.1f, 2f)
        val length = size * velocityFactor

        val angle = toRadians(direction.toDouble())
        val start = position
        val end = Offset(
            position.x + cos(angle).toFloat() * length,
            position.y + sin(angle).toFloat() * length
        )

        val alpha = (velocityFactor * 0.5f).coerceIn(0.1f, 1f)
        val colors = listOf(
            params.color.copy(alpha = alpha),
            params.color.copy(alpha = alpha * 0.7f),
            params.color.copy(alpha = 0f)
        )

        return LinearGradientShader(
            colors = colors,
            from = start,
            to = end,
            tileMode = TileMode.Clamp
        )
    }

    private fun selectTexture(): ImageBitmap {
        return if (textures.size == 1) {
            textures[0]
        } else {
            textures[random.nextInt(textures.size)]
        }
    }

    // --- Stamping Function ---

    private fun stampWithShader(
        canvas: Canvas,
        position: Offset,
        pressure: Float,
        velocity: Float,
        timeOffset: Float = 0f
    ): DirtyRect {
        // Calculate base size
        var stampSize = params.size
        if (baseConfig.pressureAffectsSize) {
            stampSize *= pressure.coerceIn(0.1f, 2f)
        }
        if (baseConfig.velocityAffectsSize) {
            val velocityFactor = (1f + velocity * 0.0001f).coerceIn(0.5f, 2f)
            stampSize *= velocityFactor
        }
        if (baseConfig.scaleVariation > 0f) {
            val variation = 1f + (random.nextFloat() - 0.5f) * baseConfig.scaleVariation
            stampSize *= variation.coerceIn(0.3f, 3f)
        }

        // Apply scatter/jitter
        val scatteredPosition = if (baseConfig.scatterRadius > 0f) {
            val scatterAngle = random.nextFloat() * 2f * PI.toFloat()
            val scatterDistance = random.nextFloat() * baseConfig.scatterRadius
            Offset(
                position.x + cos(scatterAngle) * scatterDistance,
                position.y + sin(scatterAngle) * scatterDistance
            )
        } else position

        // Create shader parameters
        val currentTime = (Clock.System.now().toEpochMilliseconds() - strokeStartTime) / 1000f + timeOffset
        val shaderParams = ShaderParams(
            mode = shaderConfig.shaderMode,
            position = scatteredPosition,
            size = stampSize,
            pressure = pressure,
            velocity = velocity,
            time = currentTime,
            direction = strokeDirection
        )

        // Create or reuse shader
        val shader = if (lastShaderParams != shaderParams || cachedShader == null) {
            createShader(shaderParams).also {
                cachedShader = it
                lastShaderParams = shaderParams
            }
        } else {
            cachedShader!!
        }

        // Calculate opacity
        var opacity = params.color.alpha
        if (baseConfig.pressureAffectsOpacity) {
            opacity *= pressure.coerceIn(0.1f, 1f)
        }
        if (baseConfig.opacityVariation > 0f) {
            val variation = 1f + (random.nextFloat() - 0.5f) * baseConfig.opacityVariation
            opacity *= variation.coerceIn(0.1f, 1f)
        }

        // Apply shader intensity
        opacity *= shaderConfig.shaderIntensity

        val halfSize = stampSize * 0.5f
        canvas.save()
        canvas.translate(scatteredPosition.x, scatteredPosition.y)

        // Calculate rotation
        val rotation = when (baseConfig.rotationMode) {
            RotationMode.STROKE_DIRECTION -> strokeDirection
            RotationMode.RANDOM -> random.nextFloat() * 360f
            RotationMode.FIXED_ANGLE -> 0f
            else -> 0f
        }
        if (rotation != 0f) {
            canvas.rotate(rotation)
        }

        // Draw texture base if blending is enabled
        if (shaderConfig.blendWithTexture) {
            val selectedTexture = selectTexture()
            texturePaint.alpha = opacity * 0.5f // Reduce base texture opacity

            canvas.drawImageRect(
                image = selectedTexture,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(selectedTexture.width, selectedTexture.height),
                dstOffset = IntOffset((-halfSize).toInt(), (-halfSize).toInt()),
                dstSize = IntSize(stampSize.toInt(), stampSize.toInt()),
                paint = texturePaint
            )
        }

        // Draw shader effect
        shaderPaint.alpha = opacity
        shaderPaint.shader = shader

        canvas.drawRect(
            Rect(-halfSize, -halfSize, halfSize, halfSize),
            shaderPaint
        )

        canvas.restore()

        // Calculate dirty rectangle with padding
        val padding = stampSize * 0.6f
        return Rect(
            scatteredPosition.x - padding,
            scatteredPosition.y - padding,
            scatteredPosition.x + padding,
            scatteredPosition.y + padding
        )
    }

    private fun updateStrokeDirection(from: Offset, to: Offset) {
        val dx = to.x - from.x
        val dy = to.y - from.y
        if (dx != 0f || dy != 0f) {
            strokeDirection = toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        }
    }

    // --- StrokeSession Implementation ---

    override fun start(event: GestureEvent): DirtyRect {
        lastPosition = event.position
        currentPosition = event.position
        residualDistance = 0f
        strokeDirection = 0f
        strokeLength = 0f
        strokeStartTime = Clock.System.now().toEpochMilliseconds()

        // Clear shader cache for new stroke
        cachedShader = null
        lastShaderParams = null

        val pressure = event.pressure ?: params.pressure
        return stampWithShader(canvas, event.position, pressure, 0f)
    }

    override fun move(event: GestureEvent): DirtyRect {
        val newPosition = event.position
        val pressure = event.pressure ?: params.pressure
        val velocity = event.velocity ?: 0f

        // Update stroke tracking
        updateStrokeDirection(currentPosition, newPosition)
        val segmentLength = currentPosition.distanceTo(newPosition)
        strokeLength += segmentLength

        val totalDistance = residualDistance + segmentLength
        var dirty: DirtyRect = null

        // Stamp at intervals based on spacing
        if (totalDistance >= spacing && spacing > 0f) {
            val numberOfStamps = (totalDistance / spacing).toInt()
            val actualSpacing = totalDistance / numberOfStamps

            for (i in 0 until numberOfStamps) {
                val t = (residualDistance + actualSpacing * (i + 1)) / (residualDistance + segmentLength)
                val stampPosition = currentPosition.lerp(newPosition, t.coerceIn(0f, 1f))

                // Add slight time offset for animation variation
                val timeOffset = i * 0.1f
                val stampDirty = stampWithShader(canvas, stampPosition, pressure, velocity, timeOffset)
                dirty = dirty.union(stampDirty)
            }

            residualDistance = totalDistance - (numberOfStamps * spacing)
        } else {
            residualDistance = totalDistance
        }

        lastPosition = currentPosition
        currentPosition = newPosition
        return dirty
    }

    override fun end(event: GestureEvent): DirtyRect {
        // Process final movement
        val moveDirty = move(event)

        // Always place a final stamp
        val pressure = event.pressure ?: params.pressure
        val velocity = event.velocity ?: 0f
        val endStampDirty = stampWithShader(canvas, event.position, pressure, velocity)

        // Clear cache to free memory
        cachedShader = null
        lastShaderParams = null

        return moveDirty.union(endStampDirty)
    }
}