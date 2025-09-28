package io.github.taalaydev.doodleverse.engine.render

import androidx.compose.ui.graphics.Canvas
import io.github.taalaydev.doodleverse.engine.util.DirtyRect
import io.github.taalaydev.doodleverse.engine.util.union

/** Args wrapper so the generic bound `A : RendererArgs` holds. */
data class ListRendererArgs(
    val items: List<RenderInvocation>
) : RendererArgs

/** Renders a list of heterogeneous renderer invocations. */
class ListRenderer : Renderer<ListRendererArgs>() {
    override fun render(canvas: Canvas, args: ListRendererArgs): DirtyRect {
        var dirty: DirtyRect = null
        for (inv in args.items) {
            dirty = dirty.union(inv.renderInto(canvas))
        }
        return dirty
    }
}
