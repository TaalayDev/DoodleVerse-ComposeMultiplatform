package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.TileMode
import io.github.taalaydev.doodleverse.engine.tool.Brush

object ShaderBrushFactory {
    /**
     * Creates a brush with an animated rainbow gradient effect
     */
    fun createRainbowBrush(texture: ImageBitmap): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.ANIMATED_GRADIENT,
                gradientColors = listOf(
                    Color.Red,
                    Color(1f, 0.5f, 0f, 1f), // Orange
                    Color.Yellow,
                    Color.Green,
                    Color.Blue,
                    Color(0.5f, 0f, 1f, 1f), // Indigo
                    Color.Magenta
                ),
                animateShader = true,
                animationSpeed = 2f,
                pressureAffectsShader = true,
                shaderIntensity = 0.8f,
                blendWithTexture = true
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.STROKE_DIRECTION,
                scaleVariation = 0.1f,
                opacityVariation = 0.1f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true
            )
        )
    }

    /**
     * Creates a pressure-sensitive glow brush with radial gradients
     */
    fun createGlowBrush(texture: ImageBitmap, glowColor: Color = Color.Cyan): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.PRESSURE_GRADIENT,
                gradientColors = listOf(
                    glowColor.copy(alpha = 1f),
                    glowColor.copy(alpha = 0.7f),
                    glowColor.copy(alpha = 0.3f),
                    glowColor.copy(alpha = 0f)
                ),
                pressureAffectsShader = true,
                shaderIntensity = 1f,
                blendWithTexture = true,
                customShaderTileMode = TileMode.Clamp
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.NONE,
                scaleVariation = 0.2f,
                opacityVariation = 0.1f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                scatterRadius = 2f
            )
        )
    }

    /**
     * Creates a velocity-based trail brush that creates motion blur effects
     */
    fun createTrailBrush(texture: ImageBitmap, trailColor: Color = Color.Blue): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.VELOCITY_GRADIENT,
                gradientColors = listOf(
                    trailColor.copy(alpha = 0.8f),
                    trailColor.copy(alpha = 0.4f),
                    trailColor.copy(alpha = 0.1f),
                    trailColor.copy(alpha = 0f)
                ),
                velocityAffectsShader = true,
                pressureAffectsShader = true,
                shaderIntensity = 0.9f,
                blendWithTexture = true
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.STROKE_DIRECTION,
                scaleVariation = 0.15f,
                opacityVariation = 0.2f,
                pressureAffectsSize = true,
                velocityAffectsSize = true,
                // spacing = 0.3f // Closer spacing for smoother trails
            )
        )
    }

    /**
     * Creates a metallic brush with sweep gradients
     */
    fun createMetallicBrush(texture: ImageBitmap, metalColor: Color = Color(0.7f, 0.7f, 0.8f, 1f)): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.SWEEP_GRADIENT,
                gradientColors = listOf(
                    metalColor.copy(alpha = 1f),
                    Color.White.copy(alpha = 0.8f),
                    metalColor.copy(alpha = 0.6f),
                    Color.Black.copy(alpha = 0.3f),
                    metalColor.copy(alpha = 1f)
                ),
                gradientStops = listOf(0f, 0.3f, 0.5f, 0.8f, 1f),
                pressureAffectsShader = true,
                shaderIntensity = 0.7f,
                blendWithTexture = true
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.RANDOM,
                rotationRandomness = 45f,
                scaleVariation = 0.1f,
                opacityVariation = 0.05f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true
            )
        )
    }

    /**
     * Creates a fire/flame brush with animated gradient
     */
    fun createFireBrush(texture: ImageBitmap): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.ANIMATED_GRADIENT,
                gradientColors = listOf(
                    Color.Yellow.copy(alpha = 1f),
                    Color(1f, 0.5f, 0f, 1f), // Orange
                    Color.Red.copy(alpha = 0.8f),
                    Color(0.5f, 0f, 0f, 0.6f), // Dark red
                    Color.Black.copy(alpha = 0.2f)
                ),
                animateShader = true,
                animationSpeed = 3f,
                pressureAffectsShader = true,
                shaderIntensity = 1f,
                blendWithTexture = true,
                customShaderTileMode = TileMode.Clamp
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.RANDOM,
                rotationRandomness = 30f,
                scaleVariation = 0.3f,
                opacityVariation = 0.2f,
                scatterRadius = 5f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                flipRandomly = true
            )
        )
    }

    /**
     * Creates a water/liquid brush with flowing gradients
     */
    fun createWaterBrush(texture: ImageBitmap): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.ANIMATED_GRADIENT,
                gradientColors = listOf(
                    Color(0f, 0.5f, 1f, 0.8f), // Light blue
                    Color(0f, 0.3f, 0.8f, 0.6f), // Medium blue
                    Color(0f, 0.2f, 0.6f, 0.4f), // Dark blue
                    Color(0f, 0.1f, 0.3f, 0.2f)  // Very dark blue
                ),
                animateShader = true,
                animationSpeed = 1.5f,
                pressureAffectsShader = true,
                velocityAffectsShader = true,
                shaderIntensity = 0.7f,
                blendWithTexture = true,
                customShaderTileMode = TileMode.Mirror
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.FOLLOW_PATH,
                scaleVariation = 0.2f,
                opacityVariation = 0.3f,
                scatterRadius = 3f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                velocityAffectsSize = true
            )
        )
    }

    /**
     * Creates a noise/texture brush for organic effects
     */
    fun createNoiseBrush(texture: ImageBitmap, baseColor: Color = Color.Gray): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.NOISE_SHADER,
                gradientColors = listOf(
                    baseColor.copy(alpha = 0.8f),
                    baseColor.copy(alpha = 0.4f),
                    baseColor.copy(alpha = 0.1f)
                ),
                animateShader = true,
                animationSpeed = 0.5f,
                pressureAffectsShader = true,
                shaderIntensity = 0.6f,
                blendWithTexture = true,
                customShaderTileMode = TileMode.Repeated
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.RANDOM,
                rotationRandomness = 180f,
                scaleVariation = 0.4f,
                opacityVariation = 0.3f,
                scatterRadius = 8f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                flipRandomly = true
            )
        )
    }

    /**
     * Creates a crystal/glass brush with prismatic effects
     */
    fun createCrystalBrush(texture: ImageBitmap): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.SWEEP_GRADIENT,
                gradientColors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color.Cyan.copy(alpha = 0.7f),
                    Color.Magenta.copy(alpha = 0.7f),
                    Color.Yellow.copy(alpha = 0.7f),
                    Color.White.copy(alpha = 0.9f)
                ),
                gradientStops = listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
                animateShader = true,
                animationSpeed = 1f,
                pressureAffectsShader = true,
                shaderIntensity = 0.8f,
                blendWithTexture = true
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.FIXED_ANGLE,
                scaleVariation = 0.05f,
                opacityVariation = 0.1f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true
            )
        )
    }

    /**
     * Creates a simple directional shader brush
     */
    fun createDirectionalShaderBrush(
        texture: ImageBitmap,
        color1: Color = Color.Black,
        color2: Color = Color.Gray
    ): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = ShaderMode.LINEAR_GRADIENT,
                gradientColors = listOf(color1, color2),
                pressureAffectsShader = true,
                shaderIntensity = 0.7f,
                blendWithTexture = true
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.STROKE_DIRECTION,
                scaleVariation = 0.1f,
                opacityVariation = 0.1f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true
            )
        )
    }

    /**
     * Creates a custom shader brush with user-defined parameters
     */
    fun createCustomShaderBrush(
        texture: ImageBitmap,
        shaderMode: ShaderMode,
        colors: List<Color>,
        animate: Boolean = false,
        intensity: Float = 1f
    ): ShaderTextureBrush {
        return ShaderTextureBrush(
            texture = texture,
            shaderConfig = ShaderTextureConfig(
                shaderMode = shaderMode,
                gradientColors = colors,
                animateShader = animate,
                animationSpeed = if (animate) 1f else 0f,
                pressureAffectsShader = true,
                shaderIntensity = intensity.coerceIn(0f, 1f),
                blendWithTexture = true
            ),
            baseConfig = TextureStampConfig(
                rotationMode = RotationMode.STROKE_DIRECTION,
                scaleVariation = 0.1f,
                opacityVariation = 0.1f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true
            )
        )
    }
}


/**
 * Extension functions for easy brush customization
 */
fun ShaderTextureBrush.withShaderIntensity(intensity: Float): ShaderTextureBrush {
    // Note: This creates a new instance since data classes are immutable
    return ShaderTextureBrush(
        texture = this.texture,
        additionalTextures = emptyList(), // Would need to expose this in the main class
        shaderConfig = ShaderTextureConfig(
            shaderMode = ShaderMode.LINEAR_GRADIENT, // Would need to expose current config
            shaderIntensity = intensity.coerceIn(0f, 1f)
        )
    )
}

fun ShaderTextureBrush.withAnimation(speed: Float): ShaderTextureBrush {
    return ShaderTextureBrush(
        texture = this.texture,
        shaderConfig = ShaderTextureConfig(
            shaderMode = ShaderMode.ANIMATED_GRADIENT,
            animateShader = true,
            animationSpeed = speed
        )
    )
}