@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rehanu04.resumematchv2.data.UserProfileStore
import com.rehanu04.resumematchv2.util.SupabaseClient
import com.rehanu04.resumematchv2.util.isOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class VaultProject(val name: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")
data class VaultExperience(val company: String = "", val role: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")

private val MONTH_OPTIONS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val YEAR_OPTIONS = (Calendar.getInstance().get(Calendar.YEAR) + 5 downTo 1980).map { it.toString() }

// ✨ NEW: Reusable Expandable Accordion Component
@Composable
fun ExpandableVaultSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("$title ($count)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun MasterVaultScreen(
    onBack: () -> Unit,
    onGoToInterview: () -> Unit = {},
    onGoToLiveVoice: () -> Unit = {}, // ✅ NEW
    userProfileStore: UserProfileStore,
    apiBaseUrl: String = "https://resumematch-ai-backend.onrender.com"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    val snackbarHostState = remember { SnackbarHostState() }

    val vaultProjects: List<VaultProject> = try {
        val listTypeProj = object : TypeToken<List<VaultProject>>() {}.type
        val parsed: List<VaultProject>? = gson.fromJson(userProfile.savedProjectsJson, listTypeProj)
        parsed?.filterNotNull()?.map {
            VaultProject(name = it.name ?: "Unknown Project", startMonth = it.startMonth ?: "", startYear = it.startYear ?: "", endMonth = it.endMonth ?: "", endYear = it.endYear ?: "", bullets = it.bullets ?: "")
        } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val vaultExperience: List<VaultExperience> = try {
        val listTypeExp = object : TypeToken<List<VaultExperience>>() {}.type
        val parsed: List<VaultExperience>? = gson.fromJson(userProfile.savedExperienceJson, listTypeExp)
        parsed?.filterNotNull()?.map {
            VaultExperience(company = it.company ?: "Unknown Company", role = it.role ?: "Unknown Role", startMonth = it.startMonth ?: "", startYear = it.startYear ?: "", endMonth = it.endMonth ?: "", endYear = it.endYear ?: "", bullets = it.bullets ?: "")
        } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val vaultSkills: List<String> = try {
        val listTypeSkill = object : TypeToken<List<String>>() {}.type
        val parsed: List<String>? = gson.fromJson(userProfile.savedSkillsJson, listTypeSkill)
        parsed?.filterNotNull()?.filter { it.isNotBlank() } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    var editingProjectIndex by remember { mutableIntStateOf(-1) }
    var editingExperienceIndex by remember { mutableIntStateOf(-1) }
    var tempProject by remember { mutableStateOf(VaultProject()) }
    var tempExperience by remember { mutableStateOf(VaultExperience()) }

    // ✨ Accordion Expansion States (Skills open by default, others closed to save space)
    var expandedSkills by remember { mutableStateOf(true) }
    var expandedProjects by remember { mutableStateOf(false) }
    var expandedExperience by remember { mutableStateOf(false) }

    var isSyncing by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analyticsStrengths by remember { mutableStateOf<List<String>>(emptyList()) }
    var analyticsGaps by remember { mutableStateOf<List<String>>(emptyList()) }
    var analyticsError by remember { mutableStateOf("") }

    val isVaultEmpty = vaultSkills.isEmpty() && vaultProjects.isEmpty() && vaultExperience.isEmpty()

    fun analyzeVault() {
        if (!isOnline(context)) { analyticsError = "You are offline."; showAnalyticsDialog = true; return }
        isAnalyzing = true; showAnalyticsDialog = true; analyticsError = ""
        scope.launch {
            try {
                val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
                val vaultDataStr = "Skills: ${vaultSkills.joinToString(", ")}\nExp: ${userProfile.savedExperienceJson}\nProjects: ${userProfile.savedProjectsJson}"
                val jsonBody = JSONObject().apply { put("vault_data", vaultDataStr); put("target_role", userProfile.targetRole.ifBlank { "Software Engineer" }) }.toString()
                val req = Request.Builder().url(apiBaseUrl.trimEnd('/') + "/v1/ai/analytics").post(jsonBody.toRequestBody("application/json".toMediaType())).build()
                val responseStr = withContext(Dispatchers.IO) { client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null } }
                if (responseStr != null) {
                    val parsed = JSONObject(responseStr)
                    val sArray = parsed.optJSONArray("strengths"); val gArray = parsed.optJSONArray("gaps")
                    val sList = mutableListOf<String>(); val gList = mutableListOf<String>()
                    if (sArray != null) for (i in 0 until sArray.length()) sList.add(sArray.getString(i))
                    if (gArray != null) for (i in 0 until gArray.length()) gList.add(gArray.getString(i))
                    analyticsStrengths = sList; analyticsGaps = gList
                } else { analyticsError = "Failed to analyze vault." }
            } catch (e: Exception) { analyticsError = "Connection Error. Render server might be asleep." } finally { isAnalyzing = false }
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Master Vault", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    // CLOUD DOWNLOAD BUTTON
                    IconButton(onClick = {
                        if (!isOnline(context)) { scope.launch { snackbarHostState.showSnackbar("You are offline!") }; return@IconButton }
                        isSyncing = true
                        scope.launch {
                            val cloudData = SupabaseClient.restoreVaultFromCloud(context)
                            if (cloudData != null) {
                                userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = cloudData.optString("projects", "[]"), savedExperienceJson = cloudData.optString("experience", "[]"), savedSkillsJson = cloudData.optString("skills", "[]")))
                                snackbarHostState.showSnackbar("⬇️ Restored from Supabase!")
                            } else { snackbarHostState.showSnackbar("❌ No cloud backup found.") }
                            isSyncing = false
                        }
                    }) {
                        if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        else Icon(Icons.Filled.CloudDownload, "Restore Backup", tint = MaterialTheme.colorScheme.primary)
                    }

                    // CLOUD UPLOAD BUTTON
                    IconButton(onClick = {
                        if (!isOnline(context)) { scope.launch { snackbarHostState.showSnackbar("You are offline!") }; return@IconButton }
                        isSyncing = true
                        scope.launch {
                            val success = SupabaseClient.backupVaultToCloud(context, userProfile.savedProjectsJson, userProfile.savedExperienceJson, userProfile.savedSkillsJson)
                            isSyncing = false
                            snackbarHostState.showSnackbar(if (success) "☁️ Backed up to Supabase!" else "❌ Cloud Backup Failed")
                        }
                    }) {
                        if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        else Icon(Icons.Filled.CloudUpload, "Cloud Backup", tint = MaterialTheme.colorScheme.primary)
                    }

                    // AI INSIGHTS BUTTON
                    if (!isVaultEmpty) {
                        IconButton(onClick = { analyzeVault() }) { Icon(Icons.Filled.AutoAwesome, "Analyze Vault", tint = MaterialTheme.colorScheme.primary) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->

        if (isVaultEmpty) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Info, "Empty", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                Spacer(Modifier.height(24.dp))
                Text("Your Vault is Empty", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text("Tap the microphone in the AI Assistant to start building your career profile!", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 40.dp) // Padding for scroll clearance
            ) {
                // ✅ Quick Insights Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { analyzeVault() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("AI Career Insights", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Tap to analyze your skills against your target role.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
                // 🎙️ Mock Interview Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onGoToInterview() // ✅ FIXED: Now actually navigates!
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Mic, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("AI Mock Interview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("Practice technical & behavioral questions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }

                // 🎧 Live Voice Coach Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onGoToLiveVoice() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Mic, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Live Voice Interview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text("Real-time voice conversation with your AI Coach.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }

                // --- 🌟 SKILLS SECTION (Collapsible) ---
                if (vaultSkills.isNotEmpty()) {
                    item {
                        ExpandableVaultSection(
                            title = "Skills",
                            icon = Icons.Filled.Star,
                            count = vaultSkills.size,
                            expanded = expandedSkills,
                            onToggle = { expandedSkills = !expandedSkills }
                        ) {
                            val chunkedSkills = vaultSkills.chunked(2)
                            Column {
                                chunkedSkills.forEach { rowSkills ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowSkills.forEach { skill ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(skill, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Icon(
                                                        Icons.Filled.Close, "Remove",
                                                        modifier = Modifier.size(16.dp).clickable {
                                                            val oldList = vaultSkills
                                                            val newList = vaultSkills.filter { it != skill }
                                                            scope.launch {
                                                                userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(newList)))
                                                                val result = snackbarHostState.showSnackbar("'$skill' removed", "UNDO", duration = SnackbarDuration.Short)
                                                                if (result == SnackbarResult.ActionPerformed) { userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(oldList))) }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        if (rowSkills.size == 1) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 🚀 PROJECTS SECTION (Collapsible) ---
                if (vaultProjects.isNotEmpty()) {
                    item {
                        ExpandableVaultSection(
                            title = "Projects",
                            icon = Icons.Filled.Star,
                            count = vaultProjects.size,
                            expanded = expandedProjects,
                            onToggle = { expandedProjects = !expandedProjects }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                vaultProjects.forEachIndexed { index, proj ->
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                        Column(Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(proj.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                IconButton(onClick = { editingProjectIndex = index; tempProject = proj }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                                IconButton(onClick = {
                                                    val oldList = vaultProjects
                                                    val newList = vaultProjects.filterIndexed { i, _ -> i != index }
                                                    scope.launch {
                                                        userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(newList)))
                                                        val result = snackbarHostState.showSnackbar("Project deleted", "UNDO", duration = SnackbarDuration.Short)
                                                        if (result == SnackbarResult.ActionPerformed) userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(oldList)))
                                                    }
                                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                                            }
                                            Text("${proj.startMonth} ${proj.startYear} — ${proj.endMonth} ${proj.endYear}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                            Spacer(Modifier.height(8.dp))
                                            Text(proj.bullets, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 💼 EXPERIENCE SECTION (Collapsible) ---
                if (vaultExperience.isNotEmpty()) {
                    item {
                        ExpandableVaultSection(
                            title = "Experience",
                            icon = Icons.Filled.Person,
                            count = vaultExperience.size,
                            expanded = expandedExperience,
                            onToggle = { expandedExperience = !expandedExperience }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                vaultExperience.forEachIndexed { index, exp ->
                                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                        Column(Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(exp.company, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                                IconButton(onClick = { editingExperienceIndex = index; tempExperience = exp }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                                IconButton(onClick = {
                                                    val oldList = vaultExperience
                                                    val newList = vaultExperience.filterIndexed { i, _ -> i != index }
                                                    scope.launch {
                                                        userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(newList)))
                                                        val result = snackbarHostState.showSnackbar("Experience deleted", "UNDO", duration = SnackbarDuration.Short)
                                                        if (result == SnackbarResult.ActionPerformed) userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(oldList)))
                                                    }
                                                }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                                            }
                                            Text(exp.role, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text("${exp.startMonth} ${exp.startYear} — ${exp.endMonth} ${exp.endYear}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                            Spacer(Modifier.height(8.dp))
                                            Text(exp.bullets, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ✅ Education & Achievement Clarification Hint
                item {
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, "Info", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Where is Education & Achievements?", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("These sections are highly specific to the PDF template you choose, so they are managed directly inside the Create Resume builder rather than the Vault!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // ✅ Analytics Dialog UI
    if (showAnalyticsDialog) {
        Dialog(onDismissRequest = { showAnalyticsDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AutoAwesome, "AI", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text("Career Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Target: ${userProfile.targetRole.ifBlank { "General Role" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(24.dp))

                    if (isAnalyzing) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Analyzing your Master Vault...")
                    } else if (analyticsError.isNotEmpty()) {
                        Text(analyticsError, color = MaterialTheme.colorScheme.error)
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("🌟 Strengths", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            analyticsStrengths.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                            Spacer(Modifier.height(16.dp))
                            Text("⚠️ Missing Skills & Gaps", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            analyticsGaps.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { showAnalyticsDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                }
            }
        }
    }
}