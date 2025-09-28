package io.github.taalaydev.doodleverse.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.createDataStorage
import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import io.github.taalaydev.doodleverse.ui.components.ThemePreview
import io.github.taalaydev.doodleverse.ui.theme.animations.AnimatedBackground
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Theme Manager ViewModel for handling theme state across the app
 */
class ThemeManager(
    private val dataStorage: DataStorage = createDataStorage()
) : ViewModel() {
    private val _currentTheme = MutableStateFlow(DoodleThemes.Cosmic)
    val currentTheme: StateFlow<DoodleTheme> = _currentTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean?>(null) // null = system default
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    private val _showAnimatedBackground = MutableStateFlow(true)
    val showAnimatedBackground: StateFlow<Boolean> = _showAnimatedBackground.asStateFlow()

    init {
        getThemeFromStorage()
    }

    private fun getThemeFromStorage() {
        viewModelScope.launch {
            _currentTheme.value = dataStorage.getString(
                "theme_id",
                DoodleThemes.Cosmic.id
            ).let { themeId -> DoodleThemes.getThemeById(themeId) }
        }
    }

    fun setTheme(theme: DoodleTheme) {
        _currentTheme.value = theme
        viewModelScope.launch { dataStorage.putString("theme_id", theme.id) }
    }

    fun setDarkMode(isDark: Boolean?) {
        _isDarkMode.value = isDark
        // TODO: Save to preferences
    }

    fun toggleAnimatedBackground() {
        _showAnimatedBackground.value = !_showAnimatedBackground.value
        // TODO: Save to preferences
    }

    fun getEffectiveColorScheme(isSystemDark: Boolean) = when (_isDarkMode.value) {
        true -> _currentTheme.value.darkColorScheme
        false -> _currentTheme.value.lightColorScheme
        null -> if (isSystemDark) _currentTheme.value.darkColorScheme else _currentTheme.value.lightColorScheme
    }

    @Composable
    fun getColorScheme(): ColorScheme {
        val isSystemDark = isSystemInDarkTheme()
        return getEffectiveColorScheme(isSystemDark)
    }
}

/**
 * AppTheme with multi-theme support and animated backgrounds
 */
@Composable
fun AppTheme(
    themeManager: ThemeManager = viewModel { ThemeManager() },
    content: @Composable () -> Unit
) {
    val currentTheme by themeManager.currentTheme.collectAsState()
    val isDarkModeOverride = currentTheme.isDark
    val showAnimatedBackground by themeManager.showAnimatedBackground.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = if (isDarkModeOverride) {
        currentTheme.darkColorScheme
    } else {
        currentTheme.lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = {
            content()
        }
    )
}

@Composable
fun AnimatedScaffold(
    topBar: @Composable (() -> Unit) = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    modifier: Modifier = Modifier,
    themeManager: ThemeManager = rememberThemeManager(),
    animateBackground: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val currentTheme by themeManager.currentTheme.collectAsState()
    val showAnimatedBackground by themeManager.showAnimatedBackground.collectAsState()

    Scaffold(
        topBar = topBar,
        modifier = modifier,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        contentWindowInsets = contentWindowInsets,
        containerColor = Color.Transparent,
        contentColor = currentTheme.lightColorScheme.onBackground
    ) { paddingValues ->
        ThemedBackgroundContainer(
            theme = currentTheme,
            showAnimatedBackground = showAnimatedBackground,
            animate = animateBackground
        ) {
            content(paddingValues)
        }
    }
}

/**
 * Container that provides themed background with optional animations
 */
@Composable
private fun ThemedBackgroundContainer(
    theme: DoodleTheme,
    showAnimatedBackground: Boolean,
    animate: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (showAnimatedBackground) {
                    theme.backgroundGradient
                } else {
                    SolidColor(MaterialTheme.colorScheme.background)
                }
            )
    ) {
        // Animated background layer
        if (showAnimatedBackground) {
            AnimatedBackground(
                animationType = theme.animationType,
                alpha = 0.3f,
                animate = animate,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Content layer
        content()
    }
}

/**
 * Theme-aware surface component for cards and containers
 */
@Composable
fun ThemedSurface(
    modifier: Modifier = Modifier,
    useThemeBackground: Boolean = false,
    alpha: Float = 0.95f,
    shape: Shape = RoundedCornerShape(8.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val themeManager: ThemeManager = viewModel { ThemeManager() }
    val currentTheme by themeManager.currentTheme.collectAsState()

    Surface(
        modifier = modifier,
        shape = shape,
        color = if (useThemeBackground) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = alpha)
        }
    ) {
        Box(
            modifier = if (useThemeBackground) {
                Modifier
                    .fillMaxSize()
                    .background(
                        currentTheme.backgroundGradient
                    )
            } else {
                Modifier.fillMaxSize()
            }
        ) {
            content()
        }
    }
}

/**
 * Composable that provides theme context
 */
@Composable
fun rememberThemeManager(): ThemeManager {
    return viewModel<ThemeManager> { ThemeManager() }
}

/**
 * Theme preference keys for persistence (to be implemented with DataStore)
 */
object ThemePreferences {
    const val THEME_ID_KEY = "theme_id"
    const val DARK_MODE_KEY = "dark_mode" // -1 = system, 0 = light, 1 = dark
    const val ANIMATED_BACKGROUND_KEY = "animated_background"
}

/**
 * Theme utilities and extensions
 */
object ThemeUtils {
    /**
     * Get theme-appropriate colors for different UI states
     */
    @Composable
    fun getAccentColor(index: Int = 0): Color {
        val themeManager: ThemeManager = viewModel()
        val currentTheme by themeManager.currentTheme.collectAsState()
        return currentTheme.accentColors.getOrElse(index) { currentTheme.accentColors.first() }
    }

    /**
     * Get theme brush for gradients
     */
    @Composable
    fun getThemeBrush(): Brush {
        val themeManager: ThemeManager = viewModel()
        val currentTheme by themeManager.currentTheme.collectAsState()
        return currentTheme.backgroundGradient
    }

    /**
     * Check if current theme is dark
     */
    @Composable
    fun isCurrentThemeDark(): Boolean {
        val themeManager: ThemeManager = viewModel()
        val isDarkModeOverride by themeManager.isDarkMode.collectAsState()
        val isSystemDark = isSystemInDarkTheme()

        return when (isDarkModeOverride) {
            true -> true
            false -> false
            null -> isSystemDark
        }
    }
}

/**
 * Theme-aware icon tint
 */
@Composable
fun getThemedIconTint(useAccent: Boolean = false): Color {
    return if (useAccent) {
        ThemeUtils.getAccentColor()
    } else {
        MaterialTheme.colorScheme.onSurface
    }
}

/**
 * Integration component for the drawing app
 */
@Composable
fun DrawingAppWithTheme(
    themeManager: ThemeManager = rememberThemeManager(),
    content: @Composable (ThemeManager) -> Unit
) {
    AppTheme(themeManager = themeManager) {
        content(themeManager)
    }
}

/**
 * Theme-aware floating action button
 */
@Composable
fun ThemedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val brush = ThemeUtils.getThemeBrush()

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .background(
                brush = brush,
                shape = RoundedCornerShape(16.dp)
            ),
        containerColor = Color.Transparent,
        content = content
    )
}

/**
 * Theme settings component for preferences/settings screen
 */
@Composable
fun ThemeSettingsSection(
    themeManager: ThemeManager,
    modifier: Modifier = Modifier
) {
    val currentTheme by themeManager.currentTheme.collectAsState()
    val isDarkModeOverride by themeManager.isDarkMode.collectAsState()
    val showAnimatedBackground by themeManager.showAnimatedBackground.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme selection
        Text(
            text = "Theme & Appearance",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Current theme preview
        ThemePreview(theme = currentTheme)

        // Dark mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (isDarkModeOverride) {
                        true -> "Always dark"
                        false -> "Always light"
                        null -> "Follow system"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = isDarkModeOverride ?: false,
                onCheckedChange = { isChecked ->
                    themeManager.setDarkMode(if (isChecked) true else null)
                }
            )
        }

        // Animated background toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Animated Background",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Show theme animations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = showAnimatedBackground,
                onCheckedChange = { themeManager.toggleAnimatedBackground() }
            )
        }

        // Theme selector button
        Card(
            onClick = { /* Open theme selector */ },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Change Theme",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Current: ${currentTheme.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = currentTheme.icon,
                    contentDescription = "Change theme",
                    tint = ThemeUtils.getAccentColor()
                )
            }
        }
    }
}