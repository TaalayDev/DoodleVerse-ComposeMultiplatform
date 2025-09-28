package io.github.taalaydev.doodleverse.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.*

/**
 * Represents different visual themes with unique color schemes and animations
 */
@Immutable
data class DoodleTheme(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme,
    val backgroundGradient: Brush,
    val accentColors: List<Color>,
    val animationType: AnimationType,
    val isDark: Boolean = false, // Indicates if this theme is primarily dark
    val isPremium: Boolean = false,
)

/**
 * Different types of animated backgrounds
 */
enum class AnimationType {
    COSMIC,      // Moving stars, planets, nebula effects
    OCEAN,       // Flowing waves, bubbles, underwater effects
    FOREST,      // Swaying trees, floating leaves, light rays
    SUNSET,      // Gradient shifts, sun movement, clouds
    NEON,        // Pulsing lights, electric arcs, grid lines
    MINIMALIST,  // Subtle geometric patterns, clean lines
    PAPER,       // Texture paper, ink drops, writing effects
    DIGITAL      // Matrix rain, code patterns, glitch effects
}

/**
 * Predefined theme collection
 */
object DoodleThemes {

    // Cosmic Theme (Default - matches current branding)
    val Cosmic = DoodleTheme(
        id = "cosmic",
        name = "Cosmic",
        description = "Explore the universe while creating",
        icon = Lucide.Sparkles,
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF6A55AE),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8E0FF),
            onPrimaryContainer = Color(0xFF4A3888),
            secondary = Color(0xFFF39C3C),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFECCC),
            onSecondaryContainer = Color(0xFF8D4E00),
            tertiary = Color(0xFF3ECCC2),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFBDF3F0),
            onTertiaryContainer = Color(0xFF00504C),
            background = Color(0xFFF8F7FF),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFCFBFF),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFCFBCFF),
            onPrimary = Color(0xFF4A3888),
            primaryContainer = Color(0xFF6A55AE),
            onPrimaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFFFFB95C),
            onSecondary = Color(0xFF492700),
            secondaryContainer = Color(0xFF6B3A00),
            onSecondaryContainer = Color(0xFFFFDCBA),
            tertiary = Color(0xFF70F7EE),
            onTertiary = Color(0xFF003735),
            background = Color(0xFF1A2C5B),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF1A2C5B),
            onSurface = Color(0xFFE6E1E5)
        ),

        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1A2C5B),
                Color(0xFF2D1B69),
                Color(0xFF4A148C)
            )
        ),
        accentColors = listOf(
            Color(0xFFF39C3C), Color(0xFF3ECCC2), Color(0xFFFFD54F),
            Color(0xFF4CAF50), Color(0xFFE91E63), Color(0xFF9C27B0)
        ),
        animationType = AnimationType.COSMIC,
        isDark = true,
    )

    // Ocean Theme
    val Ocean = DoodleTheme(
        id = "ocean",
        name = "Ocean Depths",
        description = "Dive deep into creativity",
        icon = Lucide.Waves,
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF0077BE),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCCE7FF),
            onPrimaryContainer = Color(0xFF003258),
            secondary = Color(0xFF00BCD4),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFB2EBF2),
            onSecondaryContainer = Color(0xFF006064),
            tertiary = Color(0xFF4DB6AC),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFE0F2F1),
            onTertiaryContainer = Color(0xFF004D40),
            background = Color(0xFFF0FEFF),
            onBackground = Color(0xFF0A1A1A),
            surface = Color(0xFFFAFDFF),
            onSurface = Color(0xFF0A1A1A)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF4FC3F7),
            onPrimary = Color(0xFF003258),
            primaryContainer = Color(0xFF0077BE),
            onPrimaryContainer = Color(0xFFCCE7FF),
            secondary = Color(0xFF4DD0E1),
            onSecondary = Color(0xFF006064),
            background = Color(0xFF0A1A1A),
            onBackground = Color(0xFFE1F4F5),
            surface = Color(0xFF102027),
            onSurface = Color(0xFFE1F4F5)
        ),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0277BD),
                Color(0xFF0288D1),
                Color(0xFF039BE5)
            )
        ),
        accentColors = listOf(
            Color(0xFF00BCD4), Color(0xFF4DB6AC), Color(0xFF26A69A),
            Color(0xFF00ACC1), Color(0xFF0097A7), Color(0xFF00838F)
        ),
        animationType = AnimationType.OCEAN,
        isDark = true,
        isPremium = true,
    )

    // Forest Theme
    val Forest = DoodleTheme(
        id = "forest",
        name = "Emerald Forest",
        description = "Let nature inspire your art",
        icon = Lucide.Trees,
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFC8E6C9),
            onPrimaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF558B2F),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDCEDC8),
            onSecondaryContainer = Color(0xFF33691E),
            tertiary = Color(0xFF795548),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFEFEBE9),
            onTertiaryContainer = Color(0xFF3E2723),
            background = Color(0xFFF1F8E9),
            onBackground = Color(0xFF1A1C18),
            surface = Color(0xFFF8FFF8),
            onSurface = Color(0xFF1A1C18)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF66BB6A),
            onPrimary = Color(0xFF1B5E20),
            primaryContainer = Color(0xFF2E7D32),
            onPrimaryContainer = Color(0xFFC8E6C9),
            secondary = Color(0xFF8BC34A),
            onSecondary = Color(0xFF33691E),
            background = Color(0xFF1A1C18),
            onBackground = Color(0xFFE2E3DE),
            surface = Color(0xFF232926),
            onSurface = Color(0xFFE2E3DE)
        ),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1B5E20),
                Color(0xFF2E7D32),
                Color(0xFF4CAF50)
            )
        ),
        accentColors = listOf(
            Color(0xFF8BC34A), Color(0xFF689F38), Color(0xFF558B2F),
            Color(0xFF795548), Color(0xFF8D6E63), Color(0xFFA1887F)
        ),
        animationType = AnimationType.FOREST,
        isDark = true,
        isPremium = true,
    )

    // Sunset Theme
    val Sunset = DoodleTheme(
        id = "sunset",
        name = "Golden Hour",
        description = "Warm colors of twilight",
        icon = Lucide.Sunset,
        lightColorScheme = lightColorScheme(
            primary = Color(0xFFFF6F00),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE0B2),
            onPrimaryContainer = Color(0xFFE65100),
            secondary = Color(0xFFFF5722),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFFCCBC),
            onSecondaryContainer = Color(0xFFBF360C),
            tertiary = Color(0xFFEC407A),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF8BBD9),
            onTertiaryContainer = Color(0xFF880E4F),
            background = Color(0xFFFFF3E0),
            onBackground = Color(0xFF1F1A13),
            surface = Color(0xFFFFFBFF),
            onSurface = Color(0xFF1F1A13)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFFFAB40),
            onPrimary = Color(0xFFE65100),
            primaryContainer = Color(0xFFFF6F00),
            onPrimaryContainer = Color(0xFFFFE0B2),
            secondary = Color(0xFFFF8A65),
            onSecondary = Color(0xFFBF360C),
            background = Color(0xFF1F1A13),
            onBackground = Color(0xFFF0E6D2),
            surface = Color(0xFF2A1F17),
            onSurface = Color(0xFFF0E6D2)
        ),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFF6F00),
                Color(0xFFFF8F00),
                Color(0xFFFFC107)
            )
        ),
        accentColors = listOf(
            Color(0xFFFF5722), Color(0xFFEC407A), Color(0xFFFF9800),
            Color(0xFFFFC107), Color(0xFFFFEB3B), Color(0xFFCDDC39)
        ),
        animationType = AnimationType.SUNSET,
        isDark = false,
    )

    // Neon Theme
    val Neon = DoodleTheme(
        id = "neon",
        name = "Electric Nights",
        description = "Vibrant cyberpunk aesthetics",
        icon = Lucide.Zap,
        lightColorScheme = lightColorScheme(
            primary = Color(0xFFE91E63),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF8BBD9),
            onPrimaryContainer = Color(0xFF880E4F),
            secondary = Color(0xFF00E5FF),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFFB3F5FC),
            onSecondaryContainer = Color(0xFF006064),
            tertiary = Color(0xFF76FF03),
            onTertiary = Color.Black,
            tertiaryContainer = Color(0xFFE8F5E8),
            onTertiaryContainer = Color(0xFF1B5E20),
            background = Color(0xFFF5F5F5),
            onBackground = Color(0xFF0A0A0A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0A0A0A)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFFF4081),
            onPrimary = Color(0xFF880E4F),
            primaryContainer = Color(0xFFE91E63),
            onPrimaryContainer = Color(0xFFF8BBD9),
            secondary = Color(0xFF18FFFF),
            onSecondary = Color(0xFF006064),
            background = Color(0xFF0A0A0A),
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF1A1A1A),
            onSurface = Color(0xFFE0E0E0)
        ),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0A0A),
                Color(0xFF1A0033),
                Color(0xFF330066)
            )
        ),
        accentColors = listOf(
            Color(0xFFFF4081), Color(0xFF18FFFF), Color(0xFF76FF03),
            Color(0xFFFFEA00), Color(0xFFE040FB), Color(0xFF40C4FF)
        ),
        animationType = AnimationType.NEON,
        isDark = true,
    )

    // Minimalist Theme
    val Minimalist = DoodleTheme(
        id = "minimalist",
        name = "Pure Focus",
        description = "Clean and distraction-free",
        icon = Lucide.Circle,
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF212121),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E0E0),
            onPrimaryContainer = Color(0xFF000000),
            secondary = Color(0xFF616161),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFEEEEEE),
            onSecondaryContainer = Color(0xFF212121),
            tertiary = Color(0xFF9E9E9E),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF5F5F5),
            onTertiaryContainer = Color(0xFF424242),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF000000),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF000000)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFE0E0E0),
            onPrimary = Color(0xFF000000),
            primaryContainer = Color(0xFF424242),
            onPrimaryContainer = Color(0xFFE0E0E0),
            secondary = Color(0xFFBDBDBD),
            onSecondary = Color(0xFF212121),
            background = Color(0xFF000000),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF121212),
            onSurface = Color(0xFFFFFFFF)
        ),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFF8F8F8),
                Color(0xFFF0F0F0)
            )
        ),
        accentColors = listOf(
            Color(0xFF000000), Color(0xFF424242), Color(0xFF616161),
            Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFFE0E0E0)
        ),
        animationType = AnimationType.MINIMALIST,
        isDark = false,
        isPremium = true,
    )

    // Paper Theme
    val Paper = DoodleTheme(
        id = "paper",
        name = "Classic Paper",
        description = "Traditional art on paper",
        icon = Lucide.FileText,
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF5D4037),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEFEBE9),
            onPrimaryContainer = Color(0xFF3E2723),
            secondary = Color(0xFF8D6E63),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFF3E5AB),
            onSecondaryContainer = Color(0xFF6D4C41),
            tertiary = Color(0xFFFF8F00),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFE0B2),
            onTertiaryContainer = Color(0xFFE65100),
            background = Color(0xFFFAF7F2),
            onBackground = Color(0xFF1C1917),
            surface = Color(0xFFFEFBF6),
            onSurface = Color(0xFF1C1917)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFA1887F),
            onPrimary = Color(0xFF3E2723),
            primaryContainer = Color(0xFF5D4037),
            onPrimaryContainer = Color(0xFFEFEBE9),
            secondary = Color(0xFFBCAAA4),
            onSecondary = Color(0xFF6D4C41),
            background = Color(0xFF1C1917),
            onBackground = Color(0xFFE6E1DC),
            surface = Color(0xFF2A251F),
            onSurface = Color(0xFFE6E1DC)
        ),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFAF7F2),
                Color(0xFFF5F1EC),
                Color(0xFFF0EBE6)
            )
        ),
        accentColors = listOf(
            Color(0xFF8D6E63), Color(0xFFFF8F00), Color(0xFFD7CCC8),
            Color(0xFFBCAAA4), Color(0xFFA1887F), Color(0xFF795548)
        ),
        animationType = AnimationType.PAPER,
        isDark = false,
    )

    // Digital Theme
    val Digital = DoodleTheme(
        id = "digital",
        name = "Matrix Code",
        description = "Enter the digital realm",
        icon = Lucide.Binary,
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF00C853),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFFE8F5E8),
            onPrimaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF00BCD4),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFFE0F7FA),
            onSecondaryContainer = Color(0xFF006064),
            tertiary = Color(0xFF9C27B0),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF3E5F5),
            onTertiaryContainer = Color(0xFF4A148C),
            background = Color(0xFFE8F8F5),
            onBackground = Color(0xFF0A1A0A),
            surface = Color(0xFFE8FFF8),
            onSurface = Color(0xFF0A1A0A),
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF00E676),
            onPrimary = Color(0xFF1B5E20),
            primaryContainer = Color(0xFF00C853),
            onPrimaryContainer = Color(0xFFE8F5E8),
            secondary = Color(0xFF26C6DA),
            onSecondary = Color(0xFF006064),
            background = Color(0xFF000000),
            onBackground = Color(0xFF00FF41),
            surface = Color(0xFF0A0A0A),
            onSurface = Color(0xFF00FF41)
        ),
        backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF000000),
                Color(0xFF001100),
                Color(0xFF002200)
            )
        ),
        accentColors = listOf(
            Color(0xFF00E676), Color(0xFF26C6DA), Color(0xFF9C27B0),
            Color(0xFF00FF41), Color(0xFF1DE9B6), Color(0xFF18FFFF)
        ),
        animationType = AnimationType.DIGITAL,
        isDark = true,
    )

    /**
     * All available themes
     */
    val allThemes = listOf(
        Cosmic, Ocean, Forest, Sunset, Neon, Minimalist, Paper, Digital
    )

    /**
     * Get theme by ID
     */
    fun getThemeById(id: String): DoodleTheme {
        return allThemes.find { it.id == id } ?: Cosmic
    }
}