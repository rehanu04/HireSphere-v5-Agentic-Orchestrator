@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import okhttp3.OkHttpClient
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

// ---------------------------
// API
// ---------------------------
private interface ResumeApi {
    @POST("/v1/resume/pdf")
    suspend fun createPdf(
        @Header("X-App-Key") apiKey: String,
        @Body body: ResumePdfRequest
    ): ResponseBody
}

private fun createRetrofit(baseUrl: String): ResumeApi {
    val ok = OkHttpClient.Builder().build()
    return Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(ok)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ResumeApi::class.java)
}

// ---------------------------
// Models
// ---------------------------
private enum class EditPanel {
    JOB, ABOUT, SKILLS, EXPERIENCE, PROJECTS, EDUCATION, CERTS, ACHIEVEMENTS, LINKS, TEMPLATE
}

private data class TemplateOption(val id: String, val title: String, val subtitle: String)

private data class Links(
    val linkedin: String = "", val github: String = "", val portfolio: String = "", val other: String = ""
)

private data class ExperienceEntry(
    val company: String = "", val role: String = "", val location: String = "",
    val startMonth: String = "", val startYear: String = "",
    val endMonth: String = "", val endYear: String = "",
    val tech: String = "", val bullets: String = ""
)

private data class ProjectEntry(
    val name: String = "", val tagline: String = "", val link: String = "",
    val startMonth: String = "", val startYear: String = "",
    val endMonth: String = "", val endYear: String = "",
    val tech: String = "", val bullets: String = ""
)

private data class EducationEntry(
    val degree: String = "", val university: String = "", val location: String = "",
    val startMonth: String = "", val startYear: String = "",
    val endMonth: String = "", val endYear: String = "",
    val gpa: String = "", val coursework: String = ""
)

private data class CertificationEntry(
    val name: String = "", val issuer: String = "",
    val issueMonth: String = "", val issueYear: String = "",
    val expiryMonth: String = "", val expiryYear: String = "",
    val url: String = ""
)

private data class AchievementEntry(val title: String = "", val detail: String = "")

private data class Draft(
    val templateId: String = "ats",
    val jdText: String = "",
    val firstName: String = "", val lastName: String = "",
    val targetRole: String = "", val location: String = "",
    val email: String = "", val phone: String = "", val summary: String = "",
    val profileImageB64: String = "", val profileImageName: String = "",
    val skillsLanguages: List<String> = emptyList(), val skillsFrameworks: List<String> = emptyList(),
    val skillsDatabases: List<String> = emptyList(), val skillsCloud: List<String> = emptyList(),
    val skillsAI: List<String> = emptyList(), val skillsTools: List<String> = emptyList(),
    val skillsOther: List<String> = emptyList(),
    val experience: List<ExperienceEntry> = emptyList(),
    val projects: List<ProjectEntry> = emptyList(),
    val education: List<EducationEntry> = emptyList(),
    val certs: List<CertificationEntry> = emptyList(),
    val achievements: List<AchievementEntry> = emptyList(),
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
private val ROLE_SUGGESTIONS = listOf(
    "Software Engineer (Backend)", "Backend Engineer (Python/FastAPI)", "API Engineer (FastAPI)",
    "Python Backend Engineer", "Software Engineer – Platform", "AI Engineer (Backend)",
    "Machine Learning Engineer", "Android Engineer (Kotlin)"
)

private val LANGS_SUGGESTIONS = listOf("Python", "Kotlin", "Java", "C++", "Go", "Rust", "TypeScript", "JavaScript")
private val FRAMEWORKS_SUGGESTIONS = listOf("FastAPI", "Flask", "Django", "Uvicorn", "Gunicorn", "Jetpack Compose", "React", "Spring Boot", "Ktor", "Retrofit", "OkHttp", "Coroutines", "Android View")
private val DATABASES_SUGGESTIONS = listOf("PostgreSQL", "MySQL", "SQLite", "MongoDB", "Redis", "Cassandra", "Firebase Firestore", "SQL", "NoSQL")
private val CLOUD_SUGGESTIONS = listOf("Docker", "Kubernetes", "Linux", "AWS", "GCP", "Cloud Run", "Render", "Railway", "CI/CD", "GitHub Actions", "GitLab CI", "Azure", "Serverless")
private val AI_SUGGESTIONS = listOf("Machine Learning (Backend)", "NLP", "LLM", "RAG", "Embeddings", "Vector DB", "PyTorch", "TensorFlow", "scikit-learn")
private val TOOLS_SUGGESTIONS = listOf("Git", "Jira", "Postman", "Bash", "VS Code", "Android Studio", "Swagger", "GraphQL", "PyTest", "Unit Testing", "TDD", "REST", "OpenAPI")
private val OTHER_SUGGESTIONS = listOf("Problem Solving", "Agile", "Scrum", "Code Review", "Database Design", "System Design", "Mentorship")

private val TEMPLATE_OPTIONS = listOf(
    TemplateOption("ats", "ATS Professional", "Single-column, dense, keyword-safe, no graphics"),
    TemplateOption("modern", "Modern Professional", "Human-readable layout with color accents and optional photo")
)

// ---------------------------
// Screen
// ---------------------------
@Composable
fun CreateResumeScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onBack: () -> Unit,
    apiBaseUrl: String,
    apiAppKey: String
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

    var draft by remember { mutableStateOf(Draft()) }
    var panel by remember { mutableStateOf<EditPanel?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var submitAttempted by remember { mutableStateOf(false) }

    fun openPanel(p: EditPanel) {
        errorText = null
        panel = p
    }

    fun closePanel() {
        panel = null
        keyboard?.hide()
        focus.clearFocus(force = true)
    }

    BackHandler(enabled = panel != null) {
        val now = System.currentTimeMillis()
        if (imeVisible) {
            focus.clearFocus(force = true)
            keyboard?.hide()
            return@BackHandler
        }
        if (now - imeJustHiddenAt < 250L) return@BackHandler
        closePanel()
    }

    val jobReady = draft.jdText.trim().isNotEmpty()
    val emailValid = draft.email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(draft.email).matches()
    val phoneValid = draft.phone.isBlank() || Patterns.PHONE.matcher(draft.phone).matches()

    val aboutReady = draft.firstName.trim().isNotEmpty() && draft.lastName.trim().isNotEmpty() &&
            draft.targetRole.trim().isNotEmpty() && draft.location.trim().isNotEmpty() &&
            emailValid && phoneValid

    val allSkills = flattenSkills(draft)
    val skillsReady = allSkills.isNotEmpty()

    val hasExpDateError = draft.experience.any { validateDates(it.startMonth, it.startYear, it.endMonth, it.endYear, allowFuture = false) != null }
    val hasProjDateError = draft.projects.any { validateDates(it.startMonth, it.startYear, it.endMonth, it.endYear, allowFuture = false) != null }
    val hasEduDateError = draft.education.any { validateDates(it.startMonth, it.startYear, it.endMonth, it.endYear, allowFuture = true) != null }

    val experienceReady = draft.experience.any { it.company.trim().isNotEmpty() && it.role.trim().isNotEmpty() && it.startYear.trim().isNotEmpty() && bulletsToList(it.bullets).isNotEmpty() } && !hasExpDateError
    val projectsReady = draft.projects.any { it.name.trim().isNotEmpty() && bulletsToList(it.bullets).isNotEmpty() } && !hasProjDateError
    val contentReady = experienceReady || projectsReady
    val educationReady = draft.education.any { it.degree.trim().isNotEmpty() && it.university.trim().isNotEmpty() && (it.startYear.trim().isNotEmpty() || it.endYear.trim().isNotEmpty()) } && !hasEduDateError

    suspend fun generatePdf() {
        loading = true
        errorText = null
        val req = ResumePdfRequest(
            template = draft.templateId, jd_text = draft.jdText.trim(), first_name = draft.firstName.trim(),
            last_name = draft.lastName.trim(), target_role = draft.targetRole.trim(), email = draft.email.trim(),
            phone = draft.phone.trim(), location = draft.location.trim(), summary = draft.summary.trim(),
            skills = allSkills, experience_text = buildExperienceText(draft), projects_text = buildProjectsText(draft),
            education_text = buildEducationText(draft), extras_text = buildExtrasText(draft), linkedin = draft.links.linkedin.trim(),
            github = draft.links.github.trim(), portfolio = draft.links.portfolio.trim(), profile_image_b64 = draft.profileImageB64
        )

        try {
            val api = createRetrofit(apiBaseUrl)
            val body = withContext(Dispatchers.IO) { api.createPdf(apiAppKey, req) }
            val bytes = withContext(Dispatchers.IO) { body.bytes() }
            val outFile = withContext(Dispatchers.IO) { savePdf(ctx, bytes) }
            openPdf(ctx, outFile)
        } catch (e: Exception) {
            errorText = e.message ?: "Failed to generate PDF"
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text(text = "Create Resume", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = { onToggleTheme(!isDark) }) { Icon(Icons.Filled.Brightness6, contentDescription = "Toggle Theme") }
        }

        Spacer(Modifier.height(16.dp))

        SectionCard(title = "Job Description", subtitle = if (jobReady) "JD added" else "Paste the JD you’re applying to *", status = if (jobReady) "Ready" else "Required") { openPanel(EditPanel.JOB) }
        SectionCard(title = "About", subtitle = "Name + role + location * (summary + optional photo)", status = if (aboutReady) "Ready" else "Required") { openPanel(EditPanel.ABOUT) }
        SectionCard(title = "Skills", subtitle = "Categorized skills • Min 1 skill *", status = if (skillsReady) "Ready" else "Required") { openPanel(EditPanel.SKILLS) }
        SectionCard(title = "Experience", subtitle = "Add if you have it (projects can substitute)", status = if (experienceReady) "Ready" else if (hasExpDateError) "Error" else "Optional") { openPanel(EditPanel.EXPERIENCE) }
        SectionCard(title = "Projects", subtitle = "Highly recommended for early-career * (or experience)", status = if (projectsReady) "Ready" else if (hasProjDateError) "Error" else "Required*") { openPanel(EditPanel.PROJECTS) }
        SectionCard(title = "Education", subtitle = "Degree + school + month/year range *", status = if (educationReady) "Ready" else if (hasEduDateError) "Error" else "Required") { openPanel(EditPanel.EDUCATION) }
        SectionCard(title = "Certifications", subtitle = "Optional but useful", status = if (draft.certs.isNotEmpty()) "Ready" else "Optional") { openPanel(EditPanel.CERTS) }
        SectionCard(title = "Achievements", subtitle = "Awards / rankings / scholarships", status = if (draft.achievements.isNotEmpty()) "Ready" else "Optional") { openPanel(EditPanel.ACHIEVEMENTS) }
        SectionCard(title = "Links", subtitle = "LinkedIn / GitHub / Portfolio (optional)", status = if (hasAnyLink(draft.links)) "Ready" else "Optional") { openPanel(EditPanel.LINKS) }
        SectionCard(title = "Template", subtitle = TEMPLATE_OPTIONS.firstOrNull { it.id == draft.templateId }?.title ?: "ATS Professional", status = "Ready") { openPanel(EditPanel.TEMPLATE) }

        Spacer(Modifier.height(14.dp))

        Card(modifier = Modifier.fillMaxWidth().animateContentSize(), colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Generate PDF", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(text = "Required: JD, First name, Last name, Target role, Location, ≥1 skill, (Experience OR Projects), Education. ATS template stays plain; Modern template enables accents/photo.", style = MaterialTheme.typography.bodySmall)

                if (errorText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        submitAttempted = true
                        val firstMissing = firstMissingPanel(draft)
                        if (firstMissing != null) {
                            openPanel(firstMissing)
                            Toast.makeText(ctx, "Please fix the highlighted errors before generating", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch { generatePdf() }
                    },
                    enabled = !loading
                ) { Text(if (loading) "Generating..." else "Generate PDF") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (panel != null) {
        ModalBottomSheet(onDismissRequest = { closePanel() }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, dragHandle = null) {
            when (panel!!) {
                EditPanel.JOB -> JobPanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.ABOUT -> AboutPanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.SKILLS -> SkillsPanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.EXPERIENCE -> ExperiencePanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.PROJECTS -> ProjectsPanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.EDUCATION -> EducationPanel(draft, submitAttempted, { draft = it }, { closePanel() })
                EditPanel.CERTS -> CertsPanel(draft, { draft = it }, { closePanel() })
                EditPanel.ACHIEVEMENTS -> AchievementsPanel(draft, { draft = it }, { closePanel() })
                EditPanel.LINKS -> LinksPanel(draft, { draft = it }, { closePanel() })
                EditPanel.TEMPLATE -> TemplatePanel(draft, { draft = it }, { closePanel() })
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, onDone: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().background(MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("Hide keyboard", modifier = Modifier.clickable { focus.clearFocus(force = true); keyboard?.hide() }, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
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
        if (isErr) { Spacer(Modifier.height(6.dp)); Text("Job description is required.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AboutPanel(draft: Draft, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("About", onDone)
    var roleQuery by remember { mutableStateOf(draft.targetRole) }
    var roleFocused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { roleQuery = draft.targetRole; roleFocused = false }

    val firstErr = submitAttempted && draft.firstName.trim().isEmpty()
    val lastErr = submitAttempted && draft.lastName.trim().isEmpty()
    val roleErr = submitAttempted && draft.targetRole.trim().isEmpty()
    val locErr = submitAttempted && draft.location.trim().isEmpty()

    val emailInvalid = submitAttempted && draft.email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(draft.email).matches()
    val phoneInvalid = submitAttempted && draft.phone.isNotBlank() && !Patterns.PHONE.matcher(draft.phone).matches()

    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = draft.firstName, onValueChange = { onDraft(draft.copy(firstName = it)) }, label = { Text("First name *") }, isError = firstErr, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = draft.lastName, onValueChange = { onDraft(draft.copy(lastName = it)) }, label = { Text("Last name *") }, isError = lastErr, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = roleQuery, onValueChange = { roleQuery = it; onDraft(draft.copy(targetRole = it)) }, label = { Text("Target role *") }, isError = roleErr, modifier = Modifier.fillMaxWidth().onFocusChanged { roleFocused = it.isFocused }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { roleFocused = false }))
        val normalizedQ = roleQuery.trim()
        val roleMatches = if (roleFocused && normalizedQ.isNotEmpty()) ROLE_SUGGESTIONS.filter { it.contains(normalizedQ, ignoreCase = true) }.take(8) else emptyList()
        if (roleFocused && normalizedQ.isNotEmpty()) { Spacer(Modifier.height(8.dp)); SuggestionsCard("Role suggestions", roleMatches, "No matches") { picked -> roleQuery = picked; roleFocused = false; onDraft(draft.copy(targetRole = picked)) } }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = draft.location, onValueChange = { onDraft(draft.copy(location = it)) }, label = { Text("Location (City, Country) *") }, isError = locErr, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(value = draft.email, onValueChange = { onDraft(draft.copy(email = it)) }, label = { Text("Email (optional)") }, isError = emailInvalid, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        if (emailInvalid) { Spacer(Modifier.height(4.dp)); Text("Please enter a valid email format.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(value = draft.phone, onValueChange = { onDraft(draft.copy(phone = it)) }, label = { Text("Phone (optional)") }, isError = phoneInvalid, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        if (phoneInvalid) { Spacer(Modifier.height(4.dp)); Text("Please enter a valid phone number.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(value = draft.summary, onValueChange = { onDraft(draft.copy(summary = it)) }, label = { Text("Summary (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        val ctx = LocalContext.current
        val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val encoded = readImageAsBase64(ctx, uri)
                if (encoded != null) onDraft(draft.copy(profileImageB64 = encoded.first, profileImageName = encoded.second))
                else Toast.makeText(ctx, "Could not read image", Toast.LENGTH_SHORT).show()
            }
        }
        Text("Profile photo (optional)", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(if (draft.profileImageB64.isBlank()) "Choose photo" else "Replace photo") }
            if (draft.profileImageB64.isNotBlank()) OutlinedButton(onClick = { onDraft(draft.copy(profileImageB64 = "", profileImageName = "")) }) { Text("Remove") }
        }
        if (draft.profileImageB64.isNotBlank()) { Spacer(Modifier.height(8.dp)); SmallInfoBar("Image has been squared. The Python backend will perfectly fill the circular container.") }

        if (submitAttempted && (firstErr || lastErr || roleErr || locErr)) { Spacer(Modifier.height(8.dp)); Text("Fill the required fields.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SkillsPanel(draft: Draft, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Skills", onDone)
    val categories = listOf("Languages" to draft.skillsLanguages, "Frameworks" to draft.skillsFrameworks, "Databases" to draft.skillsDatabases, "Cloud/DevOps" to draft.skillsCloud, "ML/AI" to draft.skillsAI, "Tools" to draft.skillsTools, "Other" to draft.skillsOther)
    var selectedCat by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val currentLabel = categories[selectedCat].first
    val currentList = categories[selectedCat].second
    val allSkills = flattenSkills(draft)
    val skillsErr = submitAttempted && allSkills.isEmpty()

    fun addCurrentSkill(raw: String) {
        val value = raw.trim()
        if (value.isEmpty() || allSkills.any { it.equals(value, ignoreCase = true) }) { query = ""; return }
        onDraft(updateSkillCategory(draft, currentLabel, (currentList + value).distinctBy { it.lowercase(Locale.getDefault()) }))
        query = ""
    }

    val suggestionsForCategory = when(currentLabel) {
        "Languages" -> LANGS_SUGGESTIONS; "Frameworks" -> FRAMEWORKS_SUGGESTIONS
        "Databases" -> DATABASES_SUGGESTIONS; "Cloud/DevOps" -> CLOUD_SUGGESTIONS
        "ML/AI" -> AI_SUGGESTIONS; "Tools" -> TOOLS_SUGGESTIONS; "Other" -> OTHER_SUGGESTIONS
        else -> emptyList()
    }

    val matches = if (query.trim().isNotEmpty()) suggestionsForCategory.filter { it.contains(query.trim(), ignoreCase = true) }.filterNot { allSkills.any { s -> s.equals(it, ignoreCase = true) } }.take(10) else emptyList()

    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            categories.forEachIndexed { idx, pair -> AssistPill(text = pair.first, selected = idx == selectedCat, onClick = { selectedCat = idx; query = "" }) }
        }
        Spacer(Modifier.height(12.dp))
        ChipsWrap(items = currentList, onRemove = { toRemove -> onDraft(updateSkillCategory(draft, currentLabel, currentList.filterNot { it.equals(toRemove, ignoreCase = true) })) })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search / add skill in $currentLabel") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (query.trim().isNotEmpty()) addCurrentSkill(query) else { focus.clearFocus(); keyboard?.hide() } }))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { addCurrentSkill(query) }, enabled = query.trim().isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add skill") }
            OutlinedButton(onClick = { focus.clearFocus(); keyboard?.hide() }) { Text("Hide keyboard") }
        }
        if (query.trim().isNotEmpty() && matches.isNotEmpty()) { Spacer(Modifier.height(10.dp)); SuggestionsCard("Suggestions", matches, "No matches") { picked -> addCurrentSkill(picked) } }
        if (skillsErr) { Spacer(Modifier.height(10.dp)); Text("Add at least 1 skill.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun ExperiencePanel(draft: Draft, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Experience", onDone)
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Add roles you’ve done (optional).", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { onDraft(draft.copy(experience = draft.experience + ExperienceEntry())) }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add") }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(draft.experience) { idx, item ->
                ExperienceCard(index = idx, item = item, submitAttempted = submitAttempted, showErrors = submitAttempted && !isContentReady(draft), onChange = { updated -> onDraft(draft.copy(experience = draft.experience.mapIndexed { i, e -> if (i == idx) updated else e })) }, onDelete = { onDraft(draft.copy(experience = draft.experience.filterIndexed { i, _ -> i != idx })) })
            }
        }
    }
}

@Composable
private fun ProjectsPanel(draft: Draft, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Projects", onDone)
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Projects (required if no experience).", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { onDraft(draft.copy(projects = draft.projects + ProjectEntry())) }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add") }
        }
        if (submitAttempted && !isContentReady(draft)) { Spacer(Modifier.height(8.dp)); Text("Add at least 1 Project OR 1 Experience with bullets.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(10.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(draft.projects) { idx, item ->
                ProjectCard(index = idx, item = item, submitAttempted = submitAttempted, showErrors = submitAttempted && !isContentReady(draft), onChange = { updated -> onDraft(draft.copy(projects = draft.projects.mapIndexed { i, p -> if (i == idx) updated else p })) }, onDelete = { onDraft(draft.copy(projects = draft.projects.filterIndexed { i, _ -> i != idx })) })
            }
        }
    }
}

@Composable
private fun EducationPanel(draft: Draft, submitAttempted: Boolean, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Education", onDone)
    val eduErr = submitAttempted && !draft.education.any { it.degree.trim().isNotEmpty() && it.university.trim().isNotEmpty() && (it.startYear.trim().isNotEmpty() || it.endYear.trim().isNotEmpty()) }
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f).imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Education is required.", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { onDraft(draft.copy(education = draft.education + EducationEntry())) }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add") }
        }
        if (eduErr) { Spacer(Modifier.height(8.dp)); Text("Add at least 1 education entry.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(10.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(draft.education) { idx, item ->
                EducationCard(index = idx, item = item, submitAttempted = submitAttempted, showErrors = eduErr, onChange = { updated -> onDraft(draft.copy(education = draft.education.mapIndexed { i, e -> if (i == idx) updated else e })) }, onDelete = { onDraft(draft.copy(education = draft.education.filterIndexed { i, _ -> i != idx })) })
            }
        }
    }
}

@Composable
private fun CertsPanel(draft: Draft, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Certifications", onDone)
    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Optional.", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { onDraft(draft.copy(certs = draft.certs + CertificationEntry())) }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add") }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(draft.certs) { idx, item ->
                CertCard(index = idx, item = item, onChange = { updated -> onDraft(draft.copy(certs = draft.certs.mapIndexed { i, c -> if (i == idx) updated else c })) }, onDelete = { onDraft(draft.copy(certs = draft.certs.filterIndexed { i, _ -> i != idx })) })
            }
        }
    }
}

@Composable
private fun AchievementsPanel(draft: Draft, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Achievements", onDone)
    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Optional.", modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { onDraft(draft.copy(achievements = draft.achievements + AchievementEntry())) }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add") }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 60.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
            itemsIndexed(draft.achievements) { idx, item ->
                AchievementCard(index = idx, item = item, onChange = { updated -> onDraft(draft.copy(achievements = draft.achievements.mapIndexed { i, a -> if (i == idx) updated else a })) }, onDelete = { onDraft(draft.copy(achievements = draft.achievements.filterIndexed { i, _ -> i != idx })) })
            }
        }
    }
}

@Composable
private fun LinksPanel(draft: Draft, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Links", onDone)
    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(value = draft.links.linkedin, onValueChange = { onDraft(draft.copy(links = draft.links.copy(linkedin = it))) }, label = { Text("LinkedIn URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = draft.links.github, onValueChange = { onDraft(draft.copy(links = draft.links.copy(github = it))) }, label = { Text("GitHub URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = draft.links.portfolio, onValueChange = { onDraft(draft.copy(links = draft.links.copy(portfolio = it))) }, label = { Text("Portfolio URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = draft.links.other, onValueChange = { onDraft(draft.copy(links = draft.links.copy(other = it))) }, label = { Text("Other link") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TemplatePanel(draft: Draft, onDraft: (Draft) -> Unit, onDone: () -> Unit) {
    PanelHeader("Template", onDone)
    Column(modifier = Modifier.fillMaxWidth().imePadding().padding(16.dp).verticalScroll(rememberScrollState())) {
        TEMPLATE_OPTIONS.forEach { opt ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onDraft(draft.copy(templateId = opt.id)) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (draft.templateId == opt.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(opt.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text(if (draft.templateId == opt.id) "Selected" else "", style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(4.dp))
                    Text(opt.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ---------------------------
// UI generic blocks
// ---------------------------
@Composable
private fun SectionCard(title: String, subtitle: String, status: String, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(4.dp)); Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Card(shape = RoundedCornerShape(999.dp), colors = CardDefaults.cardColors(containerColor = if(status == "Error") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface)) {
                Text(status, color = if(status == "Error") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
        }
    }
}

@Composable
private fun SuggestionsCard(title: String, items: List<String>, emptyText: String, onPick: (String) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) Text(emptyText, style = MaterialTheme.typography.bodySmall)
            else items.forEach { s -> Row(modifier = Modifier.fillMaxWidth().clickable { onPick(s) }.padding(vertical = 10.dp)) { Text(s, modifier = Modifier.weight(1f)) }; Divider() }
        }
    }
}

@Composable
private fun SmallInfoBar(text: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)) { Text(text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun AssistPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.clickable { onClick() }, shape = RoundedCornerShape(999.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) { Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun ChipsWrap(items: List<String>, onRemove: (String) -> Unit) {
    if (items.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { s ->
            Card(shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = s, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Remove", modifier = Modifier.size(13.dp).clickable { onRemove(s) }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---------------------------
// Validation & Date Dropdowns
// ---------------------------
private val MONTH_OPTIONS = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
private val YEAR_OPTIONS: List<String> by lazy { val now = Calendar.getInstance().get(Calendar.YEAR); ((now + 5) downTo 1980).map { it.toString() } }

private fun validateDates(startMonth: String, startYear: String, endMonth: String, endYear: String, allowFuture: Boolean): String? {
    if (startYear.isEmpty()) return "Start year is required"
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    val sYear = startYear.toIntOrNull() ?: 0
    val eYear = if (endYear.equals("Present", ignoreCase = true) || endYear.isEmpty()) 9999 else endYear.toIntOrNull() ?: 0

    if (!allowFuture) {
        if (sYear > currentYear) return "Start year cannot exceed present year ($currentYear)"
        if (eYear != 9999 && eYear > currentYear) return "End year cannot exceed present year ($currentYear)"
    }

    if (sYear > eYear) return "Start date cannot be after end date"

    if (sYear == eYear && sYear != 0 && startMonth.isNotEmpty() && endMonth.isNotEmpty() && !endMonth.equals("Present", true)) {
        val sIdx = MONTH_OPTIONS.indexOf(startMonth)
        val eIdx = MONTH_OPTIONS.indexOf(endMonth)
        if (sIdx > eIdx) return "Start month cannot be after end month"
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionField(
    label: String, value: String, options: List<String>, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, allowBlank: Boolean = true, blankLabel: String = "Not set", isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (value.isBlank()) blankLabel else value,
            onValueChange = {}, readOnly = true, label = { Text(label) },
            isError = isError,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowBlank) {
                DropdownMenuItem(text = { Text(blankLabel) }, onClick = { onValueChange(""); expanded = false })
            }
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false })
            }
        }
    }
}

@Composable
private fun MonthYearRangeInputs(
    startMonth: String, startYear: String, endMonth: String, endYear: String,
    onStartMonth: (String) -> Unit, onStartYear: (String) -> Unit,
    onEndMonth: (String) -> Unit, onEndYear: (String) -> Unit,
    startMonthLabel: String = "Start month", startYearLabel: String = "Start year",
    endMonthLabel: String = "End month", endYearLabel: String = "End year",
    errorMessage: String? = null
) {
    val isErr = errorMessage != null

    Text("Dates", style = MaterialTheme.typography.labelLarge, color = if(isErr) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SelectionField(label = startMonthLabel, value = startMonth, options = MONTH_OPTIONS, onValueChange = onStartMonth, modifier = Modifier.weight(1f), blankLabel = "Month", isError = isErr)
        SelectionField(label = startYearLabel, value = startYear, options = YEAR_OPTIONS, onValueChange = onStartYear, modifier = Modifier.weight(1f), allowBlank = false, isError = isErr)
    }
    Spacer(Modifier.height(10.dp))

    val endMonthOptions = listOf("Present") + MONTH_OPTIONS
    val endYearOptions = listOf("Present") + YEAR_OPTIONS

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SelectionField(label = endMonthLabel, value = endMonth, options = endMonthOptions, onValueChange = onEndMonth, modifier = Modifier.weight(1f), blankLabel = "Month", isError = isErr)
        SelectionField(label = endYearLabel, value = endYear, options = endYearOptions, onValueChange = onEndYear, modifier = Modifier.weight(1f), blankLabel = "Year", isError = isErr)
    }

    if (isErr) {
        Spacer(Modifier.height(4.dp))
        Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    } else {
        Spacer(Modifier.height(6.dp))
        Text("Select 'Present' if this role/education is ongoing.", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(6.dp))
}

// ---------------------------
// Card UI Section
// ---------------------------
@Composable
private fun ExperienceCard(index: Int, item: ExperienceEntry, submitAttempted: Boolean, showErrors: Boolean, onChange: (ExperienceEntry) -> Unit, onDelete: () -> Unit) {
    val companyErr = showErrors && item.company.trim().isEmpty()
    val roleErr = showErrors && item.role.trim().isEmpty()
    val bulletErr = showErrors && bulletsToList(item.bullets).isEmpty()
    val dateError = if (submitAttempted) validateDates(item.startMonth, item.startYear, item.endMonth, item.endYear, allowFuture = false) else null

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Experience #${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            OutlinedTextField(value = item.company, onValueChange = { onChange(item.copy(company = it)) }, label = { Text("Company *") }, isError = companyErr, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.role, onValueChange = { onChange(item.copy(role = it)) }, label = { Text("Role title *") }, isError = roleErr, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.location, onValueChange = { onChange(item.copy(location = it)) }, label = { Text("Location (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            MonthYearRangeInputs(
                startMonth = item.startMonth, startYear = item.startYear, endMonth = item.endMonth, endYear = item.endYear,
                onStartMonth = { onChange(item.copy(startMonth = it)) }, onStartYear = { onChange(item.copy(startYear = it)) },
                onEndMonth = { onChange(item.copy(endMonth = it)) }, onEndYear = { onChange(item.copy(endYear = it)) },
                errorMessage = dateError
            )
            OutlinedTextField(value = item.tech, onValueChange = { onChange(item.copy(tech = it)) }, label = { Text("Tech stack (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.bullets, onValueChange = { onChange(item.copy(bullets = it)) }, label = { Text("Bullets * (one per line)") }, isError = bulletErr, minLines = 3, modifier = Modifier.fillMaxWidth())
            if (showErrors && (companyErr || roleErr || bulletErr)) { Spacer(Modifier.height(6.dp)); Text("Missing required fields.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ProjectCard(index: Int, item: ProjectEntry, submitAttempted: Boolean, showErrors: Boolean, onChange: (ProjectEntry) -> Unit, onDelete: () -> Unit) {
    val nameErr = showErrors && item.name.trim().isEmpty()
    val bulletErr = showErrors && bulletsToList(item.bullets).isEmpty()
    val dateError = if (submitAttempted) validateDates(item.startMonth, item.startYear, item.endMonth, item.endYear, allowFuture = false) else null

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Project #${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            OutlinedTextField(value = item.name, onValueChange = { onChange(item.copy(name = it)) }, label = { Text("Project name *") }, isError = nameErr, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.tagline, onValueChange = { onChange(item.copy(tagline = it)) }, label = { Text("Tagline (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.link, onValueChange = { onChange(item.copy(link = it)) }, label = { Text("Link (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.tech, onValueChange = { onChange(item.copy(tech = it)) }, label = { Text("Tech stack (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            MonthYearRangeInputs(
                startMonth = item.startMonth, startYear = item.startYear, endMonth = item.endMonth, endYear = item.endYear,
                onStartMonth = { onChange(item.copy(startMonth = it)) }, onStartYear = { onChange(item.copy(startYear = it)) },
                onEndMonth = { onChange(item.copy(endMonth = it)) }, onEndYear = { onChange(item.copy(endYear = it)) },
                errorMessage = dateError
            )
            OutlinedTextField(value = item.bullets, onValueChange = { onChange(item.copy(bullets = it)) }, label = { Text("Bullets * (one per line)") }, isError = bulletErr, minLines = 3, modifier = Modifier.fillMaxWidth())
            if (showErrors && (nameErr || bulletErr)) { Spacer(Modifier.height(6.dp)); Text("Missing required fields.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun EducationCard(index: Int, item: EducationEntry, submitAttempted: Boolean, showErrors: Boolean, onChange: (EducationEntry) -> Unit, onDelete: () -> Unit) {
    val degreeErr = showErrors && item.degree.trim().isEmpty()
    val uniErr = showErrors && item.university.trim().isEmpty()
    val dateError = if (submitAttempted) validateDates(item.startMonth, item.startYear, item.endMonth, item.endYear, allowFuture = true) else null

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Education #${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            OutlinedTextField(value = item.degree, onValueChange = { onChange(item.copy(degree = it)) }, label = { Text("Degree *") }, isError = degreeErr, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.university, onValueChange = { onChange(item.copy(university = it)) }, label = { Text("University *") }, isError = uniErr, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.location, onValueChange = { onChange(item.copy(location = it)) }, label = { Text("Location (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            MonthYearRangeInputs(
                startMonth = item.startMonth, startYear = item.startYear, endMonth = item.endMonth, endYear = item.endYear,
                onStartMonth = { onChange(item.copy(startMonth = it)) }, onStartYear = { onChange(item.copy(startYear = it)) },
                onEndMonth = { onChange(item.copy(endMonth = it)) }, onEndYear = { onChange(item.copy(endYear = it)) },
                errorMessage = dateError
            )
            OutlinedTextField(value = item.gpa, onValueChange = { onChange(item.copy(gpa = it)) }, label = { Text("GPA (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.coursework, onValueChange = { onChange(item.copy(coursework = it)) }, label = { Text("Coursework (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            if (showErrors && (degreeErr || uniErr)) { Spacer(Modifier.height(6.dp)); Text("Degree and university are required.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun CertCard(index: Int, item: CertificationEntry, onChange: (CertificationEntry) -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Certification #${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            OutlinedTextField(value = item.name, onValueChange = { onChange(item.copy(name = it)) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.issuer, onValueChange = { onChange(item.copy(issuer = it)) }, label = { Text("Issuer") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            MonthYearRangeInputs(
                startMonth = item.issueMonth, startYear = item.issueYear, endMonth = item.expiryMonth, endYear = item.expiryYear,
                onStartMonth = { onChange(item.copy(issueMonth = it)) }, onStartYear = { onChange(item.copy(issueYear = it)) },
                onEndMonth = { onChange(item.copy(expiryMonth = it)) }, onEndYear = { onChange(item.copy(expiryYear = it)) },
                startMonthLabel = "Issue month", startYearLabel = "Issue year", endMonthLabel = "Expiry month", endYearLabel = "Expiry year"
            )
            OutlinedTextField(value = item.url, onValueChange = { onChange(item.copy(url = it)) }, label = { Text("Credential URL") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AchievementCard(index: Int, item: AchievementEntry, onChange: (AchievementEntry) -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Achievement #${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            OutlinedTextField(value = item.title, onValueChange = { onChange(item.copy(title = it)) }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = item.detail, onValueChange = { onChange(item.copy(detail = it)) }, label = { Text("Details (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TemplatePreviewMini(id: String) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            if (id == "modern") {
                Box(modifier = Modifier.fillMaxWidth().height(22.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)))
                Spacer(Modifier.height(8.dp))
                Text("Centered header + photo", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp)); Divider(); Spacer(Modifier.height(6.dp))
                Text("Accent bars", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("John Doe", style = MaterialTheme.typography.titleSmall)
                Text("Backend Engineer", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp)); Divider(); Spacer(Modifier.height(6.dp))
                Text("Summary", style = MaterialTheme.typography.labelMedium)
                Text("FastAPI • Docker • PostgreSQL", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text("Single column ATS", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------------------------
// Helpers
// ---------------------------
private fun formatMonthYear(month: String, year: String): String {
    val m = month.trim()
    val y = year.trim()
    if (m.equals("Present", ignoreCase = true) || y.equals("Present", ignoreCase = true)) return "Present"
    if (y.isEmpty()) return m
    if (m.isEmpty()) return y
    return "$m $y"
}

private fun formatRange(startMonth: String, startYear: String, endMonth: String, endYear: String): String {
    val start = formatMonthYear(startMonth, startYear)
    val end = formatMonthYear(endMonth, endYear)
    return when {
        start.isNotEmpty() && end.isNotEmpty() -> "$start – $end"
        start.isNotEmpty() -> start
        end.isNotEmpty() -> end
        else -> ""
    }
}

private fun hasAnyLink(l: Links): Boolean = l.linkedin.isNotBlank() || l.github.isNotBlank() || l.portfolio.isNotBlank() || l.other.isNotBlank()

private fun bulletsToList(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { if (it.startsWith("•")) it.removePrefix("•").trim() else it }

private fun flattenSkills(d: Draft): List<String> = (d.skillsLanguages + d.skillsFrameworks + d.skillsDatabases + d.skillsCloud + d.skillsAI + d.skillsTools + d.skillsOther).map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase(Locale.getDefault()) }

private fun updateSkillCategory(d: Draft, label: String, newList: List<String>): Draft {
    return when (label) {
        "Languages" -> d.copy(skillsLanguages = newList); "Frameworks" -> d.copy(skillsFrameworks = newList)
        "Databases" -> d.copy(skillsDatabases = newList); "Cloud/DevOps" -> d.copy(skillsCloud = newList)
        "ML/AI" -> d.copy(skillsAI = newList); "Tools" -> d.copy(skillsTools = newList)
        "Other" -> d.copy(skillsOther = newList); else -> d
    }
}

private fun isContentReady(d: Draft): Boolean = d.experience.any { it.company.trim().isNotEmpty() && it.role.trim().isNotEmpty() && it.startYear.trim().isNotEmpty() && bulletsToList(it.bullets).isNotEmpty() } || d.projects.any { it.name.trim().isNotEmpty() && bulletsToList(it.bullets).isNotEmpty() }

private fun firstMissingPanel(d: Draft): EditPanel? {
    if (d.jdText.trim().isEmpty()) return EditPanel.JOB
    if (d.firstName.trim().isEmpty() || d.lastName.trim().isEmpty() || d.targetRole.trim().isEmpty() || d.location.trim().isEmpty()) return EditPanel.ABOUT

    // Auto-navigate to panel with validation errors
    if (d.email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(d.email).matches()) return EditPanel.ABOUT
    if (d.phone.isNotBlank() && !Patterns.PHONE.matcher(d.phone).matches()) return EditPanel.ABOUT

    if (flattenSkills(d).isEmpty()) return EditPanel.SKILLS
    if (!isContentReady(d)) return EditPanel.PROJECTS

    for (e in d.experience) { if (validateDates(e.startMonth, e.startYear, e.endMonth, e.endYear, false) != null) return EditPanel.EXPERIENCE }
    for (p in d.projects) { if (validateDates(p.startMonth, p.startYear, p.endMonth, p.endYear, false) != null) return EditPanel.PROJECTS }
    for (ed in d.education) { if (validateDates(ed.startMonth, ed.startYear, ed.endMonth, ed.endYear, true) != null) return EditPanel.EDUCATION }

    if (!d.education.any { it.degree.trim().isNotEmpty() && it.university.trim().isNotEmpty() && (it.startYear.trim().isNotEmpty() || it.endYear.trim().isNotEmpty()) }) return EditPanel.EDUCATION
    return null
}

private fun buildExperienceText(d: Draft): String {
    val sb = StringBuilder()
    d.experience.forEach { e ->
        if (e.company.isBlank() && e.role.isBlank()) return@forEach
        sb.append("${e.company} — ${e.role}")
        if (e.location.trim().isNotEmpty()) sb.append(" (${e.location.trim()})")
        val dates = formatRange(e.startMonth, e.startYear, e.endMonth, e.endYear)
        if (dates.isNotEmpty()) sb.append(" | $dates")
        sb.append("\n")
        bulletsToList(e.bullets).forEach { b -> sb.append("• $b\n") }
        if (e.tech.trim().isNotEmpty()) sb.append("Tech: ${e.tech.trim()}\n")
        sb.append("\n")
    }
    return sb.toString().trim()
}

private fun buildProjectsText(d: Draft): String {
    val sb = StringBuilder()
    d.projects.forEach { p ->
        if (p.name.isBlank()) return@forEach
        sb.append(p.name.trim())
        if (p.tagline.trim().isNotEmpty()) sb.append(" — ${p.tagline.trim()}")
        val dates = formatRange(p.startMonth, p.startYear, p.endMonth, p.endYear)
        if (dates.isNotEmpty()) sb.append(" | $dates")
        sb.append("\n")
        if (p.link.trim().isNotEmpty()) sb.append("Link: ${p.link.trim()}\n")
        bulletsToList(p.bullets).forEach { b -> sb.append("• $b\n") }
        if (p.tech.trim().isNotEmpty()) sb.append("Tech: ${p.tech.trim()}\n")
        sb.append("\n")
    }
    return sb.toString().trim()
}

private fun buildEducationText(d: Draft): String {
    val sb = StringBuilder()
    d.education.forEach { e ->
        if (e.degree.isBlank() && e.university.isBlank()) return@forEach
        sb.append("${e.degree} — ${e.university}")
        if (e.location.trim().isNotEmpty()) sb.append(" (${e.location.trim()})")
        val dates = formatRange(e.startMonth, e.startYear, e.endMonth, e.endYear)
        if (dates.isNotEmpty()) sb.append(" | $dates")
        sb.append("\n")
        if (e.gpa.trim().isNotEmpty()) sb.append("GPA: ${e.gpa.trim()}\n")
        if (e.coursework.trim().isNotEmpty()) sb.append("Coursework: ${e.coursework.trim()}\n")
        sb.append("\n")
    }
    return sb.toString().trim()
}

private fun buildExtrasText(d: Draft): String {
    val sb = StringBuilder()
    if (d.certs.isNotEmpty()) {
        sb.append("Certifications:\n")
        d.certs.forEach { c ->
            val dates = formatRange(c.issueMonth, c.issueYear, c.expiryMonth, c.expiryYear)
            val line = listOf(c.name.trim(), c.issuer.trim(), dates).filter { it.isNotEmpty() }.joinToString(" — ")
            if (line.isNotEmpty()) sb.append("• $line\n")
            if (c.url.trim().isNotEmpty()) sb.append("  ${c.url.trim()}\n")
        }
        sb.append("\n")
    }
    if (d.achievements.isNotEmpty()) {
        sb.append("Achievements:\n")
        d.achievements.forEach { a ->
            if (a.title.trim().isNotEmpty()) { sb.append("• ${a.title.trim()}\n"); if (a.detail.trim().isNotEmpty()) sb.append("  ${a.detail.trim()}\n") }
        }
        sb.append("\n")
    }
    if (hasAnyLink(d.links)) {
        sb.append("Links:\n")
        if (d.links.linkedin.trim().isNotEmpty()) sb.append("• LinkedIn: ${d.links.linkedin.trim()}\n")
        if (d.links.github.trim().isNotEmpty()) sb.append("• GitHub: ${d.links.github.trim()}\n")
        if (d.links.portfolio.trim().isNotEmpty()) sb.append("• Portfolio: ${d.links.portfolio.trim()}\n")
        if (d.links.other.trim().isNotEmpty()) sb.append("• Other: ${d.links.other.trim()}\n")
    }
    return sb.toString().trim()
}

private fun readImageAsBase64(context: Context, uri: Uri): Pair<String, String>? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        if (originalBitmap == null) return null

        val size = kotlin.math.min(originalBitmap.width, originalBitmap.height)
        val xOffset = (originalBitmap.width - size) / 2
        val yOffset = (originalBitmap.height - size) / 2
        val squaredBitmap = Bitmap.createBitmap(originalBitmap, xOffset, yOffset, size, size)

        val scaledBitmap = if (size > 512) {
            Bitmap.createScaledBitmap(squaredBitmap, 512, 512, true)
        } else {
            squaredBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()

        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else "profile_image.jpg"
        } ?: "profile_image.jpg"

        Base64.encodeToString(bytes, Base64.NO_WRAP) to name
    } catch (_: Exception) { null }
}

private fun savePdf(context: Context, bytes: ByteArray): File {
    val out = File(File(context.cacheDir, "generated_pdfs").apply { mkdirs() }, "resume_${System.currentTimeMillis()}.pdf")
    FileOutputStream(out).use { it.write(bytes) }
    return out
}

private fun openPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val openIntent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        when {
            openIntent.resolveActivity(context.packageManager) != null -> { context.startActivity(Intent.createChooser(openIntent, "Open resume")); Toast.makeText(context, "PDF generated successfully", Toast.LENGTH_SHORT).show() }
            shareIntent.resolveActivity(context.packageManager) != null -> { context.startActivity(Intent.createChooser(shareIntent, "Share resume")); Toast.makeText(context, "No PDF viewer found.", Toast.LENGTH_LONG).show() }
            else -> Toast.makeText(context, "PDF saved at: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (_: Exception) { Toast.makeText(context, "PDF saved at: ${file.absolutePath}", Toast.LENGTH_LONG).show() }
}