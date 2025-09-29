package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.taalaydev.doodleverse.engine.components.BrushPreview
import io.github.taalaydev.doodleverse.engine.tool.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrushPicker(
    bottomSheetState: SheetState,
    brushes: List<Brush>,
    selectedBrush: Brush? = null,
    onBrushSelected: (Brush) -> Unit = {},
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BrushGrid(
                    brushes = brushes,
                    selectedBrush = selectedBrush,
                    onSelected = onBrushSelected,
                )
            }
        }
    }
}


@Composable
fun BrushGrid(
    brushes: List<Brush>,
    selectedBrush: Brush? = null,
    onSelected: (Brush) -> Unit = {},
) {
    val listState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        val index = brushes.indexOf(selectedBrush)
        if (index != -1) {
            listState.scrollToItem(index)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
    ) {
        items(brushes.size) { index ->
            BrushPreview(
                brushes[index],
                isSelected = selectedBrush == brushes[index],
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(brushes[index]) },
            )
        }
    }
}

@Composable
fun BrushList(
    brushes: List<Brush>,
    selectedBrush: Brush? = null,
    onSelected: (Brush) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(brushes.size) { index ->
            BrushPreview(
                brush = brushes[index],
                isSelected = selectedBrush == brushes[index],
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(brushes[index]) },
            )
        }
    }
}