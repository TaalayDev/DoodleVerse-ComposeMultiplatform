package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.taalaydev.doodleverse.brush.allPremiumBrushes
import io.github.taalaydev.doodleverse.core.Tool
import io.github.taalaydev.doodleverse.data.models.BrushData

@Composable
fun ToolPanel(
    selectedTool: Tool,
    selectedBrush: BrushData? = null,
    selectedColor: Color = Color.Black,
    onBrushSelected: (BrushData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val brushes = remember { BrushData.all() + allPremiumBrushes }

    Surface(
        modifier = modifier,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BrushList(
                brushes = brushes,
                selectedBrush = selectedBrush,
                onSelected = onBrushSelected,
            )
        }
    }
}