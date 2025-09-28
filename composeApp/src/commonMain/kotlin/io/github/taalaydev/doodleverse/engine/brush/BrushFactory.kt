package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.abstract_gradient
import doodleverse.composeapp.generated.resources.brush2
import doodleverse.composeapp.generated.resources.brush3
import doodleverse.composeapp.generated.resources.brush5
import doodleverse.composeapp.generated.resources.brush6
import doodleverse.composeapp.generated.resources.brush7
import doodleverse.composeapp.generated.resources.brush_11
import doodleverse.composeapp.generated.resources.brush_12
import doodleverse.composeapp.generated.resources.brush_circle
import doodleverse.composeapp.generated.resources.pencil
import doodleverse.composeapp.generated.resources.brush_1
import doodleverse.composeapp.generated.resources.brush_13
import doodleverse.composeapp.generated.resources.brush_14
import doodleverse.composeapp.generated.resources.brush_15
import doodleverse.composeapp.generated.resources.chinese_brush
import doodleverse.composeapp.generated.resources.confeti
import doodleverse.composeapp.generated.resources.grunge_paintbrush
import doodleverse.composeapp.generated.resources.grunge_paintbrush_1
import doodleverse.composeapp.generated.resources.irregular_freeform
import doodleverse.composeapp.generated.resources.luxury_abstract_fluid
import doodleverse.composeapp.generated.resources.marker_2
import doodleverse.composeapp.generated.resources.spakle
import doodleverse.composeapp.generated.resources.stamp_airbrush
import doodleverse.composeapp.generated.resources.stamp_pencil
import doodleverse.composeapp.generated.resources.watercolor
import doodleverse.composeapp.generated.resources.watercolor_1
import doodleverse.composeapp.generated.resources.watercolor_2
import doodleverse.composeapp.generated.resources.stain_watercolor
import io.github.taalaydev.doodleverse.engine.brush.TextureStampBrushFactory.generateCanvasTexture
import io.github.taalaydev.doodleverse.engine.brush.TextureStampBrushFactory.generateNoiseTexture
import io.github.taalaydev.doodleverse.engine.brush.TextureStampBrushFactory.generatePaperTexture
import io.github.taalaydev.doodleverse.engine.brush.shader.ShaderBrushPresets
import io.github.taalaydev.doodleverse.engine.tool.Brush
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import org.jetbrains.compose.resources.imageResource

object BrushFactory {
    val pen = PenBrush()
    val eraser = EraserBrush()

    /**
     * Creates a classic calligraphy brush with 45-degree angle
     */
    fun createClassicCalligraphyBrush(id: String, name: String): CalligraphyBrush {
        return CalligraphyBrush(
            nibAngleDegrees = 45f,
            nibAspect = 3f,
            id = ToolId(id),
            name = name
        )
    }

    /**
     * Creates a italic calligraphy brush with steeper angle
     */
    fun createItalicCalligraphyBrush(id: String, name: String): CalligraphyBrush {
        return CalligraphyBrush(
            nibAngleDegrees = 60f,
            nibAspect = 4f,
            id = ToolId(id),
            name = name
        )
    }

    /**
     * Creates a broad calligraphy brush for headers
     */
    fun createBroadCalligraphyBrush(id: String, name: String): CalligraphyBrush {
        return CalligraphyBrush(
            nibAngleDegrees = 30f,
            nibAspect = 5f,
            id = ToolId(id),
            name = name
        )
    }

    /**
     * Creates a fine calligraphy brush for detailed work
     */
    fun createFineCalligraphyBrush(id: String, name: String): CalligraphyBrush {
        return CalligraphyBrush(
            nibAngleDegrees = 45f,
            nibAspect = 2f,
            id = ToolId(id),
            name = name
        )
    }

    // ======= NEON GLOW BRUSHES =======

    /**
     * Creates a bright neon glow brush
     */
    fun createBrightNeonBrush(): NeonGlowBrush {
        return NeonGlowBrush(
            glowIntensity = 1f,
            glowSize = 4f,
            coreIntensity = 1.5f
        )
    }

    /**
     * Creates a subtle glow brush for soft lighting effects
     */
    fun createSoftGlowBrush(id: String, name: String): NeonGlowBrush {
        return NeonGlowBrush(
            glowIntensity = 0.6f,
            glowSize = 2f,
            coreIntensity = 1.1f,
            id = ToolId(id),
            name = name
        )
    }

    /**
     * Creates an intense neon brush for vibrant effects
     */
    fun createIntenseNeonBrush(): NeonGlowBrush {
        return NeonGlowBrush(
            glowIntensity = 1.2f,
            glowSize = 5f,
            coreIntensity = 2f
        )
    }

    // ======= CHALK BRUSHES =======

    /**
     * Creates a standard chalk brush
     */
    fun createChalkBrush(id: String, name: String): ChalkBrush {
        return ChalkBrush(
            textureIntensity = 0.7f,
            grainSize = 2f,
            coverage = 0.8f,
            id = ToolId(id),
            name = name
        )
    }

    /**
     * Creates a soft chalk brush for blending
     */
    fun createSoftChalkBrush(): ChalkBrush {
        return ChalkBrush(
            textureIntensity = 0.5f,
            grainSize = 3f,
            coverage = 0.6f
        )
    }

    /**
     * Creates a rough chalk brush for textured effects
     */
    fun createRoughChalkBrush(): ChalkBrush {
        return ChalkBrush(
            textureIntensity = 0.9f,
            grainSize = 1.5f,
            coverage = 0.9f
        )
    }

    /**
     * Creates a dusty chalk brush for atmospheric effects
     */
    fun createDustyChalkBrush(): ChalkBrush {
        return ChalkBrush(
            textureIntensity = 0.8f,
            grainSize = 2.5f,
            coverage = 0.5f
        )
    }

    // ======= SPRAY BRUSHES =======

    /**
     * Creates a fine airbrush for detailed work
     */
    fun createFineAirbrush(id: String, name: String): SprayBrush {
        return SprayBrush(
            particleCount = 20,
            particleSize = 1f,
            falloffPower = 3f,
            opacityVariation = 0.2f,
            id = ToolId(id),
            name = name
        )
    }

    /**
     * Creates a medium spray brush for general use
     */
    fun createMediumSprayBrush(id: String, name: String): SprayBrush {
        return SprayBrush(
            particleCount = 30,
            particleSize = 1.5f,
            falloffPower = 2f,
            opacityVariation = 0.3f,
            id = ToolId(id),
            name = name
        )
    }

    /**
     * Creates a coarse spray brush for texture effects
     */
    fun createCoarseSprayBrush(id: String, name: String): SprayBrush {
        return SprayBrush(
            particleCount = 40,
            particleSize = 2f,
            falloffPower = 1.5f,
            opacityVariation = 0.4f,
            id = ToolId(id),
            name = name
        )
    }

    /**
     * Creates a concentrated spray brush for precise control
     */
    fun createConcentratedSprayBrush(id: String, name: String): SprayBrush {
        return SprayBrush(
            particleCount = 25,
            particleSize = 1.2f,
            falloffPower = 4f,
            opacityVariation = 0.15f,
            id = ToolId(id),
            name = name
        )
    }

    // ======= HATCHING BRUSHES =======

    /**
     * Creates a fine hatching brush for detailed shading
     */
    fun createFineHatchingBrush(): HatchingBrush {
        return HatchingBrush(
            hatchSpacing = 2f,
            hatchLength = 6f,
            hatchAngle = 45f,
            doubleCross = false,
            lineVariation = 0.1f
        )
    }

    /**
     * Creates a cross-hatching brush for deep shadows
     */
    fun createCrossHatchingBrush(): HatchingBrush {
        return HatchingBrush(
            hatchSpacing = 3f,
            hatchLength = 8f,
            hatchAngle = 45f,
            doubleCross = true,
            lineVariation = 0.2f
        )
    }

    /**
     * Creates a diagonal hatching brush
     */
    fun createDiagonalHatchingBrush(): HatchingBrush {
        return HatchingBrush(
            hatchSpacing = 2.5f,
            hatchLength = 10f,
            hatchAngle = 30f,
            doubleCross = false,
            lineVariation = 0.15f
        )
    }

    /**
     * Creates a vertical hatching brush for architectural drawings
     */
    fun createVerticalHatchingBrush(): HatchingBrush {
        return HatchingBrush(
            hatchSpacing = 3f,
            hatchLength = 12f,
            hatchAngle = 90f,
            doubleCross = false,
            lineVariation = 0.1f
        )
    }

    /**
     * Creates a loose artistic hatching brush with more variation
     */
    fun createArtisticHatchingBrush(): HatchingBrush {
        return HatchingBrush(
            hatchSpacing = 4f,
            hatchLength = 15f,
            hatchAngle = 45f,
            doubleCross = true,
            lineVariation = 0.4f
        )
    }

    /**
     * Creates a collection of all available artistic brushes for easy access
     */
    fun artisticBrushCollection(): List<Brush> {
        return listOf(
            createClassicCalligraphyBrush("classic_calligraphy", "Classic Calligraphy"),
            createItalicCalligraphyBrush("italic_calligraphy", "Italic Calligraphy"),
            createBroadCalligraphyBrush("broad_calligraphy", "Broad Calligraphy"),
            createFineCalligraphyBrush("fine_calligraphy", "Fine Calligraphy"),

            // Neon Glow
            createSoftGlowBrush("soft_glow", "Soft Glow"),

            // Chalk
            createChalkBrush("chalk", "Chalk"),

            // Spray
            createFineAirbrush("fine_airbrush", "Fine Airbrush"),
            createMediumSprayBrush("medium_spray", "Medium Spray"),
            createCoarseSprayBrush("coarse_spray", "Coarse Spray"),
            createConcentratedSprayBrush("concentrated_spray", "Concentrated Spray"),
        )
    }

    @Composable
    fun brushCollection(): List<Brush> {
        val pencilTexture = imageResource(Res.drawable.stamp_pencil)
        val airBrushTexture = imageResource(Res.drawable.stamp_airbrush)
        val starBrush = remember {
            ProceduralTextureGenerator.generateStarTexture(
                size = 96,
                points = 5
            )
        }

        val brush2 = imageResource(Res.drawable.brush2)
        val brush3 = imageResource(Res.drawable.brush3)
        val brush5 = imageResource(Res.drawable.brush5)
        val brush6 = imageResource(Res.drawable.brush6)
        val brush7 = imageResource(Res.drawable.brush7)
        val brushCircle = imageResource(Res.drawable.brush_circle)
        val stampPencil = imageResource(Res.drawable.pencil)

        val markerTexture = imageResource(Res.drawable.marker_2)
        val brush13 = imageResource(Res.drawable.grunge_paintbrush)

        val chineseBrush = imageResource(Res.drawable.chinese_brush)

        val confeti = imageResource(Res.drawable.confeti)
        val irregularFreeform = imageResource(Res.drawable.irregular_freeform)

        val stainWatercolor = imageResource(Res.drawable.stain_watercolor)
        val brush17 = imageResource(Res.drawable.brush_15)

        val circularBrush = remember { ProceduralTextureGenerator.generateCircularBrushTexture(100) }

        val shaderPresets = ShaderBrushPresets.allPresets()

        return listOf(
            PenBrush(),
            TextureSoftPencilBrush(
                pencilTexture,
                id = ToolId("texture_soft_pencil"),
                name = "Sketchy Pencil"
            ),
            TexturePenBrush(
                stampPencil,
                id = ToolId("texture_pen_pencil_1"),
                name = "Soft Pencil"
            ),
            TexturePenBrush(
                pencilTexture,
                id = ToolId("texture_pen_pencil_2"),
                name = "Hard Pencil"
            ),
            TexturePenBrush(
                brush13,
                id = ToolId("texture_pen_brush13"),
                name = "Rough Brush"
            ),
            TexturePenBrush(
                airBrushTexture,
                id = ToolId("texture_pen_airbrush"),
                name = "Wet Paintbrush"
            ),

            TexturePenBrush(
                confeti,
                id = ToolId("texture_pen_confeti"),
                name = "Soft Noise Stamp"
            ),
            TexturePenBrush(
                irregularFreeform,
                id = ToolId("texture_pen_irregular_freeform"),
                name = "Rough Pencil"
            ),

            TexturePenBrush(
                brush17,
                id = ToolId("texture_pen_brush17"),
                name = "Texture Pen Brush brush17"
            ),

            TextureWatercolorBrushFactory.createControlledTexturedBrush(
                airBrushTexture,
                id = "controlled_stamp_airbrush",
                name = "Controlled Watercolor Airbrush"
            ),

            GlitterBrush(
                listOf(
                    brush2,
                    brush3,
                    brush5,
                    brush6,
                    brush7,
                    starBrush,
                    circularBrush,
                    stampPencil
                )
            ),

            RainbowBrush(),
            TextureStampBrushFactory.createHairBrush(
                circularBrush,
                id = "circular_brush",
                name = "Circular Brush"
            ),

            TextureWatercolorBrushFactory.createArtisticBrush(
                brush7,
                listOf(circularBrush, airBrushTexture),
                id = "artistic_texture_watercolor",
                name = "Artistic Texture Watercolor"
            ),

            MarkerBrush(),
            SprayBrush(),

            TextureStampBrushFactory.textureRibbon(
                starBrush,
                overlap = 0.12f,
                tint = Color(0xFF2B2B2B),
            ),
        ) + shaderPresets;
    }

    @Composable
    fun allBrushes(): List<Brush> {
        val brushCollection = brushCollection()
        return remember {
            brushCollection + artisticBrushCollection()
        }
    }
}


object TextureStampBrushFactory {

    /**
     * Creates a leaf/foliage stamp brush for natural drawing
     */
    fun createFoliageBrush(
        leafTextures: List<ImageBitmap>,
        id: String = "texture_stamp",
        name: String = "Texture Stamp"
    ): TextureStampBrush {
        return TextureStampBrush(
            texture = leafTextures.first(),
            additionalTextures = leafTextures.drop(1),
            id = ToolId(id),
            name = name,
            config = TextureStampConfig(
                rotationMode = RotationMode.RANDOM,
                scaleVariation = 0.4f,
                opacityVariation = 0.3f,
                scatterRadius = 8f,
                flipRandomly = true,
                rotationRandomness = 180f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                velocityAffectsSize = true
            )
        )
    }

    /**
     * Creates a hair/fur stamp brush
     */
    fun createHairBrush(hairTexture: ImageBitmap, id: String, name: String): TextureStampBrush {
        return TextureStampBrush(
            texture = hairTexture,
            id = ToolId(id),
            name = name,
            config = TextureStampConfig(
                rotationMode = RotationMode.FOLLOW_PATH,
                scaleVariation = 0.15f,
                opacityVariation = 0.2f,
                scatterRadius = 3f,
                flipRandomly = true,
                rotationRandomness = 15f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                velocityAffectsSize = false
            )
        )
    }

    /**
     * Creates a cloud/smoke stamp brush
     */
    fun createCloudBrush(cloudTexture: ImageBitmap): TextureStampBrush {
        return TextureStampBrush(
            texture = cloudTexture,
            config = TextureStampConfig(
                rotationMode = RotationMode.RANDOM,
                scaleVariation = 0.5f,
                opacityVariation = 0.4f,
                scatterRadius = 12f,
                flipRandomly = true,
                rotationRandomness = 360f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                velocityAffectsSize = true
            )
        )
    }

    /**
     * Creates a fabric/pattern stamp brush for texturing
     */
    fun createFabricBrush(fabricTexture: ImageBitmap): TextureStampBrush {
        return TextureStampBrush(
            texture = fabricTexture,
            config = TextureStampConfig(
                rotationMode = RotationMode.STROKE_DIRECTION,
                scaleVariation = 0.05f,
                opacityVariation = 0.1f,
                scatterRadius = 0f,
                flipRandomly = false,
                rotationRandomness = 5f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                velocityAffectsSize = false
            )
        )
    }


    /**
     * Create a simple directional stamp brush
     */
    fun createDirectionalStampBrush(texture: ImageBitmap): TextureStampBrush {
        return TextureStampBrush(
            texture = texture,
            config = TextureStampConfig(
                rotationMode = RotationMode.STROKE_DIRECTION,
                scaleVariation = 0.1f,
                opacityVariation = 0.1f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true
            )
        )
    }

    /**
     * Create a scattered organic stamp brush
     */
    fun createOrganicStampBrush(texture: ImageBitmap): TextureStampBrush {
        return TextureStampBrush(
            texture = texture,
            config = TextureStampConfig(
                rotationMode = RotationMode.RANDOM,
                scaleVariation = 0.3f,
                opacityVariation = 0.2f,
                scatterRadius = 15f,
                flipRandomly = true,
                rotationRandomness = 45f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true
            )
        )
    }

    /**
     * Create a precise stamp brush for technical drawing
     */
    fun createPrecisionStampBrush(texture: ImageBitmap, fixedAngle: Float = 0f, id: String, name: String): TextureStampBrush {
        return TextureStampBrush(
            texture = texture,
            config = TextureStampConfig(
                rotationMode = RotationMode.FIXED_ANGLE,
                scaleVariation = 0f,
                opacityVariation = 0f,
                scatterRadius = 0f,
                flipRandomly = false,
                rotationRandomness = 0f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = false
            ),
            fixedAngle = fixedAngle,
            id = ToolId(id),
            name = name
        )
    }

    fun createAnimatedStampBrush(
        textures: List<ImageBitmap>,
        animationSpeed: Float = 1f
    ): TextureStampBrush {
        // For animated effects, cycle through textures based on position along stroke
        return TextureStampBrush(
            texture = textures.first(),
            additionalTextures = textures.drop(1),
            config = TextureStampConfig(
                rotationMode = RotationMode.STROKE_DIRECTION,
                scaleVariation = 0.1f,
                opacityVariation = 0.1f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true
            )
        )
    }

    fun createPressureSensitiveDottedBrush(dotTexture: ImageBitmap): TextureStampBrush {
        return TextureStampBrush(
            texture = dotTexture,
            config = TextureStampConfig(
                rotationMode = RotationMode.NONE,
                scaleVariation = 0f,
                opacityVariation = 0f,
                scatterRadius = 0f,
                pressureAffectsSize = true,
                pressureAffectsOpacity = true,
                velocityAffectsSize = false
            )
        )
    }

    fun textureRibbon(
        texture: ImageBitmap,
        tint: Color = Color.Unspecified,
        overlap: Float = 0.15f,
        id: String = "texture_ribbon",
        name: String = "Texture Ribbon"
    ) = TextureRibbonBrush(texture, tint, overlap, id = ToolId(id), name = name)

    fun textureBristle(
        texture: ImageBitmap,
        strands: Int = 12,
        spread: Float = 0.6f,
        lengthFactor: Float = 1.1f,
        randomAngle: Float = 6f,
        tint: Color = Color.Unspecified
    ) = TextureBristleBrush(texture, strands, spread, lengthFactor, randomAngle, tint)

    fun textureSpray(
        texture: ImageBitmap,
        radius: Float,
        ratePerSecond: Float = 260f,
        tint: Color = Color.Unspecified
    ) = TextureSprayBrush(texture, radius, ratePerSecond, tint)

    /**
     * Creates an oil paint brush with canvas texture overlay
     */
    fun createOilPaintBrush(canvasTexture: ImageBitmap): TexturePaintingBrush {
        return TexturePaintingBrush(
            texture = canvasTexture,
            textureScale = 0.8f,
            textureOpacity = 0.6f,
            paintFlow = 0.9f,
            textureRotation = 0f,
            colorMixing = true
        )
    }

    /**
     * Creates an acrylic paint brush with paper texture
     */
    fun createAcrylicPaintBrush(paperTexture: ImageBitmap): TexturePaintingBrush {
        return TexturePaintingBrush(
            texture = paperTexture,
            textureScale = 1.2f,
            textureOpacity = 0.5f,
            paintFlow = 0.7f,
            textureRotation = 15f,
            colorMixing = true
        )
    }

    /**
     * Creates a watercolor brush with rough paper texture
     */
    fun createWatercolorTextureBrush(roughPaperTexture: ImageBitmap): TexturePaintingBrush {
        return TexturePaintingBrush(
            texture = roughPaperTexture,
            textureScale = 1.5f,
            textureOpacity = 0.8f,
            paintFlow = 0.6f,
            textureRotation = 0f,
            colorMixing = false // Watercolor doesn't mix as much with paper
        )
    }

    /**
     * Creates an impasto brush for thick paint effects
     */
    fun createImpastoBrush(canvasTexture: ImageBitmap): TexturePaintingBrush {
        return TexturePaintingBrush(
            texture = canvasTexture,
            textureScale = 0.6f,
            textureOpacity = 0.9f,
            paintFlow = 1.2f, // Heavy paint flow
            textureRotation = 0f,
            colorMixing = true
        )
    }

    // ======= TEXTURE SCATTER BRUSHES =======

    /**
     * Creates a foliage scatter brush for natural elements
     */
    fun createFoliageScatterBrush(leafTextures: List<ImageBitmap>): TextureScatterBrush {
        return TextureScatterBrush(
            texture = leafTextures.first(),
            additionalTextures = leafTextures.drop(1),
            scatterRadius = 20f,
            density = 0.7f,
            sizeVariation = 0.5f,
            rotationVariation = 360f,
            opacityVariation = 0.4f,
            colorTinting = true,
            followStroke = false
        )
    }

    /**
     * Creates a star scatter brush for magical effects
     */
    fun createStarScatterBrush(starTextures: List<ImageBitmap>): TextureScatterBrush {
        return TextureScatterBrush(
            texture = starTextures.first(),
            additionalTextures = starTextures.drop(1),
            scatterRadius = 25f,
            density = 0.5f,
            sizeVariation = 0.7f,
            rotationVariation = 180f,
            opacityVariation = 0.6f,
            colorTinting = true,
            followStroke = false
        )
    }

    /**
     * Creates a snow scatter brush
     */
    fun createSnowScatterBrush(snowflakeTextures: List<ImageBitmap>): TextureScatterBrush {
        return TextureScatterBrush(
            texture = snowflakeTextures.first(),
            additionalTextures = snowflakeTextures.drop(1),
            scatterRadius = 30f,
            density = 0.8f,
            sizeVariation = 0.3f,
            rotationVariation = 360f,
            opacityVariation = 0.5f,
            colorTinting = false,
            followStroke = false
        )
    }

    /**
     * Creates a directional scatter brush that follows stroke direction
     */
    fun createDirectionalScatterBrush(texture: ImageBitmap): TextureScatterBrush {
        return TextureScatterBrush(
            texture = texture,
            scatterRadius = 8f,
            density = 0.9f,
            sizeVariation = 0.2f,
            rotationVariation = 30f,
            opacityVariation = 0.2f,
            colorTinting = true,
            followStroke = true
        )
    }

    // ======= TEXTURE BLEND BRUSHES =======

    /**
     * Creates a wood grain brush with multiple texture layers
     */
    fun createWoodGrainBrush(
        baseWoodTexture: ImageBitmap,
        grainTexture: ImageBitmap,
        knotTexture: ImageBitmap
    ): TextureBlendBrush {
        val layers = listOf(
            TextureLayer(baseWoodTexture, BlendMode.SrcOver, 0.8f, 1f, 0f),
            TextureLayer(grainTexture, BlendMode.Multiply, 0.6f, 1.2f, 0f),
            TextureLayer(knotTexture, BlendMode.Overlay, 0.3f, 0.8f, 45f)
        )
        return TextureBlendBrush(layers, false, true, false)
    }

    /**
     * Creates a fabric texture brush
     */
    fun createFabricBrush(
        weavePrimaryTexture: ImageBitmap,
        weaveSecondaryTexture: ImageBitmap
    ): TextureBlendBrush {
        val layers = listOf(
            TextureLayer(weavePrimaryTexture, BlendMode.SrcOver, 0.7f, 1f, 0f),
            TextureLayer(weaveSecondaryTexture, BlendMode.Multiply, 0.5f, 1f, 90f)
        )
        return TextureBlendBrush(layers, false, true, false)
    }

    /**
     * Creates a stone texture brush with layered effects
     */
    fun createStoneBrush(
        baseStoneTexture: ImageBitmap,
        roughnessTexture: ImageBitmap,
        cracksTexture: ImageBitmap
    ): TextureBlendBrush {
        val layers = listOf(
            TextureLayer(baseStoneTexture, BlendMode.SrcOver, 0.9f, 1f, 0f),
            TextureLayer(roughnessTexture, BlendMode.Overlay, 0.4f, 1.5f, 0f),
            TextureLayer(cracksTexture, BlendMode.Multiply, 0.3f, 1f, 0f)
        )
        return TextureBlendBrush(layers, false, true, false)
    }

    /**
     * Creates an animated blend brush with cycling layers
     */
    fun createAnimatedBlendBrush(textures: List<ImageBitmap>): TextureBlendBrush {
        val layers = textures.mapIndexed { index, texture ->
            TextureLayer(
                texture = texture,
                blendMode = when (index % 3) {
                    0 -> BlendMode.SrcOver
                    1 -> BlendMode.Multiply
                    else -> BlendMode.Overlay
                },
                opacity = 0.6f,
                scale = 1f + index * 0.1f,
                rotation = index * 45f
            )
        }
        return TextureBlendBrush(layers, true, true, true)
    }

    // ======= TEXTURE DISTORTION BRUSHES =======

    /**
     * Creates a water ripple brush with wave distortion
     */
    fun createWaterRippleBrush(waterTexture: ImageBitmap): TextureDistortionBrush {
        return TextureDistortionBrush(
            texture = waterTexture,
            distortionConfig = DistortionConfig(
                type = DistortionType.RIPPLE,
                intensity = 0.4f,
                frequency = 2f,
                animated = true,
                pressureAffects = true
            ),
            enableColorShift = true,
            trailLength = 0.5f
        )
    }

    /**
     * Creates a flame brush with twist and flow distortion
     */
    fun createFlameBrush(flameTexture: ImageBitmap): TextureDistortionBrush {
        return TextureDistortionBrush(
            texture = flameTexture,
            distortionConfig = DistortionConfig(
                type = DistortionType.TWIST,
                intensity = 0.6f,
                frequency = 1.5f,
                animated = true,
                pressureAffects = true
            ),
            enableColorShift = true,
            trailLength = 0.3f
        )
    }

    /**
     * Creates a smoke brush with flow distortion
     */
    fun createSmokeBrush(smokeTexture: ImageBitmap): TextureDistortionBrush {
        return TextureDistortionBrush(
            texture = smokeTexture,
            distortionConfig = DistortionConfig(
                type = DistortionType.FLOW,
                intensity = 0.5f,
                frequency = 1f,
                animated = false,
                pressureAffects = true
            ),
            enableColorShift = false,
            trailLength = 0.8f
        )
    }

    /**
     * Creates a glass distortion brush
     */
    fun createGlassBrush(glassTexture: ImageBitmap): TextureDistortionBrush {
        return TextureDistortionBrush(
            texture = glassTexture,
            distortionConfig = DistortionConfig(
                type = DistortionType.PINCH,
                intensity = 0.3f,
                frequency = 1f,
                animated = false,
                pressureAffects = true
            ),
            enableColorShift = true,
            trailLength = 0.1f
        )
    }

    /**
     * Creates an abstract wave brush
     */
    fun createAbstractWaveBrush(abstractTexture: ImageBitmap): TextureDistortionBrush {
        return TextureDistortionBrush(
            texture = abstractTexture,
            distortionConfig = DistortionConfig(
                type = DistortionType.WAVE,
                intensity = 0.7f,
                frequency = 3f,
                animated = true,
                pressureAffects = false
            ),
            enableColorShift = true,
            trailLength = 0.2f
        )
    }

    // ======= PROCEDURAL TEXTURE GENERATORS =======

    /**
     * Generate a noise texture for various effects
     */
    fun generateNoiseTexture(size: Int = 64, octaves: Int = 4): ImageBitmap {
        val bitmap = ImageBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        // Generate Perlin-like noise
        for (y in 0 until size) {
            for (x in 0 until size) {
                var noise = 0f
                var amplitude = 1f
                var frequency = 0.05f

                repeat(octaves) {
                    noise += amplitude * (kotlin.math.sin(x * frequency) * kotlin.math.cos(y * frequency)).toFloat()
                    amplitude *= 0.5f
                    frequency *= 2f
                }

                val gray = ((noise + 1f) * 0.5f).coerceIn(0f, 1f)
                paint.color = Color(gray, gray, gray, 1f)
                canvas.drawRect(Rect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat()), paint)
            }
        }

        return bitmap
    }

    /**
     * Generate a paper texture
     */
    fun generatePaperTexture(size: Int = 128, roughness: Float = 0.3f): ImageBitmap {
        val bitmap = ImageBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        // Create paper-like texture with fibers and roughness
        for (y in 0 until size) {
            for (x in 0 until size) {
                val baseGray = 0.95f
                val fiber = kotlin.math.sin(x * 0.3f) * kotlin.math.cos(y * 0.1f) * roughness * 0.1f
                val roughnessNoise = (kotlin.random.Random.nextFloat() - 0.5f) * roughness * 0.1f
                val gray = (baseGray + fiber + roughnessNoise).coerceIn(0.8f, 1f)

                paint.color = Color(gray, gray, gray, 1f)
                canvas.drawRect(Rect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat()), paint)
            }
        }

        return bitmap
    }

    /**
     * Generate a canvas texture
     */
    fun generateCanvasTexture(size: Int = 64, weaveStrength: Float = 0.2f): ImageBitmap {
        val bitmap = ImageBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        for (y in 0 until size) {
            for (x in 0 until size) {
                val baseGray = 0.9f
                val weaveX = kotlin.math.sin(x * 0.5f) * weaveStrength * 0.1f
                val weaveY = kotlin.math.cos(y * 0.5f) * weaveStrength * 0.1f
                val gray = (baseGray + weaveX + weaveY).coerceIn(0.7f, 1f)

                paint.color = Color(gray, gray, gray, 1f)
                canvas.drawRect(Rect(x.toFloat(), y.toFloat(), (x + 1).toFloat(), (y + 1).toFloat()), paint)
            }
        }

        return bitmap
    }
}

