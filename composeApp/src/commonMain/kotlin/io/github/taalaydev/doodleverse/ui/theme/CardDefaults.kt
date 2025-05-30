package io.github.taalaydev.doodleverse.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Custom card color schemes for DoodleVerse app
 */
object DoodleVerseCardDefaults {

    /**
     * Standard card with subtle purple tint
     */
    @Composable
    fun cardColors(
        containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    ) = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor
    )

    /**
     * Primary colored card with purple background
     */
    @Composable
    fun primaryCardColors() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    /**
     * Secondary colored card with orange/yellow tones
     */
    @Composable
    fun secondaryCardColors() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )

    /**
     * Project card - special styling for project cards
     */
    @Composable
    fun projectCardColors() = CardDefaults.cardColors(
        containerColor = if (MaterialTheme.colorScheme.isLight())
            Color.White.copy(alpha = 0.95f)
        else
            NavyBlue.copy(alpha = 0.8f),
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    /**
     * Lesson card - special styling for lesson cards
     */
    @Composable
    fun lessonCardColors() = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    val cardShape = RoundedCornerShape(16.dp)
    val elevatedCardShape = RoundedCornerShape(12.dp)
}

// Extension function to check if the current theme is light
@Composable
private fun androidx.compose.material3.ColorScheme.isLight() = this.background.luminance() > 0.5f