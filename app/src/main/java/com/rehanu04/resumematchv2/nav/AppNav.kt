package com.rehanu04.resumematchv2.nav

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rehanu04.resumematchv2.data.UserProfileStore
import com.rehanu04.resumematchv2.ui.viewmodel.ActivityViewModel
import com.rehanu04.resumematchv2.data.LogEntry
import com.rehanu04.resumematchv2.ui.*

/**
 * HireSphere v5 - Central Navigation Orchestrator.
 * RESOLVED: TechnicalRoundScreen unresolved reference and naming sync[cite: 11, 16, 18].
 */
@Composable
fun AppNav(
    darkMode: Boolean, // Matches MainActivity variable[cite: 17]
    onToggleDark: (Boolean) -> Unit, // Matches MainActivity function[cite: 17]
    apiBaseUrl: String,
    apiAppKey: String
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val userProfileStore = remember { UserProfileStore(context) }
    val activityViewModel: ActivityViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {

        // --- HOME SYSTEM ---
        composable(Routes.HOME) {
            HomeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onNavigateToAnalyze = { navController.navigate(Routes.ANALYZE) },
                onNavigateToCreate = { navController.navigate(Routes.CREATE) },
                onNavigateToVault = { navController.navigate(Routes.MASTER_VAULT) },
                onNavigateToInterviewHub = { navController.navigate(Routes.INTERVIEW_HUB) },
                onNavigateToGauntlet = { navController.navigate("gauntlet_screen/TECH") },
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) }
            )
        }

        // --- MASTER VAULT ---
        composable(Routes.MASTER_VAULT) {
            MasterVaultScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onBack = { navController.popBackStack() },
                onGoToInterview = { navController.navigate("mock_interview") },
                onGoToLiveVoice = { navController.navigate("live_interview") },
                onGoToHistory = { navController.navigate("activity_log") },
                onGoToStandings = { navController.navigate("skill_standings") },
                onGoToAssistant = { navController.navigate(Routes.AI_ASSISTANT) },
                userProfileStore = userProfileStore
            )
        }

        // --- AI INTERVIEW SIMULATOR ---
        composable("mock_interview") {
            MockInterviewScreen(
                isDark = darkMode,
                onBack = { navController.popBackStack() },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
            )
        }

        // --- INTERVIEW & GAUNTLET HUB ---
        composable(Routes.INTERVIEW_HUB) {
            InterviewHubScreen(
                isDark = darkMode,
                onBack = { navController.popBackStack() },
                onNavigateToLiveVoice = { navController.navigate("live_interview") },
                onNavigateToTechnical = { navController.navigate("technical_round") },
                onNavigateToAptitude = { navController.navigate("gauntlet_screen/APTITUDE") },
                onNavigateToGroupDiscussion = { navController.navigate("gauntlet_screen/GD") },
                onNavigateToJobSimulation = { navController.navigate("gauntlet_screen/FULL_SIM") },
                techPassed = false,
                aptitudePassed = false
            )
        }

        // --- TECHNICAL GAUNTLET SCREEN ---
        composable("technical_round") {
            // FIXED: Using correct function name from your project[cite: 11, 18]
            TechnicalTurnaroundScreen(
                onBack = {
                    activityViewModel.addLog(LogEntry("MAY 05", "Technical Gauntlet", "INTERRUPTED", -2, "Exited simulation."))
                    navController.popBackStack()
                },
                onComplete = { _, _ ->
                    activityViewModel.addLog(LogEntry("MAY 05", "Technical Gauntlet", "COMPLETED", -5, "Validated gates."))
                    navController.navigate("skill_standings")
                }
            )
        }

        // --- ACTIVITY & AUDIT LOGS ---
        composable("activity_log") {
            val logs by activityViewModel.logs.collectAsState()
            LogHistoryScreen(onBack = { navController.popBackStack() }, logs = logs)
        }

        composable("skill_standings") {
            val tech by activityViewModel.techScore.collectAsState()
            val sustain by activityViewModel.sustainabilityIndex.collectAsState()
            val stability by activityViewModel.stabilityIndex.collectAsState()
            SkillStandingsScreen(onBack = { navController.popBackStack() }, techScore = tech, sustainabilityIndex = sustain, stabilityIndex = stability)
        }

        // --- CORE MODULES ---
        composable(Routes.ANALYZE) {
            AnalyzeScreen(isDark = darkMode, onToggleTheme = onToggleDark, onBack = { navController.popBackStack() }, onGoCreate = { navController.navigate(Routes.CREATE) }, onGoProfile = { navController.navigate(Routes.PROFILE) }, apiBaseUrl = apiBaseUrl, apiAppKey = apiAppKey, userProfileStore = userProfileStore)
        }

        composable(Routes.CREATE) {
            CreateResumeScreen(isDark = darkMode, onToggleTheme = onToggleDark, onBack = { navController.popBackStack() }, onGoAiAssistant = { navController.navigate(Routes.AI_ASSISTANT) }, onGoProfile = { navController.navigate(Routes.PROFILE) }, apiBaseUrl = apiBaseUrl, apiAppKey = apiAppKey, userProfileStore = userProfileStore)
        }

        composable(Routes.AI_ASSISTANT) {
            AiAssistantScreen(onBack = { navController.popBackStack() }, userProfileStore = userProfileStore, apiBaseUrl = apiBaseUrl)
        }

        composable("gauntlet_screen/APTITUDE") {
            QuantitativeAptitudeScreen(
                isDark = darkMode,
                apiBaseUrl = apiBaseUrl,
                activityViewModel = activityViewModel,
                userProfileStore = userProfileStore,
                onBack = { navController.popBackStack() }
            )
        }

        composable("gauntlet_screen/GD") {
            GroupDiscussionScreen(
                isDark = darkMode,
                activityViewModel = activityViewModel,
                onBack = { navController.popBackStack() },
                apiBaseUrl = apiBaseUrl
            )
        }

        composable("gauntlet_screen/{startStage}") { backStackEntry ->
            val startStage = backStackEntry.arguments?.getString("startStage") ?: "TECH"
            GauntletContainerScreen(
                isDark = darkMode,
                startStage = startStage,
                onExit = { navController.popBackStack() },
                apiBaseUrl = apiBaseUrl,
                activityViewModel = activityViewModel
            )
        }

        composable(Routes.PROFILE) {
            ProfileSetupScreen(
                onBack = { navController.popBackStack() },
                onGoMasterVault = { navController.navigate(Routes.MASTER_VAULT) },
                userProfileStore = userProfileStore
            )
        }

        composable("live_interview") {
            LiveInterviewScreen(
                onBack = { navController.popBackStack() },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
            )
        }
    }
}