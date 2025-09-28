package io.github.taalaydev.doodleverse.engine.render

import androidx.compose.ui.graphics.Canvas
import io.github.taalaydev.doodleverse.engine.util.DirtyRect

/**
 * An invokable drawing unit (renderer + args).
 */
interface RenderInvocation {
    /** Draws into the *current* canvas. Returns a local dirty rect (before this layer's transform). */
    fun renderInto(canvas: Canvas): DirtyRect
}

/** Convenience wrapper to create a typed invocation. */
data class Invocation<A : RendererArgs>(
    val renderer: Renderer<A>,
    val args: A
) : RenderInvocation {
    override fun renderInto(canvas: Canvas): DirtyRect = renderer.render(canvas, args)
}