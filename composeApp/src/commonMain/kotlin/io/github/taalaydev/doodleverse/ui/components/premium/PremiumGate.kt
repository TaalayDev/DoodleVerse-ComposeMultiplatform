package io.github.taalaydev.doodleverse.ui.components.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Crown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Star
import io.github.taalaydev.doodleverse.purchase.PremiumFeature
import io.github.taalaydev.doodleverse.purchase.PurchaseViewModel

@Composable
fun PremiumFeatureGate(
    feature: PremiumFeature,
    purchaseViewModel: PurchaseViewModel,
    onUpgradeClick: () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAccess by remember {
        derivedStateOf { purchaseViewModel.hasFeatureAccess(feature) }
    }

    if (hasAccess) {
        content()
    } else {
        PremiumLockedContent(
            feature = feature,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier
        )
    }
}

@Composable
private fun PremiumLockedContent(
    feature: PremiumFeature,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Lucide.Crown,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = getFeatureTitle(feature),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = getFeatureDescription(feature),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onUpgradeClick,
                colors = androidx.compose.material.ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Lucide.Crown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Upgrade to Premium")
            }
        }
    }
}

@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier,
    size: BadgeSize = BadgeSize.Small
) {
    val (iconSize, padding, textStyle) = when (size) {
        BadgeSize.Small -> Triple(12.dp, 4.dp, MaterialTheme.typography.labelSmall)
        BadgeSize.Medium -> Triple(16.dp, 6.dp, MaterialTheme.typography.labelMedium)
        BadgeSize.Large -> Triple(20.dp, 8.dp, MaterialTheme.typography.titleSmall)
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = padding * 2, vertical = padding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Lucide.Crown,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "PRO",
                style = textStyle,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

enum class BadgeSize { Small, Medium, Large }

@Composable
fun PremiumFeatureButton(
    feature: PremiumFeature,
    purchaseViewModel: PurchaseViewModel,
    onUpgradeClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    text: String
) {
    val hasAccess by remember {
        derivedStateOf { purchaseViewModel.hasFeatureAccess(feature) }
    }

    if (hasAccess) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onUpgradeClick,
            modifier = modifier,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Lucide.Crown,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Upgrade for $text")
        }
    }
}

@Composable
fun PremiumUpgradeDialog(
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit,
    premiumProduct: io.github.taalaydev.doodleverse.purchase.models.Product?,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with crown icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Crown,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Unlock Premium Lessons",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = "Get access to all premium lessons and unlock your full creative potential!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Features list
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumFeatureRow(
                        icon = Lucide.BookOpen,
                        text = "Access to all premium lessons"
                    )
                    PremiumFeatureRow(
                        icon = Lucide.Sparkles,
                        text = "Premium brushes and tools"
                    )
                    PremiumFeatureRow(
                        icon = Lucide.Star,
                        text = "All future premium content"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Price and upgrade button
                if (premiumProduct != null) {
                    Text(
                        text = "Only ${premiumProduct.price}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Maybe Later")
                    }

                    Button(
                        onClick = onUpgradeClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && premiumProduct != null
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Crown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("Upgrade Now")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer text
                Text(
                    text = "Unlock premium features and support the app development",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PremiumFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun getFeatureTitle(feature: PremiumFeature): String {
    return when (feature) {
        PremiumFeature.ALL_BRUSHES -> "Premium Brushes"
        PremiumFeature.ALL_TOOLS -> "Premium Tools"
        PremiumFeature.ALL_LESSONS -> "Premium Lessons"
        PremiumFeature.TEXTURES -> "Premium Textures"
        PremiumFeature.STICKERS -> "Premium Stickers"
        PremiumFeature.ALL_FEATURES -> "Premium Features"
    }
}

private fun getFeatureDescription(feature: PremiumFeature): String {
    return when (feature) {
        PremiumFeature.ALL_BRUSHES -> "Unlock all premium brushes for enhanced creativity."
        PremiumFeature.ALL_TOOLS -> "Access all premium tools to elevate your artwork."
        PremiumFeature.ALL_LESSONS -> "Enjoy exclusive lessons to master advanced techniques."
        PremiumFeature.TEXTURES -> "Get access to premium textures for unique effects."
        PremiumFeature.STICKERS -> "Use premium stickers to add flair to your creations."
        PremiumFeature.ALL_FEATURES -> "Unlock all premium features for the ultimate experience."
    }
}