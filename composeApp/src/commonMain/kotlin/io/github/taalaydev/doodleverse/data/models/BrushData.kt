package io.github.taalaydev.doodleverse.data.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.util.lerp
import io.github.taalaydev.doodleverse.core.rendering.DrawRenderer
import org.jetbrains.compose.resources.DrawableResource

data class BrushData(
    val id: Int,
    val name: String,
    val stroke: String,
    val brush:  DrawableResource? = null,
    val texture:  DrawableResource? = null,
    val isLocked: Boolean = false,
    val isNew: Boolean = false,
    val opacityDiff: Float = 0f,
    val colorFilter: ColorFilter? = null,
    val strokeCap: StrokeCap = StrokeCap.Butt,
    val strokeJoin: StrokeJoin = StrokeJoin.Round,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val densityOffset: Double = 5.0,
    val useBrushWidthDensity: Boolean = true,
    val random: List<Int> = listOf(0, 0),
    val sizeRandom: List<Int> = listOf(0, 0),
    val rotationRandomness: Float = 0f,
    val pathEffect: ((width: Float) -> PathEffect?)? = null,
    val customPainter: ((canvas: Canvas, size: Size, drawingPath: DrawingPath) -> Unit)? = null,
    val isShape: Boolean = false,
) {
    internal fun sizeInPixels(brushSize: Float): Int {
        if (brush != null) {
            return lerp(1, 80, brushSize)
        } else {
            return lerp(1, 80, brushSize)
        }
    }

    companion object {
        val solid = io.github.taalaydev.doodleverse.brush.solid
        val pencil = io.github.taalaydev.doodleverse.brush.pencil
        val sketchyPencil = io.github.taalaydev.doodleverse.brush.sketchyPencil
        val softPencilBrush = io.github.taalaydev.doodleverse.brush.softPencilBrush
        val hardPencilBrush = io.github.taalaydev.doodleverse.brush.hardPencilBrush
        val pencilShadingBrush = io.github.taalaydev.doodleverse.brush.pencilShadingBrush
        val star = io.github.taalaydev.doodleverse.brush.star
        val marker = io.github.taalaydev.doodleverse.brush.marker
        val zigzag = io.github.taalaydev.doodleverse.brush.zigzag
        val bubble = io.github.taalaydev.doodleverse.brush.bubble
        val heart = io.github.taalaydev.doodleverse.brush.heart
        val rainbowBrush = io.github.taalaydev.doodleverse.brush.rainbowBrush
        val watercolorBrush = io.github.taalaydev.doodleverse.brush.watercolorBrush
        val crayonBrush = io.github.taalaydev.doodleverse.brush.crayonBrush
        val sprayPaintBrush = io.github.taalaydev.doodleverse.brush.sprayPaintBrush
        val charcoalBrush = io.github.taalaydev.doodleverse.brush.charcoalBrush
        val sketchyBrush = io.github.taalaydev.doodleverse.brush.sketchyBrush
        val glitterBrush = io.github.taalaydev.doodleverse.brush.glitterBrush
        val grassBrush = io.github.taalaydev.doodleverse.brush.grassBrush
        val pixelBrush = io.github.taalaydev.doodleverse.brush.pixelBrush
        val mosaicBrush = io.github.taalaydev.doodleverse.brush.mosaicBrush
        val splatBrush = io.github.taalaydev.doodleverse.brush.splatBrush
        val galaxyBrush = io.github.taalaydev.doodleverse.brush.galaxyBrush
        val fireBrush = io.github.taalaydev.doodleverse.brush.fireBrush
        val snowflakeBrush = io.github.taalaydev.doodleverse.brush.snowflakeBrush
        val cloudBrush = io.github.taalaydev.doodleverse.brush.cloudBrush
        val confettiBrush = io.github.taalaydev.doodleverse.brush.confettiBrush
        val particleFieldBrush = io.github.taalaydev.doodleverse.brush.particleFieldBrush
        val stainedGlassBrush = io.github.taalaydev.doodleverse.brush.stainedGlassBrush
        val flowFieldBrush = io.github.taalaydev.doodleverse.brush.flowFieldBrush
        val dotCloudBrush = io.github.taalaydev.doodleverse.brush.dotCloudBrush
        val blendingBrush = io.github.taalaydev.doodleverse.brush.blendingBrush
        val textureBlendBrush = io.github.taalaydev.doodleverse.brush.textureBlendBrush
        val crossHatchBrush = io.github.taalaydev.doodleverse.brush.crossHatchBrush
        val oilBrush = io.github.taalaydev.doodleverse.brush.oilBrush
        val wetBrush = io.github.taalaydev.doodleverse.brush.wetBrush
        val acrylicBrush = io.github.taalaydev.doodleverse.brush.acrylicBrush
        val glazingBrush = io.github.taalaydev.doodleverse.brush.glazingBrush
        val impastoBrush = io.github.taalaydev.doodleverse.brush.impastoBrush
        val spongeTextureBrush = io.github.taalaydev.doodleverse.brush.spongeTextureBrush
        val bristleBrush = io.github.taalaydev.doodleverse.brush.bristleBrush
        val watercolorWashBrush = io.github.taalaydev.doodleverse.brush.watercolorWashBrush
        val watercolorDryBrushBrush = io.github.taalaydev.doodleverse.brush.watercolorDryBrushBrush
        val watercolorBleedBrush = io.github.taalaydev.doodleverse.brush.watercolorBleedBrush
        val watercolorSplatterBrush = io.github.taalaydev.doodleverse.brush.watercolorSplatterBrush
        val watercolorGranulationBrush = io.github.taalaydev.doodleverse.brush.watercolorGranulationBrush
        val blendingSmudgeBrush = io.github.taalaydev.doodleverse.brush.blendingSmudgeBrush
        val pixelBlurBrush = io.github.taalaydev.doodleverse.brush.pixelBlurBrush
        val distortionEffectBrush = io.github.taalaydev.doodleverse.brush.distortionEffectBrush
        val noiseTextureBrush = io.github.taalaydev.doodleverse.brush.noiseTextureBrush
        val graphitePencilBrush = io.github.taalaydev.doodleverse.brush.graphitePencilBrush
        val softPencilBrush6B = io.github.taalaydev.doodleverse.brush.softPencilBrush6B
        val hardPencilBrush2H = io.github.taalaydev.doodleverse.brush.hardPencilBrush2H
        val sketchPencilBrush = io.github.taalaydev.doodleverse.brush.sketchPencilBrush
        val hatchingPencilBrush = io.github.taalaydev.doodleverse.brush.hatchingPencilBrush
        val coloredPencilBrush = io.github.taalaydev.doodleverse.brush.coloredPencilBrush
        val eraser = io.github.taalaydev.doodleverse.brush.eraser
        val cleanEraser = io.github.taalaydev.doodleverse.brush.cleanEraser

        fun random(seed: Int): Float {
            return (seed * 16807L % 2147483647L).toFloat() / 2147483647L
        }

        fun all(): List<BrushData> = listOf(
            solid,
            graphitePencilBrush,
            softPencilBrush6B,
            hardPencilBrush2H,
            sketchPencilBrush,
            hatchingPencilBrush,
            coloredPencilBrush,
            sketchyPencil,
            softPencilBrush,
            hardPencilBrush,
            pencilShadingBrush,
            star,
            pixelBlurBrush,
            marker,
            zigzag,
            bubble,
            heart,
            rainbowBrush,
            watercolorBrush,
            crayonBrush,
            sprayPaintBrush,
            charcoalBrush,
            sketchyBrush,
            glitterBrush,
            grassBrush,
            pixelBrush,
            mosaicBrush,
            splatBrush,
            galaxyBrush,
            fireBrush,
            snowflakeBrush,
            cloudBrush,
            confettiBrush,
            particleFieldBrush,
            stainedGlassBrush,
            flowFieldBrush,
            dotCloudBrush,
            blendingBrush,
            textureBlendBrush,
            crossHatchBrush,
            oilBrush,
            wetBrush,
            acrylicBrush,
            glazingBrush,
            impastoBrush,
            spongeTextureBrush,
            bristleBrush,
            watercolorWashBrush,
            watercolorDryBrushBrush,
            watercolorBleedBrush,
            watercolorSplatterBrush,
            watercolorGranulationBrush,
            blendingSmudgeBrush,
            distortionEffectBrush,
            noiseTextureBrush,
        )

        @Composable
        fun getById(id: Int): BrushData {
            return all().first { it.id == id }
        }
    }
}




