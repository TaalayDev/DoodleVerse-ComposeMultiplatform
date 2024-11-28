package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextField
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.add_new_layer
import doodleverse.composeapp.generated.resources.delete_layer
import doodleverse.composeapp.generated.resources.drag_handle
import doodleverse.composeapp.generated.resources.layer_preview
import doodleverse.composeapp.generated.resources.opacity
import doodleverse.composeapp.generated.resources.toggle_visibility
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawViewModel
import io.github.taalaydev.doodleverse.ui.screens.draw.layers
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.random.Random

@Composable
fun LayersPanel(
    drawViewModel: DrawViewModel,
    modifier: Modifier = Modifier,
) {
    val state by drawViewModel.state
    val layers = state.layers
    var list by remember { mutableStateOf(List(layers.size) { it }) }
    val caches = state.caches
    val activeLayerIndex = state.currentLayerIndex

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        list = list.toMutableList().apply { add(to.index, removeAt(from.index)) }
        drawViewModel.reorderLayers(from.index, to.index)
    }

    LaunchedEffect(layers.size) {
        list = List(layers.size) { it }
    }

    Column(modifier = modifier) {
        Divider()
        LayersPanelHeader(
            opacity = state.layers[state.currentLayerIndex].opacity.toFloat(),
            onOpacityChanged = { drawViewModel.changeLayerOpacity(activeLayerIndex, it) },
        ) {
            drawViewModel.addLayer("Layer ${layers.size}")
        }
        Divider()
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = lazyListState,
        ) {
            items(layers, key = { it.id }) { layer ->
                ReorderableItem(reorderableLazyListState, key = layer.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                    val index = layers.indexOfFirst { it.id == layer.id }

                    Surface(elevation = elevation) {
                        LayerTile(
                            layer = layer,
                            preview = caches[layer.id],
                            index = index,
                            isActive = index == activeLayerIndex,
                            onLayerSelected = { drawViewModel.selectLayer(it) },
                            onLayerVisibilityChanged = {
                                drawViewModel.layerVisibilityChanged(
                                    index,
                                    isVisible = !layer.isVisible
                                )
                            },
                            onLayerDeleted = { drawViewModel.deleteLayer(it) },
                            dragHandle = {
                                IconButton(
                                    onClick = { },
                                    modifier = Modifier.size(24.dp).draggableHandle()
                                ) {
                                    Icon(
                                        imageVector = Lucide.GripVertical,
                                        contentDescription = stringResource(Res.string.drag_handle),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersPanelHeader(
    opacity: Float,
    onOpacityChanged: (Float) -> Unit = {},
    onLayerAdded: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showOpacitySlider by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.opacity) + ": ",
                fontSize = 12.sp,
                color = Color.Gray,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(4.0.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.0.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable {
                        showOpacitySlider = true
                    }
            ) {
                Text(
                    text = (opacity * 100).toInt().toString() + "%",
                    fontSize = 12.sp,
                )
            }
            DropdownMenu(
                expanded = showOpacitySlider,
                onDismissRequest = {
                    showOpacitySlider = false
                },
                modifier = Modifier.width(200.dp).padding(horizontal = 8.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.opacity),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Slider(
                        value = opacity,
                        onValueChange = onOpacityChanged,
                        valueRange = 0f..1f,
                        steps = 100,
                        interactionSource = interactionSource,
                        colors = SliderDefaults.colors(),
                        thumb = { state ->
                            SliderDefaults.Thumb(
                                interactionSource = interactionSource,
                                thumbSize = androidx.compose.ui.unit.DpSize(4.dp, 16.dp),
                                colors = SliderDefaults.colors(),
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                colors = SliderDefaults.colors(),
                                sliderState = sliderState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            )
                        }
                    )
                }
            }
        }
        IconButton(
            onClick = { onLayerAdded() }
        ) {
            Icon(
                Lucide.Plus,
                contentDescription = stringResource(Res.string.add_new_layer),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun LayerTile(
    layer: LayerModel,
    preview: ImageBitmap? = null,
    index: Int,
    isActive: Boolean,
    onLayerSelected: (Int) -> Unit,
    onLayerVisibilityChanged: ((Int) -> Unit)? = null,
    onLayerDeleted: ((Int) -> Unit)? = null,
    dragHandle: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.surface
    val contentColor = if (isActive) Color.White else Color.Black

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onLayerSelected(index) }
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dragHandle?.invoke()
            LayerPreview(preview)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = layer.name,
                color = contentColor,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f).wrapContentHeight(),
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxHeight()
            ) {
                IconButton(
                    onClick = { onLayerVisibilityChanged?.invoke(index) },
                    enabled = onLayerVisibilityChanged != null,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (layer.isVisible) Lucide.Eye else Lucide.EyeOff,
                        contentDescription = stringResource(Res.string.toggle_visibility),
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = { onLayerDeleted?.invoke(index) },
                    enabled = onLayerDeleted != null,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Lucide.Trash,
                        contentDescription = stringResource(Res.string.delete_layer),
                        tint = contentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

        }
    }
}

@Composable
fun LayerPreview(image: ImageBitmap?) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.dp, Color.White)
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(Res.string.layer_preview),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}