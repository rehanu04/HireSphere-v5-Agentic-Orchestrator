package com.rehanu04.resumematchv2.ui

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt


private const val MAX_MATCHED_CHIPS = 50
private const val MAX_MISSING_CHIPS = 50
private const val PREVIEW_CHIP_LIMIT = 12
// -----------------------------
// Models
// -----------------------------
private data class AnalyzeResult(
    val score: Int,
    val matchedCount: Int,
    val missingCount: Int,
    val matchedTop: List<String>,
    val missingTop: List<String>,
    val resumeTextLength: Int,
    val resumeText: String? = null // optional if backend returns it
)

// -----------------------------
// Known skills dictionary (for nicer display)
// Keep this reasonable; global extraction will cover unknown skills too.
// -----------------------------
private val KNOWN_SKILLS: Map<String, String> = linkedMapOf(
    // Languages
    "python" to "Python",
    "kotlin" to "Kotlin",
    "java" to "Java",
    "javascript" to "JavaScript",
    "typescript" to "TypeScript",
    "sql" to "SQL",
    "bash" to "Bash",
    "c" to "C",
    "cpp" to "C++",
    "csharp" to "C#",
    "go" to "Go",
    "rust" to "Rust",

    // Backend / API
    "fastapi" to "FastAPI",
    "uvicorn" to "Uvicorn",
    "flask" to "Flask",
    "django" to "Django",
    "node" to "Node.js",
    "node.js" to "Node.js",
    "express" to "Express",
    "spring" to "Spring",
    "spring boot" to "Spring Boot",
    "rest" to "REST",
    "rest apis" to "REST APIs",
    "http" to "HTTP",
    "openapi" to "OpenAPI",
    "swagger" to "Swagger",
    "swagger openapi" to "Swagger/OpenAPI",
    "grpc" to "gRPC",

    // Docs / tooling
    "postman" to "Postman",
    "render" to "Render",
    "regex" to "Regex",
    "text normalization" to "Text Normalization",
    "normalization" to "Normalization",
    "pypdf" to "PyPDF",
    "multipart" to "Multipart",
    "async" to "Async",
    "concurrency" to "Concurrency",

    // Android
    "android" to "Android",
    "okhttp" to "OkHttp",
    "retrofit" to "Retrofit",
    "coroutines" to "Coroutines",
    "jetpack compose" to "Jetpack Compose",
    "compose" to "Jetpack Compose",

    // Data
    "postgresql" to "PostgreSQL",
    "postgres" to "PostgreSQL",
    "mysql" to "MySQL",
    "mongodb" to "MongoDB",
    "redis" to "Redis",

    // DevOps / Cloud
    "docker" to "Docker",
    "kubernetes" to "Kubernetes",
    "k8s" to "Kubernetes",
    "terraform" to "Terraform",
    "linux" to "Linux",
    "aws" to "AWS",
    "azure" to "Azure",
    "gcp" to "GCP",
    "google cloud" to "Google Cloud",
    "google cloud platform" to "Google Cloud Platform",
    "ci/cd" to "CI/CD",
    "github actions" to "GitHub Actions",

    // AI/ML
    "machine learning" to "Machine Learning",
    "deep learning" to "Deep Learning",
    "nlp" to "NLP",
    "ai" to "AI",
    "ml" to "ML",
    "ai/ml" to "AI/ML",

    // Systems
    "distributed systems" to "Distributed Systems",
    "system design" to "System Design",
    "data structures" to "Data Structures",
    "algorithms" to "Algorithms",
    "multithreaded programming" to "Multithreaded Programming",
    "virtualization" to "Virtualization"
)

/** Variants -> canonical (lowercase) */
private val ALIASES: Map<String, String> = mapOf(
    "c++" to "cpp",
    "cplusplus" to "cpp",
    "c#" to "csharp",
    ".net" to "csharp",
    "dotnet" to "csharp",

    "nodejs" to "node.js",

    // Swagger/OpenAPI synonyms -> single canonical key
    "open api" to "swagger openapi",
    "openapi" to "swagger openapi",
    "swagger" to "swagger openapi",
    "swagger/openapi" to "swagger openapi",
    "swaggeropenapi" to "swagger openapi",

    // REST synonyms
    "rest api" to "rest",
    "rest apis" to "rest",
    "restful" to "rest",

    // Misc
    "ok http" to "okhttp",
    "git hub" to "github",
    "k8s" to "kubernetes",
    "ci cd" to "ci/cd",
    "cicd" to "ci/cd",

    // Normalization synonyms
    "text normalization" to "normalization",

    "ai ml" to "ai/ml",
    "aiml" to "ai/ml"
)

// strong stopwords to prevent garbage tokens becoming “skills”
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
    // generic job words that keep appearing as “skills”
    "backend", "engineer", "software", "title", "role",
    "deploy", "deployment", "implement", "implementation", "integration", "handling",
    "write", "build", "design", "develop", "optimize", "create",
    "request", "response", "requests", "responses",
    "api", "apis", "concepts", "nice", "required", "skills",
    "failures", "timeouts", "timeout", "failure", "analysis", "scoring",
    "documents", "document"
)

private fun isValidSkillCandidate(canonical: String): Boolean {
    val s = canonical.trim().lowercase()
    if (s.isBlank()) return false

    // 1) reject single words that are job verbs/nouns
    if (s in STOPWORDS) return false
    if (s in SKILL_BLACKLIST) return false

    // 2) reject “two-word phrases” that are clearly not skills
    // Example: "request response", "failures timeouts", "pdf documents"
    val badPhrases = listOf(
        "request response",
        "failures timeouts",
        "pdf documents",
        "okhttp http",
        "regex text",
        "python fastapi",
        "scoring analysis"
    )
    if (badPhrases.contains(s)) return false

    // 3) reject tokens that are too generic unless they are in KNOWN_SKILLS
    // e.g., "analysis", "handling"
    if (s !in KNOWN_SKILLS.keys) {
        // must look like a tech token or multiword tech phrase
        val hasTechPunct = s.contains("+") || s.contains("#") || s.contains(".") || s.contains("/")
        val isMultiWord = s.contains(" ")
        val longEnough = s.length >= 4
        if (!hasTechPunct && !isMultiWord && !longEnough) return false
        // also reject plain english words
        if (Regex("^[a-z]+$").matches(s) && s.length <= 10) return false
    }

    return true
}
private fun clamp0to100(v: Int): Int = v.coerceIn(0, 100)

private fun deriveAtsReadiness(
    resumeTextLength: Int,
    resumeText: String?,
    matchedCount: Int,
    missingCount: Int,
    fileName: String?
): Pair<Int, String?> {
    val x = resumeTextLength.coerceAtLeast(0)

    // Base score from text length (proxy for extractability/readability)
    val base = when {
        x < 600 -> 20
        x < 1000 -> 35 + ((x - 600) / 12.0).roundToInt()
        x < 1800 -> 55 + ((x - 1000) / 20.0).roundToInt()
        x < 3200 -> 72 + ((x - 1800) / 60.0).roundToInt()
        else -> 90
    }.coerceIn(0, 95)

    // Resume-likelihood heuristic: prevents "speech PDF" from showing high ATS.
    val (likelihood, _) = estimateResumeLikelihood(
        resumeText = resumeText,
        resumeTextLength = x,
        matchedCount = matchedCount,
        missingCount = missingCount,
        fileName = fileName
    )

    var ats = base

    val warning: String? = when {
        likelihood < 0.25f -> "This PDF doesn't look like a resume (low confidence). Please upload a resume PDF with sections like Skills/Experience/Education."
        likelihood < 0.45f -> "Low resume confidence: results may be unreliable. Make sure you selected the correct resume PDF."
        x < 500 -> "Low extractable text detected. If your resume is image/table heavy, export a text-based PDF for better ATS parsing."
        else -> null
    }

    if (likelihood < 0.25f) {
        ats = minOf(ats, 40)
        ats = (ats * (0.55f + 0.45f * likelihood)).roundToInt()
    } else if (likelihood < 0.45f) {
        ats = minOf(ats, 65)
        ats = (ats * (0.70f + 0.30f * likelihood)).roundToInt()
    }

    if (!fileName.isNullOrBlank() && fileName.lowercase().contains("resume") && warning != null) {
        ats = (ats + 5).coerceAtMost(80)
    }

    return clamp0to100(ats) to warning
}

private fun estimateResumeLikelihood(
    resumeText: String?,
    resumeTextLength: Int,
    matchedCount: Int,
    missingCount: Int,
    fileName: String?
): Pair<Float, String?> {
    if (!resumeText.isNullOrBlank()) {
        val t = resumeText.lowercase()

        val hasEmail = Regex("\\b[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}\\b").containsMatchIn(t)
        val hasPhone = Regex("\\b(\\+?\\d{1,3}[- .]?)?(\\(?\\d{2,4}\\)?[- .]?)?\\d{3,4}[- .]?\\d{3,4}\\b").containsMatchIn(t)
        val hasLinkedIn = t.contains("linkedin.com") || t.contains("linkedin")
        val hasGithub = t.contains("github.com") || t.contains("github")

        val headers = listOf("experience", "education", "skills", "projects", "summary", "certifications", "work experience")
        val headerHits = headers.count { h -> Regex("\\b" + Regex.escape(h) + "\\b").containsMatchIn(t) }

        val bulletHits = Regex("(^|\\n)\\s*[•\\u2022\\-\\*]\\s+").findAll(t).take(10).count()

        val speechMarkers = listOf("ladies and gentlemen", "good morning", "good evening", "thank you", "respected", "annual day", "speech")
        val speechHit = speechMarkers.any { t.contains(it) }

        var score = 0.0f
        if (hasEmail) score += 0.25f
        if (hasPhone) score += 0.20f
        if (hasLinkedIn) score += 0.10f
        if (hasGithub) score += 0.10f

        score += (headerHits.coerceAtMost(4) * 0.10f)
        if (bulletHits >= 3) score += 0.10f

        if (speechHit) score -= 0.45f

        val likelihood = score.coerceIn(0.0f, 1.0f)
        val reason = if (speechHit) "Speech-like wording detected." else null
        return likelihood to reason
    }

    val denom = (matchedCount + missingCount).coerceAtLeast(1)
    val coverage = matchedCount.toFloat() / denom.toFloat()

    var likelihood = 0.60f
    var reason: String? = null

    if (resumeTextLength < 400) {
        likelihood = 0.20f
        reason = "Very low extracted text."
    }

    if (matchedCount <= 1 && missingCount >= 4 && coverage < 0.15f) {
        likelihood = minOf(likelihood, 0.25f)
        reason = "Very low JD overlap; possibly wrong document."
    } else if (matchedCount <= 2 && coverage < 0.20f) {
        likelihood = minOf(likelihood, 0.40f)
        reason = "Low JD overlap."
    }

    val missingRatio = missingCount.toFloat() / denom.toFloat()
    if (missingRatio > 0.70f && matchedCount <= 4 && coverage < 0.30f) {
        likelihood = minOf(likelihood, 0.30f)
        reason = "Very low JD overlap; possibly wrong document."
    }

    if (!fileName.isNullOrBlank()) {
        val low = fileName.lowercase()
        if (low.contains("speech") || low.contains("annual") || low.contains("event")) {
            likelihood = minOf(likelihood, 0.20f)
            reason = "Filename suggests non-resume document."
        }
    }

    return likelihood.coerceIn(0.0f, 1.0f) to reason
}

private fun labelForScore(v: Int): String = when {
    v >= 80 -> "Strong"
    v >= 60 -> "Good"
    v >= 40 -> "Fair"
    v >= 20 -> "Low"
    else -> "Very Low"
}

// -----------------------------
// JSON parsing
// -----------------------------
private fun parseAnalyzeJson(json: String): AnalyzeResult {
    val o = JSONObject(json)
    val matched = o.optJSONArray("matched_top")
    val missing = o.optJSONArray("missing_top")

    fun arrToList(a: org.json.JSONArray?): List<String> {
        if (a == null) return emptyList()
        val out = ArrayList<String>(a.length())
        for (i in 0 until a.length()) out.add(a.optString(i))
        return out
    }

    // optional backend fields if you ever add them
    val resumeText = when {
        o.has("resume_text") -> o.optString("resume_text", null)
        o.has("resume_excerpt") -> o.optString("resume_excerpt", null)
        else -> null
    }

    return AnalyzeResult(
        score = o.optInt("score", 0),
        matchedCount = o.optInt("matched_count", 0),
        missingCount = o.optInt("missing_count", 0),
        matchedTop = arrToList(matched),
        missingTop = arrToList(missing),
        resumeTextLength = o.optInt("resume_text_length", 0),
        resumeText = resumeText
    )
}


// -----------------------------
// JD URL support (best-effort)
// -----------------------------
private fun looksLikeUrl(s: String): Boolean {
    val t = s.trim()
    return t.startsWith("http://", ignoreCase = true) ||
            t.startsWith("https://", ignoreCase = true) ||
            t.startsWith("www.", ignoreCase = true)
}

private fun normalizeUrl(s: String): String {
    val t = s.trim()
    return when {
        t.startsWith("http://", true) || t.startsWith("https://", true) -> t
        t.startsWith("www.", true) -> "https://$t"
        else -> t
    }
}

/**
 * If input is a URL, fetch the page and extract readable text (no external libs).
 * If it's plain text, return as-is.
 *
 * Call this from a background thread (IO).
 */
private fun resolveJobDescriptionText(client: OkHttpClient, input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""

    if (!looksLikeUrl(trimmed)) return trimmed

    val url = normalizeUrl(trimmed)
    val req = Request.Builder()
        .url(url)
        .get()
        .addHeader("User-Agent", "ResumeMatchV2/1.0")
        .build()

    client.newCall(req).execute().use { resp ->
        val body = resp.body?.string()
        if (!resp.isSuccessful || body.isNullOrBlank()) {
            throw IllegalStateException("Could not fetch JD URL (HTTP ${resp.code}).")
        }

        val ct = resp.header("Content-Type").orEmpty()
        val extracted = if (ct.contains("text/html", ignoreCase = true)) {
            htmlToReadableText(body)
        } else {
            body
        }

        // If extraction is too small (some pages block bots), fallback to raw body.
        return if (extracted.length < 200) body else extracted
    }
}

private fun htmlToReadableText(html: String): String {
    var s = html

    // remove scripts/styles/comments
    s = s.replace(Regex("(?is)<script.*?>.*?</script>"), " ")
    s = s.replace(Regex("(?is)<style.*?>.*?</style>"), " ")
    s = s.replace(Regex("(?is)<!--.*?-->"), " ")

    // basic tag handling
    s = s.replace(Regex("(?is)<br\\s*/?>"), "\n")
    s = s.replace(Regex("(?is)</p\\s*>"), "\n")
    s = s.replace(Regex("(?is)</div\\s*>"), "\n")
    s = s.replace(Regex("(?is)</li\\s*>"), "\n")
    s = s.replace(Regex("(?is)<li\\s*>"), " • ")
    s = s.replace(Regex("(?is)<[^>]+>"), " ")

    // decode common entities
    s = s.replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")

    // normalize whitespace
    s = s.replace(Regex("[\\t\\r]+"), " ")
    s = s.replace(Regex("\\s+"), " ").trim()

    return s
}

// -----------------------------
// File helpers
// -----------------------------
private fun getDisplayName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
    cursor.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst() && nameIndex >= 0) return it.getString(nameIndex)
    }
    return null
}

private suspend fun copyContentUriToTempFile(context: Context, uri: Uri): File {
    return withContext(Dispatchers.IO) {
        val tmp = File.createTempFile("resume_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri).use { input ->
            FileOutputStream(tmp).use { output ->
                if (input != null) input.copyTo(output)
            }
        }
        tmp
    }
}

// -----------------------------
// Skill extraction (GLOBAL)
// -----------------------------
private fun canonicalize(raw: String): String {
    val t = raw.trim().lowercase()
    if (t.isBlank()) return ""

    // normalize separators
    val cleaned = t
        .replace("\u00A0", " ")
        .replace("/", " ")
        .replace("_", " ")
        .replace("-", " ")
        .replace(Regex("[()\\[\\]{}:;,\\.\\|]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    // special casing
    val direct = when (cleaned) {
        "c++" -> "cpp"
        "c#" -> "csharp"
        else -> cleaned
    }

    // aliasing (e.g., "swagger openapi" -> "openapi", etc.)
    val alias = ALIASES[direct] ?: ALIASES[direct.replace(" ", "")]
    return alias ?: direct
}

private fun expandCompositeSkills(canonical: String): List<String> {
    val s = canonical.trim()
    if (s.isBlank()) return emptyList()
    // If it's already a known skill key or a known multiword key, keep it.
    if (s in KNOWN_SKILLS.keys) return listOf(s)

    // For composite phrases like "python fastapi" or "okhttp http",
    // extract any known skill tokens/bigrams so we don't lose important skills.
    val tokens = s.split(" ").filter { it.isNotBlank() }
    if (tokens.size <= 1) return listOf(s)

    val out = LinkedHashSet<String>()

    // 1-gram
    for (t in tokens) {
        if (t in KNOWN_SKILLS.keys) out.add(t)
    }
    // 2-gram
    for (i in 0 until tokens.size - 1) {
        val bi = tokens[i] + " " + tokens[i + 1]
        if (bi in KNOWN_SKILLS.keys) out.add(bi)
    }
    // 3-gram (for rare phrases)
    for (i in 0 until tokens.size - 2) {
        val tri = tokens[i] + " " + tokens[i + 1] + " " + tokens[i + 2]
        if (tri in KNOWN_SKILLS.keys) out.add(tri)
    }

    return if (out.isNotEmpty()) out.toList() else listOf(s)
}

private fun prettySkill(canonical: String): String {
    val k = canonical.trim().lowercase()
    return KNOWN_SKILLS[k] ?: k.split(" ").joinToString(" ") { part ->
        if (part.length <= 2) part.uppercase() else part.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Global extractor:
 * - detects known skills (dictionary)
 * - detects unknown skills using heuristics: tokens with mixed-case, acronyms, tech patterns
 * - supports multiword skills via ngrams
 */
private fun extractSkillsGlobal(text: String): Set<String> {
    if (text.isBlank()) return emptySet()

    // keep these symbols because they matter in tech: + # . /
    val norm = text
        .replace("\u00A0", " ")
        .replace(Regex("[•·]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    val lowered = norm.lowercase()

    // 1) dictionary hits (fast)
    val found = LinkedHashSet<String>()
    val dictKeys = KNOWN_SKILLS.keys.sortedByDescending { it.length }

    fun containsPhrase(phrase: String): Boolean {
        val parts = phrase.split(" ").filter { it.isNotBlank() }.map { Regex.escape(it) }
        val pattern = "\\b" + parts.joinToString("\\s+") + "\\b"
        return Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(lowered)
    }

    for (k in dictKeys) {
        val canonical = canonicalize(k)
        if (canonical.isBlank()) continue

        // handle symbols
        if (k == "ci/cd") {
            if (lowered.contains("ci/cd") || lowered.contains("ci cd") || lowered.contains("cicd")) found.add("ci/cd")
            continue
        }
        if (k == "node.js") {
            if (lowered.contains("node.js") || lowered.contains("nodejs") || containsPhrase("node js")) found.add("node.js")
            continue
        }

        if (containsPhrase(k)) found.add(canonical)
        val collapsed = k.replace(" ", "")
        if (collapsed != k && lowered.replace(" ", "").contains(collapsed)) found.add(canonical)
    }

    // 2) generic candidates from tokens (global)
    // token regex captures: FastAPI, OpenAPI, gRPC, PostgreSQL, Node.js, C++, C#, AI/ML, CI/CD, etc.
    val tokenRegex = Regex("""[A-Za-z][A-Za-z0-9\+\#\./]{1,}""")
    val tokens = tokenRegex.findAll(norm).map { it.value }.toList()

    fun looksLikeSkillToken(t: String): Boolean {
        val s = t.trim()
        if (s.length < 2) return false
        val low = s.lowercase()

        if (low in STOPWORDS) return false
        if (low.all { it.isDigit() }) return false

        // reject pure common english words
        if (low.length <= 3 && low !in setOf("ai","ml","sql","aws","gcp","api","ui")) {
            // allow common short acronyms; otherwise reject short tokens
            return false
        }

        // accept if has tech punctuation or is an acronym or camel case
        val hasTechPunct = s.contains("+") || s.contains("#") || s.contains("/") || s.contains(".")
        val isAcronym = s.length in 2..6 && s.all { it.isUpperCase() }
        val hasUpperInside = s.any { it.isUpperCase() } && s.any { it.isLowerCase() }

        return hasTechPunct || isAcronym || hasUpperInside || low in KNOWN_SKILLS.keys
    }

    tokens.forEach { t ->
        if (!looksLikeSkillToken(t)) return@forEach
        val c = canonicalize(t)
        if (c.isBlank()) return@forEach
        if (c in STOPWORDS) return@forEach
        // map common tech words
        found.add(c)
    }

    // 3) n-grams for multiword skills (2-3 word combos), using dictionary + safe heuristics
    val words = lowered
        .replace(Regex("[^a-z0-9+/# ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }

    fun validWord(w: String): Boolean =
        w.length >= 2 && w !in STOPWORDS && !w.all { it.isDigit() }

    val valid = words.filter(::validWord)
    // bigrams/trigrams
    for (i in 0 until valid.size) {
        if (i + 1 < valid.size) {
            val bi = "${valid[i]} ${valid[i + 1]}"
            val c = canonicalize(bi)
            if (c in KNOWN_SKILLS.keys) found.add(c)
            // allow some common patterns globally
            if (bi in setOf("machine learning", "deep learning", "distributed systems", "system design", "data structures")) {
                found.add(c)
            }
        }
        if (i + 2 < valid.size) {
            val tri = "${valid[i]} ${valid[i + 1]} ${valid[i + 2]}"
            val c = canonicalize(tri)
            if (c in KNOWN_SKILLS.keys) found.add(c)
        }
    }

    // normalize special tokens explicitly
    if (Regex("(?<![a-z0-9])c\\+\\+(?![a-z0-9])", RegexOption.IGNORE_CASE).containsMatchIn(norm)) found.add("cpp")
    if (Regex("(?<![a-z0-9])c#(?![a-z0-9])", RegexOption.IGNORE_CASE).containsMatchIn(norm)) found.add("csharp")
    if (Regex("\\bai/ml\\b", RegexOption.IGNORE_CASE).containsMatchIn(norm)) found.add("ai/ml")

    val cleaned = found
        .map { canonicalize(it) }
        .filter { isValidSkillCandidate(it) }
        .toLinkedHashSet()

    return cleaned
}


private fun <T> Iterable<T>.toLinkedHashSet(): LinkedHashSet<T> {
    val s = LinkedHashSet<T>()
    for (x in this) s.add(x)
    return s
}
/**
 * Resume skill inference:
 * - best: use backend resumeText if present
 * - else: combine matched_top + missing_top + filename
 */
private fun inferResumeSkills(parsed: AnalyzeResult, resumeFileName: String?): Set<String> {
    // IMPORTANT:
    // Do NOT use `missingTop` to infer resume skills.
    // `missingTop` (by definition) contains JD skills that are NOT in the resume.
    // Using it here pollutes resume skills and breaks the missing-skill calculation.
    val combined = buildString {
        if (!parsed.resumeText.isNullOrBlank()) {
            append(parsed.resumeText)
        } else {
            // Fallback: use only the backend's matched skills (these are present in the resume).
            append(parsed.matchedTop.joinToString(" "))
        }
        append(" ")
        // filename can sometimes contain tech keywords (optional)
        append(resumeFileName ?: "")
    }
    return extractSkillsGlobal(combined)
}
// -----------------------------
// Skill ranking + safe implications
// -----------------------------
private val SKILL_PRIORITY: Map<String, Int> = run {
    val ordered = listOf(
        // Core backend role signals
        "python",
        "fastapi",
        "uvicorn",
        "rest",
        "swagger openapi",
        "okhttp",
        "http",
        "regex",
        "pypdf",
        "docker",
        "render",
        "postman",
        "multipart",
        // Secondary / nice-to-have
        "async",
        "concurrency",
        "normalization",
        "android",
        "coroutines"
    )
    ordered.withIndex().associate { it.value to it.index }
}

private fun skillPriority(canonical: String): Int =
    SKILL_PRIORITY[canonical.trim().lowercase()] ?: 10_000

/**
 * Add a few safe implications to reduce "missing obvious skills" UX problems.
 * We only add when the implied skill is NOT explicitly missing.
 */
private fun applyInference(matched: MutableSet<String>, missing: Set<String>) {
    fun addIfOk(k: String) {
        val c = canonicalize(k)
        if (c.isBlank()) return
        if (c !in missing) matched.add(c)
    }

    val m = matched.map { it.lowercase() }.toSet()

    // FastAPI/Uvicorn are Python ecosystem
    if ("fastapi" in m || "uvicorn" in m) addIfOk("python")

    // OpenAPI and Swagger are tightly coupled in most JDs
    if ("openapi" in m || "swagger" in m) addIfOk("swagger openapi")

    // OkHttp is an HTTP client; keep HTTP visible
    if ("okhttp" in m) addIfOk("http")

    // Coroutines are async/concurrency signal
    if ("coroutines" in m) {
        addIfOk("async")
        addIfOk("concurrency")
    }
    if ("async" in m) addIfOk("concurrency")

    // REST APIs implies REST
    if ("rest apis" in m) addIfOk("rest")
}


// -----------------------------
// Suggestions (strong & useful)
// -----------------------------
private enum class Bucket { LANG, API, ANDROID, CLOUD, AIML, SYSTEMS, OTHER }

private fun bucketOf(s: String): Bucket {
    val low = s.lowercase()
    return when {
        low in listOf("python","java","kotlin","c","cpp","csharp","sql","javascript","typescript","go","rust","bash") -> Bucket.LANG
        low.contains("fastapi") || low.contains("uvicorn") || low.contains("rest") || low.contains("openapi") ||
                low.contains("swagger") || low.contains("grpc") || low.contains("http") -> Bucket.API
        low.contains("jetpack") || low.contains("compose") || low.contains("retrofit") || low.contains("okhttp") || low.contains("coroutines") -> Bucket.ANDROID
        low.contains("docker") || low.contains("kubernetes") || low.contains("terraform") || low.contains("aws") ||
                low.contains("gcp") || low.contains("google cloud") || low.contains("azure") || low.contains("linux") || low.contains("ci/cd") -> Bucket.CLOUD
        low.contains("machine learning") || low == "ai" || low == "ml" || low.contains("ai/ml") || low.contains("nlp") -> Bucket.AIML
        low.contains("distributed") || low.contains("system design") || low.contains("data structures") || low.contains("algorithms") ||
                low.contains("multithread") || low.contains("virtual") -> Bucket.SYSTEMS
        else -> Bucket.OTHER
    }
}

private fun buildStrongSuggestions(
    jobScore: Int,
    atsScore: Int,
    matched: List<String>,
    missing: List<String>
): List<String> {
    val out = mutableListOf<String>()
    val js = jobScore.coerceIn(0, 100)
    val ats = atsScore.coerceIn(0, 100)

    out.add("Tailor your Summary to the JD: target role + 2 strengths + 3 JD keywords (exact wording).")

    if (js < 40) {
        out.add("Job match is low: add 2 JD-aligned bullets in Projects/Experience that explicitly mention the top missing skills + results (numbers).")
        out.add("Move relevance up: put a dedicated Skills section directly below Summary. Don’t hide keywords in the bottom.")
    } else if (js < 70) {
        out.add("Job match is mid: improve keyword coverage naturally—ensure JD skills appear in Summary + Skills + at least 2 project bullets.")
    } else {
        out.add("Job match is strong: focus on proof—add metrics (latency, cost, users, accuracy) for the bullets that mention matched skills.")
    }

    if (missing.isNotEmpty()) {
        val topMissing = missing.take(12)
        val grouped = topMissing.groupBy { bucketOf(it) }

        grouped[Bucket.LANG]?.takeIf { it.isNotEmpty() }?.let {
            out.add("Languages gap: prove with 1 bullet per language (e.g., “Built X in ${it.take(3).joinToString(", ")}”).")
        }
        grouped[Bucket.API]?.takeIf { it.isNotEmpty() }?.let {
            out.add("Backend/API gap: add 1 bullet showing endpoint design + auth/error handling + OpenAPI docs (${it.take(4).joinToString(", ")}).")
        }
        grouped[Bucket.CLOUD]?.takeIf { it.isNotEmpty() }?.let {
            out.add("Cloud/DevOps gap: add a deployment bullet (Docker + platform) and mention monitoring/timeouts/retries if applicable (${it.take(4).joinToString(", ")}).")
        }
        grouped[Bucket.SYSTEMS]?.takeIf { it.isNotEmpty() }?.let {
            out.add("Systems gap: add a bullet about scalability/concurrency/perf (throughput, time saved, latency reduced) (${it.take(3).joinToString(", ")}).")
        }
        grouped[Bucket.AIML]?.takeIf { it.isNotEmpty() }?.let {
            out.add("AI/ML gap: include 1 credible ML bullet (dataset size + metric + model type). Don’t claim frameworks you haven’t used.")
        }

        out.add("Top missing skills to address first: ${topMissing.take(6).joinToString(", ")}.")
    } else {
        out.add("Missing skills list is empty: validate by ensuring your resume explicitly names the same skills as the JD (ATS keyword matching).")
    }

    if (ats < 60) {
        out.add("ATS is weak: use standard headings (Summary, Skills, Experience, Projects, Education). Avoid tables/columns/images.")
    } else if (ats < 80) {
        out.add("ATS is okay: keep formatting simple and consistent. Ensure dates/roles are clearly readable.")
    } else {
        out.add("ATS is strong: keep formatting simple; now prioritize relevance + proof mapped directly to JD.")
    }

    out.add("Checklist: (1) Summary has 3 JD keywords, (2) Skills section has top 8–12 relevant skills, (3) 2 bullets prove those skills with metrics.")
    return out.distinct()
}

// -----------------------------
// UI
// -----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(
    isDark: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onGoCreate: () -> Unit,
    apiBaseUrl: String,
    apiAppKey: String
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    var jdInput by rememberSaveable { mutableStateOf("") }
    var selectedPdfUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var selectedPdfName by rememberSaveable { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(false) }
    var rawJson by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // IMPORTANT: only set after clicking Analyze Job
    var jobAnalyzed by rememberSaveable { mutableStateOf(false) }
    var jobSkillsAfterClick by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var jobSkillKeysAfterClick by rememberSaveable { mutableStateOf(emptyList<String>()) }
    // Stores the exact JD text used for the most recent analysis (URL resolved to page text)
    var lastEffectiveJdText by rememberSaveable { mutableStateOf<String?>(null) }

    val pickPdf = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            selectedPdfName = getDisplayName(ctx, uri) ?: "selected.pdf"
        }
    }

    val jdText = jdInput.trim()
    val effectiveJdForSkills = (lastEffectiveJdText ?: jdText).trim()
    val jdSkillKeys = remember(effectiveJdForSkills) { extractSkillsGlobal(effectiveJdForSkills) }
    val jdSkillsPretty = remember(jdSkillKeys) { jdSkillKeys.map { prettySkill(it) }.distinct().sorted() }

    fun buttonLabel(): String = when {
        jdText.isBlank() -> "Enter JD to analyze"
        selectedPdfUri == null -> "Analyze Job"
        else -> "Analyze Match"
    }

    fun buttonEnabled(): Boolean = when {
        loading -> false
        jdText.isBlank() -> false
        selectedPdfUri == null -> true
        else -> apiBaseUrl.isNotBlank() && apiAppKey.isNotBlank()
    }

    fun runAnalyze() {
        errorText = null
        rawJson = null
        jobAnalyzed = false

        if (jdText.isBlank()) return

        loading = true

        scope.launch {
            try {
                val client = OkHttpClient()

                // Resolve JD: if user pasted a URL, fetch the page and extract readable text.
                val effectiveJd = withContext(Dispatchers.IO) { resolveJobDescriptionText(client, jdText) }
                lastEffectiveJdText = effectiveJd

                // JD-only: do not auto-run on typing; only compute on click.
                if (selectedPdfUri == null) {
                    val keys = extractSkillsGlobal(effectiveJd)
                        .asSequence()
                        .map { canonicalize(it) }
                        .flatMap { expandCompositeSkills(it).asSequence() }
                        .filter { isValidSkillCandidate(it) }
                        .toCollection(LinkedHashSet<String>())

                    val pretty = keys
                        .asSequence()
                        .map { prettySkill(it) }
                        .distinctBy { it.lowercase() }
                        .sorted()
                        .toList()

                    jobAnalyzed = true
                    jobSkillKeysAfterClick = keys.toList()
                    jobSkillsAfterClick = pretty
                    return@launch
                }

                val uri = selectedPdfUri ?: return@launch
                if (apiBaseUrl.isBlank() || apiAppKey.isBlank()) {
                    errorText = "Missing API base URL or app key."
                    return@launch
                }

                val tmp = copyContentUriToTempFile(ctx, uri)

                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "resume",
                        tmp.name,
                        tmp.asRequestBody("application/pdf".toMediaType())
                    )
                    .addFormDataPart("jd_text", effectiveJd)
                    // Ask backend for richer debug fields when available (does not affect normal output)
                    .addFormDataPart("debug", "true")
                    .build()

                val req = Request.Builder()
                    .url(apiBaseUrl.trimEnd('/') + "/v1/analyze/pdf")
                    .post(body)
                    // match your curl header name
                    .addHeader("x-app-key", apiAppKey)
                    .build()

                val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                val txt = resp.body?.string()

                if (!resp.isSuccessful || txt == null) {
                    errorText = "HTTP ${resp.code}: ${txt ?: "No response body"}"
                } else {
                    rawJson = txt
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Unknown error"
            } finally {
                loading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "ResumeMatch",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                tonalElevation = 0.dp
            ) {
                IconButton(onClick = { onToggleTheme(!isDark) }) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme"
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Resume (PDF)", style = MaterialTheme.typography.titleMedium)

                    Button(
                        onClick = { pickPdf.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(if (selectedPdfUri == null) "Select PDF" else "Change PDF")
                    }

                    Text(
                        text = "Selected: ${selectedPdfName ?: "None"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (selectedPdfUri != null) {
                        TextButton(
                            onClick = {
                                selectedPdfUri = null
                                selectedPdfName = null
                                rawJson = null
                                errorText = null
                            }
                        ) { Text("Clear selected PDF") }
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Job description", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = jdInput,
                        onValueChange = { jdInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 240.dp),
                        placeholder = { Text("Paste JD URL or JD text") },
                        singleLine = false,
                        maxLines = 12
                    )

                    Text(
                        "Tip: paste Requirements + Responsibilities for best results.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { runAnalyze() },
                enabled = buttonEnabled(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(if (loading) "Analyzing..." else buttonLabel())
            }

            TextButton(
                onClick = onGoCreate,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Don’t have a resume? Create one tailored for this job →")
            }

            if (errorText != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Error", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(errorText ?: "")
                    }
                }
            }

            // JD-only results: show ONLY after click
            if (selectedPdfUri == null && jobAnalyzed && jdText.isNotBlank()) {
                JobOnlyResultsCard(isDark = isDark, skills = jobSkillsAfterClick)
                JobOnlySuggestionsCard(skills = jobSkillsAfterClick)
            }

            // Match results
            val parsed = remember(rawJson) {
                rawJson?.let { runCatching { parseAnalyzeJson(it) }.getOrNull() }
            }

            if (parsed != null) {
                val jobMatch = clamp0to100(parsed.score)
                val (ats, resumeWarn) = deriveAtsReadiness(parsed.resumeTextLength, parsed.resumeText, parsed.matchedCount, parsed.missingCount, selectedPdfName)
                val resumeLikelihood = estimateResumeLikelihood(parsed.resumeText, parsed.resumeTextLength, parsed.matchedCount, parsed.missingCount, selectedPdfName).first


                // Skill chips logic:
                // The backend returns TOP matched/missing lists, not necessarily full sets.
                // If missingCount is small and covered by missingTop, we can safely show JD skills as matched
                // when they are not in missingTop (this fixes cases like Swagger not showing anywhere).
                val effectiveJdUsed = (lastEffectiveJdText ?: jdText).trim()
                val jdKeysUsed = if (jobSkillKeysAfterClick.isNotEmpty()) {
                    jobSkillKeysAfterClick
                        .asSequence()
                        .map { canonicalize(it) }
                        .filter { isValidSkillCandidate(it) }
                        .toCollection(LinkedHashSet<String>())
                } else {
                    extractSkillsGlobal(effectiveJdUsed)
                        .asSequence()
                        .map { canonicalize(it) }
                        .flatMap { expandCompositeSkills(it).asSequence() }
                        .filter { isValidSkillCandidate(it) }
                        .toCollection(LinkedHashSet<String>())
                }

                val backendMatchedKeys = parsed.matchedTop
                    .asSequence()
                    .map { canonicalize(it) }
                    .flatMap { expandCompositeSkills(it).asSequence() }
                    .filter { isValidSkillCandidate(it) }
                    .toCollection(LinkedHashSet<String>())

                val backendMissingKeys = parsed.missingTop
                    .asSequence()
                    .map { canonicalize(it) }
                    .flatMap { expandCompositeSkills(it).asSequence() }
                    .filter { isValidSkillCandidate(it) }
                    .toCollection(LinkedHashSet<String>())
                // The backend returns TOP matched/missing lists, not necessarily full sets.
                // If `missing_top` covers the backend `missing_count`, we treat it as complete and infer:
                //   matched = (JD skills) - (missing_top)
                // Otherwise, we conservatively derive missing from evidence and show the full JD-skill picture.
                val missingTarget = parsed.missingCount.coerceAtLeast(0)
                // Track only backend-missing that survives UI validation.
                val missingFromBackend = LinkedHashSet<String>(backendMissingKeys)

                // If backend returned only garbage tokens in missing_top (e.g., "title") that we filtered out,
                // do NOT inflate missing skills using heuristics just to satisfy missing_count.
                val missingDesiredSize = when {
                    parsed.missingTop.isNotEmpty() && backendMissingKeys.isEmpty() -> 0
                    else -> maxOf(missingTarget, backendMissingKeys.size)
                }

                // Evidence of skills present in the resume.
                // NOTE: backend matched/missing lists are TOP lists, not guaranteed to be complete.
                // We use backendMissingKeys as authoritative and only *fill* up to backend missing_count.
                val resumeEvidenceKeys = LinkedHashSet<String>().apply {
                    addAll(backendMatchedKeys)
                    if (!parsed.resumeText.isNullOrBlank()) {
                        val extra = extractSkillsGlobal(parsed.resumeText)
                            .asSequence()
                            .map { canonicalize(it) }
                            .flatMap { expandCompositeSkills(it).asSequence() }
                            .filter { isValidSkillCandidate(it) }
                            .toCollection(LinkedHashSet<String>())
                        addAll(extra)
                    }
                }

                // Missing candidates:
                // - Prefer JD skills not present in resumeEvidenceKeys (if we have any resume text evidence),
                // - Else fall back to JD skills not present in backendMatchedKeys.
                val derivedMissingCandidates: List<String> = if (resumeEvidenceKeys.isNotEmpty()) {
                    jdKeysUsed.filter { it !in resumeEvidenceKeys }
                } else {
                    jdKeysUsed.filter { it !in backendMatchedKeys }
                }

                val finalMissingKeys: LinkedHashSet<String> = LinkedHashSet<String>().apply {
                    // Backend missing is authoritative (may be partial list).
                    addAll(backendMissingKeys)

                    // If backend claims 0 missing, don't inflate from heuristics.
                    if (missingDesiredSize > 0) {
                        // Fill from derived candidates until we reach the desired size (missing_count or backend list size).
                        for (k in derivedMissingCandidates) {
                            if (size >= missingDesiredSize) break
                            add(k)
                        }

                        // Last-resort fill (rare): JD - backendMatched, still bounded by missingDesiredSize.
                        if (size < missingDesiredSize) {
                            for (k in jdKeysUsed) {
                                if (size >= missingDesiredSize) break
                                if (k !in backendMatchedKeys) add(k)
                            }
                        }
                    }
                }

                val finalMatchedKeys = LinkedHashSet<String>().apply {
                    // Matched within JD universe: JD - Missing
                    for (k in jdKeysUsed) if (k !in finalMissingKeys) add(k)
                    // Also include backend matched evidence (only if part of JD skill-universe to avoid noise)
                    if (jdKeysUsed.isEmpty()) {
                        addAll(backendMatchedKeys)
                    } else {
                        for (k in backendMatchedKeys) if (k in jdKeysUsed) add(k)
                    }
                }

                // Add a few safe implications (FastAPI/Uvicorn -> Python, OpenAPI -> Swagger/OpenAPI, etc.)
                applyInference(finalMatchedKeys, missingFromBackend)

                // If a skill is matched (including inferred) and it was NOT explicitly missing from the backend,
                // remove it from missing to prevent false negatives like Python showing under Missing.
                for (k in finalMatchedKeys) {
                    if (k !in missingFromBackend) finalMissingKeys.remove(k)
                }

                val matched = finalMatchedKeys
                    .toList()
                    .sortedWith(compareBy({ skillPriority(it) }, { it }))
                    .map { prettySkill(it) }
                    .distinctBy { it.lowercase() }
                    .take(MAX_MATCHED_CHIPS)

                val missing = finalMissingKeys
                    .toList()
                    .sortedWith(compareBy({ skillPriority(it) }, { it }))
                    .map { prettySkill(it) }
                    .distinctBy { it.lowercase() }
                    .take(MAX_MISSING_CHIPS)

                ResultsCard(
                    isDark = isDark,
                    atsScore = ats,
                    jobScore = jobMatch,
                    resumeWarning = resumeWarn,
                    resumeLikelihood = resumeLikelihood,
                    jdTotalCount = jdKeysUsed.size,
                    matchedCount = parsed.matchedCount,
                    missingCount = parsed.missingCount,
                    matchedTopCount = parsed.matchedTop.size,
                    missingTopCount = parsed.missingTop.size,
                    matched = matched,
                    missing = missing
                )

                SuggestionsCard(
                    jobScore = jobMatch,
                    atsScore = ats,
                    matched = matched,
                    missing = missing
                )
            } else if (rawJson != null) {
                val maybeErr = runCatching { JSONObject(rawJson!!).optString("detail") }.getOrNull()
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Response received", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (!maybeErr.isNullOrBlank()) "Backend error: $maybeErr"
                            else "Could not parse the backend response into match fields.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------
// UI components (kept consistent with your style)
// -----------------------------
@Composable
private fun JobOnlyResultsCard(isDark: Boolean, skills: List<String>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Skills found in JD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SkillsChips(isDark = isDark, chips = skills, positive = true)
        }
    }
}

@Composable
private fun JobOnlySuggestionsCard(skills: List<String>) {
    val suggestions = buildList {
        add("Mirror the top JD skills in your resume Skills section (only if honest).")
        add("Add 2 JD-aligned bullets in Projects/Experience proving these skills with metrics.")
        if (skills.isNotEmpty()) add("Top skills detected: ${skills.take(10).joinToString(", ")}.")
        add("Upload your resume PDF to calculate match score and missing skills.")
    }.distinct()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Next steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            suggestions.forEach { Text("• $it") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsCard(
    isDark: Boolean,
    atsScore: Int,
    jobScore: Int,
    resumeWarning: String?,
    resumeLikelihood: Float,
    jdTotalCount: Int,
    matchedCount: Int,
    missingCount: Int,
    matchedTopCount: Int,
    missingTopCount: Int,
    matched: List<String>,
    missing: List<String>
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var query by rememberSaveable { mutableStateOf("") }
    var grouped by rememberSaveable { mutableStateOf(false) }

    var showAllMatched by rememberSaveable { mutableStateOf(false) }
    var showAllMissing by rememberSaveable { mutableStateOf(false) }

    var showWhySheet by remember { mutableStateOf(false) }

    val q = query.trim().lowercase()

    val matchedFiltered = remember(matched, q) {
        if (q.isBlank()) matched else matched.filter { it.lowercase().contains(q) }
    }
    val missingFiltered = remember(missing, q) {
        if (q.isBlank()) missing else missing.filter { it.lowercase().contains(q) }
    }

    val matchedToShow = remember(matchedFiltered, showAllMatched, q) {
        if (q.isNotBlank() || showAllMatched) matchedFiltered else matchedFiltered.take(PREVIEW_CHIP_LIMIT)
    }
    val missingToShow = remember(missingFiltered, showAllMissing, q) {
        if (q.isNotBlank() || showAllMissing) missingFiltered else missingFiltered.take(PREVIEW_CHIP_LIMIT)
    }

    val jdCoverage = if (jdTotalCount > 0) {
        val cov = (jdTotalCount - missingCount).coerceIn(0, jdTotalCount)
        "$cov/$jdTotalCount"
    } else {
        "—"
    }

    // Whether backend top-lists are partial relative to counts.
    val matchedIsTopList = matchedTopCount in 1 until matchedCount
    val missingIsTopList = missingTopCount in 1 until missingCount

    fun shareText(): String {
        val sb = StringBuilder()
        sb.appendLine("ResumeMatch results")
        sb.appendLine("ATS: $atsScore/100")
        sb.appendLine("Job Match: $jobScore/100")
        if (jdTotalCount > 0) sb.appendLine("JD coverage: $jdCoverage")
        sb.appendLine("Matched: $matchedCount" + (if (matchedIsTopList) " (top $matchedTopCount shown by backend)" else ""))
        sb.appendLine("Missing: $missingCount" + (if (missingIsTopList) " (top $missingTopCount shown by backend)" else ""))
        sb.appendLine()
        if (matched.isNotEmpty()) {
            sb.appendLine("Matched skills:")
            sb.appendLine(matched.joinToString(", "))
            sb.appendLine()
        }
        if (missing.isNotEmpty()) {
            sb.appendLine("Missing skills:")
            sb.appendLine(missing.joinToString(", "))
            sb.appendLine()
        }
        if (!resumeWarning.isNullOrBlank()) {
            sb.appendLine("Note: $resumeWarning")
        }
        return sb.toString().trim()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Title row + actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.width(10.dp))

                // Confidence badge: show only when not high (keeps UI clean).
                if (resumeLikelihood < 0.70f) {
                    ConfidenceBadge(likelihood = resumeLikelihood)
                }

                Spacer(Modifier.weight(1f))

                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText())
                    }
                    ctx.startActivity(Intent.createChooser(intent, "Share results"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }

            if (!resumeWarning.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        resumeWarning,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            ScoreBar(label = "ATS Readiness", score = atsScore, color = Color(0xFF42A5F5))
            ScoreBar(label = "Job Match", score = jobScore, color = Color(0xFF66BB6A))

            // Counts line (clarity)
            Text(
                text = "Matched: $matchedCount • Missing: $missingCount • JD coverage: $jdCoverage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // If backend provides only top-lists, be transparent (trust).
            if (matchedIsTopList || missingIsTopList) {
                Text(
                    text = "Note: Backend returns top skill lists; counts reflect the full estimate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Search + view mode (minimal controls)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search skills…") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = grouped,
                    onClick = { grouped = !grouped },
                    label = { Text(if (grouped) "Grouped" else "Compact") }
                )
            }

            // Matched
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Matched Skills", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { showAllMatched = !showAllMatched }) {
                    Text(if (showAllMatched || q.isNotBlank()) "Top ${PREVIEW_CHIP_LIMIT}" else "All (${matchedFiltered.size})")
                }
            }

            if (matchedFiltered.isEmpty()) {
                Text(
                    "No matched skills detected from available signals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (grouped) {
                    GroupedSkillsChips(isDark = isDark, chips = matchedToShow, positive = true)
                } else {
                    SkillsChips(isDark = isDark, chips = matchedToShow, positive = true)
                }
                if (!showAllMatched && q.isBlank() && matchedFiltered.size > PREVIEW_CHIP_LIMIT) {
                    TextButton(onClick = { showAllMatched = true }) {
                        Text("Show all matched (${matchedFiltered.size})")
                    }
                }
            }

            // Missing + why button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Missing Skills", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                TextButton(onClick = { showWhySheet = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Why missing", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Why?")
                }

                TextButton(onClick = { showAllMissing = !showAllMissing }) {
                    Text(if (showAllMissing || q.isNotBlank()) "Top ${PREVIEW_CHIP_LIMIT}" else "All (${missingFiltered.size})")
                }
            }

            if (missingFiltered.isEmpty()) {
                Text(
                    "No missing skills detected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (grouped) {
                    GroupedSkillsChips(isDark = isDark, chips = missingToShow, positive = false)
                } else {
                    SkillsChips(isDark = isDark, chips = missingToShow, positive = false)
                }
                if (!showAllMissing && q.isBlank() && missingFiltered.size > PREVIEW_CHIP_LIMIT) {
                    TextButton(onClick = { showAllMissing = true }) {
                        Text("Show all missing (${missingFiltered.size})")
                    }
                }

                // Suggested bullet + copy (actionable)
                val topMissing = missingFiltered.firstOrNull()
                val suggested = topMissing?.let { suggestedBulletFor(it) }
                if (!suggested.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Suggested resume bullet", fontWeight = FontWeight.SemiBold)
                            Text(suggested, style = MaterialTheme.typography.bodySmall)

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(suggested))
                                }) { Text("Copy") }
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet (Why missing?)
    if (showWhySheet) {
        ModalBottomSheet(
            onDismissRequest = { showWhySheet = false },
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("How 'Missing Skills' works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Text(
                    "• Missing skills are JD skills not detected in the resume text extraction.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• ATS scanners depend on exact wording — include key skills in Summary/Skills/Project bullets (honestly).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• If your resume is image/table heavy, export a text-based PDF for better parsing.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (matchedIsTopList || missingIsTopList) {
                    Text(
                        "• Backend returns top lists for UI; counts may exceed the list sizes.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { showWhySheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp)
                ) { Text("Got it") }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SuggestionsCard(
    jobScore: Int,
    atsScore: Int,
    matched: List<String>,
    missing: List<String>
) {
    val suggestions = remember(jobScore, atsScore, matched, missing) {
        buildStrongSuggestions(jobScore, atsScore, matched, missing)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = {}, label = { Text("Job Match: ${labelForScore(jobScore)}") })
                AssistChip(onClick = {}, label = { Text("ATS: ${labelForScore(atsScore)}") })
            }

            suggestions.forEach { Text("• $it") }
        }
    }
}

@Composable
private fun ScoreBar(label: String, score: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$score/100", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }

        LinearProgressIndicator(
            progress = (score.coerceIn(0, 100) / 100f),
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// -----------------------------
// Chips layout (wraps cleanly, no FlowRow dependency)
// -----------------------------
@Composable
private fun SkillsChips(
    isDark: Boolean,
    chips: List<String>,
    positive: Boolean
) {
    if (chips.isEmpty()) return

    FlowWrap(
        modifier = Modifier.fillMaxWidth(),
        horizontalSpacing = 10.dp,
        verticalSpacing = 10.dp
    ) {
        chips.forEach { chip ->
            SkillChip(isDark = isDark, label = chip, positive = positive)
        }
    }
}

@Composable
private fun FlowWrap(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val hSpacePx = with(density) { horizontalSpacing.toPx().toInt() }
    val vSpacePx = with(density) { verticalSpacing.toPx().toInt() }

    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }

        var x = 0
        var y = 0
        var lineHeight = 0

        val positions = ArrayList<Pair<Int, Int>>(placeables.size)

        placeables.forEach { p ->
            if (x != 0 && x + p.width > maxWidth) {
                x = 0
                y += lineHeight + vSpacePx
                lineHeight = 0
            }
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
private fun SkillChip(isDark: Boolean, label: String, positive: Boolean) {
    val bg = when {
        positive && isDark -> Color(0xFF0B3D2E).copy(alpha = 0.65f)
        !positive && isDark -> Color(0xFF4A1010).copy(alpha = 0.65f)
        positive && !isDark -> Color(0xFFD6F5EA).copy(alpha = 0.95f)
        else -> Color(0xFFFFE0E0).copy(alpha = 0.95f)
    }

    val border = when {
        positive -> Color(0xFF2ECC71).copy(alpha = 0.55f)
        else -> Color(0xFFE74C3C).copy(alpha = 0.55f)
    }

    val textColor = if (isDark) Color.White else Color(0xFF111111)

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -----------------------------
// UI Enhancements helpers
// -----------------------------
@Composable
private fun ConfidenceBadge(likelihood: Float) {
    val (label, color) = when {
        likelihood < 0.25f -> "Resume confidence: Low" to MaterialTheme.colorScheme.error
        likelihood < 0.45f -> "Resume confidence: Medium" to MaterialTheme.colorScheme.tertiary
        else -> "Resume confidence: Medium" to MaterialTheme.colorScheme.tertiary
    }

    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.18f),
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun GroupedSkillsChips(
    isDark: Boolean,
    chips: List<String>,
    positive: Boolean
) {
    if (chips.isEmpty()) return

    val groups = remember(chips) {
        chips.groupBy { bucketOf(it) }
    }

    val order = listOf(
        Bucket.LANG,
        Bucket.API,
        Bucket.ANDROID,
        Bucket.CLOUD,
        Bucket.SYSTEMS,
        Bucket.AIML,
        Bucket.OTHER
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        order.forEach { b ->
            val list = groups[b].orEmpty()
            if (list.isNotEmpty()) {
                Text(
                    text = bucketLabel(b),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                SkillsChips(isDark = isDark, chips = list, positive = positive)
            }
        }
    }
}

private fun bucketLabel(b: Bucket): String = when (b) {
    Bucket.LANG -> "Languages"
    Bucket.API -> "Backend / APIs"
    Bucket.ANDROID -> "Android"
    Bucket.CLOUD -> "Cloud / DevOps"
    Bucket.AIML -> "AI/ML"
    Bucket.SYSTEMS -> "Systems"
    Bucket.OTHER -> "Other"
}

private fun suggestedBulletFor(skillLabel: String): String? {
    val s = skillLabel.lowercase().trim()

    return when {
        s.contains("multipart") ->
            "• Implemented multipart/form-data file upload for PDF resumes in FastAPI, with validation and robust error handling."
        s.contains("swagger") || s.contains("openapi") ->
            "• Designed REST endpoints in FastAPI and documented API contracts using Swagger/OpenAPI (interactive docs)."
        s == "rest" || s.contains("rest api") ->
            "• Built and maintained REST APIs with clear request/response schemas, status codes, and structured error responses."
        s.contains("uvicorn") ->
            "• Deployed FastAPI services with Uvicorn, tuning timeouts and logging for production-style debugging."
        s.contains("pypdf") || s.contains("pdf") ->
            "• Implemented PDF parsing and text extraction using PyPDF, handling noisy outputs with normalization."
        s.contains("regex") ->
            "• Built a regex-driven text normalization pipeline to clean and standardize extracted resume/JD text."
        s.contains("render") ->
            "• Deployed Dockerized FastAPI services to Render free tier and monitored failures/timeouts with retries."
        s.contains("postman") ->
            "• Used Postman to test and debug API workflows (auth, file uploads, error cases) with saved collections."
        s == "python" ->
            "• Built API-first backend services in Python using FastAPI with clean architecture and testable components."
        s.contains("docker") ->
            "• Containerized the backend using Docker for consistent local and cloud deployments."
        else -> null
    }
}
