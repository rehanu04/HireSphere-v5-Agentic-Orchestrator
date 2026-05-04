@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * Features: High-fidelity Nebula Background, Physics-based Pull Switch, 
 * and Durable Asset Management.
 * 
 * DESIGN SPEC: Orbit Orange Toggle & Galactic Cyan Accents.
 */

// --- DATA MODELS ---
data class VaultProject(
    val name: String = "", 
    val startMonth: String = "Not set", 
    val startYear: String = "Not set", 
    val endMonth: String = "Not set", 
    val endYear: String = "Not set", 
    val bullets: String = ""
)

data class VaultExperience(
    val company: String = "", 
    val role: String = "", 
    val startMonth: String = "Not set", 
    val startYear: String = "Not set", 
    val endMonth: String = "Not set", 
    val endYear: String = "Not set", 
    val bullets: String = ""
)

data class VaultEducation(
    val school: String = "", 
    val degree: String = "", 
    val year: String = "Year"
)

data class VaultCertification(
    val name: String = "", 
    val issuer: String = "", 
    val year: String = "Year"
)

private val MONTH_OPTIONS = listOf("Not set", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val YEAR_OPTIONS = listOf("Not set") + (Calendar.getInstance().get(Calendar.YEAR) + 2 downTo 1980).map { it.toString() }

// ==========================================
// COMPONENT: DYNAMIC NEBULA CLOUD
// ==========================================
@Composable
fun KineticBackground(accentColor: Color, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "nebula_system")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 35000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "phase"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 6000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse"
    )

    val bgColor = if (isDark) Color(0xFF010103) else Color(0xFF06020A)

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = bgColor)

        // --- Core Soft Breathing Nebula Aura ---
        drawNebulaCloud(accentColor.copy(alpha = 0.15f), phase, pulse, 0.5f, 0.4f, 1.4f)
        
        // --- Dispersing Nebula Wisps ---
        drawNebulaCloud(accentColor.copy(alpha = 0.08f), phase * 0.7f, pulse * 0.9f, 0.3f, 0.7f, 0.9f)
        drawNebulaCloud(accentColor.copy(alpha = 0.05f), phase * 1.2f, pulse * 1.1f, 0.7f, 0.2f, 1.1f)
        
        // --- Drift Layers (Luxury Parallax) ---
        drawNebulaCloud(accentColor.copy(alpha = 0.03f), phase * 0.4f, 1f, 0.2f, 0.1f, 1.6f)
    }
}

private fun DrawScope.drawNebulaCloud(color: Color, phase: Float, pulse: Float, anchorX: Float, anchorY: Float, radiusFactor: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = Offset(
                x = size.width * (anchorX + 0.15f * sin(phase.toDouble()).toFloat()),
                y = size.height * (anchorY + 0.1f * cos(phase.toDouble()).toFloat())
            ),
            radius = size.width * radiusFactor * pulse
        )
    )
}

// ==========================================
// COMPONENT: GLOSSY BOUNCY CARD
// ==========================================
@Composable
fun BouncyVaultCard(
    title: String, icon: ImageVector, accentColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "scale")

    Card(
        modifier = modifier
            .height(height = 115.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(key1 = Unit) {
                detectTapGestures(
                    onPress = { isPressed = true; try { awaitRelease() } finally { isPressed = false } },
                    onTap = { onClick() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515).copy(alpha = 0.85f)),
        border = BorderStroke(width = 1.dp, color = accentColor.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(size = 28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.1f), Color.Transparent),
                        center = Offset(x = size.width * 0.2f, y = 0f), radius = size.width
                    )
                )
            }
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(size = 46.dp).background(color = accentColor.copy(alpha = 0.2f), shape = RoundedCornerShape(size = 14.dp)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(size = 26.dp))
                }
                Spacer(modifier = Modifier.height(height = 10.dp))
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
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
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    // THEME: Galactic Cyan (Dark) vs Orbit Orange (Light/Alt)
    val galacticCyan = Color(0xFF22D3EE)
    val orbitOrange = Color(0xFFFF9F0A)
    val accentColor = if (isDark) galacticCyan else orbitOrange
    val animatedAccent by animateColorAsState(targetValue = accentColor, animationSpec = tween(durationMillis = 1200))

    // --- JSON Persistence Layers ---
    val vaultProjects: List<VaultProject> = remember(userProfile.savedProjectsJson) {
        try { gson.fromJson(userProfile.savedProjectsJson, object : TypeToken<List<VaultProject>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
    val vaultExperience: List<VaultExperience> = remember(userProfile.savedExperienceJson) {
        try { gson.fromJson(userProfile.savedExperienceJson, object : TypeToken<List<VaultExperience>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
    val vaultSkills: List<String> = remember(userProfile.savedSkillsJson) {
        try { gson.fromJson(userProfile.savedSkillsJson, object : TypeToken<List<String>>() {}.type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }
    // --- Internal Asset Management (Bypassing Missing Fields) ---
    var vaultEducation by remember { mutableStateOf(emptyList<VaultEducation>()) }
    var vaultCertifications by remember { mutableStateOf(emptyList<VaultCertification>()) }

    // --- UI State Management ---
    var editingProjectIndex by remember { mutableIntStateOf(-1) }
    var editingExpIndex by remember { mutableIntStateOf(-1) }
    var editingEduIndex by remember { mutableIntStateOf(-1) }
    var editingCertIndex by remember { mutableIntStateOf(-1) }
    var showSkillDialog by remember { mutableStateOf(false) }

    var tempProject by remember { mutableStateOf(VaultProject()) }
    var tempExperience by remember { mutableStateOf(VaultExperience()) }
    var tempEducation by remember { mutableStateOf(VaultEducation()) }
    var tempCert by remember { mutableStateOf(VaultCertification()) }
    var tempSkill by remember { mutableStateOf("") }

    var expandedSkills by remember { mutableStateOf(true) }
    var expandedProjects by remember { mutableStateOf(false) }
    var expandedExp by remember { mutableStateOf(false) }
    var expandedEdu by remember { mutableStateOf(false) }
    var expandedCerts by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        KineticBackground(accentColor = animatedAccent, isDark = isDark)

        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(text = "Master Vault", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White) },
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                            IconButton(onClick = { /* Sync Logic */ }) { Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Cloud Sync", tint = animatedAccent) }
                        }
                    },
                    actions = {
                        IconButton(onClick = onGoToAssistant) { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = animatedAccent) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues = innerPadding).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 20.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(space = 14.dp)) {
                            BouncyVaultCard(title = "History", icon = Icons.Default.History, accentColor = animatedAccent, onClick = onGoToHistory, modifier = Modifier.weight(weight = 1f))
                            BouncyVaultCard(title = "Standings", icon = Icons.Default.Assessment, accentColor = animatedAccent, onClick = onGoToStandings, modifier = Modifier.weight(weight = 1f))
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            VaultServiceCard(title = "AI Mock Interview", desc = "Adaptive high-pressure simulation.", icon = Icons.Filled.Mic, accent = animatedAccent, onClick = onGoToInterview)
                            VaultServiceCard(title = "Live Voice Round", desc = "Real-time conversational agility.", icon = Icons.Filled.RecordVoiceOver, accent = animatedAccent, onClick = onGoToLiveVoice)
                        }
                    }

                    // --- ASSET SECTIONS ---
                    item {
                        ExpandableVaultSection(title = "Skills", icon = Icons.Filled.Star, count = vaultSkills.size, expanded = expandedSkills, accent = animatedAccent, onToggle = { expandedSkills = !expandedSkills }, onAdd = { showSkillDialog = true }) {
                            if (vaultSkills.isEmpty()) AssistanceCTA(section = "Skills", onGo = onGoToAssistant, accent = animatedAccent)
                            else VaultScrollContainer(maxHeight = 280, itemCount = vaultSkills.size, threshold = 15) {
                                Column(modifier = Modifier) {
                                    vaultSkills.chunked(size = 2).forEach { row ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                                            row.forEach { skill ->
                                                Surface(shape = RoundedCornerShape(size = 18.dp), color = animatedAccent.copy(alpha = 0.1f), border = BorderStroke(width = 1.dp, color = animatedAccent.copy(alpha = 0.1f)), modifier = Modifier.weight(weight = 1f)) {
                                                    Row(modifier = Modifier.padding(all = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(text = skill, color = Color.White, modifier = Modifier.weight(weight = 1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        IconButton(onClick = { /* Delete Logic */ }, modifier = Modifier.size(20.dp)) { Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Gray) }
                                                    }
                                                }
                                            }
                                            if (row.size == 1) Spacer(modifier = Modifier.weight(weight = 1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        ExpandableVaultSection(title = "Projects", icon = Icons.Filled.Work, count = vaultProjects.size, expanded = expandedProjects, accent = animatedAccent, onToggle = { expandedProjects = !expandedProjects }, onAdd = { editingProjectIndex = -2; tempProject = VaultProject() }) {
                            if (vaultProjects.isEmpty()) AssistanceCTA(section = "Projects", onGo = onGoToAssistant, accent = animatedAccent)
                            else Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                                vaultProjects.forEachIndexed { idx, proj ->
                                    VaultAssetCard(title = proj.name, subtitle = "${proj.startMonth} ${proj.startYear} - ${proj.endMonth} ${proj.endYear}", bullets = proj.bullets, accent = animatedAccent, onEdit = { editingProjectIndex = idx; tempProject = proj }, onDelete = {})
                                }
                            }
                        }
                    }

                    item {
                        ExpandableVaultSection(title = "Experience", icon = Icons.Filled.Business, count = vaultExperience.size, expanded = expandedExp, accent = animatedAccent, onToggle = { expandedExp = !expandedExp }, onAdd = { editingExpIndex = -2; tempExperience = VaultExperience() }) {
                            if (vaultExperience.isEmpty()) AssistanceCTA(section = "Experience", onGo = onGoToAssistant, accent = animatedAccent)
                            else Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                                vaultExperience.forEachIndexed { idx, exp ->
                                    VaultAssetCard(title = exp.company, subtitle = "${exp.role} | ${exp.startMonth} - ${exp.endMonth}", bullets = exp.bullets, accent = animatedAccent, onEdit = { editingExpIndex = idx; tempExperience = exp }, onDelete = {})
                                }
                            }
                        }
                    }

                    item {
                        ExpandableVaultSection(title = "Education", icon = Icons.Filled.School, count = vaultEducation.size, expanded = expandedEdu, accent = animatedAccent, onToggle = { expandedEdu = !expandedEdu }, onAdd = { editingEduIndex = -2; tempEducation = VaultEducation() }) {
                            if (vaultEducation.isEmpty()) AssistanceCTA(section = "Education", onGo = onGoToAssistant, accent = animatedAccent)
                            else Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                                vaultEducation.forEachIndexed { i, edu -> VaultAssetCard(title = edu.school, subtitle = "${edu.degree} (${edu.year})", bullets = "", accent = animatedAccent, onEdit = { editingEduIndex = i; tempEducation = edu }, onDelete = {}) }
                            }
                        }
                    }

                    item {
                        ExpandableVaultSection(title = "Certifications", icon = Icons.Filled.Verified, count = vaultCertifications.size, expanded = expandedCerts, accent = animatedAccent, onToggle = { expandedCerts = !expandedCerts }, onAdd = { editingCertIndex = -2; tempCert = VaultCertification() }) {
                            if (vaultCertifications.isEmpty()) AssistanceCTA(section = "Certifications", onGo = onGoToAssistant, accent = animatedAccent)
                            else Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                                vaultCertifications.forEachIndexed { i, cert -> VaultAssetCard(title = cert.name, subtitle = "${cert.issuer} (${cert.year})", bullets = "", accent = animatedAccent, onEdit = { editingCertIndex = i; tempCert = cert }, onDelete = {}) }
                            }
                        }
                    }
                }
        }

        // --- Celestial Pull Switch ---
        LampPullChain(isDark = isDark, onToggleTheme = onToggleTheme, accentColor = animatedAccent)
    }

    // ==========================================
    // PREMIUM DIALOGS (CRUD)
    // ==========================================

    if (showSkillDialog) {
        PremiumVaultDialog(title = "Add Professional Skill", accentColor = animatedAccent, onDismiss = { showSkillDialog = false }, onConfirm = {
            scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedSkillsJson = gson.toJson(vaultSkills + tempSkill))) }
            showSkillDialog = false; tempSkill = ""
        }) { PremiumTextField(label = "Skill Name (e.g. Kotlin, AWS)", value = tempSkill, accent = animatedAccent) { tempSkill = it } }
    }

    if (editingProjectIndex != -1) {
        PremiumVaultDialog(title = if (editingProjectIndex == -2) "New Project" else "Edit Project", accentColor = animatedAccent, onDismiss = { editingProjectIndex = -1 }, onConfirm = {
            val newList = if (editingProjectIndex == -2) vaultProjects + tempProject else vaultProjects.toMutableList().apply { this[editingProjectIndex] = tempProject }
            scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedProjectsJson = gson.toJson(newList))); editingProjectIndex = -1 }
        }) {
            Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(space = 14.dp)) {
                PremiumTextField(label = "Project Name *", value = tempProject.name, accent = animatedAccent) { tempProject = tempProject.copy(name = it) }
                Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    PremiumSelection(options = MONTH_OPTIONS, value = tempProject.startMonth, accent = animatedAccent, modifier = Modifier.weight(1f)) { tempProject = tempProject.copy(startMonth = it) }
                    PremiumSelection(options = YEAR_OPTIONS, value = tempProject.startYear, accent = animatedAccent, modifier = Modifier.weight(1f)) { tempProject = tempProject.copy(startYear = it) }
                }
                Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    PremiumSelection(options = MONTH_OPTIONS, value = tempProject.endMonth, accent = animatedAccent, modifier = Modifier.weight(1f)) { tempProject = tempProject.copy(endMonth = it) }
                    PremiumSelection(options = YEAR_OPTIONS, value = tempProject.endYear, accent = animatedAccent, modifier = Modifier.weight(1f)) { tempProject = tempProject.copy(endYear = it) }
                }
                PremiumTextField(label = "Detailed Bullets *", value = tempProject.bullets, accent = animatedAccent, modifier = Modifier.height(height = 160.dp)) { tempProject = tempProject.copy(bullets = it) }
            }
        }
    }

    if (editingExpIndex != -1) {
        PremiumVaultDialog(title = "Work Experience", accentColor = animatedAccent, onDismiss = { editingExpIndex = -1 }, onConfirm = {
            val newList = if (editingExpIndex == -2) vaultExperience + tempExperience else vaultExperience.toMutableList().apply { this[editingExpIndex] = tempExperience }
            scope.launch { userProfileStore.saveUserProfile(userProfile.copy(savedExperienceJson = gson.toJson(newList))); editingExpIndex = -1 }
        }) {
            Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(space = 14.dp)) {
                PremiumTextField(label = "Organization", value = tempExperience.company, accent = animatedAccent) { tempExperience = tempExperience.copy(company = it) }
                PremiumTextField(label = "Designation", value = tempExperience.role, accent = animatedAccent) { tempExperience = tempExperience.copy(role = it) }
                Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    PremiumSelection(options = MONTH_OPTIONS, value = tempExperience.startMonth, accent = animatedAccent, modifier = Modifier.weight(1f)) { tempExperience = tempExperience.copy(startMonth = it) }
                    PremiumSelection(options = YEAR_OPTIONS, value = tempExperience.startYear, accent = animatedAccent, modifier = Modifier.weight(1f)) { tempExperience = tempExperience.copy(startYear = it) }
                }
                PremiumTextField(label = "Accomplishments", value = tempExperience.bullets, accent = animatedAccent, modifier = Modifier.height(height = 140.dp)) { tempExperience = tempExperience.copy(bullets = it) }
            }
        }
    }

    if (editingEduIndex != -1) {
        PremiumVaultDialog(title = "Education Detail", animatedAccent, { editingEduIndex = -1 }, {
            vaultEducation = if (editingEduIndex == -2) vaultEducation + tempEducation else vaultEducation.toMutableList().apply { this[editingEduIndex] = tempEducation }
            editingEduIndex = -1
        }) {
            Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(space = 14.dp)) {
                PremiumTextField(label = "Institution / School", value = tempEducation.school, accent = animatedAccent) { tempEducation = tempEducation.copy(school = it) }
                PremiumTextField(label = "Degree / Certification", value = tempEducation.degree, accent = animatedAccent) { tempEducation = tempEducation.copy(degree = it) }
                PremiumSelection(options = YEAR_OPTIONS, value = tempEducation.year, accent = animatedAccent) { tempEducation = tempEducation.copy(year = it) }
            }
        }
    }

    if (editingCertIndex != -1) {
        PremiumVaultDialog(title = "Certification Detail", animatedAccent, { editingCertIndex = -1 }, {
            vaultCertifications = if (editingCertIndex == -2) vaultCertifications + tempCert else vaultCertifications.toMutableList().apply { this[editingCertIndex] = tempCert }
            editingCertIndex = -1
        }) {
            Column(modifier = Modifier, verticalArrangement = Arrangement.spacedBy(space = 14.dp)) {
                PremiumTextField(label = "Certificate Title", value = tempCert.name, accent = animatedAccent) { tempCert = tempCert.copy(name = it) }
                PremiumTextField(label = "Issuing Authority", value = tempCert.issuer, accent = animatedAccent) { tempCert = tempCert.copy(issuer = it) }
                PremiumSelection(options = YEAR_OPTIONS, value = tempCert.year, accent = animatedAccent) { tempCert = tempCert.copy(year = it) }
            }
        }
    }
}

// ==========================================
// COMPONENT: PHYSICS PULL CHAIN
// ==========================================
@Composable
private fun LampPullChain(isDark: Boolean, onToggleTheme: (Boolean) -> Unit, accentColor: Color) {
    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(initialValue = Offset.Zero, typeConverter = Offset.VectorConverter) }
    var hasToggled by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val anchorX = with(density) { (LocalConfiguration.current.screenWidthDp.dp - 40.dp).toPx() }
    val anchorY = with(density) { (-15.dp).toPx() }
    val restLength = with(density) { 130.dp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(color = Color.White.copy(alpha = 0.25f), start = Offset(x = anchorX, y = anchorY), end = Offset(x = anchorX + pullOffset.value.x, y = anchorY + restLength + pullOffset.value.y), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(x = (anchorX + pullOffset.value.x - 26.dp.toPx()).roundToInt(), y = (anchorY + restLength + pullOffset.value.y - 26.dp.toPx()).roundToInt()) }
                .size(size = 52.dp)
                .pointerInput(key1 = Unit) {
                    detectDragGestures(
                        onDragEnd = { 
                            hasToggled = false
                            coroutineScope.launch { pullOffset.animateTo(targetValue = Offset.Zero, animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow)) } 
                        },
                        onDrag = { _, amt ->
                            coroutineScope.launch {
                                val next = pullOffset.value + amt
                                pullOffset.snapTo(targetValue = next)
                                if (next.y > 180f && !hasToggled) { onToggleTheme(!isDark); hasToggled = true }
                            }
                        }
                    )
                },
            shape = CircleShape, color = Color(0xFF1A1A1A), border = BorderStroke(width = 1.5.dp, color = Color.White.copy(alpha = 0.15f)), shadowElevation = 10.dp
        ) {
            Box(modifier = Modifier, contentAlignment = Alignment.Center) {
                Icon(imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.WbSunny, contentDescription = null, tint = accentColor, modifier = Modifier.size(size = 28.dp))
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS & UTILITIES
// ==========================================
@Composable
fun ExpandableVaultSection(title: String, icon: ImageVector, count: Int, expanded: Boolean, accent: Color, onToggle: () -> Unit, onAdd: () -> Unit, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)), border = BorderStroke(width = 1.dp, color = accent.copy(alpha = 0.2f)), shape = RoundedCornerShape(size = 28.dp)) {
        Column(modifier = Modifier) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(all = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(width = 14.dp))
                Text(text = "$title ($count)", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(weight = 1f))
                if (expanded) IconButton(onClick = onAdd) { Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, tint = accent) }
                Icon(imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
            }
            if (expanded) Box(modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 22.dp)) { content() }
        }
    }
}

@Composable
fun VaultAssetCard(title: String, subtitle: String, bullets: String, accent: Color, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(size = 20.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)), border = BorderStroke(width = 1.dp, color = accent.copy(alpha = 0.25f))) {
        Column(modifier = Modifier.padding(all = 18.dp)) {
            Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = Color.White, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(weight = 1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onEdit) { Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = accent, modifier = Modifier.size(size = 20.dp)) }
                IconButton(onClick = onDelete) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(size = 20.dp)) }
            }
            Text(text = subtitle, color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            if (bullets.isNotBlank()) {
                Spacer(modifier = Modifier.height(height = 10.dp))
                Text(text = bullets, color = Color.LightGray.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun PremiumVaultDialog(title: String, accentColor: Color, onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth(fraction = 0.95f).wrapContentHeight(), shape = RoundedCornerShape(size = 32.dp), color = Color(0xFF121212), border = BorderStroke(width = 2.dp, color = accentColor.copy(alpha = 0.4f))) {
            Column(modifier = Modifier.padding(all = 28.dp)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(height = 24.dp))
                Box(modifier = Modifier.weight(weight = 1f, fill = false)) { content() }
                Spacer(modifier = Modifier.height(height = 28.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(text = "CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(width = 10.dp))
                    Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = accentColor), shape = RoundedCornerShape(size = 14.dp)) {
                        Text(text = "SAVE DATA", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumTextField(label: String, value: String, accent: Color, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, 
        onValueChange = onValueChange, 
        label = { Text(text = label) }, 
        modifier = modifier.fillMaxWidth(), 
        shape = RoundedCornerShape(size = 14.dp), 
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accent, 
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f), 
            focusedLabelColor = accent,
            cursorColor = accent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSelection(options: List<String>, value: String, accent: Color, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    var exp by remember { mutableStateOf(value = false) }
    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }, modifier = modifier) {
        OutlinedTextField(
            value = value, 
            onValueChange = {}, 
            readOnly = true, 
            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(), 
            shape = RoundedCornerShape(size = 14.dp), 
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) }, 
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, cursorColor = accent)
        )
        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }, modifier = Modifier.background(Color(0xFF1A1A1A))) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(text = opt, color = Color.White) }, onClick = { onValueChange(opt); exp = false })
            }
        }
    }
}

@Composable
fun VaultServiceCard(title: String, desc: String, icon: ImageVector, accent: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(size = 28.dp), colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.08f)), border = BorderStroke(width = 1.dp, color = accent.copy(alpha = 0.25f))) {
        Row(modifier = Modifier.padding(all = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(size = 54.dp).background(color = accent.copy(alpha = 0.15f), shape = RoundedCornerShape(size = 16.dp)), contentAlignment = Alignment.Center) { 
                Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp)) 
            }
            Spacer(modifier = Modifier.width(width = 18.dp))
            Column(modifier = Modifier) { 
                Text(text = title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(text = desc, color = Color.Gray, style = MaterialTheme.typography.bodySmall) 
            }
        }
    }
}

@Composable
fun VaultScrollContainer(maxHeight: Int, itemCount: Int, threshold: Int, content: @Composable () -> Unit) {
    Box(modifier = if (itemCount > threshold) Modifier.heightIn(max = maxHeight.dp).verticalScroll(state = rememberScrollState()) else Modifier) { content() }
}

@Composable
fun AssistanceCTA(section: String, onGo: () -> Unit, accent: Color) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onGo() }, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)), border = BorderStroke(width = 1.dp, color = accent.copy(alpha = 0.25f)), shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.padding(all = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(size = 24.dp))
            Spacer(modifier = Modifier.width(width = 14.dp))
            Text(text = "Enhance your $section profile with AI insights. Tap to launch Assistant.", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}