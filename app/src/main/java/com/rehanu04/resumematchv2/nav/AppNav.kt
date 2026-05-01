package com.rehanu04.resumematchv2.nav

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rehanu04.resumematchv2.data.UserProfileStore
import com.rehanu04.resumematchv2.ui.*

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
                // THIS FIXES THE BUG! Now clicking the Vault goes to the Master Vault Screen
                onNavigateToVault = { nav.navigate(Routes.MASTER_VAULT) },
                onNavigateToInterviewHub = { nav.navigate(Routes.INTERVIEW_HUB) }
            )
        }

        composable(Routes.INTERVIEW_HUB) {
            InterviewHubScreen(
                isDark = darkMode,
                onBack = { nav.popBackStack() },
                onNavigateToLiveVoice = { nav.navigate("live_interview") },
                onNavigateToTechnical = { nav.navigate("mock_interview") },
                onNavigateToAptitude = { Toast.makeText(context, "Aptitude Tests Module Initiating...", Toast.LENGTH_SHORT).show() },
                onNavigateToGroupDiscussion = { Toast.makeText(context, "Multi-Agent GD Module Initiating...", Toast.LENGTH_SHORT).show() },
                onNavigateToJobSimulation = { Toast.makeText(context, "Immersive Job Sim Initiating...", Toast.LENGTH_SHORT).show() }
            )
        }

        composable(Routes.ANALYZE) {
            AnalyzeScreen(
                isDark = darkMode,
                onToggleTheme = onToggleDark,
                onBack = { nav.popBackStack() }, // Back Button Support Added!
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
    }
}