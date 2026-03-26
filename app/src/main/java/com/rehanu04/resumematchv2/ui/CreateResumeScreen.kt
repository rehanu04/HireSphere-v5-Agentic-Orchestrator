@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import androidx.compose.material.icons.filled.Person
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import java.util.concurrent.TimeUnit

// ---------------------------
// API & Models
// ---------------------------
private interface ResumeApi {
    @POST("/v1/resume/pdf")
    suspend fun createPdf(@Header("X-App-Key") apiKey: String, @Body body: ResumePdfRequest): ResponseBody
}

private fun createRetrofit(baseUrl: String): ResumeApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ResumeApi::class.java)
}

private enum class EditPanel { JOB, ABOUT, SKILLS, EXPERIENCE, PROJECTS, EDUCATION, CERTS, ACHIEVEMENTS, LINKS, TEMPLATE }
private data class TemplateOption(val id: String, val title: String, val subtitle: String)
private data class Links(val linkedin: String = "", val github: String = "", val portfolio: String = "", val other: String = "")
private data class ExperienceEntry(val company: String = "", val role: String = "", val location: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val tech: String = "", val bullets: String = "")
private data class ProjectEntry(val name: String = "", val tagline: String = "", val link: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val tech: String = "", val bullets: String = "")
private data class EducationEntry(val degree: String = "", val university: String = "", val location: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val gpa: String = "", val coursework: String = "")
private data class CertificationEntry(val name: String = "", val issuer: String = "", val issueMonth: String = "", val issueYear: String = "", val expiryMonth: String = "", val expiryYear: String = "", val url: String = "")
private data class AchievementEntry(val title: String = "", val detail: String = "")

private data class Draft(
    val templateId: String = "ats", val jdText: String = "", val firstName: String = "", val lastName: String = "",
    val targetRole: String = "", val location: String = "", val email: String = "", val phone: String = "",
    val summary: String = "", val profileImageB64: String = "", val profileImageName: String = "",
    val skillsLanguages: List<String> = emptyList(), val skillsFrameworks: List<String> = emptyList(),
    val skillsDatabases: List<String> = emptyList(), val skillsCloud: List<String> = emptyList(),
    val skillsAI: List<String> = emptyList(), val skillsTools: List<String> = emptyList(),
    val skillsOther: List<String> = emptyList(), val experience: List<ExperienceEntry> = emptyList(),
    val projects: List<ProjectEntry> = emptyList(), val education: List<EducationEntry> = emptyList(),
    val certs: List<CertificationEntry> = emptyList(), val achievements: List<AchievementEntry> = emptyList(),
    val links: Links = Links()
)

private data class ResumePdfRequest(
    val template: String, val jd_text: String, val first_name: String, val last_name: String,
    val target_role: String, val email: String, val phone: String, val location: String, val summary: String,
    val skills: List<String>, val experience_text: String, val projects_text: String,
    val education_text: String, val extras_text: String, val linkedin: String, val github: String,
    val portfolio: String, val profile_image_b64: String = ""
)

// ---------------------------
// Catalogs
// ---------------------------
private val ROLE_SUGGESTIONS = listOf("Software Engineer (Backend)", "Backend Engineer (Python/FastAPI)", "API Engineer (FastAPI)", "Python Backend Engineer", "Software Engineer – Platform", "AI Engineer (Backend)", "Machine Learning Engineer", "Android Engineer (Kotlin)")
private val LANGS_SUGGESTIONS = listOf("Python", "Kotlin", "Java", "C++", "Go", "Rust", "TypeScript", "JavaScript")
private val FRAMEWORKS_SUGGESTIONS = listOf("FastAPI", "Flask", "Django", "Uvicorn", "Gunicorn", "Jetpack Compose", "React", "Spring Boot", "Ktor", "Retrofit", "OkHttp", "Coroutines", "Android View")
private val DATABASES_SUGGESTIONS = listOf("PostgreSQL", "MySQL", "SQLite", "MongoDB", "Redis", "Cassandra", "Firebase Firestore", "SQL", "NoSQL")
private val CLOUD_SUGGESTIONS = listOf("Docker", "Kubernetes", "Linux", "AWS", "GCP", "Cloud Run", "Render", "Railway", "CI/CD", "GitHub Actions", "GitLab CI", "Azure", "Serverless")
private val AI_SUGGESTIONS = listOf("Machine Learning (Backend)", "NLP", "LLM", "RAG", "Embeddings", "Vector DB", "PyTorch", "TensorFlow", "scikit-learn")
private val TOOLS_SUGGESTIONS = listOf("Git", "Jira", "Postman", "Bash", "VS Code", "Android Studio", "Swagger", "GraphQL", "PyTest", "Unit Testing", "TDD", "REST", "OpenAPI")
private val OTHER_SUGGESTIONS = listOf("Problem Solving", "Agile", "Scrum", "Code Review", "Database Design", "System Design", "Mentorship")
private val TEMPLATE_OPTIONS = listOf(TemplateOption("ats", "ATS Professional", "Single-column, dense, keyword-safe, no graphics"), TemplateOption("modern", "Modern Professional", "Human-readable layout with color accents and optional photo"))

// ---------------------------
// Main Screen
// ---------------------------
@Composable
fun CreateResumeScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoAiAssistant: () -> Unit,
    onGoProfile: () -> Unit,
    apiBaseUrl: String,
    apiAppKey: String,
    userProfileStore: com.rehanu04.resumematchv2.data.UserProfileStore
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density) > 0

    var lastImeVisible by remember { mutableStateOf(false) }
    var imeJustHiddenAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(imeVisible) {
        val now = System.currentTimeMillis()
        if (lastImeVisible && !imeVisible) imeJustHiddenAt = now
        lastImeVisible = imeVisible
    }

    var showCoverLetterDialog by remember { mutableStateOf(false) }
    var coverLetterText by remember { mutableStateOf("") }
    var isGeneratingCoverLetter by remember { mutableStateOf(false) }

    var draft by remember { mutableStateOf(Draft()) }
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())
    val gson = remember { Gson() }

    // ✅ CRASH FIX: Bulletproof Vault Parsers
    val listTypeProj = object : TypeToken<List<ProjectEntry>>() {}.type
    val listTypeExp = object : TypeToken<List<ExperienceEntry>>() {}.type
    val listTypeSkill = object : TypeToken<List<String>>() {}.type

    val vaultProjects: List<ProjectEntry> = remember(userProfile.savedProjectsJson) {
        try {
            val parsed: List<ProjectEntry>? = gson.fromJson(userProfile.savedProjectsJson, listTypeProj)
            parsed?.filterNotNull()?.map { ProjectEntry(name = it.name ?: "", startMonth = it.startMonth ?: "", startYear = it.startYear ?: "", endMonth = it.endMonth ?: "", endYear = it.endYear ?: "", bullets = it.bullets ?: "") } ?: emptyList()
        } catch(e:Exception) { emptyList() }
    }

    val vaultExperience: List<ExperienceEntry> = remember(userProfile.savedExperienceJson) {
        try {
            val parsed: List<ExperienceEntry>? = gson.fromJson(userProfile.savedExperienceJson, listTypeExp)
            parsed?.filterNotNull()?.map { ExperienceEntry(company = it.company ?: "", role = it.role ?: "", startMonth = it.startMonth ?: "", startYear = it.startYear ?: "", endMonth = it.endMonth ?: "", endYear = it.endYear ?: "", bullets = it.bullets ?: "") } ?: emptyList()
        } catch(e:Exception) { emptyList() }
    }

    val vaultSkills: List<String> = remember(userProfile.savedSkillsJson) {
        try {
            val parsed: List<String>? = gson.fromJson(userProfile.savedSkillsJson, listTypeSkill)
            parsed?.filterNotNull()?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        } catch(e: Exception) { emptyList() }
    }

    LaunchedEffect(userProfile) {
        if (draft.firstName.isBlank() && userProfile.isComplete) {
            draft = draft.copy(
                firstName = userProfile.firstName, lastName = userProfile.lastName, targetRole = userProfile.targetRole,
                location = userProfile.location, email = userProfile.email, phone = userProfile.phone, summary = userProfile.summary,
                profileImageB64 = userProfile.profileImageB64, profileImageName = userProfile.profileImageName,
                links = Links(linkedin = userProfile.linkedin, github = userProfile.github, portfolio = userProfile.portfolio)
            )
        }
    }

    var panel by remember { mutableStateOf<EditPanel?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var submitAttempted by remember { mutableStateOf(false) }

    fun openPanel(p: EditPanel) { errorText = null; panel = p }
    fun closePanel() { panel = null; keyboard?.hide(); focus.clearFocus(force = true) }

    BackHandler(enabled = panel != null) {
        val now = System.currentTimeMillis()
        if (imeVisible) { focus.clearFocus(force = true); keyboard?.hide(); return@BackHandler }
        if (now - imeJustHiddenAt < 250L) return@BackHandler
        closePanel()
    }

    val jobReady = draft.jdText.trim().isNotEmpty()
    val aboutReady = draft.firstName.trim().isNotEmpty() && draft.lastName.trim().isNotEmpty() && draft.targetRole.trim().isNotEmpty() && draft.location.trim().isNotEmpty() && (draft.email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(draft.email).matches()) && (draft.phone.isBlank() || Patterns.PHONE.matcher(draft.phone).matches())
    val allSkills = flattenSkills(draft)
    val skillsReady = allSkills.isNotEmpty()
    val hasExpDateError = draft.experience.any { validateDates(it.startMonth, it.startYear, it.endMonth, it.endYear, false) != null }
    val hasProjDateError = draft.projects.any { validateDates(it.startMonth, it.startYear, it.endMonth, it.endYear, false) != null }
    val experienceReady = draft.experience.any { it.company.trim().isNotEmpty() && it.role.trim().isNotEmpty() && it.startYear.trim().isNotEmpty() && bulletsToList(it.bullets).isNotEmpty() } && !hasExpDateError
    val projectsReady = draft.projects.any { it.name.trim().isNotEmpty() && bulletsToList(it.bullets).isNotEmpty() } && !hasProjDateError
    val educationReady = draft.education.any { it.degree.trim().isNotEmpty() && it.university.trim().isNotEmpty() && (it.startYear.trim().isNotEmpty() || it.endYear.trim().isNotEmpty()) } && !draft.education.any { validateDates(it.startMonth, it.startYear, it.endMonth, it.endYear, true) != null }

    suspend fun generatePdf() {
        loading = true; errorText = null
        val req = ResumePdfRequest(template = draft.templateId, jd_text = draft.jdText.trim(), first_name = draft.firstName.trim(), last_name = draft.lastName.trim(), target_role = draft.targetRole.trim(), email = draft.email.trim(), phone = draft.phone.trim(), location = draft.location.trim(), summary = draft.summary.trim(), skills = allSkills, experience_text = buildExperienceText(draft), projects_text = buildProjectsText(draft), education_text = buildEducationText(draft), extras_text = buildExtrasText(draft), linkedin = draft.links.linkedin.trim(), github = draft.links.github.trim(), portfolio = draft.links.portfolio.trim(), profile_image_b64 = draft.profileImageB64)
        try {
            val api = createRetrofit(apiBaseUrl)
            val bytes = withContext(Dispatchers.IO) { api.createPdf(apiAppKey, req).bytes() }
            openPdf(ctx, withContext(Dispatchers.IO) { savePdf(ctx, bytes) })
        } catch (e: Exception) { errorText = "Timeout: Server is starting up. Try again in 30s." } finally { loading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Resume", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = onGoProfile) { Icon(Icons.Filled.Person, contentDescription = "Profile") }
                    IconButton(onClick = { onToggleTheme(!isDark) }) { Icon(Icons.Filled.Brightness6, contentDescription = "Toggle Theme") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onGoAiAssistant, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Mic, contentDescription = "AI Voice Agent", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SectionCard("Job Description", if (jobReady) "JD added" else "Paste the JD you’re applying to *", if (jobReady) "Ready" else "Required") { openPanel(EditPanel.JOB) }
            SectionCard("About", "Name + role + location * (summary + photo)", if (aboutReady) "Ready" else "Required") { openPanel(EditPanel.ABOUT) }
            SectionCard("Skills", "Categorized skills • Min 1 skill *", if (skillsReady) "Ready" else "Required") { openPanel(EditPanel.SKILLS) }
            SectionCard("Experience", "Add if you have it (optional)", if (experienceReady) "Ready" else if (hasExpDateError) "Error" else "Optional") { openPanel(EditPanel.EXPERIENCE) }
            SectionCard("Projects", "Highly recommended for early-career *", if (projectsReady) "Ready" else if (hasProjDateError) "Error" else "Required*") { openPanel(EditPanel.PROJECTS) }
            SectionCard("Education", "Degree + school + month/year range *", if (educationReady) "Ready" else "Required") { openPanel(EditPanel.EDUCATION) }
            SectionCard("Certifications", "Optional useful extras", if (draft.certs.isNotEmpty()) "Ready" else "Optional") { openPanel(EditPanel.CERTS) }
            SectionCard("Achievements", "Awards / scholarships", if (draft.achievements.isNotEmpty()) "Ready" else "Optional") { openPanel(EditPanel.ACHIEVEMENTS) }
            SectionCard("Links", "LinkedIn / GitHub / Portfolio", if (hasAnyLink(draft.links)) "Ready" else "Optional") { openPanel(EditPanel.LINKS) }
            SectionCard("Template", TEMPLATE_OPTIONS.firstOrNull { it.id == draft.templateId }?.title ?: "ATS Professional", "Ready") { openPanel(EditPanel.TEMPLATE) }
            Spacer(Modifier.height(14.dp))

            Card(modifier = Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Generate Output", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp))
                    Text(text = "Required: JD, About info, Skills, Content, Education.", style = MaterialTheme.typography.bodySmall)
                    if (errorText != null) { Spacer(Modifier.height(8.dp)); Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = { submitAttempted = true; val missing = firstMissingPanel(draft); if (missing != null) openPanel(missing) else scope.launch { generatePdf() } },
                            enabled = !loading
                        ) { Text(if (loading) "Generating..." else "Create PDF") }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            enabled = jobReady && !isGeneratingCoverLetter,
                            onClick = {
                                isGeneratingCoverLetter = true; showCoverLetterDialog = true; coverLetterText = ""
                                scope.launch {
                                    try {
                                        val client = okhttp3.OkHttpClient.Builder().connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS).readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build()
                                        val vaultDataStr = "Name: ${draft.firstName} ${draft.lastName}\nSkills: ${flattenSkills(draft).joinToString(", ")}\nExp: ${buildExperienceText(draft)}\nProjects: ${buildProjectsText(draft)}"
                                        val jsonBody = org.json.JSONObject().apply { put("job_description", draft.jdText); put("vault_data", vaultDataStr) }.toString()
                                        val req = okhttp3.Request.Builder().url(apiBaseUrl.trimEnd('/') + "/v1/ai/cover-letter").post(jsonBody.toRequestBody("application/json".toMediaType())).build()
                                        val responseStr = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null } }
                                        if (responseStr != null) { coverLetterText = org.json.JSONObject(responseStr).optString("cover_letter", "Error generating text.") } else { coverLetterText = "Failed to connect to backend." }
                                    } catch (e: Exception) { coverLetterText = "Timeout: Server is starting up. Try again." } finally { isGeneratingCoverLetter = false }
                                }
                            }
                        ) { Text(if (isGeneratingCoverLetter) "Writing..." else "Cover Letter") }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (panel != null) {
        ModalBottomSheet(onDismissRequest = { closePanel() }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
            when (panel!!) {
                EditPanel.JOB -> JobPanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.ABOUT -> AboutPanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.SKILLS -> SkillsPanel(draft, vaultSkills, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.EXPERIENCE -> ExperiencePanel(draft, vaultExperience, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.PROJECTS -> ProjectsPanel(draft, vaultProjects, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.EDUCATION -> EducationPanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.CERTS -> CertsPanel(draft, { draft = it }, { closePanel() })
                EditPanel.ACHIEVEMENTS -> AchievementsPanel(draft, { draft = it }, { closePanel() })
                EditPanel.LINKS -> LinksPanel(draft, { draft = it }, { closePanel() })
                EditPanel.TEMPLATE -> TemplatePanel(draft, { draft = it }, { closePanel() })
            }
        }
    }

    if (showCoverLetterDialog) {
        AlertDialog(
            onDismissRequest = { showCoverLetterDialog = false },
            title = { Text("AI Cover Letter") },
            text = {
                if (isGeneratingCoverLetter) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Drafting tailored letter...")
                    }
                } else {
                    OutlinedTextField(value = coverLetterText, onValueChange = { coverLetterText = it }, modifier = Modifier.fillMaxWidth().height(400.dp), textStyle = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = { TextButton(onClick = { showCoverLetterDialog = false }) { Text("Close") } }
        )
    }
}

// ---------------------------
// Panels (Job, About, Skills, etc.)
// ---------------------------
@Composable
private fun PanelHeader(title: String, onDone: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current; val focus = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().background(MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("Hide", modifier = Modifier.clickable { focus.clearFocus(force = true); keyboard?.hide() }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(14.dp))
            Text("Done", modifier = Modifier.clickable { onDone() }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
        Divider()
    }
}

@Composable
private fun JobPanel(draft: Draft, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Job Description", onDone)
    val isErr = submitAttempted && draft.jdText.trim().isEmpty()
    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = draft.jdText, onValueChange = { onDraft(draft.copy(jdText = it)) }, label = { Text("Paste Job Description *") }, isError = isErr, minLines = 8, modifier = Modifier.fillMaxWidth())
        if (isErr) { Spacer(Modifier.height(6.dp)); Text("Required field.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AboutPanel(draft: Draft, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("About", onDone)
    var roleQuery by remember { mutableStateOf(draft.targetRole) }; var roleFocused by remember { mutableStateOf(false) }

    // ✅ FIX: Added Validation highlights for Role and Location
    val firstErr = submitAttempted && draft.firstName.trim().isEmpty()
    val lastErr = submitAttempted && draft.lastName.trim().isEmpty()
    val roleErr = submitAttempted && draft.targetRole.trim().isEmpty()
    val locErr = submitAttempted && draft.location.trim().isEmpty()

    val emailInvalid = submitAttempted && draft.email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(draft.email).matches()
    val phoneInvalid = submitAttempted && draft.phone.isNotBlank() && !Patterns.PHONE.matcher(draft.phone).matches()

    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = draft.firstName, onValueChange = { onDraft(draft.copy(firstName = it)) }, label = { Text("First name *") }, isError = firstErr, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = draft.lastName, onValueChange = { onDraft(draft.copy(lastName = it)) }, label = { Text("Last name *") }, isError = lastErr, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = roleQuery, onValueChange = { roleQuery = it; onDraft(draft.copy(targetRole = it)) }, label = { Text("Target role *") }, isError = roleErr, modifier = Modifier.fillMaxWidth().onFocusChanged { roleFocused = it.isFocused })
        if (roleFocused && roleQuery.isNotEmpty()) {
            val matches = ROLE_SUGGESTIONS.filter { it.contains(roleQuery, true) }.take(5)
            if (matches.isNotEmpty()) SuggestionsCard("Role suggestions", matches, "No matches") { picked -> roleQuery = picked; onDraft(draft.copy(targetRole = picked)) }
        }
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = draft.location, onValueChange = { onDraft(draft.copy(location = it)) }, label = { Text("Location *") }, isError = locErr, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = draft.email, onValueChange = { onDraft(draft.copy(email = it)) }, label = { Text("Email") }, isError = emailInvalid, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = draft.phone, onValueChange = { onDraft(draft.copy(phone = it)) }, label = { Text("Phone") }, isError = phoneInvalid, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = draft.summary, onValueChange = { onDraft(draft.copy(summary = it)) }, label = { Text("Summary") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp)); Divider(); Spacer(Modifier.height(12.dp))
        val ctx = LocalContext.current
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) { val encoded = readImageAsBase64(ctx, uri); if (encoded != null) onDraft(draft.copy(profileImageB64 = encoded.first, profileImageName = encoded.second)) }
        }
        Text("Profile photo (optional)", style = MaterialTheme.typography.titleSmall); Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(if (draft.profileImageB64.isBlank()) "Choose photo" else "Replace") }
            if (draft.profileImageB64.isNotBlank()) OutlinedButton(onClick = { onDraft(draft.copy(profileImageB64 = "", profileImageName = "")) }) { Text("Remove") }
        }
    }
}

@Composable
private fun SkillsPanel(draft: Draft, vaultSkills: List<String>, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Skills", onDone)
    val ctx = LocalContext.current
    val cats = listOf("Languages" to draft.skillsLanguages, "Frameworks" to draft.skillsFrameworks, "Databases" to draft.skillsDatabases, "Cloud/DevOps" to draft.skillsCloud, "ML/AI" to draft.skillsAI, "Tools" to draft.skillsTools, "Other" to draft.skillsOther)
    var sel by remember { mutableIntStateOf(0) }; var query by remember { mutableStateOf("") }
    val currentLabel = cats[sel].first; val currentList = cats[sel].second
    fun addS(v: String) { if (v.isNotBlank()) onDraft(updateSkillCategory(draft, currentLabel, (currentList + v.trim()).distinctBy { it.lowercase() })); query = "" }

    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).verticalScroll(rememberScrollState())) {

        // ✨ AUTO MATCH SKILLS LOGIC FIX
        if (vaultSkills.isNotEmpty() && draft.jdText.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    val jdLow = draft.jdText.lowercase()
                    val matched = vaultSkills.filter { jdLow.contains(it.lowercase()) }

                    var newLangs = draft.skillsLanguages
                    var newFws = draft.skillsFrameworks
                    var newDbs = draft.skillsDatabases
                    var newCloud = draft.skillsCloud
                    var newAi = draft.skillsAI
                    var newTools = draft.skillsTools
                    var newOther = draft.skillsOther

                    matched.forEach { s ->
                        when {
                            LANGS_SUGGESTIONS.any { it.equals(s, true) } -> newLangs = (newLangs + listOf(s)).distinct()
                            FRAMEWORKS_SUGGESTIONS.any { it.equals(s, true) } -> newFws = (newFws + listOf(s)).distinct()
                            DATABASES_SUGGESTIONS.any { it.equals(s, true) } -> newDbs = (newDbs + listOf(s)).distinct()
                            CLOUD_SUGGESTIONS.any { it.equals(s, true) } -> newCloud = (newCloud + listOf(s)).distinct()
                            AI_SUGGESTIONS.any { it.equals(s, true) } -> newAi = (newAi + listOf(s)).distinct()
                            TOOLS_SUGGESTIONS.any { it.equals(s, true) } -> newTools = (newTools + listOf(s)).distinct()
                            else -> newOther = (newOther + listOf(s)).distinct()
                        }
                    }
                    onDraft(draft.copy(
                        skillsLanguages = newLangs, skillsFrameworks = newFws, skillsDatabases = newDbs,
                        skillsCloud = newCloud, skillsAI = newAi, skillsTools = newTools, skillsOther = newOther
                    ))
                    Toast.makeText(ctx, "✨ Auto-Matched ${matched.size} skills from Vault!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("✨ Auto-Match Skills from JD")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cats.forEachIndexed { i, p -> AssistPill(p.first, i == sel) { sel = i; query = "" } }
        }
        Spacer(Modifier.height(12.dp)); ChipsWrap(currentList) { toR -> onDraft(updateSkillCategory(draft, currentLabel, currentList.filterNot { it == toR })) }
        Spacer(Modifier.height(12.dp)); OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Add skill in $currentLabel") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardActions = KeyboardActions(onDone = { addS(query) }))
        val sug = when(currentLabel) { "Languages" -> LANGS_SUGGESTIONS; "Frameworks" -> FRAMEWORKS_SUGGESTIONS; "Databases" -> DATABASES_SUGGESTIONS; "Cloud/DevOps" -> CLOUD_SUGGESTIONS; "ML/AI" -> AI_SUGGESTIONS; "Tools" -> TOOLS_SUGGESTIONS; else -> OTHER_SUGGESTIONS }
        if (query.isNotEmpty()) {
            val matches = sug.filter { it.contains(query, true) }.take(5)
            if (matches.isNotEmpty()) SuggestionsCard("Suggestions", matches, "") { addS(it) }
        }
        Button(onClick = { addS(query) }, enabled = query.isNotEmpty(), modifier = Modifier.padding(top = 8.dp)) { Text("Add Skill") }
    }
}

@Composable
private fun ExperiencePanel(draft: Draft, vaultExperience: List<ExperienceEntry>, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Experience", onDone)
    val ctx = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(16.dp)) {

        // ✨ NEW: AUTO MATCH EXPERIENCE LOGIC
        if (vaultExperience.isNotEmpty() && draft.jdText.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    val jdWords = draft.jdText.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
                    val scored = vaultExperience.map { exp ->
                        val expWords = (exp.company + " " + exp.role + " " + exp.bullets).lowercase().split(Regex("\\W+"))
                        exp to expWords.count { it in jdWords }
                    }
                    val bestMatches = scored.sortedByDescending { it.second }.take(3).map { it.first }
                    onDraft(draft.copy(experience = (draft.experience + bestMatches).distinct()))
                    Toast.makeText(ctx, "✨ Added Top ${bestMatches.size} Matching Roles!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("✨ Auto-Match Top Experience from JD")
            }
        }

        if (vaultExperience.isNotEmpty()) {
            VaultSelector(
                title = "Experience",
                vaultItems = vaultExperience.map { "${it.company} - ${it.role}" },
                onItemSelected = { index -> onDraft(draft.copy(experience = draft.experience + vaultExperience[index])) }
            )
        }
        OutlinedButton(onClick = { onDraft(draft.copy(experience = draft.experience + ExperienceEntry())) }) { Icon(Icons.Filled.Add, null); Text("Add New Role") }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(draft.experience) { i, item ->
                ExperienceCard(i, item, submitAttempted, submitAttempted, { updated -> onDraft(draft.copy(experience = draft.experience.mapIndexed { idx, e -> if (idx == i) updated else e })) }, { onDraft(draft.copy(experience = draft.experience.filterIndexed { idx, _ -> idx != i })) })
            }
        }
    }
}

@Composable
private fun ProjectsPanel(draft: Draft, vaultProjects: List<ProjectEntry>, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Projects", onDone)
    val ctx = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(16.dp)) {

        // ✨ NEW: AUTO MATCH PROJECTS LOGIC
        if (vaultProjects.isNotEmpty() && draft.jdText.isNotBlank()) {
            OutlinedButton(
                onClick = {
                    val jdWords = draft.jdText.lowercase().split(Regex("\\W+")).filter { it.length > 2 }.toSet()
                    val scored = vaultProjects.map { proj ->
                        val projWords = (proj.name + " " + proj.bullets).lowercase().split(Regex("\\W+"))
                        proj to projWords.count { it in jdWords }
                    }
                    val bestMatches = scored.sortedByDescending { it.second }.take(3).map { it.first }
                    onDraft(draft.copy(projects = (draft.projects + bestMatches).distinct()))
                    Toast.makeText(ctx, "✨ Added Top ${bestMatches.size} Matching Projects!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("✨ Auto-Match Top Projects from JD")
            }
        }

        if (vaultProjects.isNotEmpty()) {
            VaultSelector(
                title = "Projects",
                vaultItems = vaultProjects.map { it.name },
                onItemSelected = { index -> onDraft(draft.copy(projects = draft.projects + vaultProjects[index])) }
            )
        }
        OutlinedButton(onClick = { onDraft(draft.copy(projects = draft.projects + ProjectEntry())) }) { Icon(Icons.Filled.Add, null); Text("Add New Project") }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(draft.projects) { i, item ->
                ProjectCard(i, item, submitAttempted, submitAttempted, { updated -> onDraft(draft.copy(projects = draft.projects.mapIndexed { idx, p -> if (idx == i) updated else p })) }, { onDraft(draft.copy(projects = draft.projects.filterIndexed { idx, _ -> idx != i })) })
            }
        }
    }
}

@Composable
private fun EducationPanel(draft: Draft, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Education", onDone)
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).padding(16.dp)) {
        OutlinedButton(onClick = { onDraft(draft.copy(education = draft.education + EducationEntry())) }) { Icon(Icons.Filled.Add, null); Text("Add Education") }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(draft.education) { i, item ->
                EducationCard(i, item, submitAttempted, submitAttempted, { updated -> onDraft(draft.copy(education = draft.education.mapIndexed { idx, e -> if (idx == i) updated else e })) }, { onDraft(draft.copy(education = draft.education.filterIndexed { idx, _ -> idx != i })) })
            }
        }
    }
}

@Composable
private fun CertsPanel(draft: Draft, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Certifications", onDone)
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedButton(onClick = { onDraft(draft.copy(certs = draft.certs + CertificationEntry())) }) { Icon(Icons.Filled.Add, null); Text("Add Cert") }
        LazyColumn(modifier = Modifier.padding(top = 10.dp)) {
            itemsIndexed(draft.certs) { i, item -> CertCard(i, item, { updated -> onDraft(draft.copy(certs = draft.certs.mapIndexed { idx, c -> if (idx == i) updated else c })) }, { onDraft(draft.copy(certs = draft.certs.filterIndexed { idx, _ -> idx != i })) }) }
        }
    }
}

@Composable
private fun AchievementsPanel(draft: Draft, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Achievements", onDone)
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedButton(onClick = { onDraft(draft.copy(achievements = draft.achievements + AchievementEntry())) }) { Icon(Icons.Filled.Add, null); Text("Add") }
        LazyColumn(modifier = Modifier.padding(top = 10.dp)) {
            itemsIndexed(draft.achievements) { i, item -> AchievementCard(i, item, { updated -> onDraft(draft.copy(achievements = draft.achievements.mapIndexed { idx, a -> if (idx == i) updated else a })) }, { onDraft(draft.copy(achievements = draft.achievements.filterIndexed { idx, _ -> idx != i })) }) }
        }
    }
}

@Composable
private fun LinksPanel(draft: Draft, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Links", onDone)
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = draft.links.linkedin, onValueChange = { onDraft(draft.copy(links = draft.links.copy(linkedin = it))) }, label = { Text("LinkedIn") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = draft.links.github, onValueChange = { onDraft(draft.copy(links = draft.links.copy(github = it))) }, label = { Text("GitHub") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp)); OutlinedTextField(value = draft.links.portfolio, onValueChange = { onDraft(draft.copy(links = draft.links.copy(portfolio = it))) }, label = { Text("Portfolio") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TemplatePanel(draft: Draft, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Template", onDone)
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        TEMPLATE_OPTIONS.forEach { opt ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onDraft(draft.copy(templateId = opt.id)) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (draft.templateId == opt.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp)) { Text(opt.title, style = MaterialTheme.typography.titleMedium); Text(opt.subtitle, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultSelector(
    title: String,
    vaultItems: List<String>,
    onItemSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Import $title from Master Vault")
        }
        if (expanded) {
            ModalBottomSheet(onDismissRequest = { expanded = false }) {
                LazyColumn(Modifier.padding(16.dp).navigationBarsPadding()) {
                    item { Text("Your Saved $title", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp)) }
                    itemsIndexed(vaultItems) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onItemSelected(index); expanded = false },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Filled.Add, contentDescription = "Add")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------
// UI Components
// ---------------------------
@Composable
private fun SectionCard(title: String, sub: String, stat: String, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(sub, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Card(shape = RoundedCornerShape(999.dp), colors = CardDefaults.cardColors(containerColor = if(stat == "Error") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)) {
                Text(stat, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, null) }
        }
    }
}

@Composable
private fun SuggestionsCard(title: String, items: List<String>, empty: String, onPick: (String) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            items.forEach { s -> Row(modifier = Modifier.fillMaxWidth().clickable { onPick(s) }.padding(vertical = 10.dp)) { Text(s) }; Divider() }
        }
    }
}

@Composable
private fun AssistPill(t: String, s: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable { onClick() }, shape = RoundedCornerShape(999.dp), colors = CardDefaults.cardColors(containerColor = if (s) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) { Text(t, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun ChipsWrap(items: List<String>, onRemove: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { s ->
            Card(shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Row(modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Delete, null, modifier = Modifier.size(14.dp).clickable { onRemove(s) })
                }
            }
        }
    }
}

// ---------------------------
// Helpers
// ---------------------------
private fun formatMonthYear(month: String, year: String): String {
    val m = month.trim(); val y = year.trim()
    if (m.equals("Present", true) || y.equals("Present", true)) return "Present"
    return if (y.isEmpty()) m else if (m.isEmpty()) y else "$m $y"
}

private fun formatRange(sM: String, sY: String, eM: String, eY: String): String {
    val s = formatMonthYear(sM, sY); val e = formatMonthYear(eM, eY)
    return when { s.isNotEmpty() && e.isNotEmpty() -> "$s – $e"; s.isNotEmpty() -> s; e.isNotEmpty() -> e; else -> "" }
}

private fun validateDates(sM: String, sY: String, eM: String, eY: String, allowF: Boolean): String? {
    if (sY.isEmpty()) return "Start year required"
    val curr = Calendar.getInstance().get(Calendar.YEAR); val sYear = sY.toIntOrNull() ?: 0
    val eYear = if (eY.equals("Present", true) || eY.isEmpty()) 9999 else eY.toIntOrNull() ?: 0
    if (!allowF && (sYear > curr || (eYear != 9999 && eYear > curr))) return "Cannot exceed $curr"
    if (sYear > eYear) return "Start cannot be after end"
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionField(label: String, value: String, options: List<String>, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, allowBlank: Boolean = true, blankLabel: String = "Not set", isError: Boolean = false) {
    var exp by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = it }, modifier = modifier) {
        OutlinedTextField(value = if (value.isBlank()) blankLabel else value, onValueChange = {}, readOnly = true, label = { Text(label) }, isError = isError, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(exp) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
            if (allowBlank) DropdownMenuItem(text = { Text(blankLabel) }, onClick = { onValueChange(""); exp = false })
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onValueChange(o); exp = false }) }
        }
    }
}

@Composable
private fun MonthYearRangeInputs(sM: String, sY: String, eM: String, eY: String, onSM: (String) -> Unit, onSY: (String) -> Unit, onEM: (String) -> Unit, onEY: (String) -> Unit, err: String? = null) {
    Text("Dates", color = if(err != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectionField("Start Month", sM, MONTH_OPTIONS, onSM, Modifier.weight(1f), isError = err != null)
        SelectionField("Start Year", sY, YEAR_OPTIONS, onSY, Modifier.weight(1f), false, isError = err != null)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectionField("End Month", eM, listOf("Present") + MONTH_OPTIONS, onEM, Modifier.weight(1f), isError = err != null)
        SelectionField("End Year", eY, listOf("Present") + YEAR_OPTIONS, onEY, Modifier.weight(1f), isError = err != null)
    }
    if (err != null) Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

// ---------------------------
// List Cards
// ---------------------------
@Composable
private fun ExperienceCard(i: Int, item: ExperienceEntry, sub: Boolean, err: Boolean, onChange: (ExperienceEntry) -> Unit, onDelete: () -> Unit) {
    val dateErr = if (sub) validateDates(item.startMonth, item.startYear, item.endMonth, item.endYear, false) else null
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row { Text("Exp #${i+1}", Modifier.weight(1f)); IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null) } }
            OutlinedTextField(value = item.company, onValueChange = { onChange(item.copy(company = it)) }, label = { Text("Company *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); OutlinedTextField(value = item.role, onValueChange = { onChange(item.copy(role = it)) }, label = { Text("Role *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); MonthYearRangeInputs(item.startMonth, item.startYear, item.endMonth, item.endYear, { onChange(item.copy(startMonth = it)) }, { onChange(item.copy(startYear = it)) }, { onChange(item.copy(endMonth = it)) }, { onChange(item.copy(endYear = it)) }, dateErr)
            OutlinedTextField(value = item.bullets, onValueChange = { onChange(item.copy(bullets = it)) }, label = { Text("Bullets *") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProjectCard(i: Int, item: ProjectEntry, sub: Boolean, err: Boolean, onChange: (ProjectEntry) -> Unit, onDelete: () -> Unit) {
    val dateErr = if (sub) validateDates(item.startMonth, item.startYear, item.endMonth, item.endYear, false) else null
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row { Text("Proj #${i+1}", Modifier.weight(1f)); IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null) } }
            OutlinedTextField(value = item.name, onValueChange = { onChange(item.copy(name = it)) }, label = { Text("Name *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); MonthYearRangeInputs(item.startMonth, item.startYear, item.endMonth, item.endYear, { onChange(item.copy(startMonth = it)) }, { onChange(item.copy(startYear = it)) }, { onChange(item.copy(endMonth = it)) }, { onChange(item.copy(endYear = it)) }, dateErr)
            OutlinedTextField(value = item.bullets, onValueChange = { onChange(item.copy(bullets = it)) }, label = { Text("Bullets *") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EducationCard(i: Int, item: EducationEntry, sub: Boolean, err: Boolean, onChange: (EducationEntry) -> Unit, onDelete: () -> Unit) {
    val dateErr = if (sub) validateDates(item.startMonth, item.startYear, item.endMonth, item.endYear, true) else null
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row { Text("Edu #${i+1}", Modifier.weight(1f)); IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null) } }
            OutlinedTextField(value = item.degree, onValueChange = { onChange(item.copy(degree = it)) }, label = { Text("Degree *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); OutlinedTextField(value = item.university, onValueChange = { onChange(item.copy(university = it)) }, label = { Text("University *") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp)); MonthYearRangeInputs(item.startMonth, item.startYear, item.endMonth, item.endYear, { onChange(item.copy(startMonth = it)) }, { onChange(item.copy(startYear = it)) }, { onChange(item.copy(endMonth = it)) }, { onChange(item.copy(endYear = it)) }, dateErr)
        }
    }
}

@Composable
private fun CertCard(i: Int, item: CertificationEntry, onChange: (CertificationEntry) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.padding(bottom = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row { Text("Cert #${i+1}", Modifier.weight(1f)); IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null) } }
            OutlinedTextField(value = item.name, onValueChange = { onChange(item.copy(name = it)) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AchievementCard(i: Int, item: AchievementEntry, onChange: (AchievementEntry) -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.padding(bottom = 8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row { Text("Achieve #${i+1}", Modifier.weight(1f)); IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, null) } }
            OutlinedTextField(value = item.title, onValueChange = { onChange(item.copy(title = it)) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun hasAnyLink(l: Links): Boolean = l.linkedin.isNotBlank() || l.github.isNotBlank() || l.portfolio.isNotBlank()
private fun bulletsToList(t: String): List<String> = t.lines().map { it.trim() }.filter { it.isNotEmpty() }
private fun flattenSkills(d: Draft): List<String> = (d.skillsLanguages + d.skillsFrameworks + d.skillsDatabases + d.skillsCloud + d.skillsAI + d.skillsTools + d.skillsOther).map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
private fun updateSkillCategory(d: Draft, l: String, n: List<String>): Draft = when (l) { "Languages" -> d.copy(skillsLanguages = n); "Frameworks" -> d.copy(skillsFrameworks = n); "Databases" -> d.copy(skillsDatabases = n); "Cloud/DevOps" -> d.copy(skillsCloud = n); "ML/AI" -> d.copy(skillsAI = n); "Tools" -> d.copy(skillsTools = n); else -> d.copy(skillsOther = n) }
private fun isContentReady(d: Draft): Boolean = d.experience.any { it.company.isNotBlank() && bulletsToList(it.bullets).isNotEmpty() } || d.projects.any { it.name.isNotBlank() && bulletsToList(it.bullets).isNotEmpty() }

private fun firstMissingPanel(d: Draft): EditPanel? {
    if (d.jdText.isBlank()) return EditPanel.JOB
    if (d.firstName.isBlank() || d.lastName.isBlank() || d.targetRole.isBlank() || d.location.isBlank()) return EditPanel.ABOUT
    if (flattenSkills(d).isEmpty()) return EditPanel.SKILLS
    if (!isContentReady(d)) return EditPanel.PROJECTS
    if (d.education.isEmpty()) return EditPanel.EDUCATION
    return null
}

private fun buildExperienceText(d: Draft): String {
    val sb = StringBuilder()
    d.experience.forEach { e ->
        if (e.company.isBlank()) return@forEach
        sb.append("${e.company} — ${e.role} | ${formatRange(e.startMonth, e.startYear, e.endMonth, e.endYear)}\n")
        bulletsToList(e.bullets).forEach { b -> sb.append("• $b\n") }
        sb.append("\n")
    }
    return sb.toString().trim()
}

private fun buildProjectsText(d: Draft): String {
    val sb = StringBuilder()
    d.projects.forEach { p ->
        if (p.name.isBlank()) return@forEach
        sb.append("${p.name} | ${formatRange(p.startMonth, p.startYear, p.endMonth, p.endYear)}\n")
        bulletsToList(p.bullets).forEach { b -> sb.append("• $b\n") }
        sb.append("\n")
    }
    return sb.toString().trim()
}

private fun buildEducationText(d: Draft): String {
    val sb = StringBuilder()
    d.education.forEach { e ->
        if (e.degree.isBlank()) return@forEach
        sb.append("${e.degree} — ${e.university} | ${formatRange(e.startMonth, e.startYear, e.endMonth, e.endYear)}\n\n")
    }
    return sb.toString().trim()
}

private fun buildExtrasText(d: Draft): String {
    val sb = StringBuilder()
    if (d.certs.isNotEmpty()) { sb.append("Certifications:\n"); d.certs.forEach { sb.append("• ${it.name}\n") }; sb.append("\n") }
    if (d.achievements.isNotEmpty()) { sb.append("Achievements:\n"); d.achievements.forEach { sb.append("• ${it.title}\n") }; sb.append("\n") }
    if (hasAnyLink(d.links)) { sb.append("Links:\n"); if(d.links.linkedin.isNotBlank()) sb.append("• LinkedIn: ${d.links.linkedin}\n"); if(d.links.github.isNotBlank()) sb.append("• GitHub: ${d.links.github}\n") }
    return sb.toString().trim()
}

private fun readImageAsBase64(ctx: Context, uri: Uri): Pair<String, String>? {
    return try {
        val s = ctx.contentResolver.openInputStream(uri) ?: return null
        val b = BitmapFactory.decodeStream(s); s.close()
        val size = min(b.width, b.height); val sq = Bitmap.createBitmap(b, (b.width - size)/2, (b.height - size)/2, size, size)
        val sc = if (size > 512) Bitmap.createScaledBitmap(sq, 512, 512, true) else sq
        val os = ByteArrayOutputStream(); sc.compress(Bitmap.CompressFormat.JPEG, 85, os)
        val name = ctx.contentResolver.query(uri, null, null, null, null)?.use { c -> val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && i >= 0) c.getString(i) else "photo.jpg" } ?: "photo.jpg"
        Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP) to name
    } catch (_: Exception) { null }
}

private fun savePdf(ctx: Context, bytes: ByteArray): File {
    val f = File(File(ctx.cacheDir, "generated_pdfs").apply { mkdirs() }, "resume_${System.currentTimeMillis()}.pdf")
    FileOutputStream(f).use { it.write(bytes) }; return f
}

private fun openPdf(ctx: Context, f: File) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
    val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try { ctx.startActivity(Intent.createChooser(i, "Open Resume")) } catch (_: Exception) { Toast.makeText(ctx, "No PDF viewer found.", Toast.LENGTH_LONG).show() }
}

private val MONTH_OPTIONS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val YEAR_OPTIONS = (Calendar.getInstance().get(Calendar.YEAR) + 5 downTo 1980).map { it.toString() }