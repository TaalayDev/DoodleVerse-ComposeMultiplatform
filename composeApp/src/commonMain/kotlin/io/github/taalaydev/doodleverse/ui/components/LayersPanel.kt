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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.GripVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Merge
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash
import com.composables.icons.lucide.X
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.add_new_layer
import doodleverse.composeapp.generated.resources.cancel
import doodleverse.composeapp.generated.resources.clear_layer
import doodleverse.composeapp.generated.resources.copy_layer
import doodleverse.composeapp.generated.resources.delete_layer
import doodleverse.composeapp.generated.resources.drag_handle
import doodleverse.composeapp.generated.resources.edit_layer_name
import doodleverse.composeapp.generated.resources.layer_name
import doodleverse.composeapp.generated.resources.layer_preview
import doodleverse.composeapp.generated.resources.merge_down
import doodleverse.composeapp.generated.resources.opacity
import doodleverse.composeapp.generated.resources.rename_layer
import doodleverse.composeapp.generated.resources.save
import doodleverse.composeapp.generated.resources.toggle_visibility
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.engine.DrawingState
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun LayersPanel(
    drawViewModel: io.github.taalaydev.doodleverse.ui.screens.canvas.DrawViewModel,
    modifier: Modifier = Modifier,
) {
    val drawController = drawViewModel.drawController
    val state = drawController.state.collectAsStateWithLifecycle()

    LayersPanel(
        state = drawController.state,
        addLayer = { drawController.addLayer("Layer ${state.value.layers.size + 1}") },
        reorderLayers = { from, to -> drawController.reorderLayers(from, to) },
        changeLayerOpacity = { layerIndex, opacity ->
            drawController.changeLayerOpacity(layerIndex, opacity)
        },
        getLayerBitmap = { layerId -> drawController.getLayerBitmap(layerId) },
        isLayerEmpty = { layerId -> drawController.isLayerEmpty(layerId) },
        selectLayer = { layerIndex -> drawController.selectLayer(layerIndex) },
        deleteLayer = { layerIndex -> drawController.deleteLayer(layerIndex) },
        duplicateLayer = { layerIndex -> drawViewModel.duplicateLayer(layerIndex) },
        updateLayerName = { layerIndex, newName ->
            drawViewModel.updateLayerName(layerIndex, newName)
        },
        clearLayer = { layerIndex -> drawViewModel.clearLayer(layerIndex) },
        layerVisibilityChanged = { layerIndex, isVisible ->
            drawViewModel.layerVisibilityChanged(layerIndex, isVisible)
        },
        mergeLayerDown = { layerIndex -> drawViewModel.mergeLayerDown(layerIndex) },
        modifier = modifier
    )
}

@Composable
fun LayersPanel(
    state: StateFlow<DrawingState>,
    addLayer: () -> Unit,
    reorderLayers: (fromIndex: Int, toIndex: Int) -> Unit,
    changeLayerOpacity: (layerIndex: Int, opacity: Float) -> Unit,
    getLayerBitmap: (layerId: Long) -> ImageBitmap?,
    isLayerEmpty: (layerId: Long) -> Boolean,
    selectLayer: (layerIndex: Int) -> Unit,
    deleteLayer: (layerIndex: Int) -> Unit,
    duplicateLayer: (layerIndex: Int) -> Unit,
    updateLayerName: (layerIndex: Int, newName: String) -> Unit,
    clearLayer: (layerIndex: Int) -> Unit,
    layerVisibilityChanged: (layerIndex: Int, isVisible: Boolean) -> Unit,
    mergeLayerDown: (layerIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawingState by state.collectAsStateWithLifecycle()
    val layers = drawingState.currentFrame.layers
    val currentLayerIndex = drawingState.currentLayerIndex

    var reorderableList by remember { mutableStateOf(layers.indices.toList()) }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        reorderableList = reorderableList.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        reorderLayers(from.index, to.index)
    }

    LaunchedEffect(layers.size) {
        reorderableList = layers.indices.toList()
    }

    Column(modifier = modifier) {
        Divider()

        LayersPanelHeader(
            currentLayer = layers.getOrNull(currentLayerIndex),
            onOpacityChanged = { opacity ->
                changeLayerOpacity(currentLayerIndex, opacity)
            },
            onLayerAdded = {
                addLayer()
            }
        )

        Divider()

        if (layers.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = lazyListState,
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(
                    items = layers,
                    key = { _, layer -> layer.id }
                ) { index, layer ->
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = layer.id
                    ) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)

                        Surface(
                            elevation = elevation,
                            color = Color.Transparent
                        ) {
                            LayerTile(
                                layer = layer,
                                preview = getLayerBitmap(layer.id),
                                index = index,
                                isActive = index == currentLayerIndex,
                                isEmpty = isLayerEmpty(layer.id),
                                onLayerSelected = { selectLayer(it) },
                                onLayerVisibilityChanged = { layerIndex ->
                                    layerVisibilityChanged(
                                        layerIndex,
                                        !layer.isVisible
                                    )
                                },
                                onLayerDeleted = { layerIndex ->
                                    if (layers.size > 1) {
                                        deleteLayer(layerIndex)
                                    }
                                },
                                onLayerDuplicated = { layerIndex ->
                                    duplicateLayer(layerIndex)
                                },
                                onLayerRenamed = { layerIndex, newName ->
                                    updateLayerName(layerIndex, newName)
                                },
                                onLayerCleared = { layerIndex ->
                                    clearLayer(layerIndex)
                                },
                                onMergeDown = { layerIndex ->
                                    if (layerIndex > 0) {
                                        mergeLayerDown(layerIndex)
                                    }
                                },
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
        } else {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No layers available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersPanelHeader(
    currentLayer: LayerModel?,
    onOpacityChanged: (Float) -> Unit = {},
    onLayerAdded: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showOpacitySlider by remember { mutableStateOf(false) }

    val opacity = currentLayer?.opacity?.toFloat() ?: 1f

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
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable {
                        showOpacitySlider = true
                    }
            ) {
                Text(
                    text = "${(opacity * 100).toInt()}%",
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(Res.string.opacity),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                                thumbSize = androidx.compose.ui.unit.DpSize(12.dp, 20.dp),
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
            onClick = onLayerAdded
        ) {
            Icon(
                Lucide.Plus,
                contentDescription = stringResource(Res.string.add_new_layer),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
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
    isEmpty: Boolean = false,
    onLayerSelected: (Int) -> Unit,
    onLayerVisibilityChanged: ((Int) -> Unit)? = null,
    onLayerDeleted: ((Int) -> Unit)? = null,
    onLayerDuplicated: ((Int) -> Unit)? = null,
    onLayerRenamed: ((Int, String) -> Unit)? = null,
    onLayerCleared: ((Int) -> Unit)? = null,
    onMergeDown: ((Int) -> Unit)? = null,
    dragHandle: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.surface

    val contentColor = if (isActive)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface

    var showContextMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onLayerSelected(index) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier.height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dragHandle?.invoke()

            LayerPreview(
                image = preview,
                isEmpty = isEmpty,
                isVisible = layer.isVisible
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = layer.name,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1
                )

                if (layer.opacity < 1.0 || isEmpty) {
                    Text(
                        text = buildString {
                            if (layer.opacity < 1.0) {
                                append("${(layer.opacity * 100).toInt()}%")
                            }
                            if (isEmpty) {
                                if (layer.opacity < 1.0) append(" • ")
                                append("Empty")
                            }
                        },
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onLayerVisibilityChanged?.invoke(index) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (layer.isVisible) Lucide.Eye else Lucide.EyeOff,
                        contentDescription = stringResource(Res.string.toggle_visibility),
                        tint = if (layer.isVisible) contentColor else contentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Layer options",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    LayerContextMenu(
                        expanded = showContextMenu,
                        onDismiss = { showContextMenu = false },
                        canDelete = onLayerDeleted != null,
                        canMergeDown = onMergeDown != null && index > 0,
                        onRename = {
                            showContextMenu = false
                            showRenameDialog = true
                        },
                        onDuplicate = {
                            showContextMenu = false
                            onLayerDuplicated?.invoke(index)
                        },
                        onClear = {
                            showContextMenu = false
                            onLayerCleared?.invoke(index)
                        },
                        onMergeDown = {
                            showContextMenu = false
                            onMergeDown?.invoke(index)
                        },
                        onDelete = {
                            showContextMenu = false
                            onLayerDeleted?.invoke(index)
                        }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        LayerRenameDialog(
            currentName = layer.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onLayerRenamed?.invoke(index, newName)
                showRenameDialog = false
            }
        )
    }
}

@Composable
fun LayerPreview(
    image: ImageBitmap?,
    isEmpty: Boolean = false,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                if (isEmpty) Color.Transparent else Color.White.copy(alpha = 0.8f),
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isEmpty -> {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = "Empty layer",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            image != null -> {
                Image(
                    bitmap = image,
                    contentDescription = stringResource(Res.string.layer_preview),
                    contentScale = ContentScale.Crop,
                    alpha = if (isVisible) 1f else 0.5f,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun LayerContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    canDelete: Boolean = true,
    canMergeDown: Boolean = false,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onClear: () -> Unit,
    onMergeDown: () -> Unit,
    onDelete: () -> Unit
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Text(stringResource(Res.string.rename_layer))
                }
            },
            onClick = onRename
        )

        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Lucide.Copy, null, modifier = Modifier.size(16.dp), tint = contentColor)
                    Text(stringResource(Res.string.copy_layer), color = contentColor)
                }
            },
            onClick = onDuplicate
        )

        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Lucide.X, null, modifier = Modifier.size(16.dp), tint = contentColor)
                    Text(stringResource(Res.string.clear_layer), color = contentColor)
                }
            },
            onClick = onClear
        )

        if (canMergeDown) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Lucide.Merge, null, modifier = Modifier.size(16.dp), tint = contentColor)
                        Text(stringResource(Res.string.merge_down), color = contentColor)
                    }
                },
                onClick = onMergeDown
            )
        }

        if (canDelete) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Lucide.Trash,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            stringResource(Res.string.delete_layer),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                onClick = onDelete
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerRenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.edit_layer_name))
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(Res.string.layer_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName.trim())
                    }
                },
                enabled = newName.isNotBlank() && newName.trim() != currentName
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}