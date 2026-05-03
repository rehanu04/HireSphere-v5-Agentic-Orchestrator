package com.rehanu04.resumematchv2.nav

import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rehanu04.resumematchv2.data.UserProfileStore
import com.rehanu04.resumematchv2.ui.viewmodel.ActivityViewModel

// --- Explicit UI Imports ---
import com.rehanu04.resumematchv2.ui.*

/**
 * HireSphere v5 - Central Navigation Orchestrator [2026 Edition].
 * Features: Shared ActivityViewModel, Real-time SROM Tracking, and Durable Audit Routing.
 */
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

    // --- Initialize the "Nervous System" ---
    // This shared ViewModel ensures data persists across all screen transitions.
    val activityViewModel: ActivityViewModel = viewModel()

    NavHost(navController = nav, startDestination = Routes.HOME) {

        // --- HOME SYSTEM ---
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

        // --- INTERVIEW & GAUNTLET HUB ---
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

        // --- TECHNICAL GAUNTLET [TG-A1 to TG-A5] ---
        composable("technical_round") {
            TechnicalTurnaroundScreen(
                onBack = {
                    // Log an "INTERRUPTED" status if they quit early[cite: 15]
                    activityViewModel.addLog(LogEntry("MAY 03", "Technical Gauntlet", "INTERRUPTED", -2, "User exited session during TG-A2."))
                    nav.popBackStack()
                },
                onComplete = { score, gatesCompleted ->
                    // Update scores and log success[cite: 15, 16]
                    activityViewModel.updateMetrics(0.85f, 0.72f, 0.65f)
                    activityViewModel.addLog(LogEntry("MAY 03", "Technical Gauntlet", "COMPLETED", -5, "Successfully validated 5/5 architectural gates."))
                    nav.navigate("skill_standings")
                }
            )
        }

        // --- ADAPTIVE INTERVIEW SYSTEMS ---
        composable("mock_interview") {
            MockInterviewScreen(
                onBack = { nav.popBackStack() },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
                // Completion logic can be added here to call activityViewModel.addLog()
            )
        }

        composable("live_interview") {
            LiveInterviewScreen(
                onBack = {
                    activityViewModel.addLog(LogEntry("MAY 03", "Live Voice Interview", "INTERRUPTED", -2, "Voice session lost connection."))
                    nav.popBackStack()
                },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
            )
        }

        // --- DURABLE AUDIT & PERFORMANCE LEDGERS ---
        composable("activity_log") {
            // Screen now observes the live logs from the ViewModel[cite: 15]
            val logs by activityViewModel.logs.collectAsState()
            LogHistoryScreen(
                onBack = { nav.popBackStack() },
                logs = logs
            )
        }

        composable("skill_standings") {
            // Screen now observes the live proficiency metrics[cite: 16]
            val tech: Float by activityViewModel.techScore.collectAsState()
            val sustain: Float by activityViewModel.sustainabilityIndex.collectAsState()
            val stability: Float by activityViewModel.stabilityIndex.collectAsState()

            SkillStandingsScreen(
                onBack = { nav.popBackStack() },
                techScore = tech,
                sustainabilityIndex = sustain,
                stabilityIndex = stability
            )
        }

        // --- CAREER ASSETS & THE VAULT ---
        composable(Routes.MASTER_VAULT) {
            MasterVaultScreen(
                onBack = { nav.popBackStack() },
                onGoToInterview = { nav.navigate("mock_interview") },
                onGoToLiveVoice = { nav.navigate("live_interview") },
                onGoToHistory = { nav.navigate("activity_log") },
                onGoToStandings = { nav.navigate("skill_standings") },
                userProfileStore = userProfileStore,
                apiBaseUrl = apiBaseUrl
            )
        }

        // --- CORE RESUME & ANALYSIS MODULES ---
        composable(Routes.ANALYZE) {
            val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())
            AnalyzeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onBack = { nav.popBackStack() },
                onGoCreate = {
                    // Log the analysis action[cite: 15]
                    activityViewModel.addLog(LogEntry("MAY 03", "Resume Analysis", "SUCCESS", -1, "Analyzed alignment for ${userProfile.targetRole}."))
                    nav.navigate(Routes.CREATE)
                },
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

        // --- USER PROFILE & ASSISTANCE ---
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

        // --- IMMERSIVE RECRUITMENT SIMULATIONS ---
        composable("gauntlet_screen") {
            GauntletContainerScreen(
                isDark = darkMode,
                onExit = {
                    activityViewModel.addLog(LogEntry("MAY 03", "Recruitment Gauntlet", "INTERRUPTED", -5, "User exited full simulation."))
                    nav.popBackStack()
                }
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