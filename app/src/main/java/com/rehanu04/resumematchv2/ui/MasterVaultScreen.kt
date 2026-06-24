@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rehanu04.resumematchv2.data.UserProfileStore
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * HireSphere v5 - Master Vault [Luxury Persona Edition].
 * Updates: Static Top-Down Gradient (No Spinning), Validated Entries, and Bi-directional Toggle[cite: 1, 3].
 */

// --- DATA MODELS[cite: 1] ---
data class VaultProject(val name: String = "", val startMonth: String = "Not set", val startYear: String = "Not set", val endMonth: String = "Not set", val endYear: String = "Not set", val bullets: String = "", val isPresent: Boolean = false)
data class VaultExperience(val company: String = "", val role: String = "", val startMonth: String = "Not set", val startYear: String = "Not set", val endMonth: String = "Not set", val endYear: String = "Not set", val bullets: String = "", val isPresent: Boolean = false)
data class VaultEducation(val school: String = "", val degree: String = "", val year: String = "Year")
data class VaultCertification(val name: String = "", val issuer: String = "", val year: String = "Year", val summary: String = "")
data class VaultAchievement(val title: String = "", val issuer: String = "", val description: String = "")

private val MONTH_OPTIONS = listOf("Not set", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val YEAR_OPTIONS = listOf("Not set") + (Calendar.getInstance().get(Calendar.YEAR) + 2 downTo 1980).map { it.toString() }

private fun formatDates(sm: String, sy: String, em: String, ey: String, isP: Boolean): String {
    val start = listOf(sm, sy).filter { it != "Not set" }.joinToString(" ").trim()
    val end = if (isP) "Present" else listOf(em, ey).filter { it != "Not set" }.joinToString(" ").trim()
    if (start.isEmpty() && end.isEmpty()) return ""
    if (start.isEmpty()) return end
    if (end.isEmpty()) return start
    return "$start - $end"
}

private fun getValidStartYears(isPresent: Boolean): List<String> = if (isPresent) YEAR_OPTIONS.filter { it == "Not set" || it.toInt() <= 2026 } else YEAR_OPTIONS

private fun getValidStartMonths(startYear: String, isPresent: Boolean): List<String> {
    if (isPresent && startYear == "2026") return MONTH_OPTIONS.subList(0, 7)
    return MONTH_OPTIONS
}

private fun getValidEndYears(startYear: String): List<String> {
    if (startYear == "Not set") return YEAR_OPTIONS
    return YEAR_OPTIONS.filter { it == "Not set" || it.toInt() >= startYear.toInt() }
}

private fun getValidEndMonths(startYear: String, startMonth: String, endYear: String): List<String> {
    if (startYear != "Not set" && startMonth != "Not set" && startYear == endYear) {
        val startIndex = MONTH_OPTIONS.indexOf(startMonth)
        if (startIndex != -1) return listOf("Not set") + MONTH_OPTIONS.subList(startIndex, MONTH_OPTIONS.size)
    }
    return MONTH_OPTIONS
}

private fun validateProjectDates(p: VaultProject): VaultProject {
    var valid = p
    if (p.isPresent) {
        if (p.startYear != "Not set" && p.startYear.toInt() > 2026) valid = valid.copy(startYear = "Not set", startMonth = "Not set")
        else if (p.startYear == "2026" && p.startMonth != "Not set" && MONTH_OPTIONS.indexOf(p.startMonth) > 6) valid = valid.copy(startMonth = "Not set")
    } else {
        if (p.startYear != "Not set" && p.endYear != "Not set") {
            if (p.startYear.toInt() > p.endYear.toInt()) valid = valid.copy(endYear = "Not set", endMonth = "Not set")
            else if (p.startYear == p.endYear && p.startMonth != "Not set" && p.endMonth != "Not set") {
                if (MONTH_OPTIONS.indexOf(p.startMonth) > MONTH_OPTIONS.indexOf(p.endMonth)) valid = valid.copy(endMonth = "Not set")
            }
        }
    }
    return valid
}

private fun validateExperienceDates(e: VaultExperience): VaultExperience {
    var valid = e
    if (e.isPresent) {
        if (e.startYear != "Not set" && e.startYear.toInt() > 2026) valid = valid.copy(startYear = "Not set", startMonth = "Not set")
        else if (e.startYear == "2026" && e.startMonth != "Not set" && MONTH_OPTIONS.indexOf(e.startMonth) > 6) valid = valid.copy(startMonth = "Not set")
    } else {
        if (e.startYear != "Not set" && e.endYear != "Not set") {
            if (e.startYear.toInt() > e.endYear.toInt()) valid = valid.copy(endYear = "Not set", endMonth = "Not set")
            else if (e.startYear == e.endYear && e.startMonth != "Not set" && e.endMonth != "Not set") {
                if (MONTH_OPTIONS.indexOf(e.startMonth) > MONTH_OPTIONS.indexOf(e.endMonth)) valid = valid.copy(endMonth = "Not set")
            }
        }
    }
    return valid
}

// ==========================================
// COMPONENT: TOP-DOWN GLOSSY GRADIENT
// FIXED: Colorful at top, Dark at bottom. NO spinning[cite: 3].
// ==========================================
@Composable
fun KineticBackground(accentColor: Color, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "glossy_wash")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(40000, easing = LinearEasing)), label = "drift"
    )
    val targetBgColor = if (isDark) Color(0xFF010103) else Color(0xFF080600)
    val animatedBgColor by animateColorAsState(targetValue = targetBgColor, animationSpec = tween(1000), label = "bgColor")

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = animatedBgColor)

        // Main Static Vertical Gradient (Colorful Top, Dark Bottom)[cite: 3]
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.6f
            )
        )

        // Subtle drifting "light washes" within the top area (No rotation)[cite: 3]
        val horizontalOffset = sin(drift.toDouble()).toFloat() * size.width * 0.15f
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = 0.1f), Color.Transparent),
                center = Offset(size.width * 0.5f + horizontalOffset, size.height * 0.1f),
                radius = size.width * 1.2f
            )
        )
    }
}

// ==========================================
// MASTER VAULT SCREEN CORE
// ==========================================
@Composable
fun MasterVaultScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoToInterview: () -> Unit = {},
    onGoToLiveVoice: () -> Unit = {},
    onGoToHistory: () -> Unit = {},
    onGoToStandings: () -> Unit = {},
    onGoToAssistant: () -> Unit = {},
    userProfileStore: UserProfileStore
) {
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    val surfaceCyan = Color(0xFF22D3EE)
    val vibrantGold = Color(0xFFFFD700)
    val accentColor = if (isDark) surfaceCyan else vibrantGold
    val animatedAccent by animateColorAsState(targetValue = accentColor, animationSpec = tween(1000), label = "accent")

    // JSON Persistence[cite: 1]
    val vaultProjects: List<VaultProject> = remember(userProfile.savedProjectsJson) {
        try { gson.fromJson(userProfile.savedProjectsJson, object : TypeToken<List<VaultProject>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
    val vaultExperience: List<VaultExperience> = remember(userProfile.savedExperienceJson) {
        try { gson.fromJson(userProfile.savedExperienceJson, object : TypeToken<List<VaultExperience>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
    val vaultSkills: List<String> = remember(userProfile.savedSkillsJson) {
        try { gson.fromJson(userProfile.savedSkillsJson, object : TypeToken<List<String>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
    var vaultEducation: List<VaultEducation> = remember(userProfile.savedEducationJson) {
        try { gson.fromJson(userProfile.savedEducationJson, object : TypeToken<List<VaultEducation>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
    var vaultCertifications: List<VaultCertification> = remember(userProfile.savedCertificationsJson) {
        try { gson.fromJson(userProfile.savedCertificationsJson, object : TypeToken<List<VaultCertification>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
    var vaultAchievements: List<VaultAchievement> = remember(userProfile.savedAchievementsJson) {
        try { gson.fromJson(userProfile.savedAchievementsJson, object : TypeToken<List<VaultAchievement>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    // Dialog & UI States
    var editingProjectIndex by remember { mutableIntStateOf(-1) }
    var editingExpIndex by remember { mutableIntStateOf(-1) }
    var editingEduIndex by remember { mutableIntStateOf(-1) }
    var editingCertIndex by remember { mutableIntStateOf(-1) }
    var editingAchIndex by remember { mutableIntStateOf(-1) }
    var showSkillDialog by remember { mutableStateOf(false) }

    var tempProject by remember { mutableStateOf(VaultProject()) }
    var tempExperience by remember { mutableStateOf(VaultExperience()) }
    var tempEducation by remember { mutableStateOf(VaultEducation()) }
    var tempCert by remember { mutableStateOf(VaultCertification()) }
    var tempAch by remember { mutableStateOf(VaultAchievement()) }
    var tempSkill by remember { mutableStateOf("") }

    var expandedSection by remember { mutableStateOf<String?>("Skills") }
    fun toggleSection(section: String) { expandedSection = if (expandedSection == section) null else section }

    Box(modifier = Modifier.fillMaxSize()) {
        KineticBackground(accentColor = animatedAccent, isDark = isDark)

        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(modifier = Modifier.fillMaxWidth().padding(end = 76.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Master Vault", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable { onGoToAssistant() },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.5f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = animatedAccent, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        BouncyVaultCard(title = "History", icon = Icons.Default.History, accentColor = animatedAccent, onClick = onGoToHistory, modifier = Modifier.weight(1f))
                        BouncyVaultCard(title = "Standings", icon = Icons.Default.Assessment, accentColor = animatedAccent, onClick = onGoToStandings, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        VaultServiceCard(title = "AI Mock Interview", desc = "Adaptive high-pressure simulation.", icon = Icons.Filled.Mic, accent = animatedAccent, onClick = onGoToInterview)
                        VaultServiceCard(title = "Live Voice Round", desc = "Real-time conversational agility.", icon = Icons.Filled.RecordVoiceOver, accent = animatedAccent, onClick = onGoToLiveVoice)
                    }
                }

                // --- ASSET SECTIONS WITH UNIFORM SCROLLING[cite: 1] ---
                item {
                    ExpandableVaultSection(title = "Skills", icon = Icons.Filled.Star, count = vaultSkills.size, expanded = expandedSection == "Skills", accent = animatedAccent, onToggle = { toggleSection("Skills") }, onAdd = { showSkillDialog = true }) {
                        if (vaultSkills.isEmpty()) AssistanceCTA(section = "Skills", onGo = onGoToAssistant, accent = animatedAccent)
                        else VaultScrollContainer(maxHeight = 320, itemCount = vaultSkills.size, threshold = 10) {
                            Column {
                                vaultSkills.chunked(2).forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { skill ->
                                            Surface(shape = RoundedCornerShape(18.dp), color = animatedAccent.copy(alpha = 0.1f), border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.1f)), modifier = Modifier.weight(1f)) {
                                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = skill, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    IconButton(onClick = { 
                                                        val index = vaultSkills.indexOf(skill)
                                                        if (index != -1) {
                                                            val newList = vaultSkills.toMutableList().apply { removeAt(index) }
                                                            scope.launch { 
                                                                userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(newList))) 
                                                                val result = snackbarHostState.showSnackbar("Item deleted", actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                    val restoredList = newList.toMutableList().apply { add(index, skill) }
                                                                    userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(restoredList)))
                                                                }
                                                            }
                                                        }
                                                    }, modifier = Modifier.size(20.dp)) { Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Gray) }
                                                }
                                            }
                                        }
                                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ExpandableVaultSection(title = "Projects", icon = Icons.Filled.Work, count = vaultProjects.size, expanded = expandedSection == "Projects", accent = animatedAccent, onToggle = { toggleSection("Projects") }, onAdd = { editingProjectIndex = -2; tempProject = VaultProject() }) {
                        if (vaultProjects.isEmpty()) AssistanceCTA(section = "Projects", onGo = onGoToAssistant, accent = animatedAccent)
                        else VaultScrollContainer(maxHeight = 320, itemCount = vaultProjects.size, threshold = 2) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                vaultProjects.forEachIndexed { idx, proj ->
                                    VaultAssetCard(title = proj.name, subtitle = formatDates(proj.startMonth, proj.startYear, proj.endMonth, proj.endYear, proj.isPresent), bullets = proj.bullets, accent = animatedAccent, onEdit = { editingProjectIndex = idx; tempProject = proj }, onDelete = {
                                        val newList = vaultProjects.toMutableList().apply { removeAt(idx) }
                                        scope.launch {
                                            userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(newList)))
                                            val result = snackbarHostState.showSnackbar("Item deleted", actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                            if (result == SnackbarResult.ActionPerformed) {
                                                val restoredList = newList.toMutableList().apply { add(idx, proj) }
                                                userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(restoredList)))
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }
                }

                item {
                    ExpandableVaultSection(title = "Experience", icon = Icons.Filled.Business, count = vaultExperience.size, expanded = expandedSection == "Experience", accent = animatedAccent, onToggle = { toggleSection("Experience") }, onAdd = { editingExpIndex = -2; tempExperience = VaultExperience() }) {
                        if (vaultExperience.isEmpty()) AssistanceCTA(section = "Experience", onGo = onGoToAssistant, accent = animatedAccent)
                        else VaultScrollContainer(maxHeight = 320, itemCount = vaultExperience.size, threshold = 2) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                vaultExperience.forEachIndexed { idx, exp ->
                                    VaultAssetCard(title = exp.company, subtitle = "${exp.role}" + (if (exp.role.isNotBlank() && formatDates(exp.startMonth, exp.startYear, exp.endMonth, exp.endYear, exp.isPresent).isNotBlank()) " | " else "") + formatDates(exp.startMonth, exp.startYear, exp.endMonth, exp.endYear, exp.isPresent), bullets = exp.bullets, accent = animatedAccent, onEdit = { editingExpIndex = idx; tempExperience = exp }, onDelete = {
                                        val newList = vaultExperience.toMutableList().apply { removeAt(idx) }
                                        scope.launch {
                                            userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(newList)))
                                            val result = snackbarHostState.showSnackbar("Item deleted", actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                            if (result == SnackbarResult.ActionPerformed) {
                                                val restoredList = newList.toMutableList().apply { add(idx, exp) }
                                                userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(restoredList)))
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }
                }

                item {
                    ExpandableVaultSection(title = "Achievements", icon = Icons.Filled.EmojiEvents, count = vaultAchievements.size, expanded = expandedSection == "Achievements", accent = animatedAccent, onToggle = { toggleSection("Achievements") }, onAdd = { editingAchIndex = -2; tempAch = VaultAchievement() }) {
                        if (vaultAchievements.isEmpty()) AssistanceCTA(section = "Achievements", onGo = onGoToAssistant, accent = animatedAccent)
                        else VaultScrollContainer(maxHeight = 320, itemCount = vaultAchievements.size, threshold = 2) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                vaultAchievements.forEachIndexed { i, ach -> VaultAssetCard(title = ach.title, subtitle = ach.issuer, bullets = ach.description, accent = animatedAccent, onEdit = { editingAchIndex = i; tempAch = ach }, onDelete = {
                                    val newList = vaultAchievements.toMutableList().apply { removeAt(i) }
                                    scope.launch {
                                        userProfileStore.saveUserProfile(userProfile.copy(savedAchievementsJson = gson.toJson(newList)))
                                        val result = snackbarHostState.showSnackbar("Item deleted", actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                        if (result == SnackbarResult.ActionPerformed) {
                                            val restoredList = newList.toMutableList().apply { add(i, ach) }
                                            userProfileStore.saveUserProfile(userProfile.copy(savedAchievementsJson = gson.toJson(restoredList)))
                                        }
                                    }
                                }) }
                            }
                        }
                    }
                }

                item {
                    ExpandableVaultSection(title = "Education", icon = Icons.Filled.School, count = vaultEducation.size, expanded = expandedSection == "Education", accent = animatedAccent, onToggle = { toggleSection("Education") }, onAdd = { editingEduIndex = -2; tempEducation = VaultEducation() }) {
                        if (vaultEducation.isEmpty()) AssistanceCTA(section = "Education", onGo = onGoToAssistant, accent = animatedAccent)
                        else VaultScrollContainer(maxHeight = 320, itemCount = vaultEducation.size, threshold = 2) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                vaultEducation.forEachIndexed { i, edu -> VaultAssetCard(title = edu.school, subtitle = "${edu.degree} (${edu.year})", bullets = "", accent = animatedAccent, onEdit = { editingEduIndex = i; tempEducation = edu }, onDelete = {
                                    val newList = vaultEducation.toMutableList().apply { removeAt(i) }
                                    scope.launch {
                                        userProfileStore.saveUserProfile(userProfile.copy(savedEducationJson = gson.toJson(newList)))
                                        val result = snackbarHostState.showSnackbar("Item deleted", actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                        if (result == SnackbarResult.ActionPerformed) {
                                            val restoredList = newList.toMutableList().apply { add(i, edu) }
                                            userProfileStore.saveUserProfile(userProfile.copy(savedEducationJson = gson.toJson(restoredList)))
                                        }
                                    }
                                }) }
                            }
                        }
                    }
                }

                item {
                    ExpandableVaultSection(title = "Certifications", icon = Icons.Filled.Verified, count = vaultCertifications.size, expanded = expandedSection == "Certifications", accent = animatedAccent, onToggle = { toggleSection("Certifications") }, onAdd = { editingCertIndex = -2; tempCert = VaultCertification() }) {
                        if (vaultCertifications.isEmpty()) AssistanceCTA(section = "Certifications", onGo = onGoToAssistant, accent = animatedAccent)
                        else VaultScrollContainer(maxHeight = 320, itemCount = vaultCertifications.size, threshold = 2) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                vaultCertifications.forEachIndexed { i, cert -> VaultAssetCard(title = cert.name, subtitle = "${cert.issuer} (${cert.year})", bullets = cert.summary, accent = animatedAccent, onEdit = { editingCertIndex = i; tempCert = cert }, onDelete = {
                                    val newList = vaultCertifications.toMutableList().apply { removeAt(i) }
                                    scope.launch {
                                        userProfileStore.saveUserProfile(userProfile.copy(savedCertificationsJson = gson.toJson(newList)))
                                        val result = snackbarHostState.showSnackbar("Item deleted", actionLabel = "UNDO", duration = SnackbarDuration.Short)
                                        if (result == SnackbarResult.ActionPerformed) {
                                            val restoredList = newList.toMutableList().apply { add(i, cert) }
                                            userProfileStore.saveUserProfile(userProfile.copy(savedCertificationsJson = gson.toJson(restoredList)))
                                        }
                                    }
                                }) }
                            }
                        }
                    }
                }
            }
        }

        // --- PULL SWITCH (RE-ENGINEERED BI-DIRECTIONAL)[cite: 1] ---
        LampPullChain(isDark = isDark, onToggleTheme = onToggleTheme, accentColor = animatedAccent)
    }

    // PREMIUM DIALOGS WITH VALIDATION[cite: 1]
    if (showSkillDialog) {
        PremiumVaultDialog(title = "Add Skill", accentColor = animatedAccent, onDismiss = { showSkillDialog = false }, onConfirm = {
            if (tempSkill.trim().isBlank()) Toast.makeText(context, "Skill name required.", Toast.LENGTH_SHORT).show()
            else { scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(vaultSkills + tempSkill.trim()))) }; showSkillDialog = false; tempSkill = "" }
        }) { PremiumTextField("Skill Name", tempSkill, animatedAccent) { tempSkill = it } }
    }

    if (editingProjectIndex != -1) {
        PremiumVaultDialog(title = "Project Detail", accentColor = animatedAccent, onDismiss = { editingProjectIndex = -1 }, onConfirm = {
            if (tempProject.name.isBlank()) Toast.makeText(context, "Project Name required.", Toast.LENGTH_SHORT).show()
            else { val finalProj = validateProjectDates(tempProject); val newList = if (editingProjectIndex == -2) vaultProjects + finalProj else vaultProjects.toMutableList().apply { this[editingProjectIndex] = finalProj }; scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(newList))); editingProjectIndex = -1 } }
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PremiumTextField("Project Name *", tempProject.name, animatedAccent) { tempProject = tempProject.copy(name = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSelection(getValidStartMonths(tempProject.startYear, tempProject.isPresent), tempProject.startMonth, animatedAccent, Modifier.weight(1f)) { tempProject = validateProjectDates(tempProject.copy(startMonth = it)) }
                    PremiumSelection(getValidStartYears(tempProject.isPresent), tempProject.startYear, animatedAccent, Modifier.weight(1f)) { tempProject = validateProjectDates(tempProject.copy(startYear = it)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSelection(getValidEndMonths(tempProject.startYear, tempProject.startMonth, tempProject.endYear), tempProject.endMonth, animatedAccent, Modifier.weight(1f), enabled = !tempProject.isPresent) { tempProject = validateProjectDates(tempProject.copy(endMonth = it)) }
                    PremiumSelection(getValidEndYears(tempProject.startYear), tempProject.endYear, animatedAccent, Modifier.weight(1f), enabled = !tempProject.isPresent) { tempProject = validateProjectDates(tempProject.copy(endYear = it)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tempProject.isPresent, onCheckedChange = { tempProject = validateProjectDates(tempProject.copy(isPresent = it)) })
                    Text("Present", color = Color.White, maxLines = 1, softWrap = false)
                }
                PremiumTextField("Detailed Bullets", tempProject.bullets, animatedAccent, Modifier.height(140.dp)) { tempProject = tempProject.copy(bullets = it) }
            }
        }
    }

    if (editingAchIndex != -1) {
        PremiumVaultDialog(title = "Achievement Detail", animatedAccent, { editingAchIndex = -1 }, {
            if (tempAch.title.isBlank()) Toast.makeText(context, "Achievement Title required.", Toast.LENGTH_SHORT).show()
            else { val newList = if (editingAchIndex == -2) vaultAchievements + tempAch else vaultAchievements.toMutableList().apply { this[editingAchIndex] = tempAch }; scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedAchievementsJson = gson.toJson(newList))); editingAchIndex = -1 } }
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PremiumTextField("Achievement Title *", tempAch.title, animatedAccent) { tempAch = tempAch.copy(title = it) }
                PremiumTextField("Description", tempAch.description, animatedAccent, Modifier.height(120.dp)) { tempAch = tempAch.copy(description = it) }
            }
        }
    }

    if (editingExpIndex != -1) {
        PremiumVaultDialog(title = "Work Experience", animatedAccent, { editingExpIndex = -1 }, {
            if (tempExperience.company.isBlank()) Toast.makeText(context, "Organization required.", Toast.LENGTH_SHORT).show()
            else { val finalExp = validateExperienceDates(tempExperience); val newList = if (editingExpIndex == -2) vaultExperience + finalExp else vaultExperience.toMutableList().apply { this[editingExpIndex] = finalExp }; scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(newList))); editingExpIndex = -1 } }
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PremiumTextField("Organization *", tempExperience.company, animatedAccent) { tempExperience = tempExperience.copy(company = it) }
                PremiumTextField("Role", tempExperience.role, animatedAccent) { tempExperience = tempExperience.copy(role = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSelection(getValidStartMonths(tempExperience.startYear, tempExperience.isPresent), tempExperience.startMonth, animatedAccent, Modifier.weight(1f)) { tempExperience = validateExperienceDates(tempExperience.copy(startMonth = it)) }
                    PremiumSelection(getValidStartYears(tempExperience.isPresent), tempExperience.startYear, animatedAccent, Modifier.weight(1f)) { tempExperience = validateExperienceDates(tempExperience.copy(startYear = it)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSelection(getValidEndMonths(tempExperience.startYear, tempExperience.startMonth, tempExperience.endYear), tempExperience.endMonth, animatedAccent, Modifier.weight(1f), enabled = !tempExperience.isPresent) { tempExperience = validateExperienceDates(tempExperience.copy(endMonth = it)) }
                    PremiumSelection(getValidEndYears(tempExperience.startYear), tempExperience.endYear, animatedAccent, Modifier.weight(1f), enabled = !tempExperience.isPresent) { tempExperience = validateExperienceDates(tempExperience.copy(endYear = it)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tempExperience.isPresent, onCheckedChange = { tempExperience = validateExperienceDates(tempExperience.copy(isPresent = it)) })
                    Text("Present", color = Color.White, maxLines = 1, softWrap = false)
                }
                PremiumTextField("Detailed Bullets", tempExperience.bullets, animatedAccent, Modifier.height(140.dp)) { tempExperience = tempExperience.copy(bullets = it) }
            }
        }
    }

    if (editingEduIndex != -1) {
        PremiumVaultDialog(title = "Education Detail", accentColor = animatedAccent, onDismiss = { editingEduIndex = -1 }, onConfirm = {
            if (tempEducation.school.isBlank()) Toast.makeText(context, "School name required.", Toast.LENGTH_SHORT).show()
            else { val newList = if (editingEduIndex == -2) vaultEducation + tempEducation else vaultEducation.toMutableList().apply { this[editingEduIndex] = tempEducation }; scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedEducationJson = gson.toJson(newList))); editingEduIndex = -1 } }
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PremiumTextField("School Name *", tempEducation.school, animatedAccent) { tempEducation = tempEducation.copy(school = it) }
                PremiumTextField("Degree", tempEducation.degree, animatedAccent) { tempEducation = tempEducation.copy(degree = it) }
                PremiumSelection(YEAR_OPTIONS, tempEducation.year, animatedAccent) { tempEducation = tempEducation.copy(year = it) }
            }
        }
    }

    if (editingCertIndex != -1) {
        PremiumVaultDialog(title = "Certification Detail", accentColor = animatedAccent, onDismiss = { editingCertIndex = -1 }, onConfirm = {
            if (tempCert.name.isBlank()) Toast.makeText(context, "Certification Name required.", Toast.LENGTH_SHORT).show()
            else { val newList = if (editingCertIndex == -2) vaultCertifications + tempCert else vaultCertifications.toMutableList().apply { this[editingCertIndex] = tempCert }; scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedCertificationsJson = gson.toJson(newList))); editingCertIndex = -1 } }
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PremiumTextField("Certification Name *", tempCert.name, animatedAccent) { tempCert = tempCert.copy(name = it) }
                PremiumTextField("Issuer", tempCert.issuer, animatedAccent) { tempCert = tempCert.copy(issuer = it) }
                PremiumSelection(YEAR_OPTIONS, tempCert.year, animatedAccent) { tempCert = tempCert.copy(year = it) }
                PremiumTextField("Summary", tempCert.summary, animatedAccent) { tempCert = tempCert.copy(summary = it) }
            }
        }
    }
}

// ==========================================
// COMPONENT: PHYSICS PULL CHAIN (STABILIZED)
// FIXED: Bi-directional toggle never sticks; Reset logic on end[cite: 1].
// ==========================================
@Composable
private fun LampPullChain(isDark: Boolean, onToggleTheme: (Boolean) -> Unit, accentColor: Color) {
    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(initialValue = Offset.Zero, typeConverter = Offset.VectorConverter) }
    var hasToggledInThisDrag by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    // Stable references for toggle logic[cite: 1]
    val currentIsDark by rememberUpdatedState(isDark)
    val toggleAction by rememberUpdatedState(onToggleTheme)

    val anchorX = with(density) { (LocalConfiguration.current.screenWidthDp.dp - 40.dp).toPx() }
    val anchorY = with(density) { (-15.dp).toPx() }
    val restLength = with(density) { 130.dp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(color = Color.White.copy(alpha = 0.25f), start = Offset(anchorX, anchorY), end = Offset(anchorX + pullOffset.value.x, anchorY + restLength + pullOffset.value.y), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .offset { IntOffset((anchorX + pullOffset.value.x - 26.dp.toPx()).roundToInt(), (anchorY + restLength + pullOffset.value.y - 26.dp.toPx()).roundToInt()) }
                .size(52.dp)
                .pointerInput(Unit) { // Stable Key
                    detectDragGestures(
                        onDragEnd = {
                            hasToggledInThisDrag = false // Essential: Reset lock on release[cite: 1]
                            coroutineScope.launch { pullOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
                        },
                        onDragCancel = { hasToggledInThisDrag = false; coroutineScope.launch { pullOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) } },
                        onDrag = { _, amt ->
                            coroutineScope.launch {
                                val next = pullOffset.value + amt
                                pullOffset.snapTo(next)
                                // Snappy bi-directional response[cite: 1]
                                if (next.y > 75f && !hasToggledInThisDrag) {
                                    toggleAction(!currentIsDark)
                                    hasToggledInThisDrag = true
                                }
                            }
                        }
                    )
                },
            shape = CircleShape, color = Color(0xFF1A1A1A), border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)), shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.WbSunny, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// --- UTILITY COMPONENTS ---

@Composable
fun VaultScrollContainer(maxHeight: Int, itemCount: Int, threshold: Int, content: @Composable () -> Unit) {
    Box(modifier = if (itemCount > threshold) Modifier.heightIn(max = maxHeight.dp).verticalScroll(rememberScrollState()) else Modifier) { content() }
}

@Composable
fun BouncyVaultCard(title: String, icon: ImageVector, accentColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "scale")
    Card(modifier = modifier.height(115.dp).graphicsLayer { scaleX = scale; scaleY = scale }.pointerInput(Unit) { detectTapGestures(onPress = { isPressed = true; try { awaitRelease() } finally { isPressed = false } }, onTap = { onClick() }) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF151515).copy(alpha = 0.85f)), border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)), shape = RoundedCornerShape(28.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawRect(brush = Brush.radialGradient(listOf(accentColor.copy(alpha = 0.1f), Color.Transparent), center = Offset(size.width * 0.2f, 0f), radius = size.width)) }
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Box(modifier = Modifier.size(46.dp).background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accentColor, modifier = Modifier.size(26.dp)) }; Spacer(Modifier.height(10.dp)); Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
fun ExpandableVaultSection(title: String, icon: ImageVector, count: Int, expanded: Boolean, accent: Color, onToggle: () -> Unit, onAdd: () -> Unit, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)), border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)), shape = RoundedCornerShape(28.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(14.dp)); Text("$title ($count)", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (expanded) IconButton(onClick = onAdd) { Icon(Icons.Default.AddCircleOutline, null, tint = accent) }
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Color.Gray)
            }
            if (expanded) Box(modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 22.dp)) { content() }
        }
    }
}

@Composable
fun VaultAssetCard(title: String, subtitle: String, bullets: String, accent: Color, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)), border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(title, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit", tint = accent, modifier = Modifier.size(20.dp)) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(20.dp)) } }
            Text(subtitle, color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            if (bullets.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(bullets, color = Color.LightGray.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
fun PremiumVaultDialog(title: String, accentColor: Color, onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { Surface(modifier = Modifier.fillMaxWidth(0.95f).wrapContentHeight(), shape = RoundedCornerShape(32.dp), color = Color(0xFF121212), border = BorderStroke(2.dp, accentColor.copy(alpha = 0.4f))) { Column(modifier = Modifier.padding(28.dp)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White); Spacer(Modifier.height(24.dp)); Box(modifier = Modifier.weight(1f, fill = false)) { content() }; Spacer(Modifier.height(28.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onDismiss) { Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Button(onConfirm, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(14.dp)) { Text("SAVE DATA", color = Color.Black, fontWeight = FontWeight.ExtraBold) } } } } }
}

@Composable
fun PremiumTextField(label: String, value: String, accent: Color, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = Color.White.copy(alpha = 0.1f), focusedLabelColor = accent, cursorColor = accent))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSelection(
    options: List<String>,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChange: (String) -> Unit
) {
    var exp by remember { mutableStateOf(false) }
    val isEnabled = enabled
    
    val displayValue = if (value != "Not set" && value != "Year" && value.length > 3 && MONTH_OPTIONS.contains(value)) {
        value.take(3).uppercase()
    } else {
        value
    }

    ExposedDropdownMenuBox(
        expanded = if (isEnabled) exp else false,
        onExpandedChange = { if (isEnabled) exp = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            enabled = isEnabled,
            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = isEnabled).fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            trailingIcon = { if (isEnabled) ExposedDropdownMenuDefaults.TrailingIcon(exp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = if (isEnabled) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                disabledBorderColor = Color.White.copy(alpha = 0.05f),
                disabledTextColor = Color.Gray,
                cursorColor = accent
            )
        )
        if (isEnabled) {
            ExposedDropdownMenu(
                expanded = exp,
                onDismissRequest = { exp = false },
                modifier = Modifier.background(Color(0xFF1A1A1A))
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, color = Color.White) },
                        onClick = { onValueChange(opt); exp = false }
                    )
                }
            }
        }
    }
}

@Composable
fun VaultServiceCard(title: String, desc: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)), border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))) {
        Row(modifier = Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(54.dp).background(accent.copy(alpha = 0.15f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp)) }; Spacer(Modifier.width(18.dp)); Column { Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold); Text(desc, color = Color.Gray, style = MaterialTheme.typography.bodySmall) } }
    }
}

@Composable
fun AssistanceCTA(section: String, onGo: () -> Unit, accent: Color) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onGo() }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)), border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)), shape = RoundedCornerShape(20.dp)) { Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = accent, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(14.dp)); Text("Enhance your $section profile with AI insights. Tap to launch Assistant.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray) } }
}