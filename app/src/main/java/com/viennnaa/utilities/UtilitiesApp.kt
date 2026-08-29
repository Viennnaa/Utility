package com.viennnaa.utilities

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.viennnaa.utilities.miniapp.MiniApp
import com.viennnaa.utilities.miniapp.MiniAppCatalog
import com.viennnaa.utilities.miniapp.findMiniApp
import com.viennnaa.utilities.ui.home.HomeScreen

private const val MINI_APP_ID_ARG = "miniAppId"

private object Routes {
    const val HOME = "home"
    const val MINI_APP = "miniApp/{$MINI_APP_ID_ARG}"

    fun miniApp(id: String) = "miniApp/$id"
}

/**
 * Hosts the home screen and every mini app registered in [MiniAppCatalog]. Mini
 * apps all share one route, so registering a new one needs no change here.
 */
@Composable
fun UtilitiesApp(miniApps: List<MiniApp> = MiniAppCatalog) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                miniApps = miniApps,
                onOpenMiniApp = { navController.navigate(Routes.miniApp(it.id)) },
            )
        }
        composable(
            route = Routes.MINI_APP,
            arguments = listOf(navArgument(MINI_APP_ID_ARG) { type = NavType.StringType }),
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
