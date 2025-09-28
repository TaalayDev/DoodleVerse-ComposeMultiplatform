package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Configuration for advanced watercolor brush behavior
 */
@Immutable
data class WatercolorConfig(
    val wetness: Float = 0.8f, // 0-1, how wet the paint is
    val flowRadius: Float = 20f, // How far paint can flow
    val pigmentDensity: Float = 0.7f, // How concentrated the pigment is
    val bleedStrength: Float = 0.4f, // How much paint bleeds into wet areas
    val dryingTime: Long = 5000L, // Time in ms for paint to "dry"
    val paperTexture: ImageBitmap? = null, // Optional paper texture
    val enableWetOnWet: Boolean = true, // Enable wet-on-wet blending
    val colorSeparation: Float = 0.2f, // Simulate color separation in watercolors
    val granulation: Float = 0.3f, // Granulation effect (pigment settling)
    val transparency: Float = 0.6f, // Base transparency
    val layering: Boolean = true, // Allow color layering and mixing
)

/**
 * Represents a wet area on the canvas where paint can flow
 */
data class WetArea(
    val position: Offset,
    val radius: Float,
    val wetness: Float,
    val color: Color,
    val timestamp: Long,
    var isActive: Boolean = true
)

/**
 * Represents flowing paint particles for simulation
 */
data class PaintParticle(
    var position: Offset,
    var velocity: Offset,
    val color: Color,
    var life: Float, // 0-1, when it reaches 0, particle dies
    var size: Float,
    val pigmentDensity: Float
)