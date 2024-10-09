package io.github.taalaydev.doodleverse.core

import io.github.taalaydev.doodleverse.data.models.BrushData

object ToolsManager {

    private val _brushes: MutableList<BrushData> = mutableListOf()
    private val _figures: MutableList<BrushData> = mutableListOf()

    lateinit var eraser: BrushData
    lateinit var pencil: BrushData
    lateinit var defaultBrush: BrushData

    // Getters for brushes, figures, and allTools
    val brushes: List<BrushData>
        get() = _brushes

    val figures: List<BrushData>
        get() = _figures

    val allTools: List<BrushData>
        get() = listOf(eraser) + _brushes + _figures

    // Function to get a specific brush by id
    fun getBrush(id: Int): BrushData {
        return allTools.first { it.id == id }
    }

    // Asynchronous function to load tools using Kotlin Coroutines
    suspend fun loadTools() {
        // Adding brushes
        _brushes.addAll(
            listOf(
                defaultBrush,
                pencil,
            )
        )

        // Adding figures
        _figures.addAll(
            listOf()
        )
    }
}
