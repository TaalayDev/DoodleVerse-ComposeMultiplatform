package io.github.taalaydev.doodleverse.engine.brush.shader

/**
 * Pre-built shader brushes using procedural effects
 */
object ShaderBrushPresets {

    /**
     * Creates a pulsating glow brush
     */
    fun createPulsatingGlowBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.PulsatingGlow,
        brushName = "Pulsating Glow"
    )

    /**
     * Creates a noise texture brush
     */
    fun createNoiseTextureBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.NoiseTexture,
        brushName = "Noise Texture"
    )

    // ---- Additions to object ShaderBrushPresets ----

    fun createInkBleedBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.InkBleed,
        brushName = "Ink Bleed"
    )

    fun createWatercolorBloomBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.WatercolorBloom,
        brushName = "Watercolor Bloom"
    )

    fun createChalkGrainBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.ChalkGrain,
        brushName = "Chalk Grain"
    )

    fun createSparkleBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.Sparkle,
        brushName = "Sparkle"
    )

    fun createFireEmberBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.FireEmber,
        brushName = "Fire Ember"
    )

    fun createHalftoneBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.Halftone,
        brushName = "Halftone"
    )

    fun createPixelMosaicBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.PixelMosaic,
        brushName = "Pixel Mosaic"
    )

    /** Useful combos */
    fun createGlowSparkleBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.Combined(
            listOf(
                ProceduralEffect.PulsatingGlow,
                ProceduralEffect.Halftone
            )
        ),
        brushName = "Glow + Halftone"
    )

    /**
     * Creates a wet ink brush that bleeds slightly.
     */
    fun createWetInkBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.WetInk,
        brushName = "Wet Ink"
    )

    /**
     * Creates a brush with a crayon-like texture.
     */
    fun createCrayonBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.Crayon,
        brushName = "Crayon"
    )

    /**
     * Creates a brush that draws a dashed line.
     */
    fun createDashedLineBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.DashedLine,
        brushName = "Dashed Line"
    )

    fun createSketchyBrush() = ShaderBrush(
        proceduralEffect = ProceduralEffect.Sketchy,
        brushName = "Sketchy"
    )

    fun createGlitterDust(density: Float = 1f, seed: Int = 13) = ShaderBrush(proceduralEffect = ProceduralEffect.GlitterDust(density, seed), brushName = "Glitter Dust")
    fun createBokehLights() = ShaderBrush(proceduralEffect = ProceduralEffect.BokehLights, brushName = "Bokeh Lights")
    fun createSmokeWisps() = ShaderBrush(proceduralEffect = ProceduralEffect.SmokeWisps, brushName = "Smoke Wisps")
    fun createRainbowPrism() = ShaderBrush(proceduralEffect = ProceduralEffect.RainbowPrism, brushName = "Rainbow Prism")
    fun createHoloGrid() = ShaderBrush(proceduralEffect = ProceduralEffect.HoloGrid, brushName = "Holo Grid")
    fun createLavaLampBlob() = ShaderBrush(proceduralEffect = ProceduralEffect.LavaLampBlob, brushName = "Lava Lamp Blob")

    fun createSmoke() = ShaderBrush(
        proceduralEffect = ProceduralEffect.Smoke,
        brushName = "Smoke"
    )

    fun createDryBrushStreaks() = ShaderBrush(proceduralEffect = ProceduralEffect.DryBrushStreaks, brushName = "Dry Brush Streaks")
    fun createCrossHatch() = ShaderBrush(proceduralEffect = ProceduralEffect.CrossHatch, brushName = "Cross‑Hatch")
    fun createStippleShade() = ShaderBrush(proceduralEffect = ProceduralEffect.StippleShade, brushName = "Stipple Shade")
    fun createGraphiteScribble() = ShaderBrush(proceduralEffect = ProceduralEffect.GraphiteScribble, brushName = "Graphite Scribble")
    fun createLeafScatter() = ShaderBrush(proceduralEffect = ProceduralEffect.LeafScatter, brushName = "Leaf Scatter")
    fun createSprayPaint() = ShaderBrush(proceduralEffect = ProceduralEffect.SprayPaint, brushName = "Spray Paint")

    /** Easy registry for your UI */
    fun allPresets(): List<ShaderBrush> = listOf(
        createPulsatingGlowBrush(),
        createNoiseTextureBrush(),
        createInkBleedBrush(),
        createWatercolorBloomBrush(),
        createChalkGrainBrush(),
        createSparkleBrush(),
        createFireEmberBrush(),
        createHalftoneBrush(),
        createPixelMosaicBrush(),
        createGlowSparkleBrush(),
        createWetInkBrush(),
        createCrayonBrush(),
        createDashedLineBrush(),
        createSketchyBrush(),
        createGlitterDust(),
        createBokehLights(),
        createSmokeWisps(),
        createRainbowPrism(),
        createHoloGrid(),
        createLavaLampBlob(),

        createSmoke(),

        createDryBrushStreaks(),
        createCrossHatch(),
        createStippleShade(),
        createGraphiteScribble(),
        createLeafScatter(),
        createSprayPaint(),
    )

}