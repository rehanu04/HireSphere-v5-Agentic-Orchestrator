@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// --- DATA MODELS ---
data class VaultProject(val name: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")
data class VaultExperience(val company: String = "", val role: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")

private val MONTH_OPTIONS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val YEAR_OPTIONS = (Calendar.getInstance().get(Calendar.YEAR) + 5 downTo 1980).map { it.toString() }

// ==========================================
// COMPONENT: KINETIC BACKGROUND
// ==========================================
@Composable
fun KineticBackground(accentColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFF030303))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width / 2, size.height * 0.2f),
                radius = size.width * 1.5f
            )
        )
    }
}

// ==========================================
// COMPONENT: BOUNCY ACTION CARD
// ==========================================
@Composable
fun BouncyVaultCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")
    val rotation by animateFloatAsState(if (isPressed) -1.5f else 0f, label = "rotation")

    Card(
        modifier = modifier
            .height(110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try { awaitRelease() } finally { isPressed = false }
                    },
                    onTap = { onClick() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0C)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SelectionField(label: String, value: String, options: List<String>, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onValueChange(opt); expanded = false })
            }
        }
    }
}

@Composable
fun ExpandableVaultSection(
    title: String,
    icon: ImageVector,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Color(0xFF22D3EE))
                Spacer(Modifier.width(12.dp))
                Text("$title ($count)", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            if (expanded) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, bottom = 20.dp)) { content() }
            }
        }
    }
}

@Composable
fun MasterVaultScreen(
    onBack: () -> Unit,
    onGoToInterview: () -> Unit = {},
    onGoToLiveVoice: () -> Unit = {},
    onGoToHistory: () -> Unit = {},
    onGoToStandings: () -> Unit = {},
    userProfileStore: UserProfileStore,
    apiBaseUrl: String = "https://resumematch-ai-backend.onrender.com"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())
    val snackbarHostState = remember { SnackbarHostState() }
    val accentCyan = Color(0xFF22D3EE)

    // --- JSON Logic ---
    val vaultProjects: List<VaultProject> = try {
        val listTypeProj = object : TypeToken<List<VaultProject>>() {}.type
        gson.fromJson<List<VaultProject>>(userProfile.savedProjectsJson, listTypeProj)?.filterNotNull()?.map {
            it.copy(name = it.name ?: "Unknown Project")
        } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val vaultExperience: List<VaultExperience> = try {
        val listTypeExp = object : TypeToken<List<VaultExperience>>() {}.type
        gson.fromJson<List<VaultExperience>>(userProfile.savedExperienceJson, listTypeExp)?.filterNotNull()?.map {
            it.copy(company = it.company ?: "Unknown Company", role = it.role ?: "Unknown Role")
        } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    val vaultSkills: List<String> = try {
        val listTypeSkill = object : TypeToken<List<String>>() {}.type
        gson.fromJson<List<String>>(userProfile.savedSkillsJson, listTypeSkill)?.filterNotNull()?.filter { it.isNotBlank() } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    // --- State Management (FIXED TYPE INFERENCE) ---
    var editingProjectIndex by remember { mutableStateOf<Int>(-1) }
    var editingExperienceIndex by remember { mutableStateOf<Int>(-1) }
    var tempProject by remember { mutableStateOf(VaultProject()) }
    var tempExperience by remember { mutableStateOf(VaultExperience()) }
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
                val jsonBody = JSONObject().apply {
                    put("vault_data", vaultDataStr)
                    put("target_role", userProfile.targetRole.ifBlank { "Software Engineer" })
                }.toString()
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
            } catch (e: Exception) { analyticsError = "Connection Error." } finally { isAnalyzing = false }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        KineticBackground(accentCyan)

        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Master Vault", fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
                    actions = {
                        IconButton(onClick = {
                            if (!isOnline(context)) { scope.launch { snackbarHostState.showSnackbar("Offline") }; return@IconButton }
                            isSyncing = true
                            scope.launch {
                                val cloudData = SupabaseClient.restoreVaultFromCloud(context)
                                if (cloudData != null) {
                                    userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = cloudData.optString("projects", "[]"), savedExperienceJson = cloudData.optString("experience", "[]"), savedSkillsJson = cloudData.optString("skills", "[]")))
                                    snackbarHostState.showSnackbar("⬇️ Restored!")
                                } else { snackbarHostState.showSnackbar("❌ No backup found.") }
                                isSyncing = false
                            }
                        }) {
                            if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accentCyan)
                            else Icon(Icons.Filled.CloudDownload, null, tint = accentCyan)
                        }

                        IconButton(onClick = {
                            if (!isOnline(context)) { scope.launch { snackbarHostState.showSnackbar("Offline") }; return@IconButton }
                            isSyncing = true
                            scope.launch {
                                val success = SupabaseClient.backupVaultToCloud(context, userProfile.savedProjectsJson, userProfile.savedExperienceJson, userProfile.savedSkillsJson)
                                isSyncing = false
                                snackbarHostState.showSnackbar(if (success) "☁️ Synced!" else "❌ Failed")
                            }
                        }) {
                            if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accentCyan)
                            else Icon(Icons.Filled.CloudUpload, null, tint = accentCyan)
                        }

                        if (!isVaultEmpty) {
                            IconButton(onClick = { analyzeVault() }) { Icon(Icons.Filled.AutoAwesome, null, tint = accentCyan) }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding -> // FIXED: PADDING AMBIGUITY
            if (isVaultEmpty) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Info, null, modifier = Modifier.size(100.dp), tint = accentCyan.copy(alpha = 0.2f))
                    Text("Your Vault is Empty", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("Build your profile via AI Assistant!", textAlign = TextAlign.Center, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 60.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BouncyVaultCard("Activity Log", Icons.Default.History, accentCyan, onGoToHistory, Modifier.weight(1f))
                            BouncyVaultCard("Standings", Icons.Default.Assessment, accentCyan, onGoToStandings, Modifier.weight(1f))
                        }
                    }

                    item {
                        VaultServiceCard("AI Career Insights", "Deep analysis against target roles.", Icons.Filled.AutoAwesome, Color(0xFF6366F1), analyzeVault)
                        Spacer(Modifier.height(12.dp))
                        VaultServiceCard("AI Mock Interview", "Behavioral & Tech simulation.", Icons.Filled.Mic, Color(0xFF8B5CF6), onGoToInterview)
                        Spacer(Modifier.height(12.dp))
                        VaultServiceCard("Live Voice Coach", "Real-time conversation.", Icons.Filled.Mic, accentCyan, onGoToLiveVoice)
                    }

                    if (vaultSkills.isNotEmpty()) {
                        item {
                            ExpandableVaultSection("Skills", Icons.Filled.Star, vaultSkills.size, expandedSkills, { expandedSkills = !expandedSkills }) {
                                Column {
                                    vaultSkills.chunked(2).forEach { rowSkills ->
                                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), Arrangement.spacedBy(8.dp)) {
                                            rowSkills.forEach { skill ->
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp), color = accentCyan.copy(alpha = 0.1f), modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(Modifier.padding(12.dp), Alignment.CenterVertically) {
                                                        Text(skill, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp).clickable {
                                                            val oldList = vaultSkills; val newList = vaultSkills.filter { it != skill }
                                                            scope.launch {
                                                                userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(newList)))
                                                                val res = snackbarHostState.showSnackbar("Removed", "UNDO")
                                                                if (res == SnackbarResult.ActionPerformed) userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(oldList)))
                                                            }
                                                        }, tint = Color.Gray)
                                                    }
                                                }
                                            }
                                            if (rowSkills.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (vaultProjects.isNotEmpty()) {
                        item {
                            ExpandableVaultSection("Projects", Icons.Filled.Star, vaultProjects.size, expandedProjects, { expandedProjects = !expandedProjects }) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    vaultProjects.forEachIndexed { index, proj ->
                                        VaultAssetCard(proj.name, "${proj.startMonth} ${proj.startYear} - ${proj.endMonth} ${proj.endYear}", proj.bullets,
                                            onEdit = { editingProjectIndex = index; tempProject = proj },
                                            onDelete = {
                                                val oldList = vaultProjects; val newList = vaultProjects.filterIndexed { i, _ -> i != index }
                                                scope.launch {
                                                    userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(newList)))
                                                    val res = snackbarHostState.showSnackbar("Deleted", "UNDO")
                                                    if (res == SnackbarResult.ActionPerformed) userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(oldList)))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (vaultExperience.isNotEmpty()) {
                        item {
                            ExpandableVaultSection("Experience", Icons.Filled.Person, vaultExperience.size, expandedExperience, { expandedExperience = !expandedExperience }) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    vaultExperience.forEachIndexed { index, exp ->
                                        VaultAssetCard("${exp.company} | ${exp.role}", "${exp.startMonth} ${exp.startYear} - ${exp.endMonth} ${exp.endYear}", exp.bullets,
                                            onEdit = { editingExperienceIndex = index; tempExperience = exp },
                                            onDelete = {
                                                val oldList = vaultExperience; val newList = vaultExperience.filterIndexed { i, _ -> i != index }
                                                scope.launch {
                                                    userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(newList)))
                                                    val res = snackbarHostState.showSnackbar("Deleted", "UNDO")
                                                    if (res == SnackbarResult.ActionPerformed) userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(oldList)))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(Modifier.padding(16.dp), Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, null, tint = accentCyan)
                                Spacer(Modifier.width(12.dp))
                                Text("Education & Awards are managed directly in the Resume Builder!", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CRUD Dialogs (Logic Fully Maintained) ---
    if (editingProjectIndex >= 0) {
        val endM = listOf("Present") + MONTH_OPTIONS; val endY = listOf("Present") + YEAR_OPTIONS
        AlertDialog(
            onDismissRequest = { editingProjectIndex = -1 },
            title = { Text("Edit Project") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tempProject.name, onValueChange = { tempProject = tempProject.copy(name = it) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    Row(Arrangement.spacedBy(8.dp)) {
                        SelectionField("Start M", tempProject.startMonth, MONTH_OPTIONS, { tempProject = tempProject.copy(startMonth = it) }, Modifier.weight(1f))
                        SelectionField("Start Y", tempProject.startYear, YEAR_OPTIONS, { tempProject = tempProject.copy(startYear = it) }, Modifier.weight(1f))
                    }
                    Row(Arrangement.spacedBy(8.dp)) {
                        SelectionField("End M", tempProject.endMonth, endM, { tempProject = tempProject.copy(endMonth = it) }, Modifier.weight(1f))
                        SelectionField("End Y", tempProject.endYear, endY, { tempProject = tempProject.copy(endYear = it) }, Modifier.weight(1f))
                    }
                    OutlinedTextField(value = tempProject.bullets, onValueChange = { tempProject = tempProject.copy(bullets = it) }, label = { Text("Bullets") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            },
            confirmButton = { Button(onClick = {
                val newList = vaultProjects.toMutableList(); newList[editingProjectIndex] = tempProject
                scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(newList))); editingProjectIndex = -1 }
            }) { Text("Save") } }
        )
    }

    if (editingExperienceIndex >= 0) {
        val endM = listOf("Present") + MONTH_OPTIONS; val endY = listOf("Present") + YEAR_OPTIONS
        AlertDialog(
            onDismissRequest = { editingExperienceIndex = -1 },
            title = { Text("Edit Experience") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tempExperience.company, onValueChange = { tempExperience = tempExperience.copy(company = it) }, label = { Text("Company") })
                    OutlinedTextField(value = tempExperience.role, onValueChange = { tempExperience = tempExperience.copy(role = it) }, label = { Text("Role") })
                    Row(Arrangement.spacedBy(8.dp)) {
                        SelectionField("Start M", tempExperience.startMonth, MONTH_OPTIONS, { tempExperience = tempExperience.copy(startMonth = it) }, Modifier.weight(1f))
                        SelectionField("Start Y", tempExperience.startYear, YEAR_OPTIONS, { tempExperience = tempExperience.copy(startYear = it) }, Modifier.weight(1f))
                    }
                    Row(Arrangement.spacedBy(8.dp)) {
                        SelectionField("End M", tempExperience.endMonth, endM, { tempExperience = tempExperience.copy(endMonth = it) }, Modifier.weight(1f))
                        SelectionField("End Y", tempExperience.endYear, endY, { tempExperience = tempExperience.copy(endYear = it) }, Modifier.weight(1f))
                    }
                    OutlinedTextField(value = tempExperience.bullets, onValueChange = { tempExperience = tempExperience.copy(bullets = it) }, label = { Text("Bullets") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            },
            confirmButton = { Button(onClick = {
                val newList = vaultExperience.toMutableList(); newList[editingExperienceIndex] = tempExperience
                scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(newList))); editingExperienceIndex = -1 }
            }) { Text("Save") } }
        )
    }

    if (showAnalyticsDialog) {
        Dialog(onDismissRequest = { showAnalyticsDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))) {
                Column(Modifier.padding(24.dp), Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(48.dp), tint = accentCyan)
                    Text("Career Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    if (isAnalyzing) CircularProgressIndicator(color = accentCyan)
                    else {
                        Column(Modifier.fillMaxWidth()) {
                            Text("🌟 Strengths", fontWeight = FontWeight.Bold, color = accentCyan)
                            analyticsStrengths.forEach { Text("• $it", color = Color.White, fontSize = 13.sp) }
                            Spacer(Modifier.height(12.dp))
                            Text("⚠️ Gaps", fontWeight = FontWeight.Bold, color = Color.Red)
                            analyticsGaps.forEach { Text("• $it", color = Color.White, fontSize = 13.sp) }
                        }
                    }
                    Button(onClick = { showAnalyticsDialog = false }, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Close") }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENT: ASSET CARD
// ==========================================
@Composable
fun VaultAssetCard(title: String, subtitle: String, bullets: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = Color(0xFF22D3EE), modifier = Modifier.size(18.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp)) }
            }
            Text(subtitle, color = Color(0xFF22D3EE), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text(bullets, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

// ==========================================
// SUB-COMPONENT: SERVICE CARD
// ==========================================
@Composable
fun VaultServiceCard(title: String, desc: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(accent.copy(alpha = 0.2f), RoundedCornerShape(14.dp)), Alignment.Center) {
                Icon(icon, null, tint = accent)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold)
                Text(desc, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}