package io.github.taalaydev.doodleverse.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Crown
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import io.github.taalaydev.doodleverse.purchase.PurchaseViewModel
import io.github.taalaydev.doodleverse.ui.components.premium.PremiumUpgradeDialog
import io.github.taalaydev.doodleverse.ui.theme.DoodleTheme
import io.github.taalaydev.doodleverse.ui.theme.DoodleThemes
import io.github.taalaydev.doodleverse.ui.theme.animations.AnimatedBackground

@Composable
fun ThemeSelector(
    currentThemeId: String,
    onThemeSelected: (DoodleTheme) -> Unit,
    modifier: Modifier = Modifier,
    displayMode: ThemeSelectorMode = ThemeSelectorMode.GRID
) {
    when (displayMode) {
        ThemeSelectorMode.HORIZONTAL -> {
            ThemeSelectorHorizontal(
                currentThemeId = currentThemeId,
                onThemeSelected = onThemeSelected,
                modifier = modifier
            )
        }
        ThemeSelectorMode.GRID -> {
            ThemeSelectorGrid(
                currentThemeId = currentThemeId,
                onThemeSelected = onThemeSelected,
                modifier = modifier
            )
        }
        ThemeSelectorMode.COMPACT -> {
            ThemeSelectorCompact(
                currentThemeId = currentThemeId,
                onThemeSelected = onThemeSelected,
                modifier = modifier
            )
        }
    }
}

enum class ThemeSelectorMode {
    HORIZONTAL,
    GRID,
    COMPACT
}

@Composable
private fun ThemeSelectorHorizontal(
    currentThemeId: String,
    onThemeSelected: (DoodleTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Choose Your Theme",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DoodleThemes.allThemes) { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = theme.id == currentThemeId,
                    onSelected = { onThemeSelected(theme) },
                    size = ThemeCardSize.MEDIUM
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectorGrid(
    currentThemeId: String,
    onThemeSelected: (DoodleTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Theme Gallery",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 200.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(DoodleThemes.allThemes) { theme ->
                ThemeCard(
                    theme = theme,
                    isSelected = theme.id == currentThemeId,
                    onSelected = { onThemeSelected(theme) },
                    size = ThemeCardSize.LARGE
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectorCompact(
    currentThemeId: String,
    onThemeSelected: (DoodleTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentTheme = DoodleThemes.getThemeById(currentThemeId)

    Box(modifier = modifier) {
        Card(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(currentTheme.backgroundGradient)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = currentTheme.icon,
                    contentDescription = currentTheme.name,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.offset(y = 60.dp)
        ) {
            Card(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(DoodleThemes.allThemes) { theme ->
                        ThemeCard(
                            theme = theme,
                            isSelected = theme.id == currentThemeId,
                            onSelected = {
                                onThemeSelected(theme)
                                expanded = false
                            },
                            size = ThemeCardSize.SMALL
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: DoodleTheme,
    isSelected: Boolean,
    onSelected: () -> Unit,
    size: ThemeCardSize,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(300),
        label = "border_width"
    )

    Card(
        onClick = onSelected,
        modifier = modifier
            .size(
                width = size.width,
                height = size.height
            )
            .scale(scale)
            .border(
                width = borderWidth,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundGradient)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AnimatedBackground(
                animationType = theme.animationType,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.4f
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(size.padding),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        imageVector = Lucide.Lock,
                        contentDescription = "Locked theme",
                        modifier = Modifier.size(size.iconSize.times(0.6f)),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size.padding),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(2.dp)
                    )

                }

                Icon(
                    imageVector = theme.icon,
                    contentDescription = theme.name,
                    tint = Color.White,
                    modifier = Modifier.size(size.iconSize)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = theme.name,
                        style = size.titleStyle,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    if (isLocked) {
                        Icon(
                            imageVector = Lucide.Crown,
                            contentDescription = "Premium",
                            modifier = Modifier.size(12.dp),
                            tint = Color.Yellow.copy(alpha = 0.9f)
                        )
                    }

                    if (size.showDescription) {
                        Text(
                            text = theme.description,
                            style = size.descriptionStyle,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                if (size.showAccentColors) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        theme.accentColors.take(4).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ThemeCardSize(
    val width: androidx.compose.ui.unit.Dp,
    val height: androidx.compose.ui.unit.Dp,
    val padding: androidx.compose.ui.unit.Dp,
    val iconSize: androidx.compose.ui.unit.Dp,
    val titleStyle: androidx.compose.ui.text.TextStyle,
    val descriptionStyle: androidx.compose.ui.text.TextStyle,
    val showDescription: Boolean,
    val showAccentColors: Boolean
) {
    companion object {
        val SMALL
            @Composable
            get() = ThemeCardSize(
                width = 80.dp,
                height = 80.dp,
                padding = 8.dp,
                iconSize = 20.dp,
                titleStyle = MaterialTheme.typography.labelSmall,
                descriptionStyle = MaterialTheme.typography.labelSmall,
                showDescription = false,
                showAccentColors = false
            )

        val MEDIUM
            @Composable
            get() = ThemeCardSize(
                width = 160.dp,
                height = 120.dp,
                padding = 12.dp,
                iconSize = 32.dp,
                titleStyle = MaterialTheme.typography.titleMedium,
                descriptionStyle = MaterialTheme.typography.bodySmall,
                showDescription = false,
                showAccentColors = true
            )

        val LARGE
            @Composable
            get() = ThemeCardSize(
                width = 200.dp,
                height = 160.dp,
                padding = 16.dp,
                iconSize = 48.dp,
                titleStyle = MaterialTheme.typography.titleLarge,
                descriptionStyle = MaterialTheme.typography.bodyMedium,
                showDescription = true,
                showAccentColors = true
            )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorSheet(
    currentThemeId: String,
    purchaseViewModel: PurchaseViewModel,
    onThemeSelected: (DoodleTheme) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val premiumStatus by purchaseViewModel.premiumStatus.collectAsStateWithLifecycle()
        val uiState by purchaseViewModel.uiState.collectAsStateWithLifecycle()
        var showPremiumDialog by remember { mutableStateOf(false) }
        val isPremium = !premiumStatus.isPremium

        if (showPremiumDialog) {
            PremiumUpgradeDialog(
                onDismiss = { showPremiumDialog = false },
                onUpgradeClick = {
                    showPremiumDialog = false
                    // Navigate to purchase screen or trigger purchase
                    val premiumProduct = purchaseViewModel.getPremiumProduct()
                    premiumProduct?.let {
                        purchaseViewModel.handleEvent(
                            io.github.taalaydev.doodleverse.purchase.PurchaseUiEvent.PurchaseProduct(it.id)
                        )
                    }
                },
                premiumProduct = purchaseViewModel.getPremiumProduct(),
                isLoading = uiState.purchaseInProgress != null
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Choose Your Theme",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 600.dp)
            ) {
                items(DoodleThemes.allThemes) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = theme.id == currentThemeId,
                        onSelected = {
                            if (theme.isPremium && !isPremium) {
                                showPremiumDialog = true
                            } else {
                                onThemeSelected(theme)
                            }
                        },
                        size = ThemeCardSize.MEDIUM,
                        isLocked = theme.isPremium && !isPremium
                    )
                }
            }
        }
    }
}

@Composable
fun ThemePreview(
    theme: DoodleTheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundGradient)
        ) {
            AnimatedBackground(
                animationType = theme.animationType,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.8f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = theme.icon,
                        contentDescription = theme.name,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )

                    Column {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = theme.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    theme.accentColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .background(color, RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}