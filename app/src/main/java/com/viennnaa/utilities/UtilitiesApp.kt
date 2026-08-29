package com.viennnaa.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.viennnaa.utilities.core.settings.AppSettings
import com.viennnaa.utilities.core.settings.ThemeMode
import com.viennnaa.utilities.core.settings.sanitizeFavourites
import com.viennnaa.utilities.core.settings.toggleFavourite
import com.viennnaa.utilities.core.shortcuts.DEEP_LINK_PREFIX
import com.viennnaa.utilities.core.storage.AppPreferences
import com.viennnaa.utilities.miniapp.MiniApp
import com.viennnaa.utilities.miniapp.MiniAppCatalog
import com.viennnaa.utilities.miniapp.findMiniApp
import com.viennnaa.utilities.ui.home.HomeScreen
import com.viennnaa.utilities.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

private const val MINI_APP_ID_ARG = "miniAppId"

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val MINI_APP = "miniApp/{$MINI_APP_ID_ARG}"

    fun miniApp(id: String) = "miniApp/$id"
}

/**
 * Hosts the home screen, settings, and every mini app registered in
 * [MiniAppCatalog]. Mini apps all share one route, so registering a new one
 * needs no change here.
 *
 * @param settings the current app settings, already collected upstream so the
 *   theme and this share one source.
 */
@Composable
fun UtilitiesApp(
    settings: AppSettings,
    preferences: AppPreferences,
    miniApps: List<MiniApp> = MiniAppCatalog,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    fun onToggleFavourite(miniApp: MiniApp) {
        scope.launch {
            val known = miniApps.map { it.id }.toSet()
            val current = sanitizeFavourites(settings.favouriteIds, known)
            preferences.setFavourites(toggleFavourite(current, miniApp.id))
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                miniApps = miniApps,
                favouriteIds = settings.favouriteIds,
                onOpenMiniApp = { navController.navigate(Routes.miniApp(it.id)) },
                onToggleFavourite = { onToggleFavourite(it) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                miniAppCount = miniApps.size,
                onThemeModeChange = { mode: ThemeMode ->
                    scope.launch { preferences.setThemeMode(mode.name) }
                },
                onDynamicColorChange = { enabled ->
                    scope.launch { preferences.setDynamicColor(enabled) }
                },
                onClearFavourites = { scope.launch { preferences.setFavourites(emptyList()) } },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.MINI_APP,
            arguments = listOf(navArgument(MINI_APP_ID_ARG) { type = NavType.StringType }),
            // Launcher shortcuts open mini apps through this, so a shortcut and
            // a tap land on the same destination.
            deepLinks = listOf(
                navDeepLink { uriPattern = "$DEEP_LINK_PREFIX{$MINI_APP_ID_ARG}" },
            ),
        ) { backStackEntry ->
            val miniApp = findMiniApp(
                id = backStackEntry.arguments?.getString(MINI_APP_ID_ARG),
                catalog = miniApps,
            )
            if (miniApp == null) {
                // Only reachable if a mini app is removed while a route to it
                // survives, e.g. through a stale shortcut. Fall back to home.
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                miniApp.screen { navController.popBackStack() }
            }
        }
    }
}
