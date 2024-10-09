package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash
import io.github.taalaydev.doodleverse.data.models.LayerModel
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


@Composable
fun LayersPanel(
    drawViewModel: DrawViewModel,
    modifier: Modifier = Modifier,
) {
    val state by drawViewModel.state
    val layers = state.layers
    val activeLayerIndex = state.currentLayerIndex

    Column(
        modifier = modifier,
    ) {
        Divider()
        LayersPanelHeader { name -> drawViewModel.addLayer(name) }
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(
                items = layers,
                key = { _, layer -> layer.id }
            ) { index, layer ->
                LayerTile(
                    layer = layer,
                    index = index,
                    isActive = index == activeLayerIndex,
                    onLayerSelected = { drawViewModel.selectLayer(it) },
                    onLayerVisibilityChanged = { drawViewModel.layerVisibilityChanged(it, !layer.isVisible) },
                    onLayerDeleted = { drawViewModel.deleteLayer(it) }
                )
            }
        }
    }
}

fun randomString(): String {
    return (1..10)
        .map { Random.nextInt(0, 36) }
        .joinToString("")
}

@Composable
fun LayersPanelHeader(onLayerAdded: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Layers",
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = { onLayerAdded("Layer ${randomString()}") }
        ) {
            Icon(Lucide.Plus, contentDescription = "Add new layer", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun LayerTile(
    layer: LayerModel,
    index: Int,
    isActive: Boolean,
    onLayerSelected: (Int) -> Unit,
    onLayerVisibilityChanged: (Int) -> Unit,
    onLayerDeleted: (Int) -> Unit
) {
    val backgroundColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
    val contentColor = if (isActive) Color.White else Color.Black

    Card(
        modifier = Modifier
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
            LayerPreview(layer)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = layer.name,
                color = contentColor,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onLayerVisibilityChanged(index) }
            ) {
                Icon(
                    imageVector = if (layer.isVisible) Lucide.Eye else Lucide.EyeOff,
                    contentDescription = "Toggle visibility",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { onLayerDeleted(index) }
            ) {
                Icon(
                    imageVector = Lucide.Trash,
                    contentDescription = "Delete layer",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun LayerPreview(layer: LayerModel) {
    var previewImage by remember { mutableStateOf<ImageBitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(layer) {
        coroutineScope.launch {
            delay(500) // Debounce

        }
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.dp, Color.White)
    ) {
        if (previewImage != null) {
            Image(
                bitmap = previewImage!!,
                contentDescription = "Layer preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Lucide.Image,
                contentDescription = "Layer preview placeholder",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}