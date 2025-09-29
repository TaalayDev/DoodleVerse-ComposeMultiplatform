package io.github.taalaydev.doodleverse.ui.screens.purchase

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.composables.icons.lucide.*
import doodleverse.composeapp.generated.resources.*
import io.github.taalaydev.doodleverse.core.Const
import io.github.taalaydev.doodleverse.purchase.*
import io.github.taalaydev.doodleverse.purchase.models.*
import io.github.taalaydev.doodleverse.ui.theme.AnimatedScaffold
import org.jetbrains.compose.resources.stringResource

@Composable
fun PurchaseScreen(
    viewModel: PurchaseViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val premiumStatus by viewModel.premiumStatus.collectAsStateWithLifecycle()

    PurchaseScreenContent(
        uiState = uiState,
        premiumStatus = premiumStatus,
        navController = navController,
        onEvent = viewModel::handleEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseScreenContent(
    uiState: PurchaseUiState,
    premiumStatus: PremiumStatus,
    navController: NavController,
    onEvent: (PurchaseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val windowSize = calculateWindowSizeClass()
    val isCompact = windowSize.widthSizeClass == WindowWidthSizeClass.Compact

    var showSuccessDialog by remember { mutableStateOf(false) }

    // Show success dialog for successful purchases
    LaunchedEffect(uiState.lastPurchaseResult) {
        if (uiState.lastPurchaseResult is PurchaseResult.Success) {
            showSuccessDialog = true
        }
    }

    if (showSuccessDialog) {
        PremiumWelcomeDialog(
            onDismiss = {
                showSuccessDialog = false
                onEvent(PurchaseUiEvent.ClearPurchaseResult)

                navController.popBackStack()
            }
        )
    }

    AnimatedScaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Lucide.ArrowLeft,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                title = {
                    Text(
                        "Premium",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            onEvent(PurchaseUiEvent.RestorePurchases)
                        }
                    ) {
                        Icon(
                            imageVector = Lucide.Repeat,
                            contentDescription = "Restore"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Restore",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->

        if (isCompact) {
            MobilePurchaseLayout(
                uiState = uiState,
                premiumStatus = premiumStatus,
                onEvent = onEvent,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            DesktopPurchaseLayout(
                uiState = uiState,
                premiumStatus = premiumStatus,
                onEvent = onEvent,
                modifier = Modifier.padding(paddingValues)
            )
        }

        // Error snackbar
        uiState.errorMessage?.let { errorMessage ->
            LaunchedEffect(errorMessage) {
                // Show error message for a few seconds then clear
                kotlinx.coroutines.delay(4000)
                onEvent(PurchaseUiEvent.ClearError)
            }
        }
    }
}
@Composable
private fun MobilePurchaseLayout(
    uiState: PurchaseUiState,
    premiumStatus: PremiumStatus,
    onEvent: (PurchaseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            PremiumHeroSection(premiumStatus)
        }

        item {
            AllFeaturesShowcase()
        }

        if (uiState.availableProducts.isNotEmpty()) {
            item {
                val product = uiState.availableProducts.first()
                PremiumProductCard(
                    product = product,
                    isPurchased = premiumStatus.isPremium,
                    isLoading = uiState.purchaseInProgress == product.id,
                    onPurchase = { onEvent(PurchaseUiEvent.PurchaseProduct(product.id)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            PrivacyAndTermsSection()
        }

//        item {
//            RestoreAndSupportSection(
//                isRestoring = uiState.isRestoring,
//                onRestore = { onEvent(PurchaseUiEvent.RestorePurchases) }
//            )
//        }
    }
}

@Composable
private fun DesktopPurchaseLayout(
    uiState: PurchaseUiState,
    premiumStatus: PremiumStatus,
    onEvent: (PurchaseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Left side - Hero and features
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            PremiumHeroSection(premiumStatus)
            AllFeaturesShowcase()
//            RestoreAndSupportSection(
//                isRestoring = uiState.isRestoring,
//                onRestore = { onEvent(PurchaseUiEvent.RestorePurchases) }
//            )
        }

        // Right side - Purchase card
        Column(
            modifier = Modifier.width(400.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.availableProducts.isNotEmpty()) {
                val product = uiState.availableProducts.first()
                PremiumProductCard(
                    product = product,
                    isPurchased = premiumStatus.isPremium,
                    isLoading = uiState.purchaseInProgress == product.id,
                    onPurchase = { onEvent(PurchaseUiEvent.PurchaseProduct(product.id)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PrivacyAndTermsSection()
        }
    }
}

@Composable
private fun PremiumHeroSection(
    premiumStatus: PremiumStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Crown icon with animation
            val infiniteTransition = rememberInfiniteTransition(label = "crown")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "crown_scale"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Crown,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            if (premiumStatus.isPremium) {
                Text(
                    text = "You're Premium! 🎉",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

//                premiumStatus.purchaseDate?.let { purchaseTime ->
//                    val date = Instant.fromEpochMilliseconds(purchaseTime)
//                        .toLocalDateTime(TimeZone.currentSystemDefault())
//                    Text(
//                        text = "Premium since ${date.date}",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }

                Text(
                    text = "Thank you for supporting DoodleVerse! You have access to all current and future premium features.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Unlock Everything",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Get instant access to all lessons, brushes, tools, and future content with one simple purchase",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AllFeaturesShowcase(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Everything Included",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Current Features
            FeatureCategory(
                title = "Available Now",
                features = listOf(
                    PremiumFeatureItem(
                        icon = Lucide.GraduationCap,
                        title = "All Drawing Lessons",
                        description = "Complete library of step-by-step drawing tutorials",
                        isAvailable = true
                    ),
                    PremiumFeatureItem(
                        icon = Lucide.Brush,
                        title = "All Premium Brushes",
                        description = "50+ professional brushes and painting tools",
                        isAvailable = true
                    ),
                    PremiumFeatureItem(
                        icon = Lucide.Wrench,
                        title = "Advanced Tools",
                        description = "Shape tools, selection tools, and advanced drawing features",
                        isAvailable = true
                    ),
                    PremiumFeatureItem(
                        icon = Lucide.Layers,
                        title = "Unlimited Layers",
                        description = "Create complex artworks with unlimited layers and blending modes",
                        isAvailable = true
                    ),
                )
            )

            // Coming Soon Features
            FeatureCategory(
                title = "Coming Soon",
                features = listOf(
                    PremiumFeatureItem(
                        icon = Lucide.Palette,
                        title = "Premium Textures",
                        description = "Artistic textures and patterns for your drawings",
                        isAvailable = false
                    ),
                    PremiumFeatureItem(
                        icon = Lucide.Sticker,
                        title = "Sticker Library",
                        description = "Thousands of stickers and clipart elements",
                        isAvailable = false
                    ),
                )
            )
        }
    }
}

@Composable
private fun FeatureCategory(
    title: String,
    features: List<PremiumFeatureItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            if (title == "Coming Soon") {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "FUTURE UPDATES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        features.forEach { feature ->
            FeatureRow(feature = feature)
        }
    }
}

@Composable
private fun FeatureRow(
    feature: PremiumFeatureItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (feature.isAvailable) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (feature.isAvailable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (feature.isAvailable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumProductCard(
    product: Product,
    isPurchased: Boolean,
    isLoading: Boolean,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "BEST VALUE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            // Title and description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "One purchase unlocks everything",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Price
            Text(
                text = product.price,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // What's included summary
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Includes:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    val includedFeatures = listOf(
                        "All drawing lessons (current + future)",
                        "Premium brushes and tools",
                        "Unlimited layers and advanced features",
                        "Premium textures (when available)",
                        "Sticker library (when available)",
                        "All future premium content"
                    )

                    includedFeatures.forEach { feature ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Purchase button
            when {
                isPurchased -> {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Premium Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                isLoading -> {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Processing...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onPurchase,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Lucide.Crown,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Get Premium Now",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreAndSupportSection(
    isRestoring: Boolean,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Need Help?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    enabled = !isRestoring,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Lucide.RotateCcw,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Restore Purchase")
                }

                OutlinedButton(
                    onClick = { /* TODO: Open support */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Lucide.MessageCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Get Support")
                }
            }

            Text(
                text = "Your purchase is protected and can be restored on any device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PrivacyAndTermsSection(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "By purchasing, you agree to our",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                uriHandler.openUri(Const.PRIVACY_POLICY_URL)
            }) {
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text("and", color = MaterialTheme.colorScheme.onSurface)
            TextButton(onClick = {
                uriHandler.openUri(Const.TERMS_OF_SERVICE_URL)
            }) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PremiumWelcomeDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Lucide.Crown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    "Welcome to Premium!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🎉 Congratulations! You now have access to:")

                val features = listOf(
                    "All drawing lessons",
                    "Premium brushes and tools",
                    "Unlimited layers",
                    "HD export capabilities",
                    "All future premium content"
                )

                features.forEach { feature ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Text(
                    "Start creating amazing artwork!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Lucide.Sparkles,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Start Creating!")
            }
        }
    )
}

// Data classes
private data class PremiumFeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isAvailable: Boolean = true
)