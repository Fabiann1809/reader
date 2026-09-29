package io.github.fabiann1809.reader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.fabiann1809.reader.ui.library.LibraryScreen
import io.github.fabiann1809.reader.ui.settings.SettingsScreen

@Composable
fun ReaderNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = LibraryRoute,
        modifier = modifier,
    ) {
        composable<LibraryRoute> {
            LibraryScreen(onOpenSettings = { navController.navigate(SettingsRoute) })
        }
        composable<SettingsRoute> {
            SettingsScreen(onNavigateUp = { navController.navigateUp() })
        }
    }
}
