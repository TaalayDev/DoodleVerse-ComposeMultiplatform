package io.github.taalaydev.doodleverse.ui.theme.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.taalaydev.doodleverse.ui.theme.AnimationType
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced animated background component with sophisticated visual effects
 */
@Composable
fun AnimatedBackground(
    animationType: AnimationType,
    modifier: Modifier = Modifier,
    alpha: Float = 0.3f,
    speedMultiplier: Float = 1f,
    animate: Boolean = true
) {
    when (animationType) {
        AnimationType.COSMIC -> CosmicBackground(modifier, alpha, speedMultiplier, animate)
        AnimationType.OCEAN -> OceanBackground(modifier, alpha, speedMultiplier, animate)
        AnimationType.FOREST -> ForestBackground(modifier, alpha, speedMultiplier, animate)
        AnimationType.SUNSET -> SunsetBackground(modifier, alpha, speedMultiplier, animate)
        AnimationType.NEON -> NeonBackground(modifier, alpha, speedMultiplier, animate)
        AnimationType.MINIMALIST -> MinimalistBackground(modifier, alpha, speedMultiplier, animate)
        AnimationType.PAPER -> PaperBackground(modifier, alpha, speedMultiplier, animate)
        AnimationType.DIGITAL -> DigitalBackground(modifier, alpha, speedMultiplier, animate)
    }
}

/**
 * Advanced Cosmic Theme: Galaxies, nebulae, meteor showers, pulsars
 */
@Composable
private fun CosmicBackground(
    modifier: Modifier,
    alpha: Float,
    speedMultiplier: Float,
    animate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic")

    val galaxyRotation by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(120000 / speedMultiplier.toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "galaxy_rotation"
        )
    } else {
        remember { mutableStateOf(45f) }
    }

    val nebulaFlow by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(25000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nebula_flow"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    val meteorShower by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "meteor_shower"
        )
    } else {
        remember { mutableStateOf(0.3f) }
    }

    val pulsarBeat by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulsar_beat"
        )
    } else {
        remember { mutableStateOf(0.7f) }
    }

    // Pre-computed cosmic elements
    val starField = remember {
        List(300) {
            CosmicStar(
                position = Offset(Random.nextFloat(), Random.nextFloat()),
                brightness = Random.nextFloat() * 0.8f + 0.2f,
                size = Random.nextFloat() * 2f + 0.5f,
                twinkleSpeed = Random.nextFloat() * 2f + 1f,
                color = listOf(Color.White, Color.Cyan, Color.Yellow, Color.Magenta).random()
            )
        }
    }

    val meteors = remember {
        List(8) {
            Meteor(
                startX = Random.nextFloat() * 0.3f - 0.1f,
                startY = Random.nextFloat() * 0.3f - 0.1f,
                velocity = Offset(Random.nextFloat() * 0.8f + 0.4f, Random.nextFloat() * 0.6f + 0.3f),
                length = Random.nextFloat() * 80f + 40f,
                intensity = Random.nextFloat() * 0.8f + 0.2f
            )
        }
    }

    val pulsars = remember {
        List(4) {
            Pulsar(
                position = Offset(Random.nextFloat(), Random.nextFloat()),
                maxRadius = Random.nextFloat() * 60f + 30f,
                pulseSpeed = Random.nextFloat() * 0.5f + 0.5f,
                color = listOf(Color.Blue, Color.Magenta, Color.Cyan).random()
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val center = Offset(canvasWidth / 2, canvasHeight / 2)

        // Draw dynamic nebula clouds
        drawAdvancedNebula(canvasWidth, canvasHeight, nebulaFlow, galaxyRotation, alpha)

        // Draw spiral galaxy arms
        drawSpiralGalaxy(center, galaxyRotation, alpha)

        // Draw enhanced star field with twinkling
        starField.forEach { star ->
            val twinkle = if (animate) {
                sin((nebulaFlow * star.twinkleSpeed * 2 * PI).toFloat()) * 0.3f + 0.7f
            } else {
                star.brightness
            }
            val starAlpha = star.brightness * twinkle * alpha

            // Draw star with subtle glow
            drawCircle(
                color = star.color.copy(alpha = starAlpha * 0.3f),
                radius = star.size * 3f,
                center = Offset(star.position.x * canvasWidth, star.position.y * canvasHeight)
            )
            drawCircle(
                color = star.color.copy(alpha = starAlpha),
                radius = star.size,
                center = Offset(star.position.x * canvasWidth, star.position.y * canvasHeight)
            )
        }

        // Draw meteor shower
        meteors.forEach { meteor ->
            drawMeteor(meteor, canvasWidth, canvasHeight, meteorShower, alpha)
        }

        // Draw pulsing pulsars
        pulsars.forEach { pulsar ->
            drawPulsar(pulsar, canvasWidth, canvasHeight, pulsarBeat, alpha)
        }

        // Draw cosmic dust and solar wind
        drawCosmicDust(canvasWidth, canvasHeight, nebulaFlow, galaxyRotation, alpha)
    }
}

/**
 * Advanced Ocean Theme: Fluid dynamics, marine life, bioluminescence
 */
@Composable
private fun OceanBackground(
    modifier: Modifier,
    alpha: Float,
    speedMultiplier: Float,
    animate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ocean")

    val wavePhase by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2 * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(12000 / speedMultiplier.toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wave_phase"
        )
    } else {
        remember { mutableStateOf(PI.toFloat()) }
    }

    val currentFlow by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "current_flow"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    val biolumGlow by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "biolum_glow"
        )
    } else {
        remember { mutableStateOf(0.6f) }
    }

    val fishSchools = remember {
        List(3) {
            FishSchool(
                centerX = Random.nextFloat(),
                centerY = Random.nextFloat() * 0.6f + 0.2f,
                fishCount = Random.nextInt(8, 15),
                schoolRadius = Random.nextFloat() * 80f + 40f,
                speed = Random.nextFloat() * 0.3f + 0.2f
            )
        }
    }

    val jellyfish = remember {
        List(6) {
            Jellyfish(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 30f + 20f,
                pulseSpeed = Random.nextFloat() * 0.8f + 0.5f,
                driftSpeed = Random.nextFloat() * 0.1f + 0.05f
            )
        }
    }

    val coralElements = remember {
        List(12) {
            CoralElement(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.3f + 0.7f,
                height = Random.nextFloat() * 60f + 30f,
                swayAmount = Random.nextFloat() * 15f + 5f,
                color = listOf(Color.Magenta, Color.Cyan, Color.Yellow, Color.Green).random()
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw depth gradient
        drawOceanDepth(canvasWidth, canvasHeight, alpha)

        // Draw advanced wave system
        drawFluidWaves(canvasWidth, canvasHeight, wavePhase, currentFlow, alpha)

        // Draw coral reef at bottom
        coralElements.forEach { coral ->
            drawCoral(coral, canvasWidth, canvasHeight, currentFlow, alpha)
        }

        // Draw swimming fish schools
        fishSchools.forEach { school ->
            drawFishSchool(school, canvasWidth, canvasHeight, currentFlow, alpha)
        }

        // Draw floating jellyfish
        jellyfish.forEach { jelly ->
            drawJellyfish(jelly, canvasWidth, canvasHeight, currentFlow, biolumGlow, alpha)
        }

        // Draw bioluminescent particles
        drawBioluminescence(canvasWidth, canvasHeight, currentFlow, biolumGlow, alpha)

        // Draw water caustics and light rays
        drawAdvancedCaustics(canvasWidth, canvasHeight, wavePhase, alpha)
    }
}

/**
 * Advanced Forest Theme: Seasonal effects, wildlife, atmospheric lighting
 */
@Composable
private fun ForestBackground(
    modifier: Modifier,
    alpha: Float,
    speedMultiplier: Float,
    animate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "forest")

    val windStrength by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wind_strength"
        )
    } else {
        remember { mutableStateOf(0.3f) }
    }

    val lightShafts by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(15000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "light_shafts"
        )
    } else {
        remember { mutableStateOf(0.7f) }
    }

    val firefliesGlow by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "fireflies_glow"
        )
    } else {
        remember { mutableStateOf(0.6f) }
    }

    val trees = remember {
        List(8) {
            ForestTree(
                x = Random.nextFloat(),
                height = Random.nextFloat() * 0.4f + 0.3f,
                swayAmount = Random.nextFloat() * 8f + 3f,
                trunkWidth = Random.nextFloat() * 15f + 8f,
                canopySize = Random.nextFloat() * 100f + 60f
            )
        }
    }

    val birds = remember {
        List(5) {
            Bird(
                startX = Random.nextFloat() * 0.2f - 0.1f,
                y = Random.nextFloat() * 0.4f + 0.1f,
                speed = Random.nextFloat() * 0.4f + 0.2f,
                wingBeat = Random.nextFloat() * 2f + 1f
            )
        }
    }

    val fireflies = remember {
        List(20) {
            Firefly(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                orbitRadius = Random.nextFloat() * 30f + 10f,
                glowIntensity = Random.nextFloat() * 0.8f + 0.2f,
                speed = Random.nextFloat() * 0.5f + 0.3f
            )
        }
    }

    val pollenParticles = remember {
        List(50) {
            PollenParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 3f + 1f,
                driftSpeed = Random.nextFloat() * 0.1f + 0.05f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw atmospheric fog
        drawForestAtmosphere(canvasWidth, canvasHeight, lightShafts, alpha)

        // Draw volumetric light shafts
        drawVolumetricLight(canvasWidth, canvasHeight, lightShafts, windStrength, alpha)

        // Draw layered trees with realistic swaying
        trees.forEach { tree ->
            drawAdvancedTree(tree, canvasWidth, canvasHeight, windStrength, alpha)
        }

        // Draw flying birds
        birds.forEach { bird ->
            drawBird(bird, canvasWidth, canvasHeight, lightShafts, alpha)
        }

        // Draw floating pollen
        pollenParticles.forEach { pollen ->
            drawPollen(pollen, canvasWidth, canvasHeight, windStrength, alpha)
        }

        // Draw magical fireflies
        fireflies.forEach { firefly ->
            drawFirefly(firefly, canvasWidth, canvasHeight, firefliesGlow, alpha)
        }

        // Draw ground vegetation
        drawGroundVegetation(canvasWidth, canvasHeight, windStrength, alpha)
    }
}

/**
 * Advanced Sunset Theme: Atmospheric effects, cloud dynamics, heat shimmer
 */
@Composable
private fun SunsetBackground(
    modifier: Modifier,
    alpha: Float,
    speedMultiplier: Float,
    animate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sunset")

    val cloudDrift by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(30000 / speedMultiplier.toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cloud_drift"
        )
    } else {
        remember { mutableStateOf(0.4f) }
    }

    val atmosphericShimmer by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(5000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "atmospheric_shimmer"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    val sunIntensity by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "sun_intensity"
        )
    } else {
        remember { mutableStateOf(0.8f) }
    }

    val cloudLayers = remember {
        List(3) { layer ->
            CloudLayer(
                clouds = List(6) {
                    VolumetricCloud(
                        x = Random.nextFloat() * 1.5f - 0.25f,
                        y = 0.2f + layer * 0.15f + Random.nextFloat() * 0.1f,
                        width = Random.nextFloat() * 200f + 100f,
                        height = Random.nextFloat() * 80f + 40f,
                        density = Random.nextFloat() * 0.8f + 0.2f,
                        speed = (layer + 1) * 0.1f + Random.nextFloat() * 0.1f
                    )
                }
            )
        }
    }

    val birds = remember {
        List(8) {
            SilhouetteBird(
                x = Random.nextFloat() * 0.3f - 0.1f,
                y = Random.nextFloat() * 0.3f + 0.2f,
                speed = Random.nextFloat() * 0.3f + 0.1f,
                wingSpan = Random.nextFloat() * 15f + 10f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw atmospheric gradient
        drawSunsetAtmosphere(canvasWidth, canvasHeight, atmosphericShimmer, alpha)

        // Draw volumetric sun with corona
        drawVolumetricSun(canvasWidth, canvasHeight, sunIntensity, atmosphericShimmer, alpha)

        // Draw layered clouds with proper depth
        cloudLayers.forEachIndexed { layerIndex, layer ->
            layer.clouds.forEach { cloud ->
                drawVolumetricCloud(cloud, canvasWidth, canvasHeight, cloudDrift, layerIndex, alpha)
            }
        }

        // Draw bird silhouettes
        birds.forEach { bird ->
            drawBirdSilhouette(bird, canvasWidth, canvasHeight, cloudDrift, alpha)
        }

        // Draw heat shimmer effect
        drawHeatShimmer(canvasWidth, canvasHeight, atmosphericShimmer, alpha)

        // Draw atmospheric scattering
        drawAtmosphericScattering(canvasWidth, canvasHeight, sunIntensity, alpha)
    }
}

/**
 * Advanced Neon Theme: Holographic effects, data streams, circuit networks
 */
@Composable
private fun NeonBackground(
    modifier: Modifier,
    alpha: Float,
    speedMultiplier: Float,
    animate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neon")

    val dataFlow by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000 / speedMultiplier.toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "data_flow"
        )
    } else {
        remember { mutableStateOf(0.6f) }
    }

    val hologramFlicker by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hologram_flicker"
        )
    } else {
        remember { mutableStateOf(0.85f) }
    }

    val circuitPulse by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "circuit_pulse"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    val dataStreams = remember {
        List(15) {
            DataStream(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                length = Random.nextInt(5, 15),
                speed = Random.nextFloat() * 0.5f + 0.3f,
                color = listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Green).random(),
                thickness = Random.nextFloat() * 3f + 1f
            )
        }
    }

    val circuitNodes = remember {
        List(20) {
            CircuitNode(
                position = Offset(Random.nextFloat(), Random.nextFloat()),
                connections = mutableListOf(),
                pulseDelay = Random.nextFloat() * 3f,
                size = Random.nextFloat() * 8f + 4f
            )
        }.apply {
            // Connect nearby nodes
            forEach { node ->
                forEach { otherNode ->
                    if (node != otherNode &&
                        (node.position - otherNode.position).getDistance() < 0.3f) {
                        node.connections.add(otherNode.position)
                    }
                }
            }
        }
    }

    val holographicElements = remember {
        List(8) {
            HolographicElement(
                center = Offset(Random.nextFloat(), Random.nextFloat()),
                size = Random.nextFloat() * 60f + 30f,
                rotationSpeed = Random.nextFloat() * 2f + 0.5f,
                complexity = Random.nextInt(3, 8)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw circuit board background
        drawCircuitBoard(canvasWidth, canvasHeight, circuitPulse, alpha)

        // Draw data streams
        dataStreams.forEach { stream ->
            drawDataStream(stream, canvasWidth, canvasHeight, dataFlow, alpha)
        }

        // Draw network nodes with connections
        circuitNodes.forEach { node ->
            drawNetworkNode(node, canvasWidth, canvasHeight, circuitPulse, alpha)
        }

        // Draw holographic projections
        holographicElements.forEach { element ->
            drawHolographicElement(element, canvasWidth, canvasHeight, hologramFlicker, dataFlow, alpha)
        }

        // Draw scanning lines
        // drawScanningLines(canvasWidth, canvasHeight, dataFlow, alpha)

        // Draw electromagnetic interference
        drawEMInterference(canvasWidth, canvasHeight, hologramFlicker, alpha)
    }
}

/**
 * Advanced Minimalist Theme: Mathematical patterns, morphing geometry
 */
@Composable
private fun MinimalistBackground(
    modifier: Modifier,
    alpha: Float,
    speedMultiplier: Float,
    animate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "minimalist")

    val geometryMorph by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "geometry_morph"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    val patternShift by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2 * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(30000 / speedMultiplier.toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pattern_shift"
        )
    } else {
        remember { mutableStateOf(PI.toFloat()) }
    }

    val geometricElements = remember {
        List(12) {
            GeometricElement(
                center = Offset(Random.nextFloat(), Random.nextFloat()),
                baseSize = Random.nextFloat() * 40f + 20f,
                sides = Random.nextInt(3, 8),
                rotationSpeed = Random.nextFloat() * 0.5f + 0.2f,
                morphIntensity = Random.nextFloat() * 0.3f + 0.1f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw mathematical grid
        drawMathematicalGrid(canvasWidth, canvasHeight, patternShift, alpha * 0.2f)

        // Draw morphing geometric shapes
        geometricElements.forEach { element ->
            drawMorphingGeometry(element, canvasWidth, canvasHeight, geometryMorph, alpha)
        }

        // Draw golden ratio spirals
        drawGoldenSpirals(canvasWidth, canvasHeight, geometryMorph, alpha)

        // Draw minimal particle system
        drawMinimalParticles(canvasWidth, canvasHeight, patternShift, alpha)
    }
}

/**
 * Advanced Paper Theme: Realistic paper physics, ink dynamics
 */
@Composable
private fun PaperBackground(
    modifier: Modifier,
    alpha: Float,
    speedMultiplier: Float,
    animate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "paper")

    val inkSpread by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(18000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ink_spread"
        )
    } else {
        remember { mutableStateOf(0.6f) }
    }

    val paperRipple by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "paper_ripple"
        )
    } else {
        remember { mutableStateOf(0.3f) }
    }

    val inkDrops = remember {
        List(6) {
            InkDrop(
                position = Offset(Random.nextFloat(), Random.nextFloat()),
                maxRadius = Random.nextFloat() * 80f + 40f,
                spreadSpeed = Random.nextFloat() * 0.3f + 0.2f,
                color = listOf(
                    Color.Black, Color.Blue, Color.Red, Color.Green
                ).random(),
                intensity = Random.nextFloat() * 0.6f + 0.2f
            )
        }
    }

    val brushStrokes = remember {
        List(8) {
            BrushStroke(
                startPos = Offset(Random.nextFloat(), Random.nextFloat()),
                endPos = Offset(Random.nextFloat(), Random.nextFloat()),
                width = Random.nextFloat() * 15f + 5f,
                opacity = Random.nextFloat() * 0.4f + 0.1f,
                segments = Random.nextInt(5, 15)
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw paper texture with fibers
        drawPaperFibers(canvasWidth, canvasHeight, paperRipple, alpha)

        // Draw ink drops with realistic spreading
        inkDrops.forEach { drop ->
            drawInkDropSpread(drop, canvasWidth, canvasHeight, inkSpread, alpha)
        }

        // Draw calligraphy brush strokes
        brushStrokes.forEach { stroke ->
            drawBrushStroke(stroke, canvasWidth, canvasHeight, inkSpread, alpha)
        }

        // Draw paper aging effects
        drawPaperAging(canvasWidth, canvasHeight, paperRipple, alpha)

        // Draw watercolor bleeding
        drawWatercolorBleeding(canvasWidth, canvasHeight, inkSpread, alpha)
    }
}

/**
 * Advanced Digital Theme: Neural networks, data visualization, quantum effects
 */
@Composable
private fun DigitalBackground(
    modifier: Modifier,
    alpha: Float,
    speedMultiplier: Float,
    animate: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "digital")

    val dataTransfer by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(6000 / speedMultiplier.toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "data_transfer"
        )
    } else {
        remember { mutableStateOf(0.4f) }
    }

    val neuralActivity by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "neural_activity"
        )
    } else {
        remember { mutableStateOf(0.7f) }
    }

    val quantumFluctuation by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000 / speedMultiplier.toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "quantum_fluctuation"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    val neuralNetwork = remember {
        NeuralNetwork(
            nodes = List(25) {
                NeuralNode(
                    position = Offset(Random.nextFloat(), Random.nextFloat()),
                    activation = Random.nextFloat(),
                    connections = mutableListOf()
                )
            }.apply {
                // Create network connections
                forEach { node ->
                    val nearbyNodes = filter { other ->
                        other != node && (node.position - other.position).getDistance() < 0.4f
                    }
                    node.connections.addAll(nearbyNodes.take(Random.nextInt(2, 5)))
                }
            }
        )
    }

    val dataPackets = remember {
        List(30) {
            DataPacket(
                startPos = Offset(Random.nextFloat(), Random.nextFloat()),
                targetPos = Offset(Random.nextFloat(), Random.nextFloat()),
                speed = Random.nextFloat() * 0.5f + 0.3f,
                size = Random.nextFloat() * 4f + 2f,
                color = listOf(Color.Green, Color.Cyan, Color.Yellow, Color.Magenta).random()
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw quantum field fluctuations
        drawQuantumField(canvasWidth, canvasHeight, quantumFluctuation, alpha)

        // Draw neural network
        drawNeuralNetwork(neuralNetwork, canvasWidth, canvasHeight, neuralActivity, alpha)

        // Draw data packets traveling through network
        dataPackets.forEach { packet ->
            drawDataPacket(packet, canvasWidth, canvasHeight, dataTransfer, alpha)
        }

        // Draw binary rain with enhanced effects
        // drawEnhancedMatrixRain(canvasWidth, canvasHeight, dataTransfer, alpha)

        // Draw digital interference patterns
        drawDigitalInterference(canvasWidth, canvasHeight, quantumFluctuation, alpha)

        // Draw holographic data visualization
        drawDataVisualization(canvasWidth, canvasHeight, neuralActivity, alpha)
    }
}

// Enhanced data classes for complex animations
private data class CosmicStar(
    val position: Offset,
    val brightness: Float,
    val size: Float,
    val twinkleSpeed: Float,
    val color: Color
)

private data class Meteor(
    val startX: Float,
    val startY: Float,
    val velocity: Offset,
    val length: Float,
    val intensity: Float
)

private data class Pulsar(
    val position: Offset,
    val maxRadius: Float,
    val pulseSpeed: Float,
    val color: Color
)

private data class FishSchool(
    val centerX: Float,
    val centerY: Float,
    val fishCount: Int,
    val schoolRadius: Float,
    val speed: Float
)

private data class Jellyfish(
    val x: Float,
    val y: Float,
    val size: Float,
    val pulseSpeed: Float,
    val driftSpeed: Float
)

private data class CoralElement(
    val x: Float,
    val y: Float,
    val height: Float,
    val swayAmount: Float,
    val color: Color
)

private data class ForestTree(
    val x: Float,
    val height: Float,
    val swayAmount: Float,
    val trunkWidth: Float,
    val canopySize: Float
)

private data class Bird(
    val startX: Float,
    val y: Float,
    val speed: Float,
    val wingBeat: Float
)

private data class Firefly(
    val x: Float,
    val y: Float,
    val orbitRadius: Float,
    val glowIntensity: Float,
    val speed: Float
)

private data class PollenParticle(
    val x: Float,
    val y: Float,
    val size: Float,
    val driftSpeed: Float
)

private data class CloudLayer(
    val clouds: List<VolumetricCloud>
)

private data class VolumetricCloud(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val density: Float,
    val speed: Float
)

private data class SilhouetteBird(
    val x: Float,
    val y: Float,
    val speed: Float,
    val wingSpan: Float
)

private data class DataStream(
    val x: Float,
    val y: Float,
    val length: Int,
    val speed: Float,
    val color: Color,
    val thickness: Float
)

private data class CircuitNode(
    val position: Offset,
    val connections: MutableList<Offset>,
    val pulseDelay: Float,
    val size: Float
)

private data class HolographicElement(
    val center: Offset,
    val size: Float,
    val rotationSpeed: Float,
    val complexity: Int
)

private data class GeometricElement(
    val center: Offset,
    val baseSize: Float,
    val sides: Int,
    val rotationSpeed: Float,
    val morphIntensity: Float
)

private data class InkDrop(
    val position: Offset,
    val maxRadius: Float,
    val spreadSpeed: Float,
    val color: Color,
    val intensity: Float
)

private data class BrushStroke(
    val startPos: Offset,
    val endPos: Offset,
    val width: Float,
    val opacity: Float,
    val segments: Int
)

private data class NeuralNetwork(
    val nodes: List<NeuralNode>
)

private data class NeuralNode(
    val position: Offset,
    val activation: Float,
    val connections: MutableList<NeuralNode>
)

private data class DataPacket(
    val startPos: Offset,
    val targetPos: Offset,
    val speed: Float,
    val size: Float,
    val color: Color
)

// COSMIC THEME DRAWING FUNCTIONS
private fun DrawScope.drawAdvancedNebula(width: Float, height: Float, flow: Float, rotation: Float, alpha: Float) {
    repeat(5) { layer ->
        val layerAlpha = alpha * (0.8f - layer * 0.15f)
        val layerRotation = rotation * (1f + layer * 0.2f)

        rotate(layerRotation) {
            val gradient = Brush.radialGradient(
                colors = listOf(
                    Color.Magenta.copy(alpha = layerAlpha * 0.6f),
                    Color.Blue.copy(alpha = layerAlpha * 0.4f),
                    Color.Cyan.copy(alpha = layerAlpha * 0.2f),
                    Color.Transparent
                ),
                center = Offset(
                    width * (0.3f + sin(flow * PI + layer).toFloat() * 0.2f),
                    height * (0.4f + cos(flow * PI + layer).toFloat() * 0.2f)
                ),
                radius = width * (0.4f + layer * 0.1f + sin(flow * 2 + layer).toFloat() * 0.1f)
            )
            drawRect(gradient)
        }
    }
}

private fun DrawScope.drawSpiralGalaxy(center: Offset, rotation: Float, alpha: Float) {
    repeat(4) { arm ->
        val armAngle = (arm * 90f + rotation * 0.3f) * PI / 180f
        val path = Path()

        for (i in 0..100) {
            val t = i / 100f
            val r = t * 200f
            val angle = armAngle + t * 4 * PI
            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat() * 0.6f

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color.White.copy(alpha = alpha * 0.3f),
            style = Stroke(width = 2f + sin(rotation * PI / 180f).toFloat())
        )
    }
}

private fun DrawScope.drawMeteor(meteor: Meteor, width: Float, height: Float, time: Float, alpha: Float) {
    val meteorX = width * (meteor.startX + time * meteor.velocity.x)
    val meteorY = height * (meteor.startY + time * meteor.velocity.y)

    if (meteorX > -meteor.length && meteorX < width + meteor.length &&
        meteorY > -meteor.length && meteorY < height + meteor.length) {

        val trailGradient = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * meteor.intensity),
                Color.Yellow.copy(alpha = alpha * meteor.intensity * 0.8f),
                Color.Transparent
            ),
            start = Offset(meteorX, meteorY),
            end = Offset(meteorX - meteor.velocity.x * meteor.length, meteorY - meteor.velocity.y * meteor.length)
        )

        drawLine(
            brush = trailGradient,
            start = Offset(meteorX, meteorY),
            end = Offset(meteorX - meteor.velocity.x * meteor.length, meteorY - meteor.velocity.y * meteor.length),
            strokeWidth = 3f
        )

        drawCircle(
            color = Color.White.copy(alpha = alpha * meteor.intensity),
            radius = 4f,
            center = Offset(meteorX, meteorY)
        )
    }
}

private fun DrawScope.drawPulsar(pulsar: Pulsar, width: Float, height: Float, beat: Float, alpha: Float) {
    val center = Offset(pulsar.position.x * width, pulsar.position.y * height)
    val radius = pulsar.maxRadius * beat * pulsar.pulseSpeed

    repeat(3) { ring ->
        val ringRadius = radius * (1f + ring * 0.3f)
        val ringAlpha = alpha * (1f - ring * 0.3f) * (1f - beat * 0.5f)

        drawCircle(
            color = pulsar.color.copy(alpha = ringAlpha),
            radius = ringRadius,
            center = center,
            style = Stroke(width = 2f)
        )
    }

    drawCircle(
        color = pulsar.color.copy(alpha = alpha * 0.8f),
        radius = 6f,
        center = center
    )
}

private fun DrawScope.drawCosmicDust(width: Float, height: Float, flow: Float, rotation: Float, alpha: Float) {
    repeat(200) { i ->
        val x = (i * 17 % width.toInt()).toFloat()
        val y = (i * 31 % height.toInt()).toFloat()
        val dustAlpha = alpha * 0.1f * (sin(flow * 2 + i * 0.1f) * 0.5f + 0.5f)

        withTransform({
            translate(sin(rotation * PI / 180f + i).toFloat() * 2f, cos(rotation * PI / 180f + i).toFloat())
        }) {
            drawCircle(
                color = Color.White.copy(alpha = dustAlpha),
                radius = 0.5f,
                center = Offset(x, y)
            )
        }
    }
}

// OCEAN THEME DRAWING FUNCTIONS
private fun DrawScope.drawOceanDepth(width: Float, height: Float, alpha: Float) {
    val depthGradient = Brush.linearGradient(
        colors = listOf(
            Color.Cyan.copy(alpha = alpha * 0.1f),
            Color.Blue.copy(alpha = alpha * 0.3f),
            Color(0xFF000033).copy(alpha = alpha * 0.5f)
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, height)
    )
    drawRect(depthGradient)
}

private fun DrawScope.drawFluidWaves(width: Float, height: Float, phase: Float, flow: Float, alpha: Float) {
    repeat(6) { layer ->
        val amplitude = 20f * (layer + 1) * (1f + sin(flow * PI).toFloat() * 0.3f)
        val frequency = 2f + layer * 0.5f
        val yOffset = height * (0.2f + layer * 0.15f)
        val layerAlpha = alpha * (0.6f - layer * 0.08f)

        val path = Path()
        path.moveTo(0f, yOffset)

        for (x in 0..width.toInt() step 3) {
            val waveY = yOffset + sin((x / width * frequency + phase + layer * 0.5f) * 2 * PI).toFloat() * amplitude
            path.lineTo(x.toFloat(), waveY)
        }

        for (x in 0..width.toInt() step 8) {
            val turbulence = sin((x / width * 8f + flow * 3f) * 2 * PI).toFloat() * 3f
            val waveY = yOffset + sin((x / width * frequency + phase + layer * 0.5f) * 2 * PI).toFloat() * amplitude + turbulence
            path.lineTo(x.toFloat(), waveY)
        }

        drawPath(
            path = path,
            color = Color.Cyan.copy(alpha = layerAlpha),
            style = Stroke(
                width = 1.5f + layer * 0.3f,
                cap = StrokeCap.Round
            )
        )
    }
}

private fun DrawScope.drawCoral(coral: CoralElement, width: Float, height: Float, flow: Float, alpha: Float) {
    val baseX = coral.x * width
    val baseY = coral.y * height
    val sway = sin(flow * 2 * PI).toFloat() * coral.swayAmount

    val path = Path()
    path.moveTo(baseX, baseY)

    repeat(10) { segment ->
        val segmentHeight = coral.height / 10f
        val segmentY = baseY - segment * segmentHeight
        val segmentSway = sway * (segment / 10f) * (segment / 10f)
        val branchWidth = (10 - segment) * 0.8f

        path.lineTo(baseX + segmentSway, segmentY)

        if (segment % 3 == 0 && segment > 2) {
            val sideLength = coral.height * 0.2f
            drawLine(
                color = coral.color.copy(alpha = alpha * 0.6f),
                start = Offset(baseX + segmentSway, segmentY),
                end = Offset(
                    baseX + segmentSway + sin(segment.toFloat()).toFloat() * sideLength,
                    segmentY - sideLength * 0.5f
                ),
                strokeWidth = branchWidth * 0.5f
            )
        }
    }

    drawPath(
        path = path,
        color = coral.color.copy(alpha = alpha * 0.8f),
        style = Stroke(width = 6f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawFishSchool(school: FishSchool, width: Float, height: Float, flow: Float, alpha: Float) {
    val schoolCenterX = (school.centerX + sin(flow * PI).toFloat() * 0.1f) * width
    val schoolCenterY = school.centerY * height

    repeat(school.fishCount) { fish ->
        val angle = (fish * 360f / school.fishCount + flow * 45f) * PI / 180f
        val distance = school.schoolRadius * (0.5f + sin(flow * 2 * PI + fish).toFloat() * 0.3f)

        val fishX = schoolCenterX + cos(angle).toFloat() * distance
        val fishY = schoolCenterY + sin(angle).toFloat() * distance * 0.6f

        val fishPath = Path().apply {
            moveTo(fishX - 8f, fishY)
            lineTo(fishX + 8f, fishY - 3f)
            lineTo(fishX + 8f, fishY + 3f)
            close()
            moveTo(fishX - 8f, fishY)
            lineTo(fishX - 15f, fishY - 4f)
            lineTo(fishX - 15f, fishY + 4f)
            close()
        }

        drawPath(
            path = fishPath,
            color = Color.White.copy(alpha = alpha * 0.4f)
        )
    }
}

private fun DrawScope.drawJellyfish(jelly: Jellyfish, width: Float, height: Float, flow: Float, glow: Float, alpha: Float) {
    val jellyX = (jelly.x + sin(flow * jelly.driftSpeed * PI).toFloat() * 0.1f) * width
    val jellyY = (jelly.y + cos(flow * jelly.driftSpeed * PI * 0.7f).toFloat() * 0.1f) * height
    val pulseFactor = sin(flow * jelly.pulseSpeed * 2 * PI).toFloat() * 0.3f + 0.7f

    val bellSize = jelly.size * pulseFactor
    drawOval(
        color = Color.Magenta.copy(alpha = alpha * 0.3f * glow),
        topLeft = Offset(jellyX - bellSize/2, jellyY - bellSize/3),
        size = Size(bellSize, bellSize * 0.6f)
    )

    repeat(6) { tentacle ->
        val tentacleAngle = tentacle * 60f * PI / 180f
        val tentacleLength = jelly.size * 1.5f
        val tentacleX = jellyX + cos(tentacleAngle).toFloat() * jelly.size * 0.3f
        val tentacleEndX = tentacleX + sin(flow * 3 + tentacle).toFloat() * 15f
        val tentacleEndY = jellyY + tentacleLength + sin(flow * 2 + tentacle).toFloat() * 10f

        drawLine(
            color = Color.Cyan.copy(alpha = alpha * 0.5f * glow),
            start = Offset(tentacleX, jellyY + jelly.size * 0.2f),
            end = Offset(tentacleEndX, tentacleEndY),
            strokeWidth = 1.5f
        )
    }
}

private fun DrawScope.drawBioluminescence(width: Float, height: Float, flow: Float, glow: Float, alpha: Float) {
    repeat(100) { particle ->
        val x = (particle * 23 % width.toInt()).toFloat()
        val y = (particle * 47 % height.toInt()).toFloat()
        val particleGlow = glow * (sin(flow * 3 + particle * 0.1f) * 0.5f + 0.5f)
        val size = 1f + particleGlow * 2f

        drawCircle(
            color = Color.Cyan.copy(alpha = alpha * particleGlow * 0.6f),
            radius = size * 2f,
            center = Offset(x, y)
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * particleGlow),
            radius = size,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawAdvancedCaustics(width: Float, height: Float, phase: Float, alpha: Float) {
    repeat(15) { i ->
        val x = width * (i / 15f)
        val intensity = sin(phase + i * 0.8f) * 0.5f + 0.5f
        val causticsPath = Path()

        causticsPath.moveTo(x, 0f)
        for (y in 0..height.toInt() step 20) {
            val waveX = x + sin((y / height + phase) * 4 * PI).toFloat() * 30f * intensity
            causticsPath.lineTo(waveX, y.toFloat())
        }

        drawPath(
            path = causticsPath,
            color = Color.Cyan.copy(alpha = alpha * intensity * 0.2f),
            style = Stroke(width = 2f + intensity * 2f)
        )
    }
}

// FOREST THEME DRAWING FUNCTIONS
private fun DrawScope.drawForestAtmosphere(width: Float, height: Float, light: Float, alpha: Float) {
    val atmosphereGradient = Brush.linearGradient(
        colors = listOf(
            Color.Yellow.copy(alpha = alpha * light * 0.1f),
            Color.Green.copy(alpha = alpha * 0.05f),
            Color.Transparent
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, height * 0.6f)
    )
    drawRect(atmosphereGradient)
}

private fun DrawScope.drawVolumetricLight(width: Float, height: Float, light: Float, wind: Float, alpha: Float) {
    repeat(8) { shaft ->
        val shaftX = width * (shaft / 8f + 0.1f) + wind * 10f
        val shaftWidth = 40f + light * 20f
        val shaftAlpha = alpha * light * 0.15f

        val lightGradient = Brush.linearGradient(
            colors = listOf(
                Color.Yellow.copy(alpha = shaftAlpha),
                Color.Transparent
            ),
            start = Offset(shaftX, 0f),
            end = Offset(shaftX + 20f, height)
        )

        drawRect(
            brush = lightGradient,
            topLeft = Offset(shaftX, 0f),
            size = Size(shaftWidth, height)
        )
    }
}

private fun DrawScope.drawAdvancedTree(tree: ForestTree, width: Float, height: Float, wind: Float, alpha: Float) {
    val treeX = tree.x * width
    val treeBottom = height
    val treeTop = height * (1f - tree.height)
    val sway = sin(wind * 2 * PI).toFloat() * tree.swayAmount

    drawLine(
        color = Color(0xFF8B4513).copy(alpha = alpha * 0.6f),
        start = Offset(treeX, treeBottom),
        end = Offset(treeX + sway * 0.3f, treeTop + tree.height * height * 0.1f),
        strokeWidth = tree.trunkWidth
    )

    // Draw canopy with multiple layers
    repeat(3) { layer ->
        val canopyY = treeTop + layer * 20f
        val canopySize = tree.canopySize * (1.2f - layer * 0.2f)
        val layerSway = sway * (1f + layer * 0.2f)

        drawCircle(
            color = Color.Green.copy(alpha = alpha * (0.4f - layer * 0.1f)),
            radius = canopySize,
            center = Offset(treeX + layerSway, canopyY)
        )
    }

    // Draw branches
    repeat(5) { branch ->
        val branchAngle = (branch * 72f + wind * 20f) * PI / 180f
        val branchLength = tree.canopySize * 0.3f
        val branchStart = Offset(treeX + sway * 0.5f, treeTop + tree.height * height * 0.3f)
        val branchEnd = Offset(
            branchStart.x + cos(branchAngle).toFloat() * branchLength,
            branchStart.y + sin(branchAngle).toFloat() * branchLength * 0.5f
        )

        drawLine(
            color = Color(0xFF8B4513).copy(alpha = alpha * 0.4f),
            start = branchStart,
            end = branchEnd,
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawBird(bird: Bird, width: Float, height: Float, light: Float, alpha: Float) {
    val birdX = (bird.startX + light * bird.speed) * width
    val birdY = bird.y * height
    val wingPhase = sin(light * bird.wingBeat * 2 * PI).toFloat()

    if (birdX > -50f && birdX < width + 50f) {
        // Draw bird body
        drawOval(
            color = Color.Black.copy(alpha = alpha * 0.6f),
            topLeft = Offset(birdX - 6f, birdY - 2f),
            size = Size(12f, 4f)
        )

        // Draw animated wings
        val wingSpread = 15f + wingPhase * 8f
        drawLine(
            color = Color.Black.copy(alpha = alpha * 0.5f),
            start = Offset(birdX - wingSpread/2, birdY),
            end = Offset(birdX + wingSpread/2, birdY - wingPhase * 5f),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawPollen(pollen: PollenParticle, width: Float, height: Float, wind: Float, alpha: Float) {
    val pollenX = (pollen.x + sin(wind * pollen.driftSpeed * PI).toFloat() * 0.1f) * width
    val pollenY = (pollen.y + wind * pollen.driftSpeed * 0.1f) * height

    if (pollenY < height + pollen.size) {
        drawCircle(
            color = Color.Yellow.copy(alpha = alpha * 0.4f),
            radius = pollen.size,
            center = Offset(pollenX, pollenY % height)
        )
    }
}

private fun DrawScope.drawFirefly(firefly: Firefly, width: Float, height: Float, glow: Float, alpha: Float) {
    val time = glow * firefly.speed
    val orbitX = firefly.x * width + cos(time * 2 * PI).toFloat() * firefly.orbitRadius
    val orbitY = firefly.y * height + sin(time * 2 * PI).toFloat() * firefly.orbitRadius * 0.6f
    val glowIntensity = glow * firefly.glowIntensity

    // Draw firefly glow
    drawCircle(
        color = Color.Yellow.copy(alpha = alpha * glowIntensity * 0.6f),
        radius = 8f,
        center = Offset(orbitX, orbitY)
    )

    // Draw firefly body
    drawCircle(
        color = Color.White.copy(alpha = alpha * glowIntensity),
        radius = 2f,
        center = Offset(orbitX, orbitY)
    )
}

private fun DrawScope.drawGroundVegetation(width: Float, height: Float, wind: Float, alpha: Float) {
    repeat(50) { i ->
        val grassX = (i * 20f) % width
        val grassHeight = 20f + (i % 3) * 10f
        val grassSway = sin(wind * 2 * PI + i * 0.1f).toFloat() * 5f

        drawLine(
            color = Color.Green.copy(alpha = alpha * 0.3f),
            start = Offset(grassX, height),
            end = Offset(grassX + grassSway, height - grassHeight),
            strokeWidth = 1.5f
        )
    }
}

// SUNSET THEME DRAWING FUNCTIONS
private fun DrawScope.drawSunsetAtmosphere(width: Float, height: Float, shimmer: Float, alpha: Float) {
    val orange = Color(0xFFFFA726)
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color.Red.copy(alpha = alpha * 0.4f),
            orange.copy(alpha = alpha * 0.3f),
            Color.Yellow.copy(alpha = alpha * 0.2f),
            Color(0xFFFFB74D).copy(alpha = alpha * 0.1f),
            Color.Transparent
        ),
        start = Offset(0f, height),
        end = Offset(0f, height * (0.3f + shimmer * 0.2f))
    )
    drawRect(gradient)
}

private fun DrawScope.drawVolumetricSun(width: Float, height: Float, intensity: Float, shimmer: Float, alpha: Float) {
    val sunX = width * 0.8f
    val sunY = height * (0.7f - shimmer * 0.2f)
    val sunRadius = 50f + intensity * 20f

    // Draw sun corona
    repeat(3) { ring ->
        val coronaRadius = sunRadius * (1.5f + ring * 0.5f)
        val coronaAlpha = alpha * intensity * (0.3f - ring * 0.08f)

        drawCircle(
            color = Color.Yellow.copy(alpha = coronaAlpha),
            radius = coronaRadius,
            center = Offset(sunX, sunY),
            style = Stroke(width = 3f + ring * 2f)
        )
    }

    val orange = Color(0xFFFFA726)
    // Draw sun disk with gradient
    val sunGradient = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = alpha * intensity),
            Color.Yellow.copy(alpha = alpha * intensity * 0.8f),
            orange.copy(alpha = alpha * intensity * 0.6f)
        ),
        center = Offset(sunX, sunY),
        radius = sunRadius
    )

    drawCircle(
        brush = sunGradient,
        radius = sunRadius,
        center = Offset(sunX, sunY)
    )
}

private fun DrawScope.drawVolumetricCloud(cloud: VolumetricCloud, width: Float, height: Float, drift: Float, layer: Int, alpha: Float) {
    val cloudX = (cloud.x + drift * cloud.speed) * width
    val wrappedX = if (cloudX > width + cloud.width) cloudX - width - cloud.width * 2 else cloudX
    val cloudY = cloud.y * height
    val layerAlpha = alpha * cloud.density * (1f - layer * 0.2f)

    // Draw cloud with multiple oval segments for realism
    repeat(3) { segment ->
        val segmentX = wrappedX + segment * cloud.width * 0.3f
        val segmentY = cloudY + sin(drift + segment).toFloat() * 10f
        val segmentWidth = cloud.width * (0.8f + segment * 0.1f)
        val segmentHeight = cloud.height * (0.9f + segment * 0.05f)

        drawOval(
            color = Color.White.copy(alpha = layerAlpha * (0.8f - segment * 0.2f)),
            topLeft = Offset(segmentX - segmentWidth/2, segmentY - segmentHeight/2),
            size = Size(segmentWidth, segmentHeight)
        )
    }
}

private fun DrawScope.drawBirdSilhouette(bird: SilhouetteBird, width: Float, height: Float, drift: Float, alpha: Float) {
    val birdX = (bird.x + drift * bird.speed) * width
    val birdY = bird.y * height

    if (birdX > -bird.wingSpan && birdX < width + bird.wingSpan) {
        val wingFlap = sin(drift * 8f).toFloat()

        // Draw bird 'V' shape
        val leftWing = Path().apply {
            moveTo(birdX, birdY)
            lineTo(birdX - bird.wingSpan/2, birdY - wingFlap * 3f)
        }
        val rightWing = Path().apply {
            moveTo(birdX, birdY)
            lineTo(birdX + bird.wingSpan/2, birdY - wingFlap * 3f)
        }

        drawPath(leftWing, Color.Black.copy(alpha = alpha * 0.6f), style = Stroke(width = 2f))
        drawPath(rightWing, Color.Black.copy(alpha = alpha * 0.6f), style = Stroke(width = 2f))
    }
}

private fun DrawScope.drawHeatShimmer(width: Float, height: Float, shimmer: Float, alpha: Float) {
    repeat(20) { i ->
        val x = width * (i / 20f)
        val distortion = sin(shimmer * 4 * PI + i * 0.5f).toFloat() * 3f

        drawLine(
            color = Color.White.copy(alpha = alpha * 0.1f),
            start = Offset(x, height * 0.8f),
            end = Offset(x + distortion, height),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawAtmosphericScattering(width: Float, height: Float, intensity: Float, alpha: Float) {
    val orange = Color(0xFFFFA726)
    val scatterGradient = Brush.radialGradient(
        colors = listOf(
            orange.copy(alpha = alpha * intensity * 0.2f),
            Color.Red.copy(alpha = alpha * intensity * 0.1f),
            Color.Transparent
        ),
        center = Offset(width * 0.8f, height * 0.7f),
        radius = width * 0.6f
    )
    drawRect(scatterGradient)
}

// NEON THEME DRAWING FUNCTIONS
private fun DrawScope.drawCircuitBoard(width: Float, height: Float, pulse: Float, alpha: Float) {
    val gridSize = 50f
    val pulseAlpha = alpha * pulse * 0.3f

    // Draw grid lines
    for (x in 0..width.toInt() step gridSize.toInt()) {
        drawLine(
            color = Color.Cyan.copy(alpha = pulseAlpha),
            start = Offset(x.toFloat(), 0f),
            end = Offset(x.toFloat(), height),
            strokeWidth = 0.5f
        )
    }

    for (y in 0..height.toInt() step gridSize.toInt()) {
        drawLine(
            color = Color.Cyan.copy(alpha = pulseAlpha),
            start = Offset(0f, y.toFloat()),
            end = Offset(width, y.toFloat()),
            strokeWidth = 0.5f
        )
    }

    // Draw circuit traces
    repeat(10) { i ->
        val startX = Random.nextFloat() * width
        val startY = Random.nextFloat() * height
        val endX = startX + (Random.nextFloat() - 0.5f) * 200f
        val endY = startY + (Random.nextFloat() - 0.5f) * 200f

        drawLine(
            color = Color.Green.copy(alpha = alpha * pulse * 0.4f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawDataStream(stream: DataStream, width: Float, height: Float, flow: Float, alpha: Float) {
    val streamX = stream.x * width
    val streamY = (stream.y + flow * stream.speed) * height

    repeat(stream.length) { segment ->
        val segmentY = streamY + segment * 15f
        if (segmentY < height + 20f && segmentY > -20f) {
            val segmentAlpha = alpha * (1f - segment.toFloat() / stream.length) * 0.8f

            drawRect(
                color = stream.color.copy(alpha = segmentAlpha),
                topLeft = Offset(streamX - stream.thickness/2, segmentY % height),
                size = Size(stream.thickness, 10f)
            )
        }
    }
}

private fun DrawScope.drawNetworkNode(node: CircuitNode, width: Float, height: Float, pulse: Float, alpha: Float) {
    val nodeCenter = Offset(node.position.x * width, node.position.y * height)
    val pulsePhase = sin((pulse + node.pulseDelay) * 2 * PI).toFloat() * 0.5f + 0.5f

    // Draw connections
    node.connections.forEach { connection ->
        val connectionCenter = Offset(connection.x * width, connection.y * height)
        val connectionAlpha = alpha * pulsePhase * 0.6f

        drawLine(
            color = Color.Cyan.copy(alpha = connectionAlpha),
            start = nodeCenter,
            end = connectionCenter,
            strokeWidth = 1f + pulsePhase * 2f
        )
    }

    // Draw node
    drawCircle(
        color = Color.Magenta.copy(alpha = alpha * pulsePhase),
        radius = node.size + pulsePhase * 4f,
        center = nodeCenter
    )

    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.8f),
        radius = node.size * 0.5f,
        center = nodeCenter
    )
}

private fun DrawScope.drawHolographicElement(element: HolographicElement, width: Float, height: Float, flicker: Float, flow: Float, alpha: Float) {
    val center = Offset(element.center.x * width, element.center.y * height)
    val rotation = flow * element.rotationSpeed * 360f
    val flickerAlpha = alpha * flicker

    rotate(rotation, center) {
        // Draw holographic geometric shape
        val path = Path()
        repeat(element.complexity) { i ->
            val angle = (i * 360f / element.complexity) * PI / 180f
            val radius = element.size * (0.5f + sin(flow * 3 + i).toFloat() * 0.2f)
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = Color.Cyan.copy(alpha = flickerAlpha * 0.3f),
            style = Stroke(width = 2f)
        )

        // Add holographic scan lines
        repeat(5) { line ->
            val lineY = center.y - element.size/2 + line * element.size/5
            drawLine(
                color = Color.White.copy(alpha = flickerAlpha * 0.2f),
                start = Offset(center.x - element.size/2, lineY),
                end = Offset(center.x + element.size/2, lineY),
                strokeWidth = 0.5f
            )
        }
    }
}

private fun DrawScope.drawScanningLines(width: Float, height: Float, flow: Float, alpha: Float) {
    repeat(3) { line ->
        val lineY = (flow + line * 0.3f) * height
        val scanY = lineY % height

        val scanGradient = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                Color.Green.copy(alpha = alpha * 0.6f),
                Color.Transparent
            ),
            start = Offset(0f, scanY - 5f),
            end = Offset(0f, scanY + 5f)
        )

        drawRect(
            brush = scanGradient,
            topLeft = Offset(0f, scanY - 5f),
            size = Size(width, 10f)
        )
    }
}

private fun DrawScope.drawEMInterference(width: Float, height: Float, flicker: Float, alpha: Float) {
    if (Random.nextFloat() < 0.1f) {
        repeat(5) { i ->
            val interferenceX = Random.nextFloat() * width
            val interferenceY = Random.nextFloat() * height
            val interferenceSize = Random.nextFloat() * 50f + 20f

            drawCircle(
                color = Color.Red.copy(alpha = alpha * flicker * 0.3f),
                radius = interferenceSize,
                center = Offset(interferenceX, interferenceY),
                style = Stroke(width = 1f)
            )
        }
    }
}

// MINIMALIST THEME DRAWING FUNCTIONS
private fun DrawScope.drawMathematicalGrid(width: Float, height: Float, shift: Float, alpha: Float) {
    val gridSpacing = 80f
    val phase = shift * 0.1f

    for (i in 0..((width / gridSpacing).toInt() + 1)) {
        val x = i * gridSpacing
        val offset = sin(phase + i * 0.2f).toFloat() * 10f

        drawLine(
            color = Color.Gray.copy(alpha = alpha),
            start = Offset(x + offset, 0f),
            end = Offset(x + offset, height),
            strokeWidth = 0.5f
        )
    }

    for (i in 0..((height / gridSpacing).toInt() + 1)) {
        val y = i * gridSpacing
        val offset = cos(phase + i * 0.2f).toFloat() * 10f

        drawLine(
            color = Color.Gray.copy(alpha = alpha),
            start = Offset(0f, y + offset),
            end = Offset(width, y + offset),
            strokeWidth = 0.5f
        )
    }
}

private fun DrawScope.drawMorphingGeometry(element: GeometricElement, width: Float, height: Float, morph: Float, alpha: Float) {
    val center = Offset(element.center.x * width, element.center.y * height)
    val morphedSize = element.baseSize * (1f + sin(morph * 2 * PI).toFloat() * element.morphIntensity)
    val rotation = morph * element.rotationSpeed * 360f

    rotate(rotation, center) {
        val path = Path()
        repeat(element.sides) { i ->
            val angle = (i * 360f / element.sides) * PI / 180f
            val radius = morphedSize * (0.8f + sin(morph * 4 * PI + i).toFloat() * 0.2f)
            val x = center.x + cos(angle).toFloat() * radius
            val y = center.y + sin(angle).toFloat() * radius

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = Color.Black.copy(alpha = alpha * 0.6f),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawGoldenSpirals(width: Float, height: Float, morph: Float, alpha: Float) {
    val centerX = width / 2f
    val centerY = height / 2f
    val goldenRatio = 1.618f

    repeat(2) { spiral ->
        val path = Path()
        val direction = if (spiral == 0) 1f else -1f

        for (i in 0..200) {
            val t = i / 50f
            val angle = t * 2 * PI * direction + morph * PI
            val radius = t * 20f * (goldenRatio + sin(morph * 2 * PI + i * 0.1f).toFloat() * 0.5f)

            val x = centerX + cos(angle).toFloat() * radius
            val y = centerY + sin(angle).toFloat() * radius

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color.Gray.copy(alpha = alpha * 0.4f),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawMinimalParticles(width: Float, height: Float, shift: Float, alpha: Float) {
    repeat(30) { i ->
        val x = (i * 47f % width)
        val y = (i * 73f % height)
        val phase = sin(shift + i * 0.3f).toFloat() * 0.5f + 0.5f
        val size = 2f + phase * 3f

        drawCircle(
            color = Color.Black.copy(alpha = alpha * phase * 0.5f),
            radius = size,
            center = Offset(x, y)
        )
    }
}

// PAPER THEME DRAWING FUNCTIONS
private fun DrawScope.drawPaperFibers(width: Float, height: Float, ripple: Float, alpha: Float) {
    repeat(300) { fiber ->
        val x = (fiber * 17 % width.toInt()).toFloat()
        val y = (fiber * 23 % height.toInt()).toFloat()
        val fiberLength = 5f + (fiber % 3) * 3f
        val angle = (fiber * 13) % 360f * PI / 180f + ripple * 0.1f

        val endX = x + cos(angle).toFloat() * fiberLength
        val endY = y + sin(angle).toFloat() * fiberLength

        drawLine(
            color = Color(0xFFBDBDBD).copy(alpha = alpha * 0.2f),
            start = Offset(x, y),
            end = Offset(endX, endY),
            strokeWidth = 0.5f
        )
    }
}

private fun DrawScope.drawInkDropSpread(drop: InkDrop, width: Float, height: Float, spread: Float, alpha: Float) {
    val center = Offset(drop.position.x * width, drop.position.y * height)
    val currentRadius = drop.maxRadius * spread * drop.spreadSpeed

    if (currentRadius > 0f) {
        // Main ink drop
        val inkGradient = Brush.radialGradient(
            colors = listOf(
                drop.color.copy(alpha = alpha * drop.intensity),
                drop.color.copy(alpha = alpha * drop.intensity * 0.7f),
                drop.color.copy(alpha = alpha * drop.intensity * 0.3f),
                Color.Transparent
            ),
            center = center,
            radius = currentRadius
        )

        drawCircle(
            brush = inkGradient,
            radius = currentRadius,
            center = center
        )

        // Ink tendrils
        repeat(8) { tendril ->
            val tendrilAngle = (tendril * 45f) * PI / 180f
            val tendrilLength = currentRadius * (0.5f + sin(spread * 2 * PI + tendril).toFloat() * 0.3f)
            val tendrilEnd = Offset(
                center.x + cos(tendrilAngle).toFloat() * tendrilLength,
                center.y + sin(tendrilAngle).toFloat() * tendrilLength
            )

            drawLine(
                color = drop.color.copy(alpha = alpha * drop.intensity * 0.6f),
                start = center,
                end = tendrilEnd,
                strokeWidth = 1f + spread * 2f
            )
        }
    }
}

private fun DrawScope.drawBrushStroke(stroke: BrushStroke, width: Float, height: Float, spread: Float, alpha: Float) {
    val startPos = Offset(stroke.startPos.x * width, stroke.startPos.y * height)
    val endPos = Offset(stroke.endPos.x * width, stroke.endPos.y * height)
    val progress = spread

    if (progress > 0f) {
        val currentEnd = Offset(
            startPos.x + (endPos.x - startPos.x) * progress,
            startPos.y + (endPos.y - startPos.y) * progress
        )

        // Create organic brush stroke path
        val path = Path()
        path.moveTo(startPos.x, startPos.y)

        repeat(stroke.segments) { segment ->
            val t = segment.toFloat() / stroke.segments * progress
            val x = startPos.x + (endPos.x - startPos.x) * t
            val y = startPos.y + (endPos.y - startPos.y) * t

            // Add some randomness for organic feel
            val variation = sin(t * PI * 4).toFloat() * 3f
            path.lineTo(x + variation, y + variation)
        }

        drawPath(
            path = path,
            color = Color.Black.copy(alpha = alpha * stroke.opacity),
            style = Stroke(
                width = stroke.width * (0.5f + sin(spread * PI).toFloat() * 0.5f),
                cap = StrokeCap.Round
            )
        )
    }
}

private fun DrawScope.drawPaperAging(width: Float, height: Float, ripple: Float, alpha: Float) {
    // Age spots
    repeat(20) { spot ->
        val x = (spot * 31f % width)
        val y = (spot * 47f % height)
        val spotSize = 3f + (spot % 4) * 2f
        val ageAlpha = alpha * 0.1f * (sin(ripple + spot).toFloat() * 0.5f + 0.5f)

        drawCircle(
            color = Color(0xFF8D6E63).copy(alpha = ageAlpha),
            radius = spotSize,
            center = Offset(x, y)
        )
    }

    // Yellowing effect
    val yellowGradient = Brush.radialGradient(
        colors = listOf(
            Color.Transparent,
            Color(0xFFFFF8E1).copy(alpha = alpha * 0.1f)
        ),
        center = Offset(width * 0.7f, height * 0.3f),
        radius = width * 0.8f
    )
    drawRect(yellowGradient)
}

private fun DrawScope.drawWatercolorBleeding(width: Float, height: Float, spread: Float, alpha: Float) {
    repeat(5) { bleeding ->
        val centerX = (bleeding * 127f % width)
        val centerY = (bleeding * 211f % height)
        val bleedRadius = 30f + sin(spread * PI + bleeding).toFloat() * 20f
        val color = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta)[bleeding]

        val bleedGradient = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha * 0.1f),
                color.copy(alpha = alpha * 0.05f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = bleedRadius
        )

        drawCircle(
            brush = bleedGradient,
            radius = bleedRadius,
            center = Offset(centerX, centerY)
        )
    }
}

// DIGITAL THEME DRAWING FUNCTIONS
private fun DrawScope.drawQuantumField(width: Float, height: Float, fluctuation: Float, alpha: Float) {
    repeat(200) { particle ->
        val x = (particle * 19f % width)
        val y = (particle * 37f % height)
        val phase = sin(fluctuation * 3 * PI + particle * 0.1f).toFloat()
        val particleAlpha = alpha * 0.3f * (phase * 0.5f + 0.5f)
        val size = 1f + abs(phase) * 2f

        drawCircle(
            color = Color.Green.copy(alpha = particleAlpha),
            radius = size,
            center = Offset(x, y)
        )

        // Quantum uncertainty visualization
        if (abs(phase) > 0.8f) {
            drawCircle(
                color = Color.Cyan.copy(alpha = alpha * 0.2f),
                radius = size * 3f,
                center = Offset(x, y),
                style = Stroke(width = 0.5f)
            )
        }
    }
}

private fun DrawScope.drawNeuralNetwork(network: NeuralNetwork, width: Float, height: Float, activity: Float, alpha: Float) {
    // Draw connections first
    network.nodes.forEach { node ->
        val nodePos = Offset(node.position.x * width, node.position.y * height)

        node.connections.forEach { connection ->
            val connectionPos = Offset(connection.position.x * width, connection.position.y * height)
            val connectionStrength = sin(activity * 2 * PI + node.activation).toFloat() * 0.5f + 0.5f

            drawLine(
                color = Color.Green.copy(alpha = alpha * connectionStrength * 0.6f),
                start = nodePos,
                end = connectionPos,
                strokeWidth = 0.5f + connectionStrength * 2f
            )
        }
    }

    // Draw nodes
    network.nodes.forEach { node ->
        val nodePos = Offset(node.position.x * width, node.position.y * height)
        val nodeActivity = sin(activity * 2 * PI + node.activation * 4).toFloat() * 0.5f + 0.5f
        val nodeSize = 3f + nodeActivity * 5f

        drawCircle(
            color = Color.Cyan.copy(alpha = alpha * nodeActivity),
            radius = nodeSize,
            center = nodePos
        )

        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.8f),
            radius = nodeSize * 0.5f,
            center = nodePos
        )
    }
}

private fun DrawScope.drawDataPacket(packet: DataPacket, width: Float, height: Float, transfer: Float, alpha: Float) {
    val currentPos = Offset(
        packet.startPos.x * width + (packet.targetPos.x - packet.startPos.x) * width * transfer * packet.speed,
        packet.startPos.y * height + (packet.targetPos.y - packet.startPos.y) * height * transfer * packet.speed
    )

    // Wrap around screen
    val wrappedPos = Offset(
        currentPos.x % width,
        currentPos.y % height
    )

    // Draw packet
    drawCircle(
        color = packet.color.copy(alpha = alpha * 0.8f),
        radius = packet.size,
        center = wrappedPos
    )

    // Draw trail
    repeat(5) { trail ->
        val trailPos = Offset(
            wrappedPos.x - trail * 5f,
            wrappedPos.y
        )
        val trailAlpha = alpha * (1f - trail * 0.2f) * 0.5f

        drawCircle(
            color = packet.color.copy(alpha = trailAlpha),
            radius = packet.size * (1f - trail * 0.15f),
            center = trailPos
        )
    }
}

private fun DrawScope.drawEnhancedMatrixRain(width: Float, height: Float, transfer: Float, alpha: Float) {
    val columnCount = (width / 20f).toInt()

    repeat(columnCount) { column ->
        val x = column * 20f
        val streamLength = Random.nextInt(10, 30)
        val streamSpeed = 0.5f + Random.nextFloat() * 0.5f
        val streamY = (transfer * streamSpeed * height) % (height + streamLength * 15f) - streamLength * 15f

        repeat(streamLength) { char ->
            val charY = streamY + char * 15f
            if (charY > 0 && charY < height) {
                val charAlpha = alpha * (1f - char.toFloat() / streamLength) * 0.8f
                val character = (Random.nextInt(2) + '0'.code).toChar() // 0 or 1

                // Draw character background glow
                drawRect(
                    color = Color.Green.copy(alpha = charAlpha * 0.3f),
                    topLeft = Offset(x - 5f, charY - 5f),
                    size = Size(15f, 15f)
                )

                // Draw character (simplified as rectangle)
                drawRect(
                    color = Color.Green.copy(alpha = charAlpha),
                    topLeft = Offset(x, charY),
                    size = Size(10f, 12f)
                )
            }
        }
    }
}

private fun DrawScope.drawDigitalInterference(width: Float, height: Float, fluctuation: Float, alpha: Float) {
    // Random digital artifacts
    if (Random.nextFloat() < 0.2f) {
        repeat(10) { artifact ->
            val artifactX = Random.nextFloat() * width
            val artifactY = Random.nextFloat() * height
            val artifactWidth = Random.nextFloat() * 50f + 10f
            val artifactHeight = Random.nextFloat() * 5f + 2f

            drawRect(
                color = Color.Red.copy(alpha = alpha * fluctuation * 0.4f),
                topLeft = Offset(artifactX, artifactY),
                size = Size(artifactWidth, artifactHeight)
            )
        }
    }

    // Digital noise
    repeat(100) { noise ->
        val x = Random.nextFloat() * width
        val y = Random.nextFloat() * height
        val intensity = sin(fluctuation * 8 * PI + noise).toFloat() * 0.5f + 0.5f

        drawRect(
            color = Color.White.copy(alpha = alpha * intensity * 0.1f),
            topLeft = Offset(x, y),
            size = Size(1f, 1f)
        )
    }
}

private fun DrawScope.drawDataVisualization(width: Float, height: Float, activity: Float, alpha: Float) {
    // Data bars
    repeat(20) { bar ->
        val barX = bar * (width / 20f)
        val barHeight = (sin(activity * 2 * PI + bar * 0.3f).toFloat() * 0.5f + 0.5f) * height * 0.3f
        val barColor = if (bar % 3 == 0) Color.Cyan else if (bar % 3 == 1) Color.Green else Color.Yellow

        val barGradient = Brush.linearGradient(
            colors = listOf(
                barColor.copy(alpha = alpha * 0.8f),
                barColor.copy(alpha = alpha * 0.4f)
            ),
            start = Offset(barX, height),
            end = Offset(barX, height - barHeight)
        )

        drawRect(
            brush = barGradient,
            topLeft = Offset(barX, height - barHeight),
            size = Size(width / 20f - 2f, barHeight)
        )
    }

    // Data flow lines
    repeat(5) { line ->
        val lineY = height * (0.1f + line * 0.15f)
        val phase = activity * 2 * PI + line

        val path = Path()
        path.moveTo(0f, lineY)

        for (x in 0..width.toInt() step 10) {
            val y = lineY + sin(phase + x * 0.01f).toFloat() * 20f
            path.lineTo(x.toFloat(), y)
        }

        drawPath(
            path = path,
            color = Color.Magenta.copy(alpha = alpha * 0.6f),
            style = Stroke(width = 2f)
        )
    }
}