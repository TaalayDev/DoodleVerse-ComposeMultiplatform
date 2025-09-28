package io.github.taalaydev.doodleverse.engine.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Path
import io.github.taalaydev.doodleverse.engine.util.DirtyRect
import io.github.taalaydev.doodleverse.engine.util.toRadians
import io.github.taalaydev.doodleverse.engine.util.union
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simple, explicit transform: translation, scale, rotation around optional pivot (in local space).
 */
data class Transform(
    val translation: Offset = Offset.Zero,
    val scale: Offset = Offset(1f, 1f),
    val rotationDeg: Float = 0f,
    val pivot: Offset? = null
)

/**
 * A hierarchical layer. It applies its own transform & clip, draws optional content,
 * renders children (z-sorted), and returns the dirty rect in the *parent's* space.
 *
 * Contract:
 * - The returned dirty rect is always in the caller's (parent) coordinate space.
 * - Child layers are asked to return their dirty in the current canvas space; we map it back.
 */
class RenderLayer(
    var id: String = "",
    var visible: Boolean = true,
    var zIndex: Float = 0f,
    var transform: Transform = Transform(),
    var clipRect: Rect? = null,
    var clipPath: Path? = null,
    var content: RenderInvocation? = null,
) {
    val children: MutableList<RenderLayer> = mutableListOf()

    // Caching/diagnostics
    private var lastDirtyInParent: Rect? = null
    var isInvalidated: Boolean = true
        private set

    fun invalidate() { isInvalidated = true }
    // fun setContent(inv: RenderInvocation?) { content = inv; invalidate() }
    fun add(child: RenderLayer): RenderLayer { children.add(child); invalidate(); return this }
    fun remove(child: RenderLayer) { children.remove(child); invalidate() }
    fun clear() { children.clear(); invalidate() }

    /**
     * Render this layer and its subtree.
     * Returns the dirty area in the *parent* coordinate space (or null if nothing drawn).
     */
    fun render(canvas: Canvas): DirtyRect {
        if (!visible) return null

        // Enter: parent space (P0)
        canvas.save()
        applyTransform(canvas, transform) // Canvas is now in post-transform space (P1)

        // Apply clipping in local (P1) space
        clipRect?.let { canvas.clipRect(it) }
        clipPath?.let { canvas.clipPath(it) }

        var unionInParent: Rect? = null

        // Draw self content in P1; returned bounds are LOCAL (pre-transform)
        content?.let { inv ->
            val localDirty = inv.renderInto(canvas) // local space
            if (localDirty != null) {
                // Map our local dirty forward to parent (P0)
                unionInParent = unionInParent.union(mapRectForward(localDirty, transform))
            }
        }

        // Draw children (they return dirty in the *current* canvas space, i.e., P1)
        val list = if (children.size > 1) children.sortedBy { it.zIndex } else children
        for (ch in list) {
            val childDirtyInP1 = ch.render(canvas) // child maps to our current (P1) space
            if (childDirtyInP1 != null) {
                // Map child's P1 dirty back to parent P0 by inverse of our transform
                val backToParent = mapRectInverse(childDirtyInP1, transform)
                unionInParent = unionInParent.union(backToParent)
            }
        }

        canvas.restore() // back to P0

        lastDirtyInParent = unionInParent
        isInvalidated = false
        return unionInParent
    }

    fun lastDirty(): DirtyRect = lastDirtyInParent
}

/* ---------------------------- Helpers ----------------------------------- */

private fun applyTransform(canvas: Canvas, t: Transform) {
    if (t.translation != Offset.Zero) {
        canvas.translate(t.translation.x, t.translation.y)
    }
    val pivot = t.pivot
    if (pivot != null) {
        canvas.translate(pivot.x, pivot.y)
    }
    if (t.rotationDeg != 0f) {
        canvas.rotate(t.rotationDeg)
    }
    if (t.scale != Offset(1f, 1f)) {
        canvas.scale(t.scale.x, t.scale.y)
    }
    if (pivot != null) {
        canvas.translate(-pivot.x, -pivot.y)
    }
}

private fun mapRectForward(r: Rect, t: Transform): Rect {
    val p = t.pivot ?: Offset.Zero
    val rad = toRadians(t.rotationDeg.toDouble())
    val c = cos(rad); val s = sin(rad)

    fun f(pt: Offset): Offset {
        var x = pt.x - p.x
        var y = pt.y - p.y
        // scale
        x *= t.scale.x
        y *= t.scale.y
        // rotate
        val rx = (x * c - y * s).toFloat()
        val ry = (x * s + y * c).toFloat()
        // unpivot + translate
        return Offset(rx + p.x + t.translation.x, ry + p.y + t.translation.y)
    }

    val pts = arrayOf(
        Offset(r.left, r.top),
        Offset(r.right, r.top),
        Offset(r.right, r.bottom),
        Offset(r.left, r.bottom)
    ).map(::f)

    val minX = pts.minOf { it.x }
    val maxX = pts.maxOf { it.x }
    val minY = pts.minOf { it.y }
    val maxY = pts.maxOf { it.y }
    return Rect(minX, minY, maxX, maxY)
}

private fun mapRectInverse(r: Rect, t: Transform): Rect {
    val p = t.pivot ?: Offset.Zero
    val rad = toRadians((-t.rotationDeg).toDouble())
    val c = cos(rad); val s = sin(rad)
    val invSx = if (t.scale.x != 0f) 1f / t.scale.x else 0f
    val invSy = if (t.scale.y != 0f) 1f / t.scale.y else 0f

    fun inv(pt: Offset): Offset {
        // remove translation
        var x = pt.x - t.translation.x
        var y = pt.y - t.translation.y
        // move to pivot
        x -= p.x; y -= p.y
        // inverse rotate
        val rx = (x * c - y * s).toFloat()
        val ry = (x * s + y * c).toFloat()
        // inverse scale
        val sx = rx * invSx
        val sy = ry * invSy
        // back from pivot
        return Offset(sx + p.x, sy + p.y)
    }

    val pts = arrayOf(
        Offset(r.left, r.top),
        Offset(r.right, r.top),
        Offset(r.right, r.bottom),
        Offset(r.left, r.bottom)
    ).map(::inv)

    val minX = pts.minOf { it.x }
    val maxX = pts.maxOf { it.x }
    val minY = pts.minOf { it.y }
    val maxY = pts.maxOf { it.y }
    return Rect(minX, minY, maxX, maxY)
}
