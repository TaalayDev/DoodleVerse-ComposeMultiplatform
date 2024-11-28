package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.add_custom_color
import doodleverse.composeapp.generated.resources.color_palette
import org.jetbrains.compose.resources.stringResource

val Color.Companion.Pink: Color
    get() = Color(0xFFE91E63)
val Color.Companion.Purple: Color
    get() = Color(0xFF9C27B0)

@Composable
fun ColorPalettePanel(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = rememberColorPickerController()

    var customColors by remember { mutableStateOf(listOf<Color>()) }

    val predefinedColors = listOf(
        Color(0xFF333333),
        Color(0xFFE57373),
        Color(0xFF81C784),
        Color(0xFF64B5F6),
        Color(0xFF9575CD),
        Color(0xFFFFD54F),
        Color(0xFF4DB6AC),
        Color(0xFFA1887F),
        Color(0xFF90A4AE),
        Color(0xFFFFFFFF),
        // Add more colors here...
    )

    LaunchedEffect(Unit) {
        controller.selectByColor(currentColor, false)
        controller.setAlpha(currentColor.alpha, false)
    }

    Column(modifier = modifier.padding(8.dp)) {
        Text(
            text = stringResource(Res.string.color_palette),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        HsvColorPicker(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(10.dp),
            controller = controller,
            onColorChanged = { colorEnvelope: ColorEnvelope ->
                if (colorEnvelope.fromUser) {
                    onColorSelected(colorEnvelope.color)
                }
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        AlphaSlider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .height(20.dp),
            controller = controller,
        )
        BrightnessSlider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .height(20.dp),
            controller = controller,
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp)
        ) {
            itemsIndexed(predefinedColors + customColors) { index, color ->
                ColorItem(color = color, onColorSelected = {
                    onColorSelected(it)
                    controller.selectByColor(it, false)
                    controller.setAlpha(it.alpha, false)
                })
            }
            item {
                AddColorButton(onColorAdd = {
                    customColors = customColors + currentColor
                })
            }
        }
    }
}

@Composable
fun ColorItem(color: Color, onColorSelected: (Color) -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(color)
            .shadow(0.2.dp, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
            .clickable { onColorSelected(color) }
    )
}

@Composable
fun AddColorButton(onColorAdd: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .shadow(0.2.dp)
            .clickable { onColorAdd() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Lucide.Plus, contentDescription = stringResource(Res.string.add_custom_color), modifier = Modifier.size(18.dp))
    }
}
