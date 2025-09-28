package io.github.taalaydev.doodleverse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import io.github.taalaydev.doodleverse.navigation.MainNavHost
import io.github.taalaydev.doodleverse.purchase.PurchaseRepository
import io.github.taalaydev.doodleverse.purchase.PurchaseUiEvent
import io.github.taalaydev.doodleverse.purchase.PurchaseViewModel
import io.github.taalaydev.doodleverse.ui.theme.DrawingAppWithTheme
import io.github.taalaydev.doodleverse.ui.theme.ThemeManager
import io.github.taalaydev.doodleverse.ui.theme.rememberThemeManager
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App(platform: Platform) {
    val navController = rememberNavController()
    val purchaseRepo = remember {
        PurchaseRepository(createInAppPurchaseManager())
    }
    val purchaseViewModel = viewModel { PurchaseViewModel(purchaseRepo) }

    LaunchedEffect(Unit) {
        purchaseViewModel.handleEvent(PurchaseUiEvent.Initialize)
    }

    DrawingAppWithTheme { themeManager ->
        MainNavHost(
            navController = navController,
            purchaseViewModel = purchaseViewModel,
            themeManager = themeManager,
            platform = platform
        )
    }
}

