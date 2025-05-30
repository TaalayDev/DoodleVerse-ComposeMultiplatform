package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.ColorEnvelope
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import io.github.taalaydev.doodleverse.core.color.ColorPaletteGenerator
import kotlinx.coroutines.launch

/**
 * Enhanced color picker with AI palette generation capabilities
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EnhancedColorPickerSheet(
    initialColor: Color = Color(0xFF333333),
    onColorSelected: (Color) -> Unit = {},
    bottomSheetState: SheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val controller = rememberColorPickerController()
    val colorPaletteGenerator = remember { ColorPaletteGenerator() }

    var showAiPalettes by remember { mutableStateOf(false) }
    var currentColor by remember { mutableStateOf(initialColor) }
    var generatedPalette by remember { mutableStateOf<ColorPaletteGenerator.Palette?>(null) }
    var selectedPaletteType by remember { mutableStateOf(ColorPaletteGenerator.PaletteType.COMPLEMENTARY) }

    // Load initial color
    LaunchedEffect(Unit) {
        controller.selectByColor(initialColor, false)
        controller.setAlpha(initialColor.alpha, false)
    }

    // Generate palette when color or type changes
    LaunchedEffect(currentColor, selectedPaletteType) {
        generatedPalette = colorPaletteGenerator.generatePalette(
            baseColor = currentColor,
            type = selectedPaletteType
        )
    }

    ModalBottomSheet(
        sheetState = bottomSheetState,
        onDismissRequest = { onDismiss() },
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                // Header with toggle for AI palettes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Color Picker",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Palettes")
                        Switch(
                            checked = showAiPalettes,
                            onCheckedChange = { showAiPalettes = it }
                        )
                    }
                }

                // Standard color picker
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (showAiPalettes) 250.dp else 450.dp)
                        .padding(10.dp),
                    controller = controller,
                    onColorChanged = { colorEnvelope: ColorEnvelope ->
                        if (colorEnvelope.fromUser) {
                            currentColor = colorEnvelope.color
                            onColorSelected(colorEnvelope.color)
                        }
                    },
                )

                Spacer(modifier = Modifier.height(10.dp))

                AlphaSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .height(35.dp),
                    controller = controller,
                )

                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .height(35.dp),
                    controller = controller,
                )

                // AI-generated color palettes
                AnimatedVisibility(
                    visible = showAiPalettes,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (generatedPalette != null) {
                            Divider(modifier = Modifier.padding(vertical = 16.dp))

                            // Palette type selector
                            Text(
                                text = "Palette Type",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Palette type chips
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ColorPaletteGenerator.PaletteType.entries.forEach { type ->
                                    FilterChip(
                                        selected = selectedPaletteType == type,
                                        onClick = { selectedPaletteType = type },
                                        label = {
                                            Text(
                                                text = type.name
                                                    .lowercase()
                                                    .capitalize()
                                                    .replace("_", " ")
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Generated palette display
                            Text(
                                text = "AI-Generated Palette",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Color palette
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                generatedPalette?.colors?.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(color)
                                            .clickable {
                                                controller.selectByColor(color, true)
                                                onColorSelected(color)
                                            }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        generatedPalette?.let {
                                            scope.launch {
                                                //savePalette(it)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Lucide.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Palette")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        generatedPalette?.let {
                                            //sharePalette(it)
                                        }
                                    }
                                ) {
                                    Icon(Lucide.Share2, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

/**
 * Button to open the enhanced color picker
 */
@Composable
fun EnhancedColorPickerButton(
    currentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(currentColor, RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            Lucide.Wand,
            contentDescription = "AI Color Palette",
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(4.dp))

        Text("Pick Color")
    }
}

// Extension to capitalize first letter of string
private fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}