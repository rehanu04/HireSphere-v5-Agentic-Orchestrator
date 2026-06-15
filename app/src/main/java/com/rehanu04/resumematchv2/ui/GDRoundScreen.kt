@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rehanu04.resumematchv2.data.LogEntry
import com.rehanu04.resumematchv2.ui.viewmodel.ActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit




// ── Data Models ───────────────────────────────────────────────────────────────

private data class GDMessage(
    val speaker: String,
    val text: String,
    val color: Color,
    val isUser: Boolean = false
)

private enum class GDPhase { LOBBY, RUNNING, WRAPPING_UP, SCORECARD }

private data class GDAgent(
    val name: String,
    val role: String,        // "against" | "for" | "neutral"
    val emoji: String,
    val color: Color,
    val ttsGetter: () -> TextToSpeech?,
    val utterancePrefix: String
)

// ── Premium Dark Background (radial Cyan glow only — no lamp/orbit) ────────────

@Composable
private fun GDOrbitBackground(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030303))
    ) {
        // Atmospheric radial Cyan glow centred at top-third
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.22f),
                    radius = size.height * 1.05f
                )
            )
        }
    }
}

// ── Custom Topic Validation ───────────────────────────────────────────────────

private fun validateCustomTopic(topic: String): Float {
    val trimmed = topic.trim()
    if (trimmed.isEmpty()) return 0f
    val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.size < 2) return 0.2f
    
    val lowerTopic = trimmed.lowercase()
    val blockedWords = listOf("fuck", "shit", "bitch", "ass", "cunt", "kill", "murder", "suicide", "porn", "sex", "nazi", "racist", "terrorist")
    if (blockedWords.any { lowerTopic.contains(it) }) return 0.1f
    
    return 1.0f 
}

// ── Topic Bank & Seed Anchor Matrix ───────────────────────────────────────────

private data class TopicSeedAnchor(
    val alexAgainst: String,
    val samFor: String,
    val chrisNeutral: String
)

private val GD_TOPICS = listOf(
    "Remote Work vs. Office Culture",
    "Artificial Intelligence in Healthcare",
    "Data Privacy in the Digital Age",
    "Sustainable Technology & Green Computing",
    "The Future of Electric Vehicles",
    "Social Media's Impact on Mental Health",
    "Blockchain & Decentralized Finance",
    "Cybersecurity Risks in IoT Ecosystems",
    "Microservices vs. Monolithic Architecture",
    "Quantum Computing & Cryptography",
    "E-Commerce Growth & Supply Chain Logistics",
    "Renewable Energy Systems & Grid Modernization",
    "Agile Software Development vs. Waterfall Method",
    "EdTech & the Future of Online Education",
    "5G Rollout & Telecom Infrastructure Expansion"
)

private val GD_TOPIC_ANCHORS = mapOf(
    "Remote Work vs. Office Culture" to TopicSeedAnchor(
        alexAgainst = "isolation, loss of synergy, supervision difficulty, company culture dilution",
        samFor = "geographic flexibility, higher developer focus, reduced overhead, talent pool expansion",
        chrisNeutral = "hybrid structure transitions, tracking tooling compliance, implementation costs"
    ),
    "Artificial Intelligence in Healthcare" to TopicSeedAnchor(
        alexAgainst = "data breaches, diagnostics liability, medical empathy loss, implementation cost barriers",
        samFor = "early oncology detection, rapid drug discovery workflows, reducing clinician burnout, robotic surgery precision",
        chrisNeutral = "patient compliance frameworks, medical manufacturing scaling curves, deployment timelines"
    ),
    "Data Privacy in the Digital Age" to TopicSeedAnchor(
        alexAgainst = "user tracking, consent fatigue, commercial surveillance, security breach leaks",
        samFor = "zero-knowledge encryption, consumer agency, trust-based monetization, secure cloud ecosystems",
        chrisNeutral = "GDPR compliance audits, regulatory fines timelines, multi-national data laws"
    ),
    "Sustainable Technology & Green Computing" to TopicSeedAnchor(
        alexAgainst = "electronic waste, server cooling energy, recycling constraints, greenwashing",
        samFor = "carbon-neutral cloud, renewable energy sourcing, chip efficiency breakthroughs, server longevity",
        chrisNeutral = "sustainability metrics standards, regulatory enforcement dates, supply chain audits"
    ),
    "The Future of Electric Vehicles" to TopicSeedAnchor(
        alexAgainst = "battery mineral shortages, charging grid overload, cold weather range drop, recycling cost",
        samFor = "zero tailpipe emissions, lower maintenance cost, instant torque, autonomous driving convergence",
        chrisNeutral = "charging grid construction speed, battery regulations, vehicle transition curves"
    ),
    "Social Media's Impact on Mental Health" to TopicSeedAnchor(
        alexAgainst = "dopamine addiction loop, cyberbullying, sleep deprivation, echo chamber polarization",
        samFor = "global peer support networks, mental health awareness, virtual community spaces, support accessibility",
        chrisNeutral = "algorithmic safety regulation, screen time monitoring metrics, verification timelines"
    ),
    "Blockchain & Decentralized Finance" to TopicSeedAnchor(
        alexAgainst = "smart contract exploits, extreme market volatility, scaling issues, systemic fraud risks",
        samFor = "permissionless transactions, financial inclusion, smart contract automation, reduced intermediary fees",
        chrisNeutral = "stablecoin reserve legislation, AML/KYC enforcement, central bank policy schedules"
    ),
    "Cybersecurity Risks in IoT Ecosystems" to TopicSeedAnchor(
        alexAgainst = "botnet exploits, default credential vulnerabilities, device lifecycle neglect, physical safety risks",
        samFor = "automated threat response, encrypted edge communication, firmware signature verification, isolated networks",
        chrisNeutral = "standardized IoT security certs, hardware testing timelines, device manufacturing regulations"
    ),
    "Microservices vs. Monolithic Architecture" to TopicSeedAnchor(
        alexAgainst = "network latency overhead, database transaction complexity, configuration drift, trace complexity",
        samFor = "independent service deployments, team ownership boundaries, technology stack flexibility, fault containment",
        chrisNeutral = "service registry migration cost, refactoring timelines, monitoring tool licensing"
    ),
    "Quantum Computing & Cryptography" to TopicSeedAnchor(
        alexAgainst = "existing RSA break threat, extreme physical stability costs, programming complexity, hardware immaturity",
        samFor = "unbreakable quantum key distribution, molecular simulation speed, complex chemical modeling, optimization solving",
        chrisNeutral = "NIST post-quantum standards, migration timeline phases, government compliance deadlines"
    ),
    "E-Commerce Growth & Supply Chain Logistics" to TopicSeedAnchor(
        alexAgainst = "last-mile emission increases, warehouse worker burnout, inventory distortions, return policy losses",
        samFor = "AI demand forecasting, automated fulfillment hubs, route optimization algorithms, global supply visibility",
        chrisNeutral = "maritime import regulations, customs processing timelines, fleet electrification schedules"
    ),
    "Renewable Energy Systems & Grid Modernization" to TopicSeedAnchor(
        alexAgainst = "grid instability issues, battery storage limits, high initial infrastructure cost, weather dependency",
        samFor = "distributed solar generation, smart grid load balancing, clean power source abundance, decentralized production",
        chrisNeutral = "utility interconnection approvals, grid infrastructure lifespans, federal funding cycles"
    ),
    "Agile Software Development vs. Waterfall Method" to TopicSeedAnchor(
        alexAgainst = "scope creep risks, documentation neglect, sprint fatigue, unpredictable budget forecasts",
        samFor = "rapid feedback loop integration, iterative quality improvements, team adaptation speed, early value delivery",
        chrisNeutral = "enterprise contract alignments, milestone compliance gates, project delivery schedules"
    ),
    "EdTech & the Future of Online Education" to TopicSeedAnchor(
        alexAgainst = "screen fatigue, digital divide exclusion, cheating vulnerability, hands-on learning deficit",
        samFor = "personalized learning paths, global education accessibility, gamified student engagement, interactive simulations",
        chrisNeutral = "accreditation board regulations, university curriculum adoption timelines, student compliance auditing"
    ),
    "5G Rollout & Telecom Infrastructure Expansion" to TopicSeedAnchor(
        alexAgainst = "high cell density cost, range constraints, hardware cybersecurity vulnerability, deployment disruption",
        samFor = "ultra-low latency communication, massive IoT sensor densities, edge computing enablement, high speed streaming",
        chrisNeutral = "spectrum license bidding schedules, city zoning approval timelines, cell tower buildout schedules"
    )
)


// ── Scoring Helpers ───────────────────────────────────────────────────────────

private fun computeInitiativeScore(messages: List<GDMessage>, userSpokeFirst: Boolean): Int {
    val userMessages = messages.filter { it.isUser }
    if (userMessages.isEmpty()) return 0
    val totalWords = userMessages.sumOf { it.text.split(Regex("\\s+")).filter { w -> w.isNotBlank() }.size }
    if (totalWords < 10) return 10

    var score = if (userSpokeFirst) 20 else 0
    val proactiveWords = listOf("suggest", "propose", "let's", "consider", "recommend", "initiate", "start", "believe", "agree", "disagree", "point")
    for (msg in userMessages) {
        val lower = msg.text.lowercase()
        for (word in proactiveWords) { if (lower.contains(word)) score += 5 }
    }
    return score.coerceAtMost(100)
}

private fun computeContentScore(messages: List<GDMessage>, topic: String): Int {
    val userMessages = messages.filter { it.isUser }
    if (userMessages.isEmpty()) return 0
    val totalWords = userMessages.sumOf { it.text.split(Regex("\\s+")).filter { w -> w.isNotBlank() }.size }
    if (totalWords < 10) return 10

    val topicKeywords = mapOf(
        "Remote Work" to listOf("productivity", "collaboration", "async", "hybrid", "balance", "commute", "culture"),
        "Artificial Intelligence" to listOf("model", "data", "algorithm", "neural", "accuracy", "bias", "training"),
        "Data Privacy" to listOf("gdpr", "consent", "encryption", "breach", "compliance", "regulation", "anonymization"),
        "Sustainable" to listOf("carbon", "renewable", "energy", "efficiency", "footprint", "green", "emission"),
        "Electric" to listOf("battery", "range", "charging", "ev", "infrastructure", "grid", "emission"),
        "Social Media" to listOf("engagement", "algorithm", "anxiety", "comparison", "attention", "wellbeing", "dopamine")
    )
    val relevantKeywords = topicKeywords.entries.firstOrNull { topic.contains(it.key, ignoreCase = true) }?.value
        ?: topic.lowercase().split(Regex("\\s+")).filter { it.length > 4 }.take(7)
    val userText = userMessages.joinToString(" ") { it.text }.lowercase()
    val hits = relevantKeywords.count { userText.contains(it) }
    val densityScore = if (relevantKeywords.isNotEmpty()) (hits.toFloat() / relevantKeywords.size * 60).toInt() else 0
    val volumeScore = (totalWords / 15).coerceAtMost(40)
    return (densityScore + volumeScore).coerceAtMost(100)
}

private fun computeCohesionScore(messages: List<GDMessage>, interruptionCount: Int): Int {
    val userMessages = messages.filter { it.isUser }
    if (userMessages.isEmpty()) return 0
    val totalWords = userMessages.sumOf { it.text.split(Regex("\\s+")).filter { w -> w.isNotBlank() }.size }
    if (totalWords < 10) return 10
    
    val completeCount = userMessages.count { msg -> msg.text.trim().lastOrNull() in listOf('.', '?', '!') }
    val completionRatio = completeCount.toFloat() / userMessages.size
    
    var score = (100 - interruptionCount * 12).coerceAtLeast(0)
    if (completionRatio < 0.5f) {
        score = (score * 0.5f).toInt()
    }
    return score
}

private fun computeImpactScore(messages: List<GDMessage>): Int {
    val userMessages = messages.filter { it.isUser }
    if (userMessages.isEmpty()) return 0
    val totalWords = userMessages.sumOf { it.text.split(Regex("\\s+")).filter { w -> w.isNotBlank() }.size }
    if (totalWords < 10) return 10

    val avgWords = userMessages.map { it.text.split(" ").filter { w -> w.isNotBlank() }.size }.average()
    val completeCount = userMessages.count { msg -> msg.text.trim().lastOrNull() in listOf('.', '?', '!') }
    val completionRatio = completeCount.toFloat() / userMessages.size

    var score = ((avgWords / 15.0 * 60).toInt() + (completionRatio * 40).toInt()).coerceAtMost(100)
    if (completionRatio < 0.5f) {
        score = (score * 0.5f).toInt()
    }
    return score
}

// ── Main Composable ───────────────────────────────────────────────────────────

@Composable
fun GroupDiscussionScreen(
    isDark: Boolean,
    activityViewModel: ActivityViewModel?,
    onBack: () -> Unit,
    apiBaseUrl: String = "https://resumematch-ai-backend.onrender.com/"
) {
    val accent    = Color(0xFF22D3EE)
    val textColor = if (isDark) Color.White else Color(0xFF111827)
    val surface   = if (isDark) Color(0xFF18181B) else Color.White
    val cardBg    = Color(0xFF1A1C1E).copy(alpha = 0.6f)

    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    val httpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    // ── Permission ──────────────────────────────────────────────────────────
    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasMicPermission = granted
    }

    // ── Phase & Session State ───────────────────────────────────────────────
    var phase by remember { mutableStateOf(GDPhase.LOBBY) }
    var selectedTopic by remember { mutableStateOf(GD_TOPICS.random()) }
    var customTopicInput by remember { mutableStateOf("") }
    var isCustomTopicMode by remember { mutableStateOf(false) }

    var messages by remember { mutableStateOf(listOf<GDMessage>()) }
    var activeSpeaker by remember { mutableStateOf("") }
    var activeSpeakerStartTime by remember { mutableLongStateOf(0L) }
    var floorOwner by remember { mutableStateOf("") }
    var activeAgentJob by remember { mutableStateOf<Job?>(null) }
    var isMicActive by remember { mutableStateOf(false) }
    var isAgentThinking by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(300) }
    var userSpeechStartMs by remember { mutableLongStateOf(0L) }
    var interruptionCount by remember { mutableIntStateOf(0) }
    var userSpokeFirst by remember { mutableStateOf(false) }
    var userHasSpoken by remember { mutableStateOf(false) }

    // Scorecard metrics
    var initiativeScore by remember { mutableIntStateOf(0) }
    var contentScore by remember { mutableIntStateOf(0) }
    var cohesionScore by remember { mutableIntStateOf(0) }
    var impactScore by remember { mutableIntStateOf(0) }

    // Conversation history for LLM context (pairs of speaker/text)
    var conversationHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    
    // Server Wakeup & Topic Preparation Phase
    var isPreparingBackend by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // ── TTS Engines ─────────────────────────────────────────────────────────
    var ttsAlex: TextToSpeech? by remember { mutableStateOf(null) }
    var ttsSam: TextToSpeech? by remember { mutableStateOf(null) }
    var ttsChris: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        ttsAlex = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsAlex?.language = Locale.US
                ttsAlex?.setSpeechRate(0.92f)
                ttsAlex?.setPitch(0.80f)   // Deep Male Tone
            }
        }
        ttsSam = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsSam?.language = Locale.US
                ttsSam?.setSpeechRate(1.05f)
                ttsSam?.setPitch(1.25f)    // Clear Female Tone
            }
        }
        ttsChris = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsChris?.language = Locale.US
                ttsChris?.setSpeechRate(0.88f)
                ttsChris?.setPitch(0.70f)  // Aged Baritone
            }
        }
        onDispose {
            ttsAlex?.shutdown()
            ttsSam?.shutdown()
            ttsChris?.shutdown()
        }
    }

    // ── Speech Recognizer ───────────────────────────────────────────────────
    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx.applicationContext)
        onDispose { speechRecognizer?.destroy() }
    }

    fun stopAllAgents() {
        activeAgentJob?.cancel()
        isAgentThinking = false
        ttsAlex?.stop()
        ttsSam?.stop()
        ttsChris?.stop()
    }

    fun startListening() {
        if (!hasMicPermission || phase != GDPhase.RUNNING) return
        isMicActive = true
        activeSpeaker = "You"
        floorOwner = "You"
        userSpeechStartMs = System.currentTimeMillis()
        ContextCompat.getMainExecutor(ctx.applicationContext).execute {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                isMicActive = false
                activeSpeaker = ""
                floorOwner = ""
            }
        }
    }

    fun stopListening() {
        isMicActive = false
        speechRecognizer?.stopListening()
    }

    // ── LLM-Backed Agent Turn (self-contained per agent) ─────────────────────
    // Each call is fire-and-forget; onDone fires when TTS utterance fully completes
    fun speakAgentWithLLM(
        agentName: String,
        agentRole: String,   // "against" | "for" | "neutral"
        agentColor: Color,
        tts: TextToSpeech?,
        utterancePrefix: String,
        preProvidedText: String? = null,
        interjection: String = "",
        onDone: () -> Unit
    ) {
        if (isAgentThinking && preProvidedText == null) {
            onDone()
            return
        }

        activeAgentJob = scope.launch(Dispatchers.Main) {
            if (isAgentThinking && preProvidedText == null) {
                if (floorOwner == agentName) {
                    floorOwner = ""
                    activeSpeaker = ""
                }
                onDone()
                return@launch
            }

            try {
                // Instantly speak the interjection to mask network latency
                if (interjection.isNotEmpty()) {
                    tts?.speak(interjection, TextToSpeech.QUEUE_ADD, null, null)
                }

                val lastSpeaker = conversationHistory.lastOrNull()?.first ?: ""
                val userJustSpoke = lastSpeaker == "You"
                val lastUserText = conversationHistory.lastOrNull { it.first == "You" }?.second ?: ""

                val isMetaConversational = lastUserText.contains("Why isn't anyone speaking", ignoreCase = true) ||
                        lastUserText.contains("Why isn't anyone talking", ignoreCase = true) ||
                        lastUserText.contains("Is anyone there", ignoreCase = true) ||
                        lastUserText.contains("Are you there", ignoreCase = true) ||
                        lastUserText.contains("Rehan", ignoreCase = true) ||
                        lastUserText.contains("Alex", ignoreCase = true) ||
                        lastUserText.contains("Sam", ignoreCase = true) ||
                        lastUserText.contains("Chris", ignoreCase = true)

                val replyText = if (preProvidedText != null) {
                    preProvidedText
                } else {
                    isAgentThinking = true

                    val anchors = GD_TOPIC_ANCHORS[selectedTopic]
                    val stanceKeywords = if (anchors != null) {
                        when (agentRole) {
                            "against" -> anchors.alexAgainst
                            "for" -> anchors.samFor
                            else -> anchors.chrisNeutral
                        }
                    } else "specific realities of $selectedTopic"

                    val rawReply = withContext(Dispatchers.IO) {
                        try {
                            val chatHistoryStr = JSONArray().apply {
                                conversationHistory.forEach { (spk, txt) ->
                                    val formattedSpk = when (spk) {
                                        "Alex" -> "CANDIDATE: ALEX (SKEPTIC)"
                                        "Sam" -> "CANDIDATE: SAM (VISIONARY)"
                                        "Chris" -> "CANDIDATE: CHRIS (NEUTRAL)"
                                        "You" -> "USER: REHAN"
                                        else -> spk
                                    }
                                    put("[$formattedSpk]: $txt")
                                }
                            }.toString()

                            val chatbotOverride = "CRITICAL CHATBOT INSTRUCTION: You are $agentName. Stance: $agentRole. Read the entire chat history, analyze the latest point made by the last speaker, and DIRECTLY ADDRESS IT. Do not monologue. Do not summarize. Directly counter or support the last point."
                            val repetitionBan = "REPETITION BAN: You are STRICTLY FORBIDDEN from repeating previously used phrases. Never use generic filler."
                            val conversationalFlow = "FORMAT: Use short, punchy, human-like chat responses. Be dynamic and highly reactive."
                            
                            val baseMeta = "$chatbotOverride\n$repetitionBan\n$conversationalFlow"
                            
                            val metaGateInstruction = if (isMetaConversational) {
                                "META-CONVERSATION: The user ('$lastUserText') is talking outside the debate. Acknowledge this directly as a chatbot."
                            } else ""
                            
                            val finalMetaInstruction = if (metaGateInstruction.isNotEmpty()) "$metaGateInstruction\n$baseMeta" else baseMeta

                            val requestBody = JSONObject().apply {
                                put("target_role", "$agentName (Stance: $agentRole)")
                                put("job_description", "Group Discussion Topic: $selectedTopic.\n$finalMetaInstruction")
                                put("vault_data", "")
                                put("chat_history", chatHistoryStr)
                                put("user_audio_text", "")
                                put("elapsed_seconds", 0)
                            }.toString()

                            val req = Request.Builder()
                                .url(apiBaseUrl.trimEnd('/') + "/v1/gauntlet/gd-turn")
                                .post(requestBody.toRequestBody("application/json".toMediaType()))
                                .build()
                                
                            val resp = httpClient.newCall(req).execute()
                            
                            if (!resp.isSuccessful) {
                                null // Trigger fallback phrases to buy time during rate limits (e.g. HTTP 500)
                            } else {
                                val bodyString = resp.body?.string()
                                if (bodyString.isNullOrBlank()) null
                                else {
                                    var cleanedBody = bodyString.trim()
                                    if (cleanedBody.startsWith("```json")) {
                                        cleanedBody = cleanedBody.removePrefix("```json").removeSuffix("```").trim()
                                    } else if (cleanedBody.startsWith("```")) {
                                        cleanedBody = cleanedBody.removePrefix("```").removeSuffix("```").trim()
                                    }
                                    
                                    try {
                                        val jsonObject = JSONObject(cleanedBody)
                                        val reply = jsonObject.optString("reply", "").ifBlank { 
                                            jsonObject.optString("ai_reply", "") 
                                        }
                                        reply.ifBlank { cleanedBody }
                                    } catch (e: Exception) {
                                        // If it's not valid JSON, the LLM probably just returned raw text directly.
                                        cleanedBody
                                    }
                                }
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e // Do not swallow cancellation
                        } catch (e: Exception) {
                            null // Trigger fallback phrases on network exceptions
                        }
                    } 
                    
                    rawReply ?: run {
                        val fallbackPhrases = when (agentRole) {
                            "against" -> listOf(
                                "That's an interesting perspective, but I'm still convinced there are significant risks we aren't discussing.",
                                "You make a fair point, though I still have my reservations about the structural approach.",
                                "I hear what you're saying. However, the potential downsides still seem too high to me.",
                                "That's a bold claim. I'm just not sure the data fully supports that kind of optimism.",
                                "I see where you're coming from, but we really need to verify how that aligns with reality.",
                                "Valid point, but I still believe we need a much more cautious approach here."
                            )
                            "for" -> listOf(
                                "I completely agree with the direction of that point. The scaling potential here is huge.",
                                "Exactly. If we organize our approach around those benefits, the upside is massive.",
                                "That's a great perspective. I think leveraging that effectively will accelerate our goals.",
                                "I'm fully on board with that optimistic vision. It aligns perfectly with what we need.",
                                "Spot on. The upside potential of that concept is exactly why we should push forward.",
                                "I see it the exact same way. It's a great opportunity to accelerate the outcome."
                            )
                            else -> listOf(
                                "Both sides have valid points here. It's really about finding that neutral baseline.",
                                "I'm carefully weighing both sides of that argument. It's definitely not black and white.",
                                "That's a complex angle. We need to balance the implications before moving too fast.",
                                "I think the middle ground is the safest bet here. Compliance and metrics are key.",
                                "I see the merit in both arguments. It's a very nuanced topic.",
                                "Let's evaluate the objective reality of that statement. There are pros and cons to both."
                            )
                        }
                        fallbackPhrases.random()
                    }
                }

                val finalReply = if (interjection.isNotEmpty()) "$interjection $replyText" else replyText

                withContext(Dispatchers.Main) {
                    if (phase != GDPhase.RUNNING) {
                        onDone()
                        return@withContext
                    }

                    isAgentThinking = false // UI state lock cleared instantly

                    val uid = "${utterancePrefix}_${System.currentTimeMillis()}"
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(u: String?) {
                            if (u == uid) {
                                scope.launch(Dispatchers.Main) {
                                    if (phase == GDPhase.RUNNING && floorOwner == agentName) {
                                        conversationHistory = conversationHistory + (agentName to finalReply)
                                        messages = messages + GDMessage(agentName, finalReply, agentColor)
                                        activeSpeaker = agentName
                                        activeSpeakerStartTime = System.currentTimeMillis()
                                    }
                                }
                            }
                        }
                        override fun onDone(u: String?) {
                            if (u == uid) {
                                scope.launch(Dispatchers.Main) {
                                    if (floorOwner == agentName) {
                                        activeSpeaker = ""
                                        floorOwner = ""
                                    }
                                    onDone()
                                }
                            }
                        }
                        override fun onError(u: String?) {
                            if (u == uid) {
                                scope.launch(Dispatchers.Main) {
                                    if (floorOwner == agentName) {
                                        activeSpeaker = ""
                                        floorOwner = ""
                                    }
                                    onDone()
                                }
                            }
                        }
                    })

                    val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, uid) }
                    // Speak ONLY the replyText, since the interjection was already spoken above!
                    val result = tts?.speak(replyText, TextToSpeech.QUEUE_ADD, params, uid)
                    
                    if (result == TextToSpeech.ERROR || tts == null) {
                        if (floorOwner == agentName) {
                            activeSpeaker = ""
                            floorOwner = ""
                        }
                        onDone()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (floorOwner == agentName) {
                        activeSpeaker = ""
                        floorOwner = ""
                        isAgentThinking = false
                    }
                    onDone()
                }
            }
        }
    }

    // ── Decentralised Autonomous Agent Loops ─────────────────────────────────
    // Each agent runs in its own independent coroutine — no sequential locks.
    // They continuously debate each other even when the user is silent.
    // After each utterance, a randomised 800-2000ms window creates natural competition.

    // Track whether autonomous debate loops have started
    var agentLoopsStarted by remember { mutableStateOf(false) }

    // Verbal interjection prefixes each agent uses to claim the floor
    val alexInterjections  = listOf("I hear you, but — ", "That's a valid point, however — ", "I have a slightly different take — ", "Let's look at the other side — ")
    val samInterjections   = listOf("I completely agree, and — ", "Adding to that thought — ", "That's exactly it, plus — ", "To build on that idea — ")
    val chrisInterjections = listOf("Let's bring this back to practical terms — ", "If we look at the actual numbers — ", "In the real world, — ", "What does this mean for execution? — ")

    fun launchAutonomousAgentLoops() {
        if (agentLoopsStarted) return
        agentLoopsStarted = true

        // ── Asynchronous AI-on-AI Interruption Monitor ──
        scope.launch(Dispatchers.Default) {
            while (phase == GDPhase.RUNNING) {
                delay((3000L..5000L).random())
                var currentSpeaker = ""
                withContext(Dispatchers.Main) { currentSpeaker = activeSpeaker }
                
                if (currentSpeaker.isNotEmpty() && currentSpeaker != "You") {
                    // 15% chance to interrupt someone else's speech
                    if ((1..100).random() <= 15 && conversationHistory.size > 2) {
                        val interrupters = listOf("Alex", "Sam", "Chris").filter { it != currentSpeaker }
                        val interrupter = interrupters.random()
                        
                        withContext(Dispatchers.Main) {
                            val speakerDuration = System.currentTimeMillis() - activeSpeakerStartTime
                            if (activeSpeaker == currentSpeaker && !isMicActive && phase == GDPhase.RUNNING && speakerDuration >= 10_000L) {
                                stopAllAgents()
                                floorOwner = interrupter
                                activeSpeaker = interrupter
                                interruptionCount++
                                
                                val role = when (interrupter) {
                                    "Alex" -> "against"
                                    "Sam" -> "for"
                                    else -> "neutral"
                                }
                                val color = when (interrupter) {
                                    "Alex" -> Color(0xFF60A5FA)
                                    "Sam" -> Color(0xFFF472B6)
                                    else -> Color(0xFF34D399)
                                }
                                val tts = when (interrupter) {
                                    "Alex" -> ttsAlex
                                    "Sam" -> ttsSam
                                    else -> ttsChris
                                }
                                val prefix = when (interrupter) {
                                    "Alex" -> "GD_ALEX"
                                    "Sam" -> "GD_SAM"
                                    else -> "GD_CHRIS"
                                }
                                val interjection = listOf(
                                    "Hold on, I have to jump in here — ",
                                    "Wait, let's be careful about that — ",
                                    "I see it a bit differently — "
                                ).random()
                                
                                speakAgentWithLLM(
                                    agentName = interrupter,
                                    agentRole = role,
                                    agentColor = color,
                                    tts = tts,
                                    utterancePrefix = "${prefix}_INT",
                                    interjection = interjection,
                                    onDone = {} 
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Alex (Against) independent loop ──
        scope.launch(Dispatchers.Default) {
            delay((100L..250L).random())
            while (phase == GDPhase.RUNNING) {
                val lastSpeaker = conversationHistory.lastOrNull()?.first
                if (!isMicActive && (floorOwner == "" || floorOwner == "Alex") && !isAgentThinking && lastSpeaker != "Alex") {
                    var acquired = false
                    withContext(Dispatchers.Main) {
                        if (!isMicActive && (floorOwner == "" || floorOwner == "Alex") && !isAgentThinking && lastSpeaker != "Alex") {
                            floorOwner = "Alex"
                            acquired = true
                        }
                    }
                    if (acquired) {
                        var done = false
                        withContext(Dispatchers.Main) {
                            // Prepend floor-claiming interjection if conditions are met
                            val useInterjection = (0..3).random() == 0 && conversationHistory.size > 2
                            val interjection = if (useInterjection) alexInterjections.random() else ""
                            speakAgentWithLLM(
                                agentName = "Alex", agentRole = "against",
                                agentColor = Color(0xFF60A5FA), tts = ttsAlex,
                                utterancePrefix = "GD_ALEX", interjection = interjection, onDone = { done = true }
                            )
                        }
                        while (!done && floorOwner == "Alex") { delay(100L) }
                    }
                }
                delay((150L..300L).random())
            }
        }

        // ── Sam (For) independent loop ──
        scope.launch(Dispatchers.Default) {
            delay((200L..350L).random())
            while (phase == GDPhase.RUNNING) {
                val lastSpeaker = conversationHistory.lastOrNull()?.first
                if (!isMicActive && (floorOwner == "" || floorOwner == "Sam") && !isAgentThinking && lastSpeaker != "Sam") {
                    var acquired = false
                    withContext(Dispatchers.Main) {
                        if (!isMicActive && (floorOwner == "" || floorOwner == "Sam") && !isAgentThinking && lastSpeaker != "Sam") {
                            floorOwner = "Sam"
                            acquired = true
                        }
                    }
                    if (acquired) {
                        var done = false
                        withContext(Dispatchers.Main) {
                            val useInterjection = (0..3).random() == 0 && conversationHistory.size > 2
                            val interjection = if (useInterjection) samInterjections.random() else ""
                            speakAgentWithLLM(
                                agentName = "Sam", agentRole = "for",
                                agentColor = Color(0xFFF472B6), tts = ttsSam,
                                utterancePrefix = "GD_SAM", interjection = interjection, onDone = { done = true }
                            )
                        }
                        while (!done && floorOwner == "Sam") { delay(100L) }
                    }
                }
                delay((150L..300L).random())
            }
        }

        // ── Chris (Neutral) independent loop ──
        scope.launch(Dispatchers.Default) {
            delay((300L..450L).random())
            while (phase == GDPhase.RUNNING) {
                val lastSpeaker = conversationHistory.lastOrNull()?.first
                if (!isMicActive && (floorOwner == "" || floorOwner == "Chris") && !isAgentThinking && lastSpeaker != "Chris") {
                    var acquired = false
                    withContext(Dispatchers.Main) {
                        if (!isMicActive && (floorOwner == "" || floorOwner == "Chris") && !isAgentThinking && lastSpeaker != "Chris") {
                            floorOwner = "Chris"
                            acquired = true
                        }
                    }
                    if (acquired) {
                        var done = false
                        withContext(Dispatchers.Main) {
                            val useInterjection = (0..3).random() == 0 && conversationHistory.size > 2
                            val interjection = if (useInterjection) chrisInterjections.random() else ""
                            speakAgentWithLLM(
                                agentName = "Chris", agentRole = "neutral",
                                agentColor = Color(0xFF34D399), tts = ttsChris,
                                utterancePrefix = "GD_CHRIS", interjection = interjection, onDone = { done = true }
                            )
                        }
                        while (!done && floorOwner == "Chris") { delay(100L) }
                    }
                }
                delay((150L..300L).random())
            }
        }
    }

    // ── Session Countdown ───────────────────────────────────────────────────
    LaunchedEffect(phase) {
        if (phase != GDPhase.RUNNING) return@LaunchedEffect
        while (timeLeft > 0 && phase == GDPhase.RUNNING) {
            delay(1000L)
            if (!isPreparingBackend) {
                timeLeft--
                if (timeLeft == 10) {
                    try {
                        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
                    } catch (e: Exception) {}
                }
            }
        }
        if (timeLeft <= 0 && phase == GDPhase.RUNNING) {
            phase = GDPhase.WRAPPING_UP
        }
    }

    // ── Grace Period (Wrap-Up) Monitor ──────────────────────────────────────
    LaunchedEffect(phase) {
        if (phase == GDPhase.WRAPPING_UP) {
            // Wait for any current active speaker or thinking agent to finish
            while (activeSpeaker.isNotEmpty() || isMicActive || isAgentThinking) {
                delay(500L)
            }
            
            // If the user spoke last just as the timer expired, allow one AI agent to give a final reply
            val lastSpeaker = conversationHistory.lastOrNull()?.first
            if (lastSpeaker == "You") {
                val responder = listOf("Alex", "Sam", "Chris").random()
                floorOwner = responder
                isAgentThinking = true
                var done = false
                val role = when (responder) { "Alex" -> "against"; "Sam" -> "for"; else -> "neutral" }
                val color = when (responder) { "Alex" -> Color(0xFF60A5FA); "Sam" -> Color(0xFFF472B6); else -> Color(0xFF34D399) }
                val tts = when (responder) { "Alex" -> ttsAlex; "Sam" -> ttsSam; else -> ttsChris }
                val prefix = when (responder) { "Alex" -> "GD_ALEX"; "Sam" -> "GD_SAM"; else -> "GD_CHRIS" }
                
                speakAgentWithLLM(
                    agentName = responder,
                    agentRole = role,
                    agentColor = color,
                    tts = tts,
                    utterancePrefix = prefix,
                    preProvidedText = null,
                    onDone = { done = true }
                )
                
                while (!done) { delay(100L) }
                while (activeSpeaker.isNotEmpty() || isAgentThinking) { delay(500L) }
            }
            
            // Formally conclude the discussion
            stopListening()
            stopAllAgents()
            activeSpeaker = ""
            floorOwner = ""
            
            initiativeScore = computeInitiativeScore(messages, userSpokeFirst)
            contentScore    = computeContentScore(messages, selectedTopic)
            cohesionScore   = computeCohesionScore(messages, interruptionCount)
            impactScore     = computeImpactScore(messages)
            
            val avgScore = (initiativeScore + contentScore + cohesionScore + impactScore) / 4
            val tag = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date()).uppercase()
            activityViewModel?.addLog(
                LogEntry(
                    date        = tag,
                    title       = "Group Discussion — ${selectedTopic.take(30)}",
                    status      = if (avgScore >= 50) "PASSED" else "NEEDS WORK",
                    credits     = avgScore / 10,
                    description = "Initiative: $initiativeScore | Content: $contentScore | Cohesion: $cohesionScore | Impact: $impactScore"
                )
            )
            phase = GDPhase.SCORECARD
        }
    }

    // ── SpeechRecognizer Listener ───────────────────────────────────────────
    DisposableEffect(speechRecognizer, phase) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {
                stopAllAgents()
                activeSpeaker = "You"
                floorOwner = "You"
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isMicActive = false }
            override fun onError(error: Int) {
                isMicActive = false
                activeSpeaker = ""
                floorOwner = ""
                isAgentThinking = false
                launchAutonomousAgentLoops()
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.takeIf { it.isNotBlank() }
                isMicActive = false
                if (text != null) {
                    if (!userHasSpoken && messages.none { it.isUser }) userSpokeFirst = true
                    userHasSpoken = true
                    conversationHistory = conversationHistory + ("You" to text)
                    messages = messages + GDMessage("You", text, accent, isUser = true)
                    
                    val lowerText = text.lowercase()
                    when {
                        lowerText.contains("alex") -> floorOwner = "Alex"
                        lowerText.contains("sam") -> floorOwner = "Sam"
                        lowerText.contains("chris") -> floorOwner = "Chris"
                        else -> floorOwner = "" // Allow any available agent to jump in
                    }
                } else {
                    floorOwner = ""
                }
                activeSpeaker = ""
                isAgentThinking = false
                // Agents will autonomously respond within their randomised delay windows
                launchAutonomousAgentLoops()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {}
    }

    // ── UI ──────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        GDOrbitBackground(accentColor = accent)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("HIRESPHERE", style = MaterialTheme.typography.labelSmall,
                                color = accent, letterSpacing = 2.sp, fontSize = 10.sp)
                            Text(
                                when (phase) {
                                    GDPhase.LOBBY    -> "Group Discussion"
                                    GDPhase.RUNNING, GDPhase.WRAPPING_UP -> selectedTopic
                                    GDPhase.SCORECARD -> "GD Audit Scorecard"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = textColor, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        if (phase == GDPhase.RUNNING) {
                            IconButton(onClick = {
                                stopAllAgents(); stopListening()
                                activeSpeaker = ""
                                floorOwner = ""
                                initiativeScore = computeInitiativeScore(messages, userSpokeFirst)
                                contentScore    = computeContentScore(messages, selectedTopic)
                                cohesionScore   = computeCohesionScore(messages, interruptionCount)
                                impactScore     = computeImpactScore(messages)
                                val avgScore = (initiativeScore + contentScore + cohesionScore + impactScore) / 4
                                val tag = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date()).uppercase()
                                activityViewModel?.addLog(LogEntry(tag, "Group Discussion — ${selectedTopic.take(30)}",
                                    if (avgScore >= 50) "PASSED" else "NEEDS WORK", avgScore / 10,
                                    "Initiative: $initiativeScore | Content: $contentScore | Cohesion: $cohesionScore | Impact: $impactScore"))
                                phase = GDPhase.SCORECARD
                            }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                        } else {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.Close, null, tint = textColor)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) },
                label = "gdPhase",
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) { currentPhase ->
                when (currentPhase) {
                    GDPhase.LOBBY -> GDLobbyView(
                        isDark = isDark, accent = accent, textColor = textColor, cardBg = cardBg,
                        selectedTopic = selectedTopic, topics = GD_TOPICS,
                        customTopicInput = customTopicInput,
                        isCustomTopicMode = isCustomTopicMode,
                        onTopicSelect = { selectedTopic = it },
                        onCustomTopicChange = { customTopicInput = it },
                        onModeToggle = { isCustomTopicMode = it },
                        onJoin = {
                            if (!hasMicPermission) {
                                permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isCustomTopicMode) {
                                    selectedTopic = customTopicInput
                                }
                                phase = GDPhase.RUNNING
                                isPreparingBackend = true
                                scope.launch {
                                    // 1. Fire a dummy ping to wake up the Render backend asynchronously
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val req = Request.Builder().url(apiBaseUrl.trimEnd('/') + "/health").get().build()
                                            httpClient.newCall(req).execute()
                                        } catch (e: Exception) {}
                                    }
                                    
                                    // 2. Explicit 2-second topic preparation delay
                                    delay(2000L)
                                    isPreparingBackend = false
                                    
                                    if (phase == GDPhase.RUNNING && !isMicActive && floorOwner == "") {
                                        floorOwner = "Alex"
                                        speakAgentWithLLM(
                                            agentName = "Alex",
                                            agentRole = "against",
                                            agentColor = Color(0xFF60A5FA),
                                            tts = ttsAlex,
                                            utterancePrefix = "GD_OPEN_ALEX",
                                            preProvidedText = "Alright, let's open the discussion on $selectedTopic. Personally, I'm quite skeptical because there are significant underlying risks that people often ignore.",
                                            onDone = {
                                                launchAutonomousAgentLoops()
                                            }
                                        )
                                    } else {
                                        launchAutonomousAgentLoops()
                                    }
                                }
                            }
                        }
                    )
                    GDPhase.RUNNING, GDPhase.WRAPPING_UP -> GDRunningView(
                        isDark = isDark, accent = accent, textColor = textColor,
                        cardBg = cardBg, surface = surface,
                        messages = messages, listState = listState,
                        activeSpeaker = activeSpeaker,
                        isMicActive = isMicActive,
                        isAgentThinking = isAgentThinking,
                        isPreparingBackend = isPreparingBackend,
                        timeLeft = timeLeft,
                        onMicToggle = {
                            if (isMicActive) {
                                stopListening()
                                activeSpeaker = ""
                                floorOwner = ""
                                isAgentThinking = false
                                launchAutonomousAgentLoops()
                            } else {
                                stopAllAgents()
                                startListening()
                            }
                        }
                    )
                    GDPhase.SCORECARD -> GDScorecardView(
                        isDark = isDark, accent = accent, textColor = textColor,
                        cardBg = cardBg, surface = surface,
                        initiativeScore = initiativeScore, contentScore = contentScore,
                        cohesionScore = cohesionScore, impactScore = impactScore,
                        topic = selectedTopic,
                        conversationHistory = conversationHistory,
                        onExit = onBack,
                        httpClient = httpClient,
                        apiBaseUrl = apiBaseUrl
                    )
                }
            }
        }
    }
}

// ── Lobby Phase ───────────────────────────────────────────────────────────────

@Composable
private fun GDLobbyView(
    isDark: Boolean, accent: Color, textColor: Color, cardBg: Color,
    selectedTopic: String, topics: List<String>,
    customTopicInput: String,
    isCustomTopicMode: Boolean,
    onTopicSelect: (String) -> Unit,
    onCustomTopicChange: (String) -> Unit,
    onModeToggle: (Boolean) -> Unit,
    onJoin: () -> Unit
) {
    val isValid = !isCustomTopicMode || validateCustomTopic(customTopicInput) >= 0.45f
    val scope = rememberCoroutineScope()
    var isRolling by remember { mutableStateOf(false) }

    val glassBg     = Color(0xFF1A1C1E).copy(alpha = 0.6f)
    val glassBorder = BorderStroke(1.dp, Color(0x3300F5FF))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Icon(Icons.Default.Groups, null, tint = accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("Panel Discussion Simulator", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text("3 Live AI Panelists · Real-time speech · Behavioral scoring", fontSize = 13.sp,
                color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))

            // Panelist card
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = glassBg),
                border = glassBorder
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Today's Panelists", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
                    Spacer(Modifier.height(16.dp))
                    GDPanelistRow("🔵", "Alex", "Always Against · Analytical Skeptic", Color(0xFF60A5FA))
                    Spacer(Modifier.height(12.dp))
                    GDPanelistRow("🌸", "Sam", "Always For · Visionary Strategist", Color(0xFFF472B6))
                    Spacer(Modifier.height(12.dp))
                    GDPanelistRow("🟢", "Chris", "Neutral · Pragmatic Operator", Color(0xFF34D399))
                    Spacer(Modifier.height(12.dp))
                    GDPanelistRow("⚡", "You (Rehan)", "Candidate · Your Live Perspective", accent)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Topic selection card
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = glassBg),
                border = glassBorder
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Discussion Topic Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Random Practice", "Manual Entry").forEach { tab ->
                            val isSelected = (tab == "Manual Entry" && isCustomTopicMode) || (tab == "Random Practice" && !isCustomTopicMode)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { onModeToggle(tab == "Manual Entry") }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isSelected) accent else textColor.copy(alpha = 0.6f),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (!isCustomTopicMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = selectedTopic,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (!isRolling) {
                                    isRolling = true
                                    scope.launch {
                                        listOf(50L, 50L, 50L, 50L, 100L, 100L, 200L, 200L, 400L).forEach { d ->
                                            delay(d); onTopicSelect(topics.random())
                                        }
                                        isRolling = false
                                    }
                                }
                            },
                            enabled = !isRolling,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, accent)
                        ) {
                            Text(
                                text = if (isRolling) "🎲 SHUFFLING..." else "🎲 ROLL RANDOM CASE STUDY",
                                color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = customTopicInput,
                            onValueChange = onCustomTopicChange,
                            label = { Text("Custom Topic Statement", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent, unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = accent, cursorColor = accent,
                                focusedTextColor = textColor, unfocusedTextColor = textColor
                            ),
                            maxLines = 2
                        )
                        Spacer(Modifier.height(14.dp))
                        if (customTopicInput.isNotEmpty()) {
                            val score = validateCustomTopic(customTopicInput)
                            val invalid = score < 0.45f
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth().height(48.dp)
                                    .background((if (invalid) Color.Red else accent).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(1.dp, (if (invalid) Color.Red else accent).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (invalid) {
                                    Text("⚠ Topic Unfit for Simulation (Provide more industrial context)", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("✓", color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Topic Validated (Score: ${"%.2f".format(score)})", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }

        // Fixed bottom action bar — Galactic Cyan / Teal alpha glow gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00F5FF).copy(alpha = 0.06f),
                            Color(0xFF00F5FF).copy(alpha = 0.18f)
                        )
                    )
                )
                .padding(20.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
                    label = "btnScale"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth().height(52.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isValid) accent else Color.Gray.copy(alpha = 0.3f))
                        .pointerInput(isValid) {
                            if (isValid) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        try { awaitRelease() } finally { isPressed = false }
                                    },
                                    onTap = { onJoin() }
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, null,
                            tint = if (isValid) Color.Black else Color.DarkGray,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("START SIMULATION", fontWeight = FontWeight.ExtraBold,
                            color = if (isValid) Color.Black else Color.DarkGray,
                            letterSpacing = 1.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Microphone required · 5 min session · Live AI evaluation",
                    fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun GDPanelistRow(emoji: String, name: String, role: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            Text(role, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ── Running Phase ─────────────────────────────────────────────────────────────

@Composable
private fun GDRunningView(
    isDark: Boolean, accent: Color, textColor: Color, cardBg: Color, surface: Color,
    messages: List<GDMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    activeSpeaker: String,
    isMicActive: Boolean,
    isAgentThinking: Boolean,
    isPreparingBackend: Boolean,
    timeLeft: Int,
    onMicToggle: () -> Unit
) {
    val agents = listOf(
        Triple("Alex",  "🔵", Color(0xFF60A5FA)),
        Triple("Sam",   "🌸", Color(0xFFF472B6)),
        Triple("Chris", "🟢", Color(0xFF34D399)),
        Triple("You",   "⚡",  accent)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Column(Modifier.fillMaxSize()) {
        
        if (isPreparingBackend) {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp).padding(16.dp).clip(RoundedCornerShape(18.dp)).background(cardBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accent, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Scanning Topic Insights & Initiating AI...", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            // Avatar Grid
            Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            agents.chunked(2).forEach { pair ->
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { (name, emoji, color) ->
                        val isActive = activeSpeaker == name || activeSpeaker.startsWith(name)
                        Card(
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isActive) color.copy(alpha = 0.12f) else cardBg),
                            border = BorderStroke(
                                width = if (isActive) 2.5.dp else 1.dp,
                                color = if (isActive) accent.copy(alpha = glowAlpha) else Color.Gray.copy(0.2f)
                            ),
                            elevation = CardDefaults.cardElevation(if (isActive) 8.dp else 0.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emoji, fontSize = 28.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = if (isActive) accent else textColor)
                                    AnimatedVisibility(visible = isActive) {
                                        Text(
                                            if (isAgentThinking && name != "You") "Thinking…" else "Speaking…",
                                            fontSize = 10.sp, color = accent, letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Agent thinking indicator
        AnimatedVisibility(visible = isAgentThinking) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = accent
                )
                Spacer(Modifier.width(8.dp))
                Text("Agent formulating response…", fontSize = 11.sp, color = Color.Gray, letterSpacing = 0.5.sp)
            }
        }

        // Dialogue Feed
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!msg.isUser) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(msg.color.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) { Text(msg.speaker.first().toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = msg.color) }
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start) {
                        Text(msg.speaker, fontSize = 10.sp, color = msg.color, fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 2.dp))
                        Card(
                            shape = RoundedCornerShape(
                                topStart = if (msg.isUser) 16.dp else 4.dp,
                                topEnd = if (msg.isUser) 4.dp else 16.dp,
                                bottomStart = 16.dp, bottomEnd = 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (msg.isUser) accent.copy(0.15f) else Color(0xFF18181B)
                            ),
                            border = BorderStroke(1.dp, msg.color.copy(0.3f)),
                            modifier = Modifier.widthIn(max = 260.dp)
                        ) {
                            Text(msg.text, fontSize = 13.sp, color = textColor, lineHeight = 18.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                        }
                    }
                }
            }
        }

        // Session controls
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            val progressFraction = timeLeft / 300f
            val progressColor = when {
                progressFraction > 0.5f -> accent
                progressFraction > 0.25f -> Color(0xFFFBBF24)
                else -> Color(0xFFEF4444)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val mins = timeLeft / 60; val secs = timeLeft % 60
                Text("%02d:%02d".format(mins, secs), fontSize = 13.sp, color = progressColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = progressColor, trackColor = Color.Gray.copy(0.2f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onMicToggle,
                enabled = true,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMicActive) Color(0xFF10B981) else accent.copy(alpha = 0.15f),
                    disabledContainerColor = Color.Gray.copy(0.2f)
                ),
                border = BorderStroke(1.5.dp, if (isMicActive) Color(0xFF10B981) else accent)
            ) {
                Icon(
                    imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Mic",
                    tint = if (isMicActive) Color.Black else accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = when {
                        isAgentThinking -> "AGENTS THINKING — TAP TO INTERRUPT"
                        isMicActive -> "SPEAKING — TAP TO YIELD"
                        else -> "TAP TO SPEAK"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isMicActive) Color.Black else accent,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Scorecard Phase ───────────────────────────────────────────────────────────

@Composable
private fun GDScorecardView(
    isDark: Boolean, accent: Color, textColor: Color, cardBg: Color, surface: Color,
    initiativeScore: Int, contentScore: Int, cohesionScore: Int, impactScore: Int,
    topic: String, conversationHistory: List<Pair<String, String>>, onExit: () -> Unit,
    httpClient: okhttp3.OkHttpClient, apiBaseUrl: String
) {
    val avgScore = (initiativeScore + contentScore + cohesionScore + impactScore) / 4
    val passed = avgScore >= 50
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var aiInsights by remember { mutableStateOf<String?>(null) }
    var isLoadingInsights by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val chatHistoryArray = org.json.JSONArray()
                for ((speaker, text) in conversationHistory) {
                    chatHistoryArray.put("[$speaker]: $text")
                }
                
                val requestBody = org.json.JSONObject().apply {
                    put("target_role", "Expert Evaluator")
                    put("job_description", "You are an expert GD Evaluator assessing the user 'Rehan' on the topic '$topic'. Provide concise behavioral feedback. Format your response clearly: 1) What Rehan did well. 2) What Rehan should improve, specifically referencing actual points from the chat. Address Rehan directly as 'you'. Instead of just printing what Rehan should have said, explain *why* saying it would have been better. Do not mention numeric scores. Output plain text directly, no JSON formatting.")
                    put("vault_data", "")
                    put("chat_history", chatHistoryArray.toString())
                    put("user_audio_text", "")
                    put("elapsed_seconds", 0)
                }.toString()

                val req = okhttp3.Request.Builder()
                    .url(apiBaseUrl.trimEnd('/') + "/v1/ai/live-interview")
                    .post(okhttp3.RequestBody.create("application/json".toMediaType(), requestBody))
                    .build()
                    
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    var bodyString = resp.body?.string()?.trim() ?: ""
                    if (bodyString.startsWith("```json")) {
                        bodyString = bodyString.removePrefix("```json").removeSuffix("```").trim()
                    }
                    try {
                        val jsonObject = org.json.JSONObject(bodyString)
                        aiInsights = jsonObject.optString("reply", "").ifBlank { jsonObject.optString("ai_reply", "") }.ifBlank { bodyString }
                    } catch (e: Exception) {
                        aiInsights = bodyString
                    }
                } else {
                    aiInsights = "Unable to fetch AI insights at this time."
                }
            } catch (e: Exception) {
                aiInsights = "Network error: Could not fetch AI insights."
            } finally {
                isLoadingInsights = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(Icons.Default.Groups, null, tint = accent, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(12.dp))
            Text("GD Behavioral Audit", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text("Topic: $topic", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (passed) Color(0xFF10B981).copy(0.12f) else Color(0xFFEF4444).copy(0.10f)),
                border = BorderStroke(1.5.dp, if (passed) Color(0xFF10B981) else Color(0xFFEF4444))
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$avgScore", fontSize = 52.sp, fontWeight = FontWeight.Black,
                        color = if (passed) Color(0xFF10B981) else Color(0xFFEF4444))
                    Text("Overall Score", fontSize = 14.sp, color = textColor)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (passed) "✓  STRONG PERFORMANCE" else "⚠  NEEDS DEVELOPMENT",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (passed) Color(0xFF10B981) else Color(0xFFEF4444),
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Behavioral Metrics", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                color = textColor, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        items(
            listOf(
                Triple("Initiative & Leadership", initiativeScore, "Did you open the topic or guide the discussion forward?"),
                Triple("Content & Relevance", contentScore, "Structural analytical depth and keyword relevance to the topic."),
                Triple("Team Cohesion & Etiquette", cohesionScore, "Balance of polite interjections vs. aggressive overrides."),
                Triple("Communication Impact", impactScore, "Quality and clarity of your spoken contributions.")
            )
        ) { (title, score, desc) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E).copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, accent.copy(0.15f))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor,
                            modifier = Modifier.weight(1f))
                        Text("$score", fontSize = 22.sp, fontWeight = FontWeight.Black,
                            color = when {
                                score >= 70 -> Color(0xFF10B981)
                                score >= 40 -> Color(0xFFFBBF24)
                                else -> Color(0xFFEF4444)
                            })
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = when {
                            score >= 70 -> Color(0xFF10B981)
                            score >= 40 -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        },
                        trackColor = Color.Gray.copy(0.15f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(desc, fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp)
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E).copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, accent.copy(0.3f))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(androidx.compose.material.icons.Icons.Default.Groups, null, tint = accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AI Behavioral Insights", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (isLoadingInsights) {
                        Text("Analyzing transcript...", fontSize = 14.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else {
                        Text(aiInsights ?: "", fontSize = 14.sp, color = textColor, lineHeight = 20.sp)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { coroutineScope.launch { saveTranscriptToDownloads(context, topic, conversationHistory) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, accent)
            ) { Text("DOWNLOAD TRANSCRIPT", fontWeight = FontWeight.Bold, color = accent, letterSpacing = 1.sp) }
            
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text("RETURN TO HUB", fontWeight = FontWeight.ExtraBold, color = Color.Black, letterSpacing = 1.sp) }
        }
    }
}

suspend fun saveTranscriptToDownloads(context: Context, topic: String, history: List<Pair<String, String>>) {
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "GD_Transcript_${topic.replace(" ", "_")}.txt")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HireSphere")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val text = java.lang.StringBuilder()
                    text.append("=== Group Discussion Transcript ===\n")
                    text.append("Topic: $topic\n\n")
                    for ((spk, msg) in history) {
                        text.append("$spk: $msg\n\n")
                    }
                    outputStream.write(text.toString().toByteArray())
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Transcript saved to Downloads/HireSphere", Toast.LENGTH_LONG).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
