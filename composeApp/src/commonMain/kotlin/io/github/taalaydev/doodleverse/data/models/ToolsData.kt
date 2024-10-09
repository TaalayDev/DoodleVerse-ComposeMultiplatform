package io.github.taalaydev.doodleverse.data.models

data class ToolsData(
    val defaultBrush: BrushData,
    val pencil: BrushData,
    val eraser: BrushData,
    val brushes: List<BrushData>,
    val figures: List<BrushData>
)