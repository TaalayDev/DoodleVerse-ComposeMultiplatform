package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.*
import io.github.taalaydev.doodleverse.core.color.ColorPaletteGenerator
import kotlinx.coroutines.launch

/**
 * Dialog for generating and managing color palettes
 */
@Composable
fun ColorPaletteDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    extractFromImage: ((ImageBitmap) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val colorPaletteGenerator = remember { ColorPaletteGenerator() }

    var baseColor by remember { mutableStateOf(initialColor) }
    var selectedPaletteType by remember { mutableStateOf(ColorPaletteGenerator.PaletteType.COMPLEMENTARY) }
    var generatedPalette by remember { mutableStateOf<ColorPaletteGenerator.Palette?>(null) }
    var savedPalettes by remember { mutableStateOf<List<ColorPaletteGenerator.Palette>>(emptyList()) }

    // Load saved palettes
    LaunchedEffect(Unit) {
        // savedPalettes = loadSavedPalettes()
    }

    // Generate initial palette
    LaunchedEffect(baseColor, selectedPaletteType) {
        generatedPalette = colorPaletteGenerator.generatePalette(
            baseColor = baseColor,
            type = selectedPaletteType
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Color Palette",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Lucide.X, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Generate") },
                        icon = { Icon(Lucide.Palette, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Saved") },
                        icon = { Icon(Lucide.Save, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Image") },
                        icon = { Icon(Lucide.Image, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> GenerateTab(
                            baseColor = baseColor,
                            onBaseColorChange = { baseColor = it },
                            selectedPaletteType = selectedPaletteType,
                            onPaletteTypeChange = { selectedPaletteType = it },
                            generatedPalette = generatedPalette,
                            onSavePalette = {
                                scope.launch {
                                    generatedPalette?.let {
                                        //savePalette(it)
                                        //savedPalettes = loadSavedPalettes()
                                    }
                                }
                            },
                            onSharePalette = {
                                // generatedPalette?.let { sharePalette(it) }
                            },
                            onColorSelected = onColorSelected
                        )
                        1 -> SavedPalettesTab(
                            palettes = savedPalettes,
                            onPaletteDeleted = {
                                scope.launch {
                                    // savedPalettes = loadSavedPalettes().filter { palette -> palette.id != it.id }
                                }
                            },
                            onColorSelected = onColorSelected
                        )
                        2 -> ImagePaletteTab(
                            onExtractFromImage = { extractFromImage?.invoke(it) },
                            onColorSelected = onColorSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenerateTab(
    baseColor: Color,
    onBaseColorChange: (Color) -> Unit,
    selectedPaletteType: ColorPaletteGenerator.PaletteType,
    onPaletteTypeChange: (ColorPaletteGenerator.PaletteType) -> Unit,
    generatedPalette: ColorPaletteGenerator.Palette?,
    onSavePalette: () -> Unit,
    onSharePalette: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Base color selector
        Text(
            text = "Base Color",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        ColorPickerCompact(
            color = baseColor,
            onColorChange = onBaseColorChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Palette type selector
        Text(
            text = "Palette Type",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ColorPaletteGenerator.PaletteType.values()) { type ->
                PaletteTypeChip(
                    type = type,
                    selected = selectedPaletteType == type,
                    onClick = { onPaletteTypeChange(type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Generated palette
        if (generatedPalette != null) {
            Text(
                text = "Generated Palette",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            ColorPaletteDisplay(
                palette = generatedPalette,
                onColorSelected = onColorSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onSharePalette,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Lucide.Share2, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }

                Button(onClick = onSavePalette) {
                    Icon(Lucide.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun SavedPalettesTab(
    palettes: List<ColorPaletteGenerator.Palette>,
    onPaletteDeleted: (ColorPaletteGenerator.Palette) -> Unit,
    onColorSelected: (Color) -> Unit
) {
    if (palettes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Lucide.Save,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No saved palettes yet",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Generate and save a palette to see it here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            palettes.forEach { palette ->
                SavedPaletteItem(
                    palette = palette,
                    onDelete = { onPaletteDeleted(palette) },
                    onColorSelected = onColorSelected
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ImagePaletteTab(
    onExtractFromImage: (ImageBitmap) -> Unit,
    onColorSelected: (Color) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Lucide.Upload,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Extract colors from image",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Upload an image to extract a color palette",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // This would trigger file picking functionality
                // which would then call onExtractFromImage with the selected image
            }
        ) {
            Icon(Lucide.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Upload Image")
        }
    }
}

@Composable
private fun ColorPickerCompact(
    color: Color,
    onColorChange: (Color) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current color display
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clickable { showColorPicker = true }
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Color hex value
        Text(
            text = "#${color.toHexString()}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.weight(1f))

        // Edit button
        OutlinedButton(onClick = { showColorPicker = true }) {
            Icon(Lucide.Pipette, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pick")
        }
    }

    if (showColorPicker) {
        Dialog(onDismissRequest = { showColorPicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Add a compact color picker here
                    // This would typically use a third-party color picker library
                    // or a simplified custom implementation

                    // For this example, we'll just show a set of predefined colors
                    val predefinedColors = listOf(
                        Color.Red, Color.Green, Color.Blue,
                        Color.Yellow, Color.Cyan, Color.Magenta,
                        Color.White, Color.Black, Color.Gray
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(predefinedColors) { predefinedColor ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(predefinedColor)
                                    .border(
                                        width = if (color == predefinedColor) 2.dp else 1.dp,
                                        color = if (color == predefinedColor)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onColorChange(predefinedColor)
                                        showColorPicker = false
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showColorPicker = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaletteTypeChip(
    type: ColorPaletteGenerator.PaletteType,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = type.name.lowercase().capitalize().replace("_", " ")
            )
        },
        leadingIcon = {
            if (selected) {
                Icon(
                    Lucide.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    )
}

@Composable
private fun ColorPaletteDisplay(
    palette: ColorPaletteGenerator.Palette,
    onColorSelected: (Color) -> Unit
) {
    Column {
        // Palette colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            palette.colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                        .clickable { onColorSelected(color) }
                ) {
                    // Show color info on hover (not implemented here)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Color swatches with hex values
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            palette.colors.forEach { color ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable { onColorSelected(color) }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "#${color.toHexString().take(6)}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPaletteItem(
    palette: ColorPaletteGenerator.Palette,
    onDelete: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = palette.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = palette.type.name.lowercase().capitalize().replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Lucide.Trash, contentDescription = "Delete")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Palette display
            ColorPaletteDisplay(
                palette = palette,
                onColorSelected = onColorSelected
            )
        }
    }
}

// Extension to convert Color to hex string
private fun Color.toHexString(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()

    return "${red.toString(16).padStart(2, '0')}${green.toString(16).padStart(2, '0')}${blue.toString(16).padStart(2, '0')}"
}

// Extension to capitalize first letter of string
private fun String.capitalize(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}