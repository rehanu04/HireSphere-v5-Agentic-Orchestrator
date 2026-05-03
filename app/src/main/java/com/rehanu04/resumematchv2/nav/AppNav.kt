package com.rehanu04.resumematchv2.nav

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rehanu04.resumematchv2.data.UserProfileStore

// Explicit imports for every UI screen to resolve the "Unresolved Reference" errors
import com.rehanu04.resumematchv2.ui.HomeScreen
import com.rehanu04.resumematchv2.ui.InterviewHubScreen
import com.rehanu04.resumematchv2.ui.AnalyzeScreen
import com.rehanu04.resumematchv2.ui.CreateResumeScreen
import com.rehanu04.resumematchv2.ui.ProfileSetupScreen
import com.rehanu04.resumematchv2.ui.AiAssistantScreen
import com.rehanu04.resumematchv2.ui.MasterVaultScreen
import com.rehanu04.resumematchv2.ui.MockInterviewScreen
import com.rehanu04.resumematchv2.ui.LiveInterviewScreen
import com.rehanu04.resumematchv2.ui.GauntletContainerScreen
import com.rehanu04.resumematchv2.ui.TechnicalTurnaroundScreen

@Composable
fun AppNav(
    darkMode: Boolean,
    onToggleDark: (Boolean) -> Unit,
    apiBaseUrl: String,
    apiAppKey: String
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val userProfileStore = remember { UserProfileStore(context) }

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onNavigateToAnalyze = { nav.navigate(Routes.ANALYZE) },
                onNavigateToCreate = { nav.navigate(Routes.CREATE) },
                onNavigateToVault = { nav.navigate(Routes.MASTER_VAULT) },
                onNavigateToInterviewHub = { nav.navigate(Routes.INTERVIEW_HUB) },
                onNavigateToGauntlet = { nav.navigate("gauntlet_screen") }
            )
        }

        composable(Routes.INTERVIEW_HUB) {
            InterviewHubScreen(
                isDark = darkMode,
                onBack = { nav.popBackStack() },
                onNavigateToLiveVoice = { nav.navigate("live_interview") },
                onNavigateToTechnical = { nav.navigate("technical_round") },
                onNavigateToAptitude = { nav.navigate("gauntlet_screen/APTITUDE") },
                onNavigateToGroupDiscussion = { nav.navigate("gauntlet_screen/GD") },
                onNavigateToJobSimulation = { nav.navigate("gauntlet_screen/FULL_SIM") },
                techPassed = false,
                aptitudePassed = false
            )
        }

        composable("technical_round") {
            TechnicalTurnaroundScreen(
                onBack = { nav.popBackStack() },
                onComplete = { score, gatesCompleted ->
                    nav.popBackStack()
                }
            )
        }

        // --- Standard Screens ---
        composable(Routes.ANALYZE) {
            AnalyzeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onBack = { nav.popBackStack() },
                onGoCreate = { nav.navigate(Routes.CREATE) },
                onGoProfile = { nav.navigate(Routes.PROFILE) },
                apiBaseUrl = apiBaseUrl,
                apiAppKey = apiAppKey,
                userProfileStore = userProfileStore
            )
        }
        composable(Routes.CREATE) {
            CreateResumeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onBack = { nav.popBackStack() },
                onGoAiAssistant = { nav.navigate(Routes.AI_ASSISTANT) },
                onGoProfile = { nav.navigate(Routes.PROFILE) },
                apiBaseUrl = apiBaseUrl,
                apiAppKey = apiAppKey,
                userProfileStore = userProfileStore
            )
        }
        composable(Routes.PROFILE) {
            ProfileSetupScreen(
                userProfileStore = userProfileStore,
                onBack = { nav.popBackStack() },
                onGoMasterVault = { nav.navigate(Routes.MASTER_VAULT) }
            )
        }
        composable(Routes.AI_ASSISTANT) {
            AiAssistantScreen(
                onBack = { nav.popBackStack() },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
            )
        }
        composable(Routes.MASTER_VAULT) {
            MasterVaultScreen(
                onBack = { nav.popBackStack() },
                onGoToInterview = { nav.navigate("mock_interview") },
                onGoToLiveVoice = { nav.navigate("live_interview") },
                userProfileStore = userProfileStore
            )
        }
        composable("mock_interview") {
            MockInterviewScreen(
                onBack = { nav.popBackStack() },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
            )
        }
        composable("live_interview") {
            LiveInterviewScreen(
                onBack = { nav.popBackStack() },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
            )
        }
        composable("gauntlet_screen") {
            GauntletContainerScreen(
                isDark = darkMode,
                onExit = { nav.popBackStack() }
            )
        }
        composable("gauntlet_screen/{startStage}") { backStackEntry ->
            val startStage = backStackEntry.arguments?.getString("startStage") ?: "TECH"
            GauntletContainerScreen(
                startStage = startStage,
                isDark = darkMode,
                onExit = { nav.popBackStack() }
            )
        }
    }
}