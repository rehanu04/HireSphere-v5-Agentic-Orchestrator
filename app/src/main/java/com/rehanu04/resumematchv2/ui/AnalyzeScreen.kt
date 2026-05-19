@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

private const val MAX_MATCHED_CHIPS = 50
private const val MAX_MISSING_CHIPS = 50
private const val PREVIEW_CHIP_LIMIT = 12

// -----------------------------
// Models
// -----------------------------
private data class AnalyzeResult(
    val score: Int, val matchedCount: Int, val missingCount: Int,
    val matchedTop: List<String>, val missingTop: List<String>,
    val resumeTextLength: Int, val resumeText: String? = null
)

data class ProactiveAnalyzeRequest(@SerializedName("vault_data") val vaultData: String, @SerializedName("job_description") val jobDescription: String)
data class ProactiveAnalyzeResponse(@SerializedName("ats_score") val atsScore: Int, @SerializedName("missing_skills") val missingSkills: List<String>, @SerializedName("is_intervened") val isIntervened: Boolean, @SerializedName("agent_message") val agentMessage: String)

private val KNOWN_SKILLS: Map<String, String> = linkedMapOf(
    "python" to "Python", "kotlin" to "Kotlin", "java" to "Java", "javascript" to "JavaScript",
    "typescript" to "TypeScript", "sql" to "SQL", "bash" to "Bash", "c" to "C", "cpp" to "C++",
    "csharp" to "C#", "go" to "Go", "rust" to "Rust", "fastapi" to "FastAPI", "uvicorn" to "Uvicorn",
    "flask" to "Flask", "django" to "Django", "node" to "Node.js", "node.js" to "Node.js",
    "express" to "Express", "spring" to "Spring", "spring boot" to "Spring Boot", "rest" to "REST",
    "rest apis" to "REST APIs", "http" to "HTTP", "openapi" to "OpenAPI", "swagger" to "Swagger",
    "swagger openapi" to "Swagger/OpenAPI", "grpc" to "gRPC", "postman" to "Postman",
    "render" to "Render", "regex" to "Regex", "text normalization" to "Text Normalization",
    "normalization" to "Normalization", "pypdf" to "PyPDF", "multipart" to "Multipart",
    "async" to "Async", "concurrency" to "Concurrency", "android" to "Android", "okhttp" to "OkHttp",
    "retrofit" to "Retrofit", "coroutines" to "Coroutines", "jetpack compose" to "Jetpack Compose",
    "compose" to "Jetpack Compose", "postgresql" to "PostgreSQL", "postgres" to "PostgreSQL",
    "mysql" to "MySQL", "mongodb" to "MongoDB", "redis" to "Redis", "docker" to "Docker",
    "kubernetes" to "Kubernetes", "k8s" to "Kubernetes", "terraform" to "Terraform",
    "linux" to "Linux", "aws" to "AWS", "azure" to "Azure", "gcp" to "GCP",
    "google cloud" to "Google Cloud", "google cloud platform" to "Google Cloud Platform",
    "ci/cd" to "CI/CD", "github actions" to "GitHub Actions", "machine learning" to "Machine Learning",
    "deep learning" to "Deep Learning", "nlp" to "NLP", "ai" to "AI", "ml" to "ML",
    "ai/ml" to "AI/ML", "distributed systems" to "Distributed Systems", "system design" to "System Design",
    "data structures" to "Data Structures", "algorithms" to "Algorithms",
    "multithreaded programming" to "Multithreaded Programming", "virtualization" to "Virtualization"
)

private val ALIASES: Map<String, String> = mapOf(
    "c++" to "cpp", "cplusplus" to "cpp", "c#" to "csharp", ".net" to "csharp", "dotnet" to "csharp",
    "nodejs" to "node.js", "open api" to "swagger openapi", "openapi" to "swagger openapi",
    "swagger" to "swagger openapi", "swagger/openapi" to "swagger openapi",
    "swaggeropenapi" to "swagger openapi", "rest api" to "rest", "rest apis" to "rest",
    "restful" to "rest", "ok http" to "okhttp", "git hub" to "github", "k8s" to "kubernetes",
    "ci cd" to "ci/cd", "cicd" to "ci/cd", "text normalization" to "normalization",
    "ai ml" to "ai/ml", "aiml" to "ai/ml"
)

private val STOPWORDS = setOf(
    "the","and","or","to","of","in","for","with","on","at","as","a","an","by","from","is","are","was","were",
    "this","that","these","those","it","its","be","been","being","will","would","should","can","could","may","might",
    "role","about","job","description","responsibilities","requirements","required","preferred","qualification","qualifications",
    "experience","strong","ability","skills","skill","knowledge","familiarity","hands","work","working","team","teams",
    "collaborate","collaboration","collaborative","communication","well","documented","clean","efficient","design","develop",
    "build","optimize","scalable","systems","system","across","ensure","high","availability","reliability","performance",
    "projects","project","architecture","reviews","technical","documentation","plus","bonus"
)

private val SKILL_BLACKLIST = setOf(
    "backend", "engineer", "software", "title", "role", "deploy", "deployment", "implement",
    "implementation", "integration", "handling", "write", "build", "design", "develop",
    "optimize", "create", "request", "response", "requests", "responses", "api", "apis",
    "concepts", "nice", "required", "skills", "failures", "timeouts", "timeout", "failure",
    "analysis", "scoring", "documents", "document"
)

private fun isValidSkillCandidate(canonical: String): Boolean {
    val s = canonical.trim().lowercase()
    if (s.isBlank() || s in STOPWORDS || s in SKILL_BLACKLIST) return false
    val badPhrases = listOf("request response", "failures timeouts", "pdf documents", "okhttp http", "regex text", "python fastapi", "scoring analysis")
    if (badPhrases.contains(s)) return false
    if (s !in KNOWN_SKILLS.keys) {
        val hasTechPunct = s.contains("+") || s.contains("#") || s.contains(".") || s.contains("/")
        val isMultiWord = s.contains(" ")
        val longEnough = s.length >= 4
        if (!hasTechPunct && !isMultiWord && !longEnough) return false
        if (Regex("^[a-z]+$").matches(s) && s.length <= 10) return false
    }
    return true
}

private fun clamp0to100(v: Int): Int = v.coerceIn(0, 100)

private fun deriveAtsReadiness(resumeTextLength: Int, resumeText: String?, matchedCount: Int, missingCount: Int, fileName: String?): Pair<Int, String?> {
    val x = resumeTextLength.coerceAtLeast(0)
    val base = when {
        x < 600 -> 20; x < 1000 -> 35 + ((x - 600) / 12.0).roundToInt(); x < 1800 -> 55 + ((x - 1000) / 20.0).roundToInt(); x < 3200 -> 72 + ((x - 1800) / 60.0).roundToInt(); else -> 90
    }.coerceIn(0, 95)
    val (likelihood, _) = estimateResumeLikelihood(resumeText, x, matchedCount, missingCount, fileName)
    var ats = base
    val warning: String? = when {
        likelihood < 0.25f -> "This PDF doesn't look like a resume. Please upload a resume PDF."
        likelihood < 0.45f -> "Low resume confidence: results may be unreliable."
        x < 500 -> "Low extractable text detected. Export a text-based PDF for better ATS parsing."
        else -> null
    }
    if (likelihood < 0.25f) ats = (minOf(ats, 40) * (0.55f + 0.45f * likelihood)).roundToInt()
    else if (likelihood < 0.45f) ats = (minOf(ats, 65) * (0.70f + 0.30f * likelihood)).roundToInt()
    if (!fileName.isNullOrBlank() && fileName.lowercase().contains("resume") && warning != null) ats = (ats + 5).coerceAtMost(80)
    return clamp0to100(ats) to warning
}

private fun estimateResumeLikelihood(resumeText: String?, resumeTextLength: Int, matchedCount: Int, missingCount: Int, fileName: String?): Pair<Float, String?> {
    if (!resumeText.isNullOrBlank()) {
        val t = resumeText.lowercase()
        val hasEmail = Regex("\\b[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}\\b").containsMatchIn(t)
        val hasPhone = Regex("\\b(\\+?\\d{1,3}[- .]?)?(\\(?\\d{2,4}\\)?[- .]?)?\\d{3,4}[- .]?\\d{3,4}\\b").containsMatchIn(t)
        val hasLinkedIn = t.contains("linkedin.com") || t.contains("linkedin")
        val hasGithub = t.contains("github.com") || t.contains("github")
        val headers = listOf("experience", "education", "skills", "projects", "summary")
        val headerHits = headers.count { h -> Regex("\\b" + Regex.escape(h) + "\\b").containsMatchIn(t) }
        val bulletHits = Regex("(^|\\n)\\s*[•\\u2022\\-\\*]\\s+").findAll(t).take(10).count()
        var score = 0.0f
        if (hasEmail) score += 0.25f; if (hasPhone) score += 0.20f; if (hasLinkedIn) score += 0.10f; if (hasGithub) score += 0.10f
        score += (headerHits.coerceAtMost(4) * 0.10f); if (bulletHits >= 3) score += 0.10f
        return score.coerceIn(0.0f, 1.0f) to null
    }
    val denom = (matchedCount + missingCount).coerceAtLeast(1)
    val coverage = matchedCount.toFloat() / denom.toFloat()
    var likelihood = 0.60f
    if (resumeTextLength < 400) likelihood = 0.20f
    if (matchedCount <= 1 && missingCount >= 4 && coverage < 0.15f) likelihood = minOf(likelihood, 0.25f)
    return likelihood.coerceIn(0.0f, 1.0f) to null
}

private fun labelForScore(v: Int): String = when { v >= 80 -> "Strong"; v >= 60 -> "Good"; v >= 40 -> "Fair"; v >= 20 -> "Low"; else -> "Very Low" }

private fun parseAnalyzeJson(json: String): AnalyzeResult {
    val o = JSONObject(json)
    fun arrToList(a: org.json.JSONArray?): List<String> {
        if (a == null) return emptyList()
        val out = ArrayList<String>(a.length())
        for (i in 0 until a.length()) out.add(a.optString(i))
        return out
    }
    val resumeText = when { o.has("resume_text") -> o.optString("resume_text", null); o.has("resume_excerpt") -> o.optString("resume_excerpt", null); else -> null }
    return AnalyzeResult(o.optInt("score", 0), o.optInt("matched_count", 0), o.optInt("missing_count", 0), arrToList(o.optJSONArray("matched_top")), arrToList(o.optJSONArray("missing_top")), o.optInt("resume_text_length", 0), resumeText)
}

private fun looksLikeUrl(s: String): Boolean { val t = s.trim(); return t.startsWith("http://", true) || t.startsWith("https://", true) || t.startsWith("www.", true) }
private fun normalizeUrl(s: String): String { val t = s.trim(); return when { t.startsWith("http://", true) || t.startsWith("https://", true) -> t; t.startsWith("www.", true) -> "https://$t"; else -> t } }

private fun resolveJobDescriptionText(client: OkHttpClient, input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""
    if (!looksLikeUrl(trimmed)) return trimmed
    val url = normalizeUrl(trimmed)
    if (url.contains("linkedin.com", ignoreCase = true)) throw IllegalStateException("Cannot access LinkedIn securely. Please copy and paste the job description text directly.")

    val req = Request.Builder().url(url).get().addHeader("User-Agent", "ResumeMatchV2/1.0").build()
    client.newCall(req).execute().use { resp ->
        val body = resp.body?.string()
        if (!resp.isSuccessful || body.isNullOrBlank()) throw IllegalStateException("Could not fetch JD URL (HTTP ${resp.code}).")
        val ct = resp.header("Content-Type").orEmpty()
        val extracted = if (ct.contains("text/html", ignoreCase = true)) htmlToReadableText(body) else body
        return if (extracted.length < 200) body else extracted
    }
}

private fun isLikelyJobDescription(text: String): Boolean {
    if (text.trim().length < 50) return true
    val t = text.lowercase()
    val errorWords = listOf("exception", "build failed", "stacktrace", "compilation error")
    val jdWords = listOf("experience", "requirement", "responsibility", "qualification", "skill", "role", "job")
    if (errorWords.count { t.contains(it) } > 0 && jdWords.count { t.contains(it) } < 2) return false
    return true
}

private fun htmlToReadableText(html: String): String {
    var s = html
    s = s.replace(Regex("(?is)<script.*?>.*?</script>"), " ").replace(Regex("(?is)<style.*?>.*?</style>"), " ")
    s = s.replace(Regex("(?is)<br\\s*/?>"), "\n").replace(Regex("(?is)</p\\s*>"), "\n").replace(Regex("(?is)</li\\s*>"), "\n").replace(Regex("(?is)<li\\s*>"), " • ").replace(Regex("(?is)<[^>]+>"), " ")
    return s.replace("&nbsp;", " ").replace(Regex("[\\t\\r]+"), " ").replace(Regex("\\s+"), " ").trim()
}

private fun getDisplayName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    cursor.use { val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (it.moveToFirst() && nameIndex >= 0) return it.getString(nameIndex) }
    return null
}

private suspend fun copyContentUriToTempFile(context: Context, uri: Uri): File {
    return withContext(Dispatchers.IO) {
        val tmp = File.createTempFile("resume_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input -> FileOutputStream(tmp).use { output -> if (input != null) input.copyTo(output) } }
        tmp
    }
}

private fun canonicalize(raw: String): String {
    val t = raw.trim().lowercase()
    if (t.isBlank()) return ""
    val cleaned = t.replace("\u00A0", " ").replace("/", " ").replace("_", " ").replace("-", " ").replace(Regex("[()\\[\\]{}:;,\\.\\|]"), " ").replace(Regex("\\s+"), " ").trim()
    val direct = when (cleaned) { "c++" -> "cpp"; "c#" -> "csharp"; else -> cleaned }
    return ALIASES[direct] ?: ALIASES[direct.replace(" ", "")] ?: direct
}

private fun expandCompositeSkills(canonical: String): List<String> {
    val s = canonical.trim()
    if (s.isBlank()) return emptyList()
    if (s in KNOWN_SKILLS.keys) return listOf(s)
    val tokens = s.split(" ").filter { it.isNotBlank() }
    if (tokens.size <= 1) return listOf(s)
    val out = LinkedHashSet<String>()
    for (t in tokens) if (t in KNOWN_SKILLS.keys) out.add(t)
    for (i in 0 until tokens.size - 1) { val bi = tokens[i] + " " + tokens[i + 1]; if (bi in KNOWN_SKILLS.keys) out.add(bi) }
    for (i in 0 until tokens.size - 2) { val tri = tokens[i] + " " + tokens[i + 1] + " " + tokens[i + 2]; if (tri in KNOWN_SKILLS.keys) out.add(tri) }
    return if (out.isNotEmpty()) out.toList() else listOf(s)
}

private fun prettySkill(canonical: String): String {
    val k = canonical.trim().lowercase()
    return KNOWN_SKILLS[k] ?: k.split(" ").joinToString(" ") { part -> if (part.length <= 2) part.uppercase() else part.replaceFirstChar { it.uppercase() } }
}

private fun extractSkillsGlobal(text: String): Set<String> {
    if (text.isBlank()) return emptySet()
    val norm = text.replace("\u00A0", " ").replace(Regex("[•·]"), " ").replace(Regex("\\s+"), " ").trim()
    val lowered = norm.lowercase()
    val found = LinkedHashSet<String>()
    val dictKeys = KNOWN_SKILLS.keys.sortedByDescending { it.length }

    for (k in dictKeys) {
        val canonical = canonicalize(k)
        if (canonical.isBlank()) continue
        if (k == "ci/cd") { if (lowered.contains("ci/cd") || lowered.contains("ci cd") || lowered.contains("cicd")) found.add("ci/cd"); continue }
        if (k == "node.js") { if (lowered.contains("node.js") || lowered.contains("nodejs")) found.add("node.js"); continue }
        if (Regex("\\b" + Regex.escape(k) + "\\b", RegexOption.IGNORE_CASE).containsMatchIn(lowered)) found.add(canonical)
    }

    val words = lowered.replace(Regex("[^a-z0-9+/# ]"), " ").replace(Regex("\\s+"), " ").trim().split(" ").filter { it.isNotBlank() }
    val valid = words.filter { w -> w.length >= 2 && w !in STOPWORDS && !w.all { it.isDigit() } }
    for (i in 0 until valid.size) {
        if (i + 1 < valid.size) {
            val bi = "${valid[i]} ${valid[i + 1]}"; val c = canonicalize(bi)
            if (c in KNOWN_SKILLS.keys) found.add(c)
        }
    }
    return found.map { canonicalize(it) }.filter { isValidSkillCandidate(it) }.toLinkedHashSet()
}

private fun <T> Iterable<T>.toLinkedHashSet(): LinkedHashSet<T> { val s = LinkedHashSet<T>(); for (x in this) s.add(x); return s }

private val SKILL_PRIORITY: Map<String, Int> = run { val ordered = listOf("python", "fastapi", "uvicorn", "rest", "swagger openapi", "okhttp", "docker", "render", "postman", "android", "coroutines"); ordered.withIndex().associate { it.value to it.index } }
private fun skillPriority(canonical: String): Int = SKILL_PRIORITY[canonical.trim().lowercase()] ?: 10_000

private fun applyInference(matched: MutableSet<String>, missing: Set<String>) {
    fun addIfOk(k: String) { val c = canonicalize(k); if (c.isNotBlank() && c !in missing) matched.add(c) }
    val m = matched.map { it.lowercase() }.toSet()
    if ("fastapi" in m || "uvicorn" in m) addIfOk("python")
    if ("openapi" in m || "swagger" in m) addIfOk("swagger openapi")
    if ("coroutines" in m) { addIfOk("async"); addIfOk("concurrency") }
}

private enum class Bucket { LANG, API, ANDROID, CLOUD, AIML, SYSTEMS, OTHER }
private fun bucketOf(s: String): Bucket {
    val low = s.lowercase()
    return when {
        low in listOf("python","java","kotlin","c","cpp","csharp","sql","javascript","typescript","go","rust","bash") -> Bucket.LANG
        low.contains("fastapi") || low.contains("uvicorn") || low.contains("rest") || low.contains("openapi") || low.contains("http") -> Bucket.API
        low.contains("jetpack") || low.contains("compose") || low.contains("retrofit") || low.contains("okhttp") || low.contains("coroutines") -> Bucket.ANDROID
        low.contains("docker") || low.contains("kubernetes") || low.contains("aws") || low.contains("gcp") || low.contains("azure") || low.contains("ci/cd") -> Bucket.CLOUD
        low.contains("machine learning") || low == "ai" || low == "ml" || low.contains("ai/ml") -> Bucket.AIML
        low.contains("distributed") || low.contains("system design") || low.contains("data structures") -> Bucket.SYSTEMS
        else -> Bucket.OTHER
    }
}

private fun bucketLabel(b: Bucket): String = when (b) { Bucket.LANG -> "Languages"; Bucket.API -> "Backend / APIs"; Bucket.ANDROID -> "Android"; Bucket.CLOUD -> "Cloud / DevOps"; Bucket.AIML -> "AI/ML"; Bucket.SYSTEMS -> "Systems"; Bucket.OTHER -> "Other" }

private fun buildStrongSuggestions(jobScore: Int, atsScore: Int, matched: List<String>, missing: List<String>): List<String> {
    val out = mutableListOf<String>()
    out.add("Tailor your Summary to the JD: target role + 2 strengths + 3 JD keywords.")
    if (jobScore < 40) out.add("Job match is low: add 2 JD-aligned bullets in Projects/Experience explicitly mentioning top missing skills.")
    if (missing.isNotEmpty()) out.add("Top missing skills to address first: ${missing.take(6).joinToString(", ")}.")
    out.add("Checklist: (1) Summary has 3 JD keywords, (2) Skills section has top relevant skills, (3) 2 bullets prove those skills with metrics.")
    return out.distinct()
}

// -----------------------------
// UI - AnalyzeScreen
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onBack: () -> Unit,
    onGoCreate: () -> Unit,
    onGoProfile: () -> Unit,
    apiBaseUrl: String,
    apiAppKey: String,
    userProfileStore: com.rehanu04.resumematchv2.data.UserProfileStore
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())
    val vaultData = remember(userProfile) { "Skills: ${userProfile.savedSkillsJson}\nExperience: ${userProfile.savedExperienceJson}\nProjects: ${userProfile.savedProjectsJson}" }

    var jdInput by rememberSaveable { mutableStateOf("") }
    var selectedPdfUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedPdfName by rememberSaveable { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(false) }
    var rawJson by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var proactiveLoading by remember { mutableStateOf(false) }
    var proactiveResult by remember { mutableStateOf<ProactiveAnalyzeResponse?>(null) }
    var jobAnalyzed by rememberSaveable { mutableStateOf(false) }

    var jobSkillsAfterClick by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var jobSkillKeysAfterClick by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var lastEffectiveJdText by rememberSaveable { mutableStateOf<String?>(null) }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { selectedPdfUri = uri; selectedPdfName = getDisplayName(ctx, uri) ?: "selected.pdf" }
    }

    val jdText = jdInput.trim()
    fun buttonEnabled(): Boolean = !loading && !proactiveLoading && jdText.isNotBlank() && apiBaseUrl.isNotBlank()

    // --- MONOCHROME THEME COLORS ---
    val bgColor = if (isDark) Color(0xFF030303) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    val animatedCoreColor by animateColorAsState(targetValue = if (isDark) Color.White else Color.Black, animationSpec = tween(800))
    val animatedCardBgColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF),
        animationSpec = tween(800)
    )
    val cardTextColor = if (isDark) Color.White else Color.Black
    val cardSubTextColor = if (isDark) Color.LightGray else Color.DarkGray

    val primaryBtnBg = if (isDark) Color(0xFF222222) else Color(0xFFE2E8F0)
    val primaryBtnText = if (isDark) Color.White else Color.Black

    fun runProactiveAnalyze() {
        errorText = null; proactiveResult = null; rawJson = null; jobAnalyzed = false
        if (jdText.isBlank()) return
        proactiveLoading = true

        scope.launch {
            try {
                val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
                val requestObj = ProactiveAnalyzeRequest(vaultData, jdText)
                val body = Gson().toJson(requestObj).toRequestBody("application/json".toMediaType())

                val reqBuilder = Request.Builder().url(apiBaseUrl.trimEnd('/') + "/v1/ai/analyze-proactive").post(body)
                if (apiAppKey.isNotBlank()) reqBuilder.addHeader("x-app-key", apiAppKey)

                val response = withContext(Dispatchers.IO) { client.newCall(reqBuilder.build()).execute() }
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    proactiveResult = Gson().fromJson(responseBody, ProactiveAnalyzeResponse::class.java)
                } else errorText = "Analysis Failed: HTTP ${response.code}\n${responseBody ?: ""}"
            } catch (e: Exception) { errorText = "Network Error: ${e.message}" } finally { proactiveLoading = false }
        }
    }

    fun runAnalyze() {
        errorText = null; rawJson = null; jobAnalyzed = false; proactiveResult = null
        if (jdText.isBlank()) return
        loading = true

        scope.launch {
            try {
                val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
                val effectiveJd = withContext(Dispatchers.IO) { resolveJobDescriptionText(client, jdText) }
                lastEffectiveJdText = effectiveJd

                if (!isLikelyJobDescription(effectiveJd)) {
                    errorText = "The provided text does not look like a Job Description. Please paste a valid JD."
                    loading = false; return@launch
                }

                val uri = selectedPdfUri
                if (uri == null) {
                    val jdKeys = extractSkillsGlobal(effectiveJd).asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>())
                    jobSkillKeysAfterClick = jdKeys.toList()
                    jobSkillsAfterClick = jdKeys.toList().sortedWith(compareBy({ skillPriority(it) }, { it })).map { prettySkill(it) }.distinctBy { it.lowercase() }.take(MAX_MATCHED_CHIPS)
                    jobAnalyzed = true; loading = false; return@launch
                }

                val tmp = copyContentUriToTempFile(ctx, uri)
                val body = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("resume", tmp.name, tmp.asRequestBody("application/pdf".toMediaType())).addFormDataPart("jd_text", effectiveJd).addFormDataPart("debug", "true").build()

                val reqBuilder = Request.Builder().url(apiBaseUrl.trimEnd('/') + "/v1/analyze/pdf").post(body)
                if (apiAppKey.isNotBlank()) reqBuilder.addHeader("x-app-key", apiAppKey)

                val resp = withContext(Dispatchers.IO) { client.newCall(reqBuilder.build()).execute() }
                val txt = resp.body?.string()

                if (!resp.isSuccessful || txt == null) errorText = "HTTP ${resp.code}: ${txt ?: "No response"}" else rawJson = txt
            } catch (e: Exception) { errorText = e.message ?: "Unknown error" } finally { loading = false }
        }
    }

    // --- MAIN UI WRAPPER ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor) // Base color for the entire app
    ) {
        // LAYER 1: The Canvas is perfectly behind everything
        DiagonalFlowingLinesCanvas(isDark = isDark)

        // LAYER 2: Main Layout
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding()
        ) {
            // THE GLASS HEADER (Transparent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 80.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor) }
                Text("Analyze", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.weight(1f).padding(start = 4.dp))
                IconButton(onClick = onGoProfile) { Icon(Icons.Filled.Person, contentDescription = "AI Profile", tint = textColor) }
            }

            // THE SCROLLABLE AREA (Clipped perfectly at the top edge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!userProfile.isComplete) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onGoProfile() },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = animatedCardBgColor),
                            border = BorderStroke(1.dp, animatedCoreColor.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Info, contentDescription = "Info", tint = animatedCoreColor)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Set up your AI Profile", fontWeight = FontWeight.Bold, color = cardTextColor)
                                    Text("Tap here to save your details to the Master Vault.", style = MaterialTheme.typography.bodySmall, color = cardSubTextColor)
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = animatedCardBgColor),
                        border = BorderStroke(1.dp, animatedCoreColor.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Resume (PDF)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cardTextColor)
                            Button(
                                onClick = { pickPdf.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryBtnBg)
                            ) { Text(if (selectedPdfUri == null) "Select PDF" else "Change PDF", color = primaryBtnText, fontWeight = FontWeight.Bold) }
                            Text(text = "Selected: ${selectedPdfName ?: "None"}", style = MaterialTheme.typography.bodySmall, color = cardSubTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (selectedPdfUri != null) {
                                TextButton(onClick = { selectedPdfUri = null; selectedPdfName = null; rawJson = null; errorText = null; proactiveResult = null }) { Text("Clear selected PDF", color = animatedCoreColor) }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = animatedCardBgColor),
                        border = BorderStroke(1.dp, animatedCoreColor.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Job description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cardTextColor)
                            OutlinedTextField(
                                value = jdInput, onValueChange = { jdInput = it },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp),
                                placeholder = { Text("Paste JD URL or JD text", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedBorderColor = animatedCoreColor,
                                    unfocusedBorderColor = animatedCoreColor.copy(alpha = 0.3f),
                                    focusedTextColor = cardTextColor
                                )
                            )
                        }
                    }

                    // TINY SHOOTING STAR BUTTON 1
                    Button(
                        onClick = { runProactiveAnalyze() },
                        enabled = buttonEnabled(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .glowingBorderTrail(proactiveLoading, animatedCoreColor),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBtnBg,
                            disabledContainerColor = primaryBtnBg.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, animatedCoreColor.copy(alpha = 0.3f))
                    ) {
                        if (proactiveLoading) {
                            Text("Agent Analyzing", color = primaryBtnText, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(18.dp), tint = primaryBtnText)
                            Spacer(Modifier.width(8.dp))
                            Text("Proactive Analyze Vault", color = primaryBtnText, fontWeight = FontWeight.Bold)
                        }
                    }

                    // TINY SHOOTING STAR BUTTON 2
                    Button(
                        onClick = { runAnalyze() },
                        enabled = buttonEnabled() && !proactiveLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .glowingBorderTrail(loading, animatedCoreColor),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBtnBg,
                            disabledContainerColor = primaryBtnBg.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, animatedCoreColor.copy(alpha = 0.3f))
                    ) {
                        if (loading) {
                            Text("Analyzing PDF", color = primaryBtnText, fontWeight = FontWeight.Bold)
                        } else {
                            Text(if (selectedPdfUri == null) "Analyze Job" else "Analyze PDF Match", color = primaryBtnText, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = onGoCreate, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Don’t have a resume? Create one tailored for this job →", color = cardSubTextColor) }

                    if (errorText != null) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1010).copy(alpha = 0.8f))) {
                            Column(modifier = Modifier.padding(16.dp)) { Text("Error", fontWeight = FontWeight.Bold, color = Color.White); Spacer(Modifier.height(6.dp)); Text(errorText ?: "", color = Color.White) }
                        }
                    }

                    if (proactiveResult != null) {
                        val result = proactiveResult!!
                        AnimatedVisibility(visible = result.isIntervened, enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A1010).copy(alpha = 0.8f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Agent", tint = Color(0xFFEF4444))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Agent Intervention", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                    }
                                    Text(result.agentMessage, style = MaterialTheme.typography.bodyLarge, color = Color.White, modifier = Modifier.padding(bottom = 16.dp))
                                    Button(onClick = onGoProfile, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                                        Text("Update Master Vault Now", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = animatedCardBgColor),
                            border = BorderStroke(1.dp, animatedCoreColor.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Vault ATS Score: ${result.atsScore}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (result.atsScore >= 70) animatedCoreColor else Color(0xFFEF4444)
                                )
                                if (result.missingSkills.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Missing Skills:", fontWeight = FontWeight.Bold, color = cardSubTextColor)
                                    result.missingSkills.forEach { skill -> Text("• $skill", style = MaterialTheme.typography.bodyMedium, color = cardTextColor) }
                                }
                            }
                        }
                    }

                    if (selectedPdfUri == null && jobAnalyzed && jdText.isNotBlank()) {
                        JobOnlyResultsCard(skills = jobSkillsAfterClick, coreColor = animatedCoreColor, cardBgColor = animatedCardBgColor, cardTextColor = cardTextColor, cardSubTextColor = cardSubTextColor)
                        JobOnlySuggestionsCard(skills = jobSkillsAfterClick, coreColor = animatedCoreColor, cardBgColor = animatedCardBgColor, cardTextColor = cardTextColor, cardSubTextColor = cardSubTextColor)
                    }

                    val parsed = remember(rawJson) { rawJson?.let { runCatching { parseAnalyzeJson(it) }.getOrNull() } }

                    if (parsed != null) {
                        val jobMatch = clamp0to100(parsed.score)
                        val (ats, resumeWarn) = deriveAtsReadiness(parsed.resumeTextLength, parsed.resumeText, parsed.matchedCount, parsed.missingCount, selectedPdfName)
                        val resumeLikelihood = estimateResumeLikelihood(parsed.resumeText, parsed.resumeTextLength, parsed.matchedCount, parsed.missingCount, selectedPdfName).first
                        val effectiveJdUsed = (lastEffectiveJdText ?: jdText).trim()
                        val jdKeysUsed = if (jobSkillKeysAfterClick.isNotEmpty()) { jobSkillKeysAfterClick.asSequence().map { canonicalize(it) }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>()) } else { extractSkillsGlobal(effectiveJdUsed).asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>()) }
                        val backendMatchedKeys = parsed.matchedTop.asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>())
                        val backendMissingKeys = parsed.missingTop.asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>())
                        val missingTarget = parsed.missingCount.coerceAtLeast(0)
                        val missingFromBackend = LinkedHashSet<String>(backendMissingKeys)
                        val missingDesiredSize = when { parsed.missingTop.isNotEmpty() && backendMissingKeys.isEmpty() -> 0 else -> maxOf(missingTarget, backendMissingKeys.size) }
                        val resumeEvidenceKeys = LinkedHashSet<String>().apply { addAll(backendMatchedKeys); if (!parsed.resumeText.isNullOrBlank()) { val extra = extractSkillsGlobal(parsed.resumeText).asSequence().map { canonicalize(it) }.flatMap { expandCompositeSkills(it).asSequence() }.filter { isValidSkillCandidate(it) }.toCollection(LinkedHashSet<String>()); addAll(extra) } }
                        val derivedMissingCandidates: List<String> = if (resumeEvidenceKeys.isNotEmpty()) { jdKeysUsed.filter { it !in resumeEvidenceKeys } } else { jdKeysUsed.filter { it !in backendMatchedKeys } }
                        val finalMissingKeys: LinkedHashSet<String> = LinkedHashSet<String>().apply { addAll(backendMissingKeys); if (missingDesiredSize > 0) { for (k in derivedMissingCandidates) { if (size >= missingDesiredSize) break; add(k) }; if (size < missingDesiredSize) { for (k in jdKeysUsed) { if (size >= missingDesiredSize) break; if (k !in backendMatchedKeys) add(k) } } } }
                        val finalMatchedKeys = LinkedHashSet<String>().apply { for (k in jdKeysUsed) if (k !in finalMissingKeys) add(k); if (jdKeysUsed.isEmpty()) { addAll(backendMatchedKeys) } else { for (k in backendMatchedKeys) if (k in jdKeysUsed) add(k) } }
                        applyInference(finalMatchedKeys, missingFromBackend)
                        for (k in finalMatchedKeys) { if (k !in missingFromBackend) finalMissingKeys.remove(k) }
                        val matched = finalMatchedKeys.toList().sortedWith(compareBy({ skillPriority(it) }, { it })).map { prettySkill(it) }.distinctBy { it.lowercase() }.take(MAX_MATCHED_CHIPS)
                        val missing = finalMissingKeys.toList().sortedWith(compareBy({ skillPriority(it) }, { it })).map { prettySkill(it) }.distinctBy { it.lowercase() }.take(MAX_MISSING_CHIPS)

                        ResultsCard(atsScore = ats, jobScore = jobMatch, resumeWarning = resumeWarn, resumeLikelihood = resumeLikelihood, jdTotalCount = jdKeysUsed.size, matchedCount = parsed.matchedCount, missingCount = parsed.missingCount, matchedTopCount = parsed.matchedTop.size, missingTopCount = parsed.missingTop.size, matched = matched, missing = missing, coreColor = animatedCoreColor, cardBgColor = animatedCardBgColor, cardTextColor = cardTextColor, cardSubTextColor = cardSubTextColor)
                        SuggestionsCard(jobScore = jobMatch, atsScore = ats, matched = matched, missing = missing, coreColor = animatedCoreColor, cardBgColor = animatedCardBgColor, cardTextColor = cardTextColor, cardSubTextColor = cardSubTextColor)
                    } else if (rawJson != null) {
                        val maybeErr = runCatching { JSONObject(rawJson!!).optString("detail") }.getOrNull()
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = animatedCardBgColor)) {
                            Column(modifier = Modifier.padding(16.dp)) { Text("Response received", fontWeight = FontWeight.Bold, color = cardTextColor); Spacer(Modifier.height(8.dp)); Text(text = if (!maybeErr.isNullOrBlank()) "Backend error: $maybeErr" else "Could not parse the backend response.", style = MaterialTheme.typography.bodyMedium, color = cardSubTextColor) }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        // LAYER 3: Floating Pull Chain overlay
        AnalyzePullChain(isDark = isDark, onToggleTheme = onToggleTheme)
    }
}

// -----------------------------
// PERFECT DIAGONAL DATA STREAM
// -----------------------------
@Composable
fun DiagonalFlowingLinesCanvas(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )

    val lineColor = if (isDark) Color.White else Color.Black

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val gapX = 140.dp.toPx()
        val shiftX = phase * (gapX * 4f)
        val linesCount = (w / gapX).toInt() + 6

        for (i in -4 .. linesCount) {
            val startX = i * gapX + shiftX - w * 0.2f
            val startY = -h * 0.2f

            val endX = startX + w * 0.8f
            val endY = h * 1.2f

            val cp1x = startX + w * 0.4f
            val cp1y = h * 0.2f
            val cp2x = endX - w * 0.4f
            val cp2y = h * 0.8f

            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(cp1x, cp1y, cp2x, cp2y, endX, endY)
            }

            val lineIndex = (i + 1000) % 4
            val strokeWidth = when (lineIndex) {
                0 -> 1.5.dp.toPx()
                1 -> 2.dp.toPx()
                2 -> 1.5f.dp.toPx()
                else -> 1.dp.toPx()
            }
            val alpha = when (lineIndex) {
                0 -> 0.15f
                1 -> 0.25f
                2 -> 0.20f
                else -> 0.12f
            }

            drawPath(
                path = path,
                color = lineColor.copy(alpha = alpha),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

// -----------------------------
// TINY CLOCKWISE SHOOTING STAR TRAIL
// -----------------------------
fun Modifier.glowingBorderTrail(isAnalyzing: Boolean, color: Color): Modifier = composed {
    if (!isAnalyzing) return@composed this

    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        // Speed of the star orbiting the button
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart)
    )
    val measure = remember { PathMeasure() }
    val path = remember { Path() }

    this.drawWithContent {
        drawContent()

        // The button has a BorderStroke of exactly 1.dp.
        // To lock the path EXACTLY onto the centerline of the border, the inset must be 0.5.dp
        val borderWidth = 1.dp.toPx()
        val inset = borderWidth / 2f
        val cornerRadius = size.height / 2f

        path.reset()
        // addRoundRect naturally draws Top-Left -> Top-Right -> Bottom-Right -> Bottom-Left (Clockwise)
        path.addRoundRect(
            RoundRect(
                left = inset,
                top = inset,
                right = size.width - inset,
                bottom = size.height - inset,
                cornerRadius = CornerRadius(cornerRadius - inset)
            )
        )

        measure.setPath(path, false)
        val length = measure.length

        // Short, clean tail (15% of the perimeter)
        val tailLength = length * 0.15f
        val currentDistance = phase * length

        // Draw the tail with uniform thickness (1.5.dp), only fading the alpha to zero
        val steps = 20 // High step count for buttery smooth fade
        for (i in 0 until steps) {
            // Step alpha goes from ~0.8 down to 0
            val stepAlpha = (1f - (i.toFloat() / steps)) * 0.8f
            val segmentStart = currentDistance - (tailLength * (i + 1) / steps)
            val segmentEnd = currentDistance - (tailLength * i / steps)

            val segment = Path()

            if (segmentStart < 0 && segmentEnd < 0) {
                measure.getSegment(segmentStart + length, segmentEnd + length, segment, true)
            } else if (segmentStart < 0) {
                measure.getSegment(segmentStart + length, length, segment, true)
                measure.getSegment(0f, segmentEnd, segment, true)
            } else {
                measure.getSegment(segmentStart, segmentEnd, segment, true)
            }

            drawPath(
                path = segment,
                color = color.copy(alpha = stepAlpha),
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Draw the Tiny, High-Energy Glowing Star right at the front
        val pos = measure.getPosition(if (currentDistance >= length) 0f else currentDistance)
        if (pos != Offset.Unspecified) {
            // Very faint outer glow
            drawCircle(
                color = color.copy(alpha = 0.4f),
                radius = 3.dp.toPx(),
                center = pos
            )
            // Mid energy glow
            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = 1.5.dp.toPx(),
                center = pos
            )
            // Tiny, pristine pure-white core
            drawCircle(
                color = Color.White,
                radius = 0.8.dp.toPx(),
                center = pos
            )
        }
    }
}

// -----------------------------
// OMNI-DIRECTIONAL PULL CHAIN
// -----------------------------
@Composable
fun AnalyzePullChain(isDark: Boolean, onToggleTheme: (Boolean) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var hasToggled by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val currentIsDark by rememberUpdatedState(isDark)
    val currentOnToggle by rememberUpdatedState(onToggleTheme)

    val anchorXDp = LocalConfiguration.current.screenWidthDp.dp - 44.dp
    val anchorX = with(density) { anchorXDp.toPx() }
    val anchorY = with(density) { -20.dp.toPx() }
    val restLength = with(density) { 100.dp.toPx() }

    val linkColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val currentX = anchorX + pullOffset.value.x
        val currentY = anchorY + restLength + pullOffset.value.y

        drawLine(color = linkColor, start = Offset(anchorX, anchorY), end = Offset(currentX, currentY), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = (anchorX + pullOffset.value.x - 24.dp.toPx()).roundToInt(), y = (anchorY + restLength + pullOffset.value.y - 24.dp.toPx()).roundToInt()) }
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isDark) Color(0xFF030303) else Color.White)
                .background(if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            hasToggled = false
                            coroutineScope.launch { pullOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow)) }
                        },
                        onDragCancel = {
                            hasToggled = false
                            coroutineScope.launch { pullOffset.animateTo(Offset.Zero, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow)) }
                        },
                        onDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                val newOffset = pullOffset.value + dragAmount
                                pullOffset.snapTo(newOffset)
                                if (newOffset.getDistance() > 150f && !hasToggled) {
                                    currentOnToggle(!currentIsDark)
                                    hasToggled = true
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.WbSunny,
                contentDescription = "Pull to switch theme",
                tint = if (isDark) Color.White else Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// -----------------------------
// LUXURY UI COMPONENTS
// -----------------------------
@Composable
private fun JobOnlyResultsCard(skills: List<String>, coreColor: Color, cardBgColor: Color, cardTextColor: Color, cardSubTextColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBgColor), border = BorderStroke(1.dp, coreColor.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Skills found in JD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cardTextColor)
            SkillsChips(chips = skills, positive = true, coreColor = coreColor, cardTextColor = cardTextColor)
        }
    }
}

@Composable
private fun JobOnlySuggestionsCard(skills: List<String>, coreColor: Color, cardBgColor: Color, cardTextColor: Color, cardSubTextColor: Color) {
    val suggestions = buildList {
        add("Mirror the top JD skills in your resume Skills section (only if honest).")
        add("Add 2 JD-aligned bullets in Projects/Experience proving these skills with metrics.")
        if (skills.isNotEmpty()) add("Top skills detected: ${skills.take(10).joinToString(", ")}.")
        add("Upload your resume PDF to calculate match score and missing skills.")
    }.distinct()
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBgColor), border = BorderStroke(1.dp, coreColor.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Next steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cardTextColor)
            suggestions.forEach { Text("• $it", color = cardSubTextColor) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsCard(atsScore: Int, jobScore: Int, resumeWarning: String?, resumeLikelihood: Float, jdTotalCount: Int, matchedCount: Int, missingCount: Int, matchedTopCount: Int, missingTopCount: Int, matched: List<String>, missing: List<String>, coreColor: Color, cardBgColor: Color, cardTextColor: Color, cardSubTextColor: Color) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var grouped by rememberSaveable { mutableStateOf(false) }
    var showAllMatched by rememberSaveable { mutableStateOf(false) }
    var showAllMissing by rememberSaveable { mutableStateOf(false) }
    var showWhySheet by remember { mutableStateOf(false) }
    val q = query.trim().lowercase()
    val matchedFiltered = remember(matched, q) { if (q.isBlank()) matched else matched.filter { it.lowercase().contains(q) } }
    val missingFiltered = remember(missing, q) { if (q.isBlank()) missing else missing.filter { it.lowercase().contains(q) } }
    val matchedToShow = remember(matchedFiltered, showAllMatched, q) { if (q.isNotBlank() || showAllMatched) matchedFiltered else matchedFiltered.take(PREVIEW_CHIP_LIMIT) }
    val missingToShow = remember(missingFiltered, showAllMissing, q) { if (q.isNotBlank() || showAllMissing) missingFiltered else missingFiltered.take(PREVIEW_CHIP_LIMIT) }
    val jdCoverage = if (jdTotalCount > 0) { val cov = (jdTotalCount - missingCount).coerceIn(0, jdTotalCount); "$cov/$jdTotalCount" } else { "—" }

    fun shareText(): String {
        val sb = StringBuilder()
        sb.appendLine("ResumeMatch results\nATS: $atsScore/100\nJob Match: $jobScore/100\nJD coverage: $jdCoverage")
        if (matched.isNotEmpty()) sb.appendLine("Matched skills:\n${matched.joinToString(", ")}")
        if (missing.isNotEmpty()) sb.appendLine("Missing skills:\n${missing.joinToString(", ")}")
        return sb.toString().trim()
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBgColor), border = BorderStroke(1.dp, coreColor.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cardTextColor)
                Spacer(Modifier.width(10.dp))
                if (resumeLikelihood < 0.70f) ConfidenceBadge(likelihood = resumeLikelihood)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText()) }; ctx.startActivity(Intent.createChooser(intent, "Share results")) }) { Icon(Icons.Default.Share, contentDescription = "Share", tint = coreColor) }
            }
            if (!resumeWarning.isNullOrBlank()) { Surface(color = Color(0xFF4A1010), contentColor = Color.White, shape = RoundedCornerShape(12.dp)) { Text(resumeWarning, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) } }
            ScoreBar(label = "ATS Readiness", score = atsScore, color = coreColor, cardTextColor = cardTextColor)
            ScoreBar(label = "Job Match", score = jobScore, color = Color(0xFF10B981), cardTextColor = cardTextColor)
            Text(text = "Matched: $matchedCount • Missing: $missingCount • JD coverage: $jdCoverage", style = MaterialTheme.typography.bodySmall, color = cardSubTextColor)

            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search skills…", color=Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedBorderColor = coreColor, unfocusedBorderColor = coreColor.copy(alpha=0.3f), focusedTextColor = cardTextColor, unfocusedTextColor = cardTextColor)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { FilterChip(selected = grouped, onClick = { grouped = !grouped }, label = { Text(if (grouped) "Grouped" else "Compact", color=cardTextColor) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = coreColor.copy(alpha=0.3f))) }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Matched Skills", fontWeight = FontWeight.Bold, color = cardTextColor, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAllMatched = !showAllMatched }) { Text(if (showAllMatched || q.isNotBlank()) "Top ${PREVIEW_CHIP_LIMIT}" else "All (${matchedFiltered.size})", color = coreColor) }
            }
            if (matchedFiltered.isEmpty()) { Text("No matched skills detected.", style = MaterialTheme.typography.bodySmall, color = cardSubTextColor) } else {
                if (grouped) GroupedSkillsChips(chips = matchedToShow, positive = true, coreColor = coreColor, cardTextColor = cardTextColor, cardSubTextColor = cardSubTextColor) else SkillsChips(chips = matchedToShow, positive = true, coreColor = coreColor, cardTextColor = cardTextColor)
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Missing Skills", fontWeight = FontWeight.Bold, color = cardTextColor, modifier = Modifier.weight(1f))
                TextButton(onClick = { showWhySheet = true }) { Icon(Icons.Default.Info, contentDescription = "Why missing", modifier = Modifier.size(18.dp), tint=coreColor); Spacer(Modifier.width(6.dp)); Text("Why?", color = coreColor) }
                TextButton(onClick = { showAllMissing = !showAllMissing }) { Text(if (showAllMissing || q.isNotBlank()) "Top ${PREVIEW_CHIP_LIMIT}" else "All (${missingFiltered.size})", color = coreColor) }
            }
            if (missingFiltered.isEmpty()) { Text("No missing skills detected.", style = MaterialTheme.typography.bodySmall, color = cardSubTextColor) } else {
                if (grouped) GroupedSkillsChips(chips = missingToShow, positive = false, coreColor = coreColor, cardTextColor = cardTextColor, cardSubTextColor = cardSubTextColor) else SkillsChips(chips = missingToShow, positive = false, coreColor = coreColor, cardTextColor = cardTextColor)
                val topMissing = missingFiltered.firstOrNull()
                val suggested = topMissing?.let { suggestedBulletFor(it) }
                if (!suggested.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.Gray.copy(alpha=0.2f)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Suggested resume bullet", fontWeight = FontWeight.Bold, color = cardTextColor)
                            Text(suggested, style = MaterialTheme.typography.bodySmall, color = cardSubTextColor)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { TextButton(onClick = { clipboard.setText(AnnotatedString(text = suggested)) }) { Text("Copy", color=coreColor) } }
                        }
                    }
                }
            }
        }
    }
    if (showWhySheet) {
        ModalBottomSheet(onDismissRequest = { showWhySheet = false }, containerColor = cardBgColor) {
            Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("How 'Missing Skills' works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cardTextColor)
                Text("• Missing skills are JD skills not detected in the resume.", style = MaterialTheme.typography.bodyMedium, color=cardSubTextColor)
                Text("• ATS scanners depend on exact wording.", style = MaterialTheme.typography.bodyMedium, color=cardSubTextColor)
                Spacer(Modifier.height(4.dp))
                Button(onClick = { showWhySheet = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = coreColor)) { Text("Got it", color=Color.Black) }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SuggestionsCard(jobScore: Int, atsScore: Int, matched: List<String>, missing: List<String>, coreColor: Color, cardBgColor: Color, cardTextColor: Color, cardSubTextColor: Color) {
    val suggestions = remember(jobScore, atsScore, matched, missing) { buildStrongSuggestions(jobScore, atsScore, matched, missing) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = cardBgColor), border = BorderStroke(1.dp, coreColor.copy(alpha = 0.3f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cardTextColor)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = {}, label = { Text("Job Match: ${labelForScore(jobScore)}", color=cardTextColor) }, colors = AssistChipDefaults.assistChipColors(containerColor = Color.Gray.copy(alpha=0.2f)))
                AssistChip(onClick = {}, label = { Text("ATS: ${labelForScore(atsScore)}", color=cardTextColor) }, colors = AssistChipDefaults.assistChipColors(containerColor = Color.Gray.copy(alpha=0.2f)))
            }
            suggestions.forEach { Text("• $it", color=cardSubTextColor) }
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int, color: Color, cardTextColor: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = cardTextColor)
            Text("$score/100", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = cardTextColor)
        }
        LinearProgressIndicator(progress = (score.coerceIn(0, 100) / 100f), modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), color = color, trackColor = Color.Gray.copy(alpha=0.3f))
    }
}

@Composable
private fun SkillsChips(chips: List<String>, positive: Boolean, coreColor: Color, cardTextColor: Color) {
    if (chips.isEmpty()) return
    FlowWrap(modifier = Modifier.fillMaxWidth(), horizontalSpacing = 10.dp, verticalSpacing = 10.dp) {
        chips.forEach { chip -> SkillChip(label = chip, positive = positive, coreColor = coreColor, cardTextColor = cardTextColor) }
    }
}

@Composable
private fun FlowWrap(modifier: Modifier = Modifier, horizontalSpacing: Dp = 8.dp, verticalSpacing: Dp = 8.dp, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val hSpacePx = with(density) { horizontalSpacing.toPx().toInt() }
    val vSpacePx = with(density) { verticalSpacing.toPx().toInt() }

    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        var x = 0; var y = 0; var lineHeight = 0
        val positions = ArrayList<Pair<Int, Int>>(placeables.size)

        placeables.forEach { p ->
            if (x != 0 && x + p.width > maxWidth) { x = 0; y += lineHeight + vSpacePx; lineHeight = 0 }
            positions.add(x to y)
            x += p.width + hSpacePx
            lineHeight = maxOf(lineHeight, p.height)
        }
        val height = (y + lineHeight).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width = maxWidth, height = height) {
            placeables.forEachIndexed { i, p ->
                val (px, py) = positions[i]
                p.placeRelative(px, py)
            }
        }
    }
}

@Composable
private fun SkillChip(label: String, positive: Boolean, coreColor: Color, cardTextColor: Color) {
    val bg = if (positive) coreColor.copy(alpha = 0.15f) else Color(0xFFE74C3C).copy(alpha = 0.15f)
    val border = if (positive) coreColor.copy(alpha = 0.5f) else Color(0xFFE74C3C).copy(alpha = 0.5f)

    Surface(shape = RoundedCornerShape(999.dp), color = bg, border = BorderStroke(1.dp, border)) {
        Text(text = label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), color = cardTextColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ConfidenceBadge(likelihood: Float) {
    val (label, color) = when {
        likelihood < 0.25f -> "Resume confidence: Low" to Color(0xFFEF4444)
        else -> "Resume confidence: Medium" to Color(0xFFF59E0B)
    }
    AssistChip(onClick = {}, enabled = false, label = { Text(label, color = Color.White) }, colors = AssistChipDefaults.assistChipColors(containerColor = color.copy(alpha = 0.2f), disabledContainerColor = color.copy(alpha=0.2f)))
}

@Composable
private fun GroupedSkillsChips(chips: List<String>, positive: Boolean, coreColor: Color, cardTextColor: Color, cardSubTextColor: Color) {
    if (chips.isEmpty()) return
    val groups = remember(chips) { chips.groupBy { bucketOf(it) } }
    val order = listOf(Bucket.LANG, Bucket.API, Bucket.ANDROID, Bucket.CLOUD, Bucket.SYSTEMS, Bucket.AIML, Bucket.OTHER)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        order.forEach { b ->
            val list = groups[b].orEmpty()
            if (list.isNotEmpty()) {
                Text(text = bucketLabel(b), style = MaterialTheme.typography.bodySmall, color = cardSubTextColor, fontWeight = FontWeight.Bold)
                SkillsChips(chips = list, positive = positive, coreColor = coreColor, cardTextColor = cardTextColor)
            }
        }
    }
}

private fun suggestedBulletFor(skillLabel: String): String? {
    val s = skillLabel.lowercase().trim()
    return when {
        s.contains("multipart") -> "• Implemented multipart/form-data file upload for PDF resumes in FastAPI, with validation and robust error handling."
        s.contains("swagger") || s.contains("openapi") -> "• Designed REST endpoints in FastAPI and documented API contracts using Swagger/OpenAPI (interactive docs)."
        s == "rest" || s.contains("rest api") -> "• Built and maintained REST APIs with clear request/response schemas, status codes, and structured error responses."
        s.contains("uvicorn") -> "• Deployed FastAPI services with Uvicorn, tuning timeouts and logging for production-style debugging."
        s.contains("pypdf") || s.contains("pdf") -> "• Implemented PDF parsing and text extraction using PyPDF, handling noisy outputs with normalization."
        s.contains("regex") -> "• Built a regex-driven text normalization pipeline to clean and standardize extracted resume/JD text."
        s.contains("render") -> "• Deployed Dockerized FastAPI services to Render free tier and monitored failures/timeouts with retries."
        s.contains("postman") -> "• Used Postman to test and debug API workflows (auth, file uploads, error cases) with saved collections."
        s == "python" -> "• Built API-first backend services in Python using FastAPI with clean architecture and testable components."
        s.contains("docker") -> "• Containerized the backend using Docker for consistent local and cloud deployments."
        else -> null
    }
}