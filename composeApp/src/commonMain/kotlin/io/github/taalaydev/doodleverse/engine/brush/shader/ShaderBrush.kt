package io.github.taalaydev.doodleverse.engine.brush.shader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import io.github.taalaydev.doodleverse.PlatformShader
import io.github.taalaydev.doodleverse.ShaderFactory
import io.github.taalaydev.doodleverse.engine.util.*
import io.github.taalaydev.doodleverse.engine.gesture.GestureEvent
import io.github.taalaydev.doodleverse.engine.tool.ProceduralBrush
import io.github.taalaydev.doodleverse.engine.tool.BrushParams
import io.github.taalaydev.doodleverse.engine.tool.StrokeSession
import io.github.taalaydev.doodleverse.engine.tool.ToolId
import kotlinx.datetime.Clock
import kotlin.math.*

/**
 * State passed to shader effects for dynamic updates
 */
data class ShaderState(
    val position: Offset,
    val pressure: Float,
    val velocity: Float,
    val time: Float,
    val strokeLength: Float,
    val color: Color,
    val size: Float
)

/**
 * A brush that can use platform-specific shaders or fallback to procedural effects.
 * On platforms that support shaders (Android 33+, iOS with Metal, etc.), it will use
 * native shader rendering. Otherwise, it falls back to procedural effects.
 *
 * @param shaderSource Optional shader source code (platform-specific format)
 * @param proceduralEffect Procedural effect function for fallback/cross-platform support
 * @param uniformUpdater Optional function to update shader uniforms
 * @param brushName Custom name for this shader brush
 */
class ShaderBrush(
    private val shaderSource: String? = null,
    private val proceduralEffect: ProceduralEffect = ProceduralEffect.Rainbow,
    private val uniformUpdater: ((PlatformShader, ShaderState) -> Unit)? = null,
    private val brushName: String = "Shader Brush"
) : ProceduralBrush() {

    override val id = ToolId("shader_brush_${shaderSource?.hashCode() ?: proceduralEffect.hashCode()}")
    override val name: String = brushName

    override fun startSession(
        canvas: Canvas,
        params: BrushParams,
    ): StrokeSession = ShaderStrokeSession(
        canvas,
        shaderSource,
        proceduralEffect,
        params,
        uniformUpdater
    )

    private class ShaderStrokeSession(
        private val canvas: Canvas,
        private val shaderSource: String?,
        private val proceduralEffect: ProceduralEffect,
        params: BrushParams,
        private val uniformUpdater: ((PlatformShader, ShaderState) -> Unit)?
    ) : StrokeSession(params) {

        private val startTime = Clock.System.now().toEpochMilliseconds()
        private val platformShader = shaderSource?.let { ShaderFactory.create(it) }
        private val useNativeShader = platformShader != null && ShaderFactory.isSupported()

        private val paint = Paint().apply {
            isAntiAlias = true
            blendMode = params.blendMode
            if (useNativeShader) {
                shader = platformShader?.toShader()
            }
        }

        private val stepPx: Float = params.size * 0.2f
        private val path = Path()

        // State tracking
        private var p0: Offset? = null
        private var p1: Offset? = null
        private var lastMid: Offset? = null
        private var lastPressure: Float = params.pressure
        private var lastVelocity: Float = params.velocity
        private var residual: Float = 0f
        private var totalStrokeLength: Float = 0f

        init {
            // Set default uniforms if using native shader
            if (useNativeShader && platformShader != null) {
                platformShader.setUniform("u_size", params.size)
                platformShader.setUniform("u_color",
                    params.color.red, params.color.green,
                    params.color.blue, params.color.alpha)
                platformShader.setUniform("u_time", 0f)
                platformShader.setUniform("u_pressure", params.pressure)
            }
        }

        private fun dist(a: Offset, b: Offset): Float = hypot(b.x - a.x, b.y - a.y)
        private fun midevent(a: Offset, b: Offset): Offset = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)

        private fun radiusFor(pressure: Float): Float =
            max(0.5f, params.size * pressure * 0.5f)

        private fun drawStamp(
            canvas: Canvas,
            center: Offset,
            radius: Float,
            pressure: Float,
            velocity: Float
        ): Rect {
            val currentTime = (Clock.System.now().toEpochMilliseconds() - startTime) / 1000f

            val state = ShaderState(
                position = center,
                pressure = pressure,
                velocity = velocity,
                time = currentTime,
                strokeLength = totalStrokeLength,
                color = params.color,
                size = params.size
            )

            return if (useNativeShader && platformShader != null) {
                // Update shader uniforms
                platformShader.setUniform("u_time", currentTime)
                platformShader.setUniform("u_pressure", pressure)
                platformShader.setUniform("u_velocity", velocity)
                platformShader.setUniform("u_strokeLength", totalStrokeLength)
                platformShader.setUniform("u_position", center.x, center.y)

                // Call custom uniform updater
                uniformUpdater?.invoke(platformShader, state)

                paint.alpha = params.color.alpha * (0.5f + pressure * 0.5f)
                canvas.drawCircle(center, radius, paint)

                Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
            } else {
                // Use procedural effect fallback
                proceduralEffect.apply(canvas, center, radius, state, paint)
            }
        }

        private fun qPoint(a: Offset, b: Offset, c: Offset, t: Float): Offset {
            val one = 1f - t
            val x = one * one * a.x + 2f * one * t * b.x + t * t * c.x
            val y = one * one * a.y + 2f * one * t * b.y + t * t * c.y
            return Offset(x, y)
        }

        private fun walkQuadratic(
            canvas: Canvas,
            a: Offset,
            b: Offset,
            c: Offset,
            pStart: Float,
            pEnd: Float,
            vStart: Float,
            vEnd: Float
        ): DirtyRect {
            val approxLen = dist(a, c) + 0.5f * (dist(a, b) + dist(b, c) - dist(a, c))
            val subdivisions = max(8, ceil(approxLen / 6f).toInt())
            val dt = 1f / subdivisions

            var dirty: DirtyRect = null
            var prev = a
            var acc = residual

            var t = dt
            while (t <= 1f + 1e-4f) {
                val cur = qPoint(a, b, c, t.coerceAtMost(1f))
                val seg = dist(prev, cur)
                totalStrokeLength += seg

                var remain = seg
                while (acc + remain >= stepPx && remain > 0f) {
                    val need = stepPx - acc
                    val f = (need / remain).coerceIn(0f, 1f)
                    val hit = prev.lerp(cur, f)
                    val tt = (t - dt) + dt * f
                    val press = pStart + (pEnd - pStart) * tt
                    val vel = vStart + (vEnd - vStart) * tt

                    dirty = dirty.union(
                        drawStamp(canvas, hit, radiusFor(press), press, vel)
                    )

                    prev = hit
                    remain -= need
                    acc = 0f
                }

                acc += remain
                prev = cur
                t += dt
            }

            residual = acc
            return dirty
        }

        override fun start(event: GestureEvent): DirtyRect {
            p0 = event.position
            p1 = null
            lastMid = null
            residual = 0f
            totalStrokeLength = 0f
            lastPressure = event.pressure ?: params.pressure
            lastVelocity = event.velocity ?: params.velocity

            path.reset()
            path.moveTo(event.position.x, event.position.y)

            return drawStamp(
                canvas,
                event.position,
                radiusFor(lastPressure),
                lastPressure,
                lastVelocity
            )
        }

        override fun move(event: GestureEvent): DirtyRect {
            val newP = event.position
            val newPressure = event.pressure ?: params.pressure
            val newVelocity = event.velocity ?: params.velocity

            var dirty: DirtyRect = null
            when {
                p0 == null -> {
                    dirty = start(event)
                }
                p1 == null -> {
                    p1 = newP
                    val a = p0!!
                    val m01 = midevent(a, newP)
                    if (lastMid == null) {
                        path.moveTo(a.x, a.y)
                        lastMid = a
                    }
                    path.quadraticBezierTo(a.x, a.y, m01.x, m01.y)
                    dirty = dirty.union(
                        walkQuadratic(
                            canvas = canvas,
                            a = lastMid!!,
                            b = a,
                            c = m01,
                            pStart = lastPressure,
                            pEnd = newPressure,
                            vStart = lastVelocity,
                            vEnd = newVelocity
                        )
                    )
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
                    dirty = dirty.union(
                        walkQuadratic(
                            canvas = canvas,
                            a = lastMid!!,
                            b = a1,
                            c = m2,
                            pStart = lastPressure,
                            pEnd = newPressure,
                            vStart = lastVelocity,
                            vEnd = newVelocity
                        )
                    )

                    lastMid = m2
                    p0 = a1
                    p1 = a2
                }
            }

            lastPressure = newPressure
            lastVelocity = newVelocity
            return dirty
        }

        override fun end(event: GestureEvent): DirtyRect {
            val endPos = event.position
            val endPressure = event.pressure ?: params.pressure
            val endVelocity = event.velocity ?: params.velocity
            val moved = move(event)

            var dirty: DirtyRect = moved
            val a = lastMid
            val ctrl = p1
            if (a != null && ctrl != null) {
                path.quadraticBezierTo(ctrl.x, ctrl.y, ctrl.x, ctrl.y)
                dirty = dirty.union(
                    walkQuadratic(
                        canvas = canvas,
                        a = a,
                        b = ctrl,
                        c = ctrl,
                        pStart = lastPressure,
                        pEnd = endPressure,
                        vStart = lastVelocity,
                        vEnd = endVelocity
                    )
                )
            }

            dirty = dirty.union(
                drawStamp(canvas, endPos, radiusFor(endPressure), endPressure, endVelocity)
            )

            return dirty
        }
    }
}