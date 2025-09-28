package io.github.taalaydev.doodleverse.engine.brush

import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.math.*
import kotlin.random.Random

/**
 * Configuration for texture watercolor brush behavior
 */
@Immutable
data class TextureWatercolorConfig(
    // Watercolor physics
    val wetness: Float = 0.7f,
    val flowRadius: Float = 25f,
    val pigmentDensity: Float = 0.6f,
    val bleedStrength: Float = 0.5f,
    val dryingTime: Long = 4000L,
    val transparency: Float = 0.7f,
    val enableWetOnWet: Boolean = true,

    // Texture behavior
    val textureMode: TextureMode = TextureMode.FLOW_PAINTING,
    val textureScale: Float = 1f,
    val textureRotation: Float = 0f, // Fixed rotation in degrees
    val textureRandomRotation: Float = 15f, // Random rotation range
    val textureOpacity: Float = 0.8f,
    val textureBlendMode: BlendMode = BlendMode.Multiply,
    val textureSpacing: Float = 0.6f, // Spacing between texture applications (0-1)

    // Advanced texture effects
    val textureDistortion: Float = 0.2f, // How much to distort texture based on wetness
    val pigmentSeparation: Boolean = true, // Separate texture colors based on wetness
    val textureGranulation: Float = 0.3f, // Granular texture effects
    val paperInteraction: Float = 0.4f, // How texture interacts with paper

    // Performance settings
    val maxTextureSize: Int = 256, // Maximum texture size for performance
    val enableTextureCache: Boolean = true,
    val simulationQuality: SimulationQuality = SimulationQuality.HIGH
)

enum class TextureMode {
    STAMP, // Discrete texture stamps
    FLOW_PAINTING, // Continuous texture painting with flow
    BLEND_MODE, // Texture blends with existing paint
    PIGMENT_SCATTER // Texture acts as pigment particles
}

enum class SimulationQuality {
    LOW, MEDIUM, HIGH, ULTRA
}

/**
 * Represents a texture element in the watercolor simulation
 */
private data class TextureParticle(
    var position: Offset,
    var velocity: Offset,
    val originalTexture: ImageBitmap,
    var currentTexture: ImageBitmap,
    var rotation: Float,
    var scale: Float,
    var opacity: Float,
    var wetness: Float,
    var life: Float, // 0-1
    val color: Color,
    var isFlowing: Boolean = false
)

/**
 * Texture watercolor brush that combines realistic watercolor physics
 * with texture-based painting for artistic effects
 */
class TextureWatercolorBrush(
    override val texture: ImageBitmap,
    private val additionalTextures: List<ImageBitmap> = emptyList(),
    private val config: TextureWatercolorConfig = TextureWatercolorConfig(),
    override val id: ToolId = ToolId("texture_watercolor"),
    override val name: String = "Texture Watercolor"
) : TextureBrush() {

    // All available textures
    private val allTextures = listOf(texture) + additionalTextures

    override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession =
        TextureWatercolorStrokeSession(canvas, allTextures, config, params)
}

private class TextureWatercolorStrokeSession(
    private val canvas: Canvas,
    private val textures: List<ImageBitmap>,
    private val config: TextureWatercolorConfig,
    params: BrushParams
) : StrokeSession(params) {

    // Path and smoothing
    private val path = Path()
    private var p0: Offset? = null
    private var p1: Offset? = null
    private var lastMid: Offset? = null

    // Watercolor simulation state
    private val wetAreas = mutableListOf<WetArea>()
    private val textureParticles = mutableListOf<TextureParticle>()
    private val flowingTextures = mutableListOf<FlowingTexture>()

    // Rendering state
    private var lastTexturePosition: Offset? = null
    private var accumulatedDistance = 0f
    private val spacing = params.size * config.textureSpacing

    // Random number generator with consistent seed
    private val random = Random(params.hashCode() + Clock.System.now().toEpochMilliseconds().toInt())

    // Simulation management
    private val simulationScope = CoroutineScope(Dispatchers.Default)
    private var isSimulationActive = false

    // Texture cache for performance
    private val textureCache = mutableMapOf<String, ImageBitmap>()

    // Paint configurations
    private val texturePaint = Paint().apply {
        isAntiAlias = true
        blendMode = config.textureBlendMode
        alpha = config.textureOpacity
    }

    private val watercolorPaint = Paint().apply {
        isAntiAlias = true
        blendMode = BlendMode.SrcOver
    }

    private val flowPaint = Paint().apply {
        isAntiAlias = true
        blendMode = BlendMode.Multiply
        alpha = 0.6f
    }

    override fun start(event: GestureEvent): DirtyRect {
        p0 = event.position
        p1 = null
        lastMid = null
        lastTexturePosition = null
        accumulatedDistance = 0f

        path.reset()
        path.moveTo(event.position.x, event.position.y)

        // Start simulation
        startTextureWatercolorSimulation()

        // Create initial wet area
        createWetArea(event.position, params.size * config.wetness, config.wetness)

        // Apply initial texture
        return applyTextureWatercolor(event.position, event.pressure ?: params.pressure, isInitial = true)
    }

    override fun move(event: GestureEvent): DirtyRect {
        val newP = event.position
        var dirty: DirtyRect = null

        // Update path with smoothing
        when {
            p0 == null -> dirty = start(event)
            p1 == null -> {
                p1 = newP
                val a = p0!!
                val m01 = midevent(a, newP)
                if (lastMid == null) {
                    path.moveTo(a.x, a.y)
                    lastMid = a
                }
                path.quadraticBezierTo(a.x, a.y, m01.x, m01.y)
                dirty = dirty.union(paintAlongPath(lastMid!!, m01, event))
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
                dirty = dirty.union(paintAlongPath(lastMid!!, m2, event))

                lastMid = m2
                p0 = a1
                p1 = a2
            }
        }

        // Create wet area for this position
        createWetArea(newP, params.size * config.wetness, config.wetness)

        return dirty
    }

    override fun end(event: GestureEvent): DirtyRect {
        val moved = move(event)

        // Stop simulation after drying time
        simulationScope.launch {
            delay(config.dryingTime)
            stopTextureWatercolorSimulation()
        }

        return moved
    }

    private fun applyTextureWatercolor(position: Offset, pressure: Float, isInitial: Boolean = false): DirtyRect {
        var dirty: DirtyRect = null

        when (config.textureMode) {
            TextureMode.STAMP -> {
                dirty = dirty.union(applyTextureStamp(position, pressure))
            }
            TextureMode.FLOW_PAINTING -> {
                dirty = dirty.union(applyFlowPainting(position, pressure))
            }
            TextureMode.BLEND_MODE -> {
                dirty = dirty.union(applyBlendPainting(position, pressure))
            }
            TextureMode.PIGMENT_SCATTER -> {
                dirty = dirty.union(applyPigmentScatter(position, pressure))
            }
        }

        // Apply watercolor effects if wet-on-wet is enabled
        if (config.enableWetOnWet) {
            dirty = dirty.union(applyWetOnWetTextureEffects(position, pressure))
        }

        return dirty
    }

    private fun applyTextureStamp(position: Offset, pressure: Float): DirtyRect {
        // Check spacing
        lastTexturePosition?.let { lastPos ->
            accumulatedDistance += position.distanceTo(lastPos)
        }

        if (lastTexturePosition == null || accumulatedDistance >= spacing) {
            accumulatedDistance = 0f
            lastTexturePosition = position

            // Select texture
            val selectedTexture = selectTexture()

            // Calculate stamp properties
            val stampSize = params.size * pressure * config.textureScale
            val rotation = config.textureRotation + (random.nextFloat() - 0.5f) * config.textureRandomRotation
            val opacity = config.textureOpacity * pressure * config.transparency

            // Apply watercolor distortion
            val distortedTexture = if (config.textureDistortion > 0f) {
                applyWatercolorDistortion(selectedTexture, config.textureDistortion, pressure)
            } else selectedTexture

            // Render texture stamp
            return renderTextureStamp(position, distortedTexture, stampSize, rotation, opacity)
        }

        return null
    }

    private fun applyFlowPainting(position: Offset, pressure: Float): DirtyRect {
        var dirty: DirtyRect = null

        // Create flowing texture particles
        if (isSimulationActive) {
            val particleCount = (pressure * 3f).toInt().coerceAtLeast(1)
            repeat(particleCount) {
                val texture = selectTexture()
                val angle = random.nextFloat() * 2 * PI.toFloat()
                val speed = random.nextFloat() * 2f + 0.5f
                val velocity = Offset(cos(angle) * speed, sin(angle) * speed)

                val particle = TextureParticle(
                    position = position + Offset(
                        (random.nextFloat() - 0.5f) * params.size * 0.3f,
                        (random.nextFloat() - 0.5f) * params.size * 0.3f
                    ),
                    velocity = velocity,
                    originalTexture = texture,
                    currentTexture = texture,
                    rotation = random.nextFloat() * 360f,
                    scale = (random.nextFloat() * 0.5f + 0.5f) * pressure,
                    opacity = config.textureOpacity * pressure,
                    wetness = config.wetness,
                    life = 1f,
                    color = params.color,
                    isFlowing = true
                )
                textureParticles.add(particle)
            }
        }

        // Render immediate texture application
        val selectedTexture = selectTexture()
        val size = params.size * pressure * 0.8f
        dirty = dirty.union(renderTextureStamp(position, selectedTexture, size, 0f, config.textureOpacity * pressure))

        return dirty
    }

    private fun applyBlendPainting(position: Offset, pressure: Float): DirtyRect {
        // First apply base watercolor
        watercolorPaint.color = params.color.copy(alpha = params.color.alpha * config.transparency * pressure)
        canvas.drawCircle(position, params.size * 0.5f * pressure, watercolorPaint)

        // Then blend texture on top
        val selectedTexture = selectTexture()
        val blendedTexture = createBlendedTexture(selectedTexture, params.color, pressure)

        return renderTextureStamp(
            position = position,
            texture = blendedTexture,
            size = params.size * pressure,
            rotation = config.textureRotation,
            opacity = config.textureOpacity * 0.7f
        )
    }

    private fun applyPigmentScatter(position: Offset, pressure: Float): DirtyRect {
        var dirty: DirtyRect = null

        // Create multiple small texture particles scattered around the position
        val scatterCount = (pressure * 8f).toInt().coerceAtLeast(3)
        val scatterRadius = params.size * 0.6f

        repeat(scatterCount) {
            val angle = random.nextFloat() * 2 * PI.toFloat()
            val distance = random.nextFloat() * scatterRadius
            val scatterPos = Offset(
                position.x + cos(angle) * distance,
                position.y + sin(angle) * distance
            )

            val selectedTexture = selectTexture()
            val particleSize = (random.nextFloat() * 0.4f + 0.2f) * params.size * pressure
            val particleOpacity = config.textureOpacity * (random.nextFloat() * 0.5f + 0.3f) * pressure
            val particleRotation = random.nextFloat() * 360f

            dirty = dirty.union(
                renderTextureStamp(
                    position = scatterPos,
                    texture = selectedTexture,
                    size = particleSize,
                    rotation = particleRotation,
                    opacity = particleOpacity
                )
            )
        }

        return dirty
    }

    private fun applyWetOnWetTextureEffects(position: Offset, pressure: Float): DirtyRect {
        var dirty: DirtyRect = null
        val currentTime = Clock.System.now().toEpochMilliseconds()

        // Find nearby wet areas for texture bleeding
        wetAreas.filter { wetArea ->
            val age = currentTime - wetArea.timestamp
            val isNotDry = age < config.dryingTime
            val isInRange = position.distanceTo(wetArea.position) < config.flowRadius
            wetArea.isActive && isNotDry && isInRange
        }.forEach { wetArea ->
            val distance = position.distanceTo(wetArea.position)
            val bleedStrength = config.bleedStrength * (1f - distance / config.flowRadius)

            if (bleedStrength > 0.2f) {
                dirty = dirty.union(createTextureBleed(position, wetArea, bleedStrength))
            }
        }

        return dirty
    }

    private fun createTextureBleed(position: Offset, wetArea: WetArea, strength: Float): DirtyRect {
        val bleedTexture = selectTexture()
        val bleedSize = params.size * strength * 1.2f
        val bleedPosition = position.lerp(wetArea.position, strength * 0.4f)

        // Create flowing texture effect
        val flowingTexture = FlowingTexture(
            texture = bleedTexture,
            startPosition = position,
            endPosition = wetArea.position,
            size = bleedSize,
            opacity = strength * 0.6f,
            flowSpeed = 2f,
            life = 1f
        )
        flowingTextures.add(flowingTexture)

        return renderTextureStamp(
            position = bleedPosition,
            texture = bleedTexture,
            size = bleedSize,
            rotation = random.nextFloat() * 360f,
            opacity = strength * 0.4f
        )
    }

    private fun paintAlongPath(start: Offset, end: Offset, event: GestureEvent): DirtyRect {
        val distance = start.distanceTo(end)
        val steps = max(1, (distance / (params.size * 0.3f)).toInt())
        var dirty: DirtyRect = null

        for (i in 0..steps) {
            val t = if (steps == 0) 0f else i.toFloat() / steps
            val position = start.lerp(end, t)
            val pressure = event.pressure ?: params.pressure

            dirty = dirty.union(applyTextureWatercolor(position, pressure))
        }

        return dirty
    }

    private fun renderTextureStamp(
        position: Offset,
        texture: ImageBitmap,
        size: Float,
        rotation: Float,
        opacity: Float
    ): DirtyRect {
        val halfSize = size * 0.5f

        // Configure paint
        texturePaint.alpha = opacity.coerceIn(0f, 1f)
        texturePaint.colorFilter = ColorFilter.tint(params.color, BlendMode.SrcIn)

        canvas.save()
        canvas.translate(position.x, position.y)
        canvas.rotate(rotation)

        val destRect = Rect(-halfSize, -halfSize, halfSize, halfSize)

        canvas.drawImageRect(
            image = texture,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(texture.width, texture.height),
            dstOffset = IntOffset(destRect.left.toInt(), destRect.top.toInt()),
            dstSize = IntSize(destRect.width.toInt(), destRect.height.toInt()),
            paint = texturePaint
        )

        canvas.restore()

        // Return dirty rect with padding for rotation
        val padding = size * 0.6f
        return Rect(
            position.x - padding,
            position.y - padding,
            position.x + padding,
            position.y + padding
        )
    }

    private fun selectTexture(): ImageBitmap {
        return if (textures.size == 1) {
            textures[0]
        } else {
            textures[random.nextInt(textures.size)]
        }
    }

    private fun applyWatercolorDistortion(texture: ImageBitmap, distortion: Float, pressure: Float): ImageBitmap {
        if (distortion <= 0f) return texture

        val cacheKey = "distorted_${texture.hashCode()}_${distortion}_${pressure}"

        return if (config.enableTextureCache) {
            textureCache.getOrPut(cacheKey) {
                createDistortedTexture(texture, distortion, pressure)
            }
        } else {
            createDistortedTexture(texture, distortion, pressure)
        }
    }

    private fun createDistortedTexture(texture: ImageBitmap, distortion: Float, pressure: Float): ImageBitmap {
        // Simple distortion by scaling irregularly
        val distortedBitmap = ImageBitmap(texture.width, texture.height)
        val distortedCanvas = Canvas(distortedBitmap)

        // Apply slight scaling variations to simulate wet paper distortion
        val scaleVariation = distortion * pressure * 0.1f
        val scaleX = 1f + (random.nextFloat() - 0.5f) * scaleVariation
        val scaleY = 1f + (random.nextFloat() - 0.5f) * scaleVariation

        distortedCanvas.save()
        distortedCanvas.translate(texture.width * 0.5f, texture.height * 0.5f)
        distortedCanvas.scale(scaleX, scaleY)
        distortedCanvas.translate(-texture.width * 0.5f, -texture.height * 0.5f)

        distortedCanvas.drawImage(texture, Offset.Zero, Paint())
        distortedCanvas.restore()

        return distortedBitmap
    }

    private fun createBlendedTexture(texture: ImageBitmap, color: Color, pressure: Float): ImageBitmap {
        val cacheKey = "blended_${texture.hashCode()}_${color.value}_${pressure}"

        return if (config.enableTextureCache) {
            textureCache.getOrPut(cacheKey) {
                performTextureBlending(texture, color, pressure)
            }
        } else {
            performTextureBlending(texture, color, pressure)
        }
    }

    private fun performTextureBlending(texture: ImageBitmap, color: Color, pressure: Float): ImageBitmap {
        val blendedBitmap = ImageBitmap(texture.width, texture.height)
        val blendedCanvas = Canvas(blendedBitmap)

        // First draw the texture
        blendedCanvas.drawImage(texture, Offset.Zero, Paint())

        // Then apply color tint with watercolor transparency
        val tintPaint = Paint().apply {
            this.color = color.copy(alpha = color.alpha * pressure * 0.6f)
            blendMode = BlendMode.Multiply
        }

        blendedCanvas.drawRect(
            Rect(0f, 0f, texture.width.toFloat(), texture.height.toFloat()),
            tintPaint
        )

        return blendedBitmap
    }

    private fun startTextureWatercolorSimulation() {
        isSimulationActive = true
        simulationScope.launch {
            while (isSimulationActive) {
//                updateTextureParticles()
//                updateFlowingTextures()
                cleanupOldWetAreas()
                delay(16) // ~60 FPS
            }
        }
    }

    private fun stopTextureWatercolorSimulation() {
        isSimulationActive = false
        textureParticles.clear()
        flowingTextures.clear()
    }

    // Optional helper for batching draw calls
    private data class TextureDrawCmd(
        val position: Offset,
        val texture: ImageBitmap,
        val size: Float,
        val rotation: Float,
        val opacity: Float
    )

    private suspend fun updateTextureParticles() {
        val dt = 0.016f
        val gravity = Offset(0f, 1f)

        // Prepare reusable buffers to avoid per-frame allocations (could be fields)
        val survivors = ArrayList<TextureParticle>(textureParticles.size)
        val drawQueue = ArrayList<TextureDrawCmd>(textureParticles.size)

        // 1) Update physics & decide fate OFF the main thread
        for (p in textureParticles) {
            if (!p.isFlowing) continue

            // Physics
            p.velocity += gravity * dt * p.wetness
            p.velocity *= 0.95f            // fluid friction
            p.position += p.velocity * dt
            p.life -= dt * 0.8f
            p.opacity *= 0.998f
            p.scale *= 0.999f

            // Alive?
            val alive = p.life > 0f && p.opacity > 0.05f && p.scale > 0.1f
            if (alive) {
                survivors += p
                // Queue a draw command (no UI work here)
                drawQueue += TextureDrawCmd(
                    position = p.position,
                    texture  = p.currentTexture,
                    size     = params.size * p.scale,
                    rotation = p.rotation,
                    opacity  = (p.opacity * p.life).coerceIn(0f, 1f)
                )
            }
        }

        // Replace list content in one go
        textureParticles.clear()
        textureParticles.addAll(survivors)

        // 2) Render all survivors ON the main thread in one batch
        if (drawQueue.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                for (cmd in drawQueue) {
                    renderTextureStamp(
                        position = cmd.position,
                        texture  = cmd.texture,
                        size     = cmd.size,
                        rotation = cmd.rotation,
                        opacity  = cmd.opacity
                    )
                }
            }
        }
    }

    private data class FlowDrawCmd(
        val position: Offset,
        val texture: ImageBitmap,
        val size: Float,
        val rotation: Float,
        val opacity: Float
    )

    private suspend fun updateFlowingTextures() {
        val dt = 0.016f

        val survivors = ArrayList<FlowingTexture>(flowingTextures.size)
        val drawQueue = ArrayList<FlowDrawCmd>(flowingTextures.size)

        // 1) Update & queue draws off the main thread
        for (ft in flowingTextures) {
            ft.life -= dt * 0.5f

            if (ft.life > 0f) {
                val progress = (1f - ft.life).coerceIn(0f, 1f)
                val pos = ft.startPosition.lerp(ft.endPosition, progress)

                drawQueue += FlowDrawCmd(
                    position = pos,
                    texture  = ft.texture,
                    size     = ft.size * ft.life,
                    rotation = progress * 90f, // rotate while flowing
                    opacity  = (ft.opacity * ft.life).coerceIn(0f, 1f)
                )
                survivors += ft
            }
        }

        // Replace the list content in one go
        flowingTextures.clear()
        flowingTextures.addAll(survivors)

        // 2) Render on main thread in one batch
        if (drawQueue.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                for (cmd in drawQueue) {
                    renderTextureStamp(
                        position = cmd.position,
                        texture  = cmd.texture,
                        size     = cmd.size,
                        rotation = cmd.rotation,
                        opacity  = cmd.opacity
                    )
                }
            }
        }
    }


    // Helper classes and functions
    private fun createWetArea(position: Offset, radius: Float, wetness: Float) {
        val wetArea = WetArea(
            position = position,
            radius = radius,
            wetness = wetness,
            color = params.color,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        wetAreas.add(wetArea)
    }

    private fun cleanupOldWetAreas() {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        wetAreas.removeAll { wetArea ->
            val age = currentTime - wetArea.timestamp
            age > config.dryingTime
        }
    }

    private fun midevent(a: Offset, b: Offset) =
        Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
}

/**
 * Data class for flowing texture effects
 */
private data class FlowingTexture(
    val texture: ImageBitmap,
    val startPosition: Offset,
    val endPosition: Offset,
    val size: Float,
    var opacity: Float,
    val flowSpeed: Float,
    var life: Float
)

/**
 * Factory for creating different texture watercolor brush configurations
 */
object TextureWatercolorBrushFactory {

    /**
     * Creates a soft textured watercolor brush for organic painting
     */
    fun createSoftTexturedBrush(texture: ImageBitmap): TextureWatercolorBrush {
        return TextureWatercolorBrush(
            texture = texture,
            config = TextureWatercolorConfig(
                textureMode = TextureMode.FLOW_PAINTING,
                wetness = 0.8f,
                flowRadius = 30f,
                bleedStrength = 0.5f,
                textureScale = 0.8f,
                textureRandomRotation = 45f,
                textureOpacity = 0.7f,
                textureDistortion = 0.3f,
                enableWetOnWet = true
            )
        )
    }

    /**
     * Creates a controlled textured watercolor brush for precise work
     */
    fun createControlledTexturedBrush(texture: ImageBitmap, id: String, name: String): TextureWatercolorBrush {
        return TextureWatercolorBrush(
            texture = texture,
            id = ToolId(id),
            name = name,
            config = TextureWatercolorConfig(
                textureMode = TextureMode.STAMP,
                wetness = 0.5f,
                flowRadius = 15f,
                bleedStrength = 0.2f,
                textureScale = 1f,
                textureRandomRotation = 5f,
                textureSpacing = 0.45f,
                enableWetOnWet = false,
                textureBlendMode = BlendMode.SrcOver,
                simulationQuality = SimulationQuality.ULTRA
            )
        )
    }

    /**
     * Creates a natural media brush that mimics traditional watercolor textures
     */
    fun createNaturalMediaBrush(
        paperTexture: ImageBitmap,
        pigmentTextures: List<ImageBitmap>
    ): TextureWatercolorBrush {
        return TextureWatercolorBrush(
            texture = paperTexture,
            additionalTextures = pigmentTextures,
            config = TextureWatercolorConfig(
                textureMode = TextureMode.PIGMENT_SCATTER,
                wetness = 0.9f,
                flowRadius = 35f,
                bleedStrength = 0.6f,
                textureScale = 1.2f,
                textureRandomRotation = 180f,
                textureDistortion = 0.4f,
                pigmentSeparation = true,
                textureGranulation = 0.5f,
                transparency = 0.6f
            )
        )
    }

    /**
     * Creates a blend mode brush for layered watercolor effects
     */
    fun createBlendModeBrush(texture: ImageBitmap): TextureWatercolorBrush {
        return TextureWatercolorBrush(
            texture = texture,
            config = TextureWatercolorConfig(
                textureMode = TextureMode.BLEND_MODE,
                wetness = 0.7f,
                textureBlendMode = BlendMode.Overlay,
                textureOpacity = 0.5f,
                transparency = 0.8f,
                enableWetOnWet = true
            )
        )
    }

    /**
     * Creates a high-performance brush for mobile devices
     */
    fun createPerformanceBrush(texture: ImageBitmap): TextureWatercolorBrush {
        return TextureWatercolorBrush(
            texture = texture,
            config = TextureWatercolorConfig(
                textureMode = TextureMode.STAMP,
                wetness = 0.6f,
                flowRadius = 20f,
                simulationQuality = SimulationQuality.MEDIUM,
                enableTextureCache = true,
                maxTextureSize = 128,
                dryingTime = 2000L
            )
        )
    }

    /**
     * Creates an artistic experimental brush with all effects enabled
     */
    fun createArtisticBrush(
        mainTexture: ImageBitmap,
        additionalTextures: List<ImageBitmap>,
        id: String = "artistic_texture_watercolor",
        name: String = "Artistic Texture Watercolor"
    ): TextureWatercolorBrush {
        return TextureWatercolorBrush(
            texture = mainTexture,
            additionalTextures = additionalTextures,
            id = ToolId(id),
            name = name,
            config = TextureWatercolorConfig(
                textureMode = TextureMode.FLOW_PAINTING,
                wetness = 0.9f,
                flowRadius = 40f,
                bleedStrength = 0.7f,
                textureScale = 1.5f,
                textureRandomRotation = 90f,
                textureDistortion = 0.5f,
                pigmentSeparation = true,
                textureGranulation = 0.6f,
                paperInteraction = 0.8f,
                simulationQuality = SimulationQuality.ULTRA
            )
        )
    }
}