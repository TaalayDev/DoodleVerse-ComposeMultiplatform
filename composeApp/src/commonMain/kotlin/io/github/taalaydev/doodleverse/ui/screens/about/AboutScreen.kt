package io.github.taalaydev.doodleverse.ui.screens.about

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.composables.icons.lucide.*
import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.about_app_description
import doodleverse.composeapp.generated.resources.about_app_title
import doodleverse.composeapp.generated.resources.app_name
import doodleverse.composeapp.generated.resources.back
import doodleverse.composeapp.generated.resources.developed_by
import doodleverse.composeapp.generated.resources.developer_name
import doodleverse.composeapp.generated.resources.features
import doodleverse.composeapp.generated.resources.features_list
import doodleverse.composeapp.generated.resources.logo
import doodleverse.composeapp.generated.resources.version_number
import doodleverse.composeapp.generated.resources.visit_website
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.ui.theme.AnimatedScaffold
import io.github.taalaydev.doodleverse.ui.theme.DoodleVerseCardDefaults
import io.github.taalaydev.doodleverse.ui.theme.ThemeManager
import io.github.taalaydev.doodleverse.ui.theme.rememberThemeManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    platform: Platform,
    navController: NavController,
    themeManager: ThemeManager = rememberThemeManager(),
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    AnimatedScaffold(
        themeManager = themeManager,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.about_app_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Lucide.ArrowLeft,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(animatedProgress.value)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.alpha(animatedProgress.value)
            )

            Text(
                text = stringResource(Res.string.version_number, "1.0.0"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(animatedProgress.value)
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(animatedProgress.value),
                colors = DoodleVerseCardDefaults.primaryCardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.features),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    stringArrayResource(Res.array.features_list).forEach { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Lucide.CircleCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(feature)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(animatedProgress.value),
                colors = DoodleVerseCardDefaults.primaryCardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.about_app_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.about_app_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(animatedProgress.value),
                colors = DoodleVerseCardDefaults.primaryCardColors()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.developed_by),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Lucide.User,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(Res.string.developer_name),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            platform.launchUrl("https://taalaydev.github.io/")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Lucide.Globe, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.visit_website))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}