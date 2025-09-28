package io.github.taalaydev.doodleverse.engine.tool

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.gesture.GestureSession
import io.github.taalaydev.doodleverse.engine.gesture.HandleCanvasGestureSession

/**
 * Base class for brush parameters.
 * Used to define common properties for different types of brushes.
 *
 * @property color The color of the brush.
 * @property size The size of the brush.
 * @property spacing The spacing between brush strokes.
 * @property pressure The pressure applied to the brush (default is 1f).
 * @property velocity The velocity of the brush stroke (default is 0f).
 */
@Immutable
data class BrushParams(
    val color: Color,
    val size: Float,
    val pressure: Float = 1f,
    val velocity: Float = 0f,
    val blendMode: BlendMode = BlendMode.SrcOver,
)

/**
 * Per-stroke state holder created by a Brush.
 * Keep last point, residual spacing, paths, random seeds, etc. here.
 */
abstract class StrokeSession(
    params: BrushParams
) : GestureSession<BrushParams>(params) {
    abstract override fun start(event: GestureEvent): DirtyRect
    abstract override fun move(event: GestureEvent): DirtyRect
    abstract override fun end(event: GestureEvent): DirtyRect
}

/**
 * Base class for different types of brushes.
 * Extend this class to create custom brushes with specific behaviors.
 */
abstract class Brush : Tool, HandleCanvasGestureSession<BrushParams>() {
    /**
     * Create a new stroke session with initial point.
     *
     * @param params The parameters for the brush stroke.
     * @return A new StrokeSession instance.
     */
    abstract override fun startSession(canvas: Canvas, params: BrushParams): StrokeSession
}

/**
 * Brush that uses an image texture to create its strokes.
 */
abstract class TextureBrush : Brush() {
    abstract val texture: ImageBitmap
}

/**
 * Brush that uses a procedural algorithm to generate its texture or pattern.
 */
abstract class ProceduralBrush : Brush()

/**
 * Brush that uses a predefined shape to create its strokes.
 */
abstract class ShapeBrush : Brush()
