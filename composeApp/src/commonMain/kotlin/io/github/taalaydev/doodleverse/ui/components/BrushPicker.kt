package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.unit.dp
import io.github.taalaydev.doodleverse.core.DrawRenderer
import io.github.taalaydev.doodleverse.data.models.BrushData
import io.github.taalaydev.doodleverse.data.models.DrawingPath


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrushPicker(
    bottomSheetState: SheetState,
    selectedBrush: BrushData? = null,
    onSelected: (BrushData) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val brushes = BrushData.all()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = bottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            color = MaterialTheme.colors.surface,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BrushGrid(
                    brushes = brushes,
                    selectedBrush = selectedBrush,
                    onSelected = onSelected,
                )
            }
        }
    }
}

@Composable
fun BrushGrid(
    brushes: List<BrushData>,
    selectedBrush: BrushData? = null,
    onSelected: (BrushData) -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(brushes.size) { index ->
            BrushPreview(
                brushes[index],
                isSelected = selectedBrush?.id == brushes[index].id,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelected(brushes[index]) },
            )
        }
    }
}

@Composable
fun BrushPreview(
    brush: BrushData,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var imageCanvas by remember { mutableStateOf<Canvas?>(null) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val initialPath = remember(canvasSize) {
        val width = canvasSize.width.coerceAtLeast(1f)
        val height = canvasSize.height.coerceAtLeast(1f)
        val midWidth = width / 2
        val midHeight = height / 2
        val quarterWidth = width / 4
        val quarterHeight = height / 4
        val eighthWidth = width / 8

        DrawingPath(
            brush = brush,
            color = Color.Black,
            size = 10f,
            path = Path().apply {
                moveTo(15f, midHeight)
                cubicTo(
                    15f, midHeight - quarterHeight,
                    midWidth - eighthWidth, midHeight - quarterHeight,
                    midWidth, midHeight
                )
                cubicTo(
                    midWidth + eighthWidth, midHeight + quarterHeight,
                    width - 15f, midHeight + quarterHeight,
                    width - 15f, midHeight
                )

                moveTo(width - 15f, midHeight)

                close()
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colors.primary else Color.LightGray,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .padding(8.dp),
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize(),
            ) {
                val canvasWidth = size.width.toInt().coerceAtLeast(1)
                val canvasHeight = size.height.toInt().coerceAtLeast(1)
                if (bitmap == null || bitmap?.width != canvasWidth || bitmap?.height != canvasHeight) {
                    bitmap = ImageBitmap(canvasWidth, canvasHeight)
                    imageCanvas = Canvas(bitmap!!)
                    canvasSize = size
                }

                if (brush.brush != null) {
                    DrawRenderer.drawBrushStampsBetweenPoints(
                        imageCanvas!!,
                        Offset(0f, 0f),
                        Offset(size.width, size.height),
                        Paint().apply {
                            color = Color.Black
                            style = PaintingStyle.Stroke
                            strokeWidth = 10f
                            strokeCap = StrokeCap.Round
                            strokeJoin = StrokeJoin.Round
                        },
                        initialPath,
                    )
                } else {
                    DrawRenderer.drawPath(
                        canvas = imageCanvas!!,
                        drawingPath = initialPath,
                        paint = Paint().apply {
                            color = Color.Black
                            style = PaintingStyle.Stroke
                            strokeWidth = 10f
                            strokeCap = brush.strokeCap
                            strokeJoin = brush.strokeJoin
                            alpha = 1f - brush.opacityDiff
                        },
                        size = size,
                    )
                }

                if (bitmap != null) {
                    drawImage(bitmap!!, Offset(0f, 0f))
                }
            }
        }
        Text(text = brush.name)
    }
}