package com.rehanu04.resumematchv2.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNav(
    darkMode: Boolean,
    onToggleDark: (Boolean) -> Unit,
    apiBaseUrl: String,
    apiAppKey: String
) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Routes.ANALYZE) {
        composable(Routes.ANALYZE) {
            // POSITIONAL args to avoid param-name mismatch issues
            com.rehanu04.resumematchv2.ui.AnalyzeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onGoCreate = { nav.navigate(Routes.CREATE) },
                apiBaseUrl = apiBaseUrl,
                apiAppKey = apiAppKey
            )
        }
        composable(Routes.CREATE) {
            com.rehanu04.resumematchv2.ui.CreateResumeScreen(
                darkMode,
                onToggleDark,
                { nav.popBackStack() },
                apiBaseUrl,
                apiAppKey
            )
        }
    }
}
