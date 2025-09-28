package io.github.taalaydev.doodleverse.engine.render

import androidx.compose.ui.graphics.Canvas
import io.github.taalaydev.doodleverse.engine.util.DirtyRect

/** Marker for renderer argument packets. */
interface RendererArgs

/**
 * Generic renderer. Each implementation supplies its own argument type.
 * Returning DirtyRect keeps it consistent with your engine invalidation. */
abstract class Renderer<A : RendererArgs> {
    abstract fun render(canvas: Canvas, args: A): DirtyRect
}