package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import io.github.taalaydev.doodleverse.core.color.ColorPaletteGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * A comprehensive color picker with harmony palette generation.
 *
 * @param initialColor The initial color to display
 * @param onColorSelected Callback when a color is selected
 * @param modifier Modifier for the color picker
 */
@Composable
fun HarmonyColorPicker(
    initialColor: Color = Color(0xFF333333),
    onColorSelected: (Color) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val paletteGenerator = remember { ColorPaletteGenerator() }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var currentColor by remember { mutableStateOf(initialColor) }
    var currentHue by remember { mutableStateOf(initialColor.toHSV()[0]) }
    var currentSatVal by remember { mutableStateOf(Offset(initialColor.toHSV()[1], 1f - initialColor.toHSV()[2])) }
    var currentAlpha by remember { mutableStateOf(initialColor.alpha) }

    var generatedPalettes by remember { mutableStateOf<List<ColorPaletteGenerator.Palette>>(emptyList()) }
    var savedPalettes by remember { mutableStateOf<List<ColorPaletteGenerator.Palette>>(emptyList()) }

    // RGB values for input
    var redValue by remember { mutableStateOf((currentColor.red * 255).roundToInt()) }
    var greenValue by remember { mutableStateOf((currentColor.green * 255).roundToInt()) }
    var blueValue by remember { mutableStateOf((currentColor.blue * 255).roundToInt()) }

    // Hex value for input
    var hexValue by remember { mutableStateOf(currentColor.toHexString()) }

    // Generate initial palettes
    LaunchedEffect(Unit) {
        // Create initial palettes for all palette types
        val palettes = ColorPaletteGenerator.PaletteType.values().map { type ->
            paletteGenerator.generatePalette(initialColor, type, 5)
        }
        generatedPalettes = palettes
    }

    // Update color components when current color changes
    LaunchedEffect(currentColor) {
        val hsv = currentColor.toHSV()
        currentHue = hsv[0]
        currentSatVal = Offset(hsv[1], 1f - hsv[2])
        redValue = (currentColor.red * 255).roundToInt()
        greenValue = (currentColor.green * 255).roundToInt()
        blueValue = (currentColor.blue * 255).roundToInt()
        hexValue = currentColor.toHexString()

        onColorSelected(currentColor)
    }

    // Update color when RGB values change
    LaunchedEffect(redValue, greenValue, blueValue) {
        val newColor = Color(
            red = redValue / 255f,
            green = greenValue / 255f,
            blue = blueValue / 255f,
            alpha = currentAlpha
        )
        if (newColor != currentColor) {
            currentColor = newColor
            val hsv = newColor.toHSV()
            currentHue = hsv[0]
            currentSatVal = Offset(hsv[1], 1f - hsv[2])
        }
    }

    // Update palettes when color changes
    LaunchedEffect(currentColor) {
        scope.launch {
            // Generate palettes for all types
            val palettes = ColorPaletteGenerator.PaletteType.values().map { type ->
                paletteGenerator.generatePalette(currentColor, type, 5)
            }
            generatedPalettes = palettes
        }
    }

    // Main component structure
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        brush = checkerboardBrush(32f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(currentColor)
                ) {
                    Text(
                        text = hexValue,
                        color = if (currentColor.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Color Picker") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Harmonies") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Saved") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab content
            when (selectedTabIndex) {
                0 -> {
                    // Color Picker Tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Hue selector
                        Text(
                            text = "Hue",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HueSelector(
                            hue = currentHue,
                            onHueChanged = { newHue ->
                                currentHue = newHue
                                val hsv = floatArrayOf(newHue, currentSatVal.x, 1f - currentSatVal.y)
                                currentColor = hsvToColor(hsv[0], hsv[1], hsv[2], currentAlpha)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Saturation/Value selector
                        Text(
                            text = "Saturation & Brightness",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SaturationValueSelector(
                            hue = currentHue,
                            satVal = currentSatVal,
                            onSatValChanged = { newSatVal ->
                                currentSatVal = newSatVal
                                val hsv = floatArrayOf(currentHue, newSatVal.x, 1f - newSatVal.y)
                                currentColor = hsvToColor(hsv[0], hsv[1], hsv[2], currentAlpha)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Alpha slider
                        Text(
                            text = "Opacity (${(currentAlpha * 100).roundToInt()}%)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AlphaSlider(
                            color = currentColor,
                            value = currentAlpha,
                            onValueChange = { alpha ->
                                currentAlpha = alpha
                                currentColor = currentColor.copy(alpha = alpha)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Input controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // RGB controls
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "RGB",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ColorInput(
                                        value = redValue,
                                        onValueChange = { redValue = it.coerceIn(0, 255) },
                                        label = "R",
                                        textColor = Color.Red,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ColorInput(
                                        value = greenValue,
                                        onValueChange = { greenValue = it.coerceIn(0, 255) },
                                        label = "G",
                                        textColor = Color.Green,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ColorInput(
                                        value = blueValue,
                                        onValueChange = { blueValue = it.coerceIn(0, 255) },
                                        label = "B",
                                        textColor = Color.Blue,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Hex input
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Hex",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = hexValue,
                                    onValueChange = { value ->
                                        if (value.length <= 9) { // Max length including #
                                            hexValue = value
                                            try {
                                                val cleanHex = if (value.startsWith("#")) value else "#$value"
                                                if (cleanHex.length >= 7) { // At least #RRGGBB
                                                    //val color = Color(android.graphics.Color.parseColor(cleanHex))
                                                    //currentColor = color
                                                }
                                            } catch (e: Exception) {
                                                // Invalid hex
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Ascii,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick colors
                        Text(
                            text = "Quick Colors",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        QuickColorPalette(
                            onColorSelected = { color ->
                                currentColor = color
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Save color button
                        Button(
                            onClick = {
                                val newPalette = ColorPaletteGenerator.Palette(
                                    name = "Custom Palette",
                                    colors = listOf(currentColor),
                                    type = ColorPaletteGenerator.PaletteType.MONOCHROMATIC
                                )
                                savedPalettes = savedPalettes + newPalette
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Lucide.Save, contentDescription = "Save current color")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Current Color")
                        }
                    }
                }
                1 -> {
                    // Harmonies Tab
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Color Harmonies",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Based on your selected color: ${currentColor.toHexString()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        generatedPalettes.forEach { palette ->
                            PaletteRow(
                                palette = palette,
                                onColorSelected = { color ->
                                    currentColor = color
                                },
                                onSavePalette = {
                                    savedPalettes = savedPalettes + palette
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                2 -> {
                    // Saved Tab
                    if (savedPalettes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Lucide.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No saved palettes yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Save colors or palettes from the other tabs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "Saved Palettes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            savedPalettes.forEachIndexed { index, palette ->
                                SavedPaletteRow(
                                    palette = palette,
                                    onColorSelected = { color ->
                                        currentColor = color
                                    },
                                    onDeletePalette = {
                                        savedPalettes = savedPalettes.toMutableList().apply {
                                            removeAt(index)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaturationValueSelector(
    hue: Float,
    satVal: Offset,
    onSatValChanged: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .shadow(1.dp, RoundedCornerShape(8.dp))
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newSatVal = Offset(
                            (offset.x / width).coerceIn(0f, 1f),
                            (offset.y / height).coerceIn(0f, 1f)
                        )
                        onSatValChanged(newSatVal)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newSatVal = Offset(
                            (change.position.x / width).coerceIn(0f, 1f),
                            (change.position.y / height).coerceIn(0f, 1f)
                        )
                        onSatValChanged(newSatVal)
                    }
                }
        ) {
            // Draw saturation-value plane with current hue
            val satValShader = object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    return LinearGradientShader(
                        colors = listOf(Color.White, hsvToColor(hue, 1f, 1f)),
                        from = Offset.Zero,
                        to = Offset(size.width, 0f)
                    )
                }
            }

            // Draw the saturation gradient (horizontal white to color)
            drawRect(satValShader)

            // Draw the value gradient (vertical transparent to black)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = size.height
                )
            )

            // Draw selector circle
            val center = Offset(
                satVal.x * size.width,
                satVal.y * size.height
            )

            drawCircle(
                color = Color.White,
                radius = 12f,
                center = center,
                style = Stroke(width = 2f)
            )

            drawCircle(
                color = Color.Black,
                radius = 12f,
                center = center,
                style = Stroke(width = 1f)
            )
        }
    }
}

@Composable
private fun HueSelector(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .shadow(1.dp, RoundedCornerShape(8.dp))
    ) {
        val width = constraints.maxWidth.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newHue = (offset.x / width * 360f).coerceIn(0f, 360f)
                        onHueChanged(newHue)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newHue = (change.position.x / width * 360f).coerceIn(0f, 360f)
                        onHueChanged(newHue)
                    }
                }
        ) {
            // Draw hue gradient
            val hueColors = (0..360 step 60).map {
                hsvToColor(it.toFloat(), 1f, 1f)
            }

            drawRect(
                brush = Brush.horizontalGradient(colors = hueColors)
            )

            // Draw hue selector indicator
            val selectorPosition = Offset(hue / 360f * size.width, size.height / 2)

            drawCircle(
                color = Color.White,
                radius = size.height / 2,
                center = selectorPosition,
                style = Stroke(width = 2f)
            )

            drawCircle(
                color = Color.Black,
                radius = size.height / 2,
                center = selectorPosition,
                style = Stroke(width = 1f)
            )
        }
    }
}

@Composable
private fun AlphaSlider(
    color: Color,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .background(
                brush = checkerboardBrush(8f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        val width = constraints.maxWidth.toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newValue = (offset.x / width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newValue = (change.position.x / width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                }
        ) {
            // Draw alpha gradient
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0f),
                        color.copy(alpha = 1f)
                    )
                )
            )

            // Draw selector
            val selectorPosition = Offset(value * size.width, size.height / 2)

            drawCircle(
                color = Color.White,
                radius = size.height / 2,
                center = selectorPosition,
                style = Stroke(width = 2f)
            )

            drawCircle(
                color = Color.Black,
                radius = size.height / 2,
                center = selectorPosition,
                style = Stroke(width = 1f)
            )
        }
    }
}

@Composable
private fun QuickColorPalette(
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = remember {
        listOf(
            Color.Black,
            Color.DarkGray,
            Color.Gray,
            Color.LightGray,
            Color.White,
            Color.Red,
            Color(0xFFF44336), // Red
            Color(0xFFE91E63), // Pink
            Color(0xFF9C27B0), // Purple
            Color(0xFF673AB7), // Deep Purple
            Color(0xFF3F51B5), // Indigo
            Color(0xFF2196F3), // Blue
            Color(0xFF03A9F4), // Light Blue
            Color(0xFF00BCD4), // Cyan
            Color(0xFF009688), // Teal
            Color(0xFF4CAF50), // Green
            Color(0xFF8BC34A), // Light Green
            Color(0xFFCDDC39), // Lime
            Color(0xFFFFEB3B), // Yellow
            Color(0xFFFFC107), // Amber
            Color(0xFFFF9800), // Orange
            Color(0xFFFF5722)  // Deep Orange
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.LightGray, CircleShape)
                    .clickable { onColorSelected(color) }
            )
        }
    }
}

@Composable
private fun PaletteRow(
    palette: ColorPaletteGenerator.Palette,
    onColorSelected: (Color) -> Unit,
    onSavePalette: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = palette.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            IconButton(onClick = onSavePalette) {
                Icon(Lucide.Save, contentDescription = "Save palette")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(palette.colors) { color ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .clickable { onColorSelected(color) }
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = color.toHexString().take(7),
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = if (color.luminance() > 0.5f) Color.Black else Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedPaletteRow(
    palette: ColorPaletteGenerator.Palette,
    onColorSelected: (Color) -> Unit,
    onDeletePalette: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = palette.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            IconButton(onClick = onDeletePalette) {
                Icon(Lucide.Trash, contentDescription = "Delete palette")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            palette.colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .clickable { onColorSelected(color) }
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (palette.colors.size <= 5) {
                        Text(
                            text = color.toHexString().take(7),
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = if (color.luminance() > 0.5f) Color.Black else Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    textColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let {
                    onValueChange(it.coerceIn(0, 255))
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Creates a checkerboard pattern brush for transparency visualization
 */
private fun checkerboardBrush(tileSize: Float): Brush {
    return object : ShaderBrush() {
        override fun createShader(size: Size): Shader {
            return ImageShader(
                createCheckerboardImage(tileSize),
                TileMode.Repeated,
                TileMode.Repeated
            )
        }
    }
}

/**
 * Creates an ImageBitmap with a checkerboard pattern
 */
private fun createCheckerboardImage(tileSize: Float): ImageBitmap {
    val tilePixels = tileSize.toInt()
    val halfTilePixels = tilePixels / 2
    val width = tilePixels * 2
    val height = tilePixels * 2

    return ImageBitmap(width, height).apply {
        val canvas = Canvas(this)
        val lightColor = Color(0xFFCCCCCC)
        val darkColor = Color(0xFFAAAAAA)

        canvas.drawRect(
            left = 0f,
            top = 0f,
            right = halfTilePixels.toFloat(),
            bottom = halfTilePixels.toFloat(),
            paint = Paint().apply { color = lightColor }
        )

        canvas.drawRect(
            left = halfTilePixels.toFloat(),
            top = 0f,
            right = tilePixels.toFloat(),
            bottom = halfTilePixels.toFloat(),
            paint = Paint().apply { color = darkColor }
        )

        canvas.drawRect(
            left = 0f,
            top = halfTilePixels.toFloat(),
            right = halfTilePixels.toFloat(),
            bottom = tilePixels.toFloat(),
            paint = Paint().apply { color = darkColor }
        )

        canvas.drawRect(
            left = halfTilePixels.toFloat(),
            top = halfTilePixels.toFloat(),
            right = tilePixels.toFloat(),
            bottom = tilePixels.toFloat(),
            paint = Paint().apply { color = lightColor }
        )
    }
}

/**
 * Utility functions for color conversion
 */

private fun Color.toHSV(): FloatArray {
    val hsv = FloatArray(3)

    return hsv
}

private fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
    val hsv = floatArrayOf(h, s, v)
    // val argb = android.graphics.Color.HSVToColor((alpha * 255).toInt(), hsv)
    return Color(0xFF000000)
}

private fun Color.toHexString(): String {
    val alpha = (alpha * 255).roundToInt()
    val red = (red * 255).roundToInt()
    val green = (green * 255).roundToInt()
    val blue = (blue * 255).roundToInt()

    return if (alpha < 255) {
        String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
    } else {
        String.format("#%02X%02X%02X", red, green, blue)
    }
}

private fun String.Companion.format(format: String, vararg args: Any): String {
    return format.replace(Regex("\\{\\}")) {
        ""
    }
}

/**
 * Calculate the relative luminance of the color
 */
private fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else ((red + 0.055f) / 1.055f).pow(2.4f)
    val g = if (green <= 0.03928f) green / 12.92f else ((green + 0.055f) / 1.055f).pow(2.4f)
    val b = if (blue <= 0.03928f) blue / 12.92f else ((blue + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

// Example usage in a dialog
@Composable
fun HarmonyColorPickerDialog(
    initialColor: Color = Color(0xFF333333),
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Color") },
        text = {
            HarmonyColorPicker(
                initialColor = initialColor,
                onColorSelected = onColorSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Done")
            }
        },
        dismissButton = {}
    )
}