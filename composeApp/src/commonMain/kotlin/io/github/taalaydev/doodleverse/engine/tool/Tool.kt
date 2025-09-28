package io.github.taalaydev.doodleverse.engine.tool

import kotlin.jvm.JvmInline

interface Tool {
    /**
     * Unique identifier for the brush.
     * */
    val id: ToolId

    /**
     * Human-readable name for the brush.
     * */
    val name: String
}

@JvmInline
value class ToolId(val value: String)

