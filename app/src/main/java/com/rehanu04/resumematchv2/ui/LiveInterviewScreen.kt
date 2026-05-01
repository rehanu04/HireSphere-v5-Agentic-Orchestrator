@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.rehanu04.resumematchv2.data.UserProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.random.Random

data class LiveInterviewRequest(
    @SerializedName("target_role") val targetRole: String,
    @SerializedName("job_description") val jobDescription: String,
    @SerializedName("vault_data") val vaultData: String,
    @SerializedName("chat_history") val chatHistory: String,
    @SerializedName("user_audio_text") val userAudioText: String,
    @SerializedName("elapsed_seconds") val elapsedSeconds: Int
)

data class LiveInterviewResponse(
    @SerializedName("ai_reply") val aiReply: String,
    @SerializedName("is_concluded") val isConcluded: Boolean
)

data class ChatTurn(val role: String, val text: String)

data class InterviewFeedback(
    val hireability: String,
    val communicationFeedback: String,
    val technicalFeedback: String,
    val improvementAreas: List<String>,
    val vaultCorrections: List<String>
)

@Composable
fun LiveInterviewScreen(
    onBack: () -> Unit,
    userProfileStore: UserProfileStore,
    apiBaseUrl: String
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // INCREASED TIMEOUT TO 120s FOR RENDER COLD STARTS
    val httpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = null)
    val vaultData = remember(userProfile) {
        "Skills: ${userProfile?.savedSkillsJson}\nExperience: ${userProfile?.savedExperienceJson}\nProjects: ${userProfile?.savedProjectsJson}"
    }

    var hasMicPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasMicPermission = granted }

    var isStarted by remember { mutableStateOf(false) }
    var isInterviewing by remember { mutableStateOf(false) }
    var isConcluded by remember { mutableStateOf(false) }

    var isAIThinking by remember { mutableStateOf(false) }
    var isAITalking by remember { mutableStateOf(false) }
    var isMicHot by remember { mutableStateOf(false) }

    var transcript by remember { mutableStateOf(listOf<ChatTurn>()) }
    var currentTranscript by remember { mutableStateOf("") }
    var elapsedSeconds by remember { mutableStateOf(0) }
    val maxSeconds = 300

    var isFetchingFeedback by remember { mutableStateOf(false) }
    var feedbackData by remember { mutableStateOf<InterviewFeedback?>(null) }
    var postInterviewTab by remember { mutableStateOf(0) }

    var showExitDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    var speechRecognizer: SpeechRecognizer? by remember { mutableStateOf(null) }

    fun startContinuousListening() {
        if (!hasMicPermission || isConcluded || isAITalking || isAIThinking || !isInterviewing) return

        ctx.mainExecutor.execute {
            isMicHot = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
            }
            speechRecognizer?.startListening(intent)
        }
    }

    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.05f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        isAITalking = false
                        if (isInterviewing && !isConcluded) {
                            ctx.mainExecutor.execute { startContinuousListening() }
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        isAITalking = false
                        ctx.mainExecutor.execute { startContinuousListening() }
                    }
                })
            }
        }
        onDispose { tts?.shutdown() }
    }

    DisposableEffect(Unit) {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
        onDispose { speechRecognizer?.destroy() }
    }

    fun fetchFeedbackReport() {
        isFetchingFeedback = true
        scope.launch {
            try {
                val historyString = transcript.joinToString("\n") { "${it.role}: ${it.text}" }
                val jsonBody = JSONObject().apply {
                    put("target_role", userProfile?.targetRole ?: "Software Engineer")
                    put("chat_history", historyString)
                }.toString()

                val req = Request.Builder()
                    .url(apiBaseUrl.trimEnd('/') + "/v1/ai/interview-feedback")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val responseStr = withContext(Dispatchers.IO) {
                    httpClient.newCall(req).execute().use { response -> if (response.isSuccessful) response.body?.string() else null }
                }

                if (responseStr != null) {
                    val parsed = JSONObject(responseStr)

                    val areasArray = parsed.optJSONArray("improvement_areas")
                    val areasList = mutableListOf<String>()
                    if (areasArray != null) {
                        for (i in 0 until areasArray.length()) areasList.add(areasArray.getString(i))
                    }

                    val correctionsArray = parsed.optJSONArray("vault_corrections")
                    val correctionsList = mutableListOf<String>()
                    if (correctionsArray != null) {
                        for (i in 0 until correctionsArray.length()) correctionsList.add(correctionsArray.getString(i))
                    }

                    feedbackData = InterviewFeedback(
                        hireability = parsed.optString("hireability", "Unknown"),
                        communicationFeedback = parsed.optString("communication_feedback", ""),
                        technicalFeedback = parsed.optString("technical_feedback", ""),
                        improvementAreas = areasList,
                        vaultCorrections = correctionsList
                    )
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Unknown Error"
                Toast.makeText(ctx, "Scorecard Error: $err", Toast.LENGTH_LONG).show()
            } finally {
                isFetchingFeedback = false
            }
        }
    }

    fun endInterview(timeUp: Boolean = false) {
        isInterviewing = false
        isAIThinking = false
        isMicHot = false
        speechRecognizer?.stopListening()

        if (timeUp) {
            isAITalking = true
            tts?.speak("Our time is up. Let's conclude the interview and review your scorecard.", TextToSpeech.QUEUE_FLUSH, null, "LiveInterviewTTS_END")
            scope.launch {
                delay(4000)
                isAITalking = false
                isConcluded = true
                fetchFeedbackReport()
            }
        } else {
            isConcluded = true
            tts?.stop()
            fetchFeedbackReport()
        }
    }

    LaunchedEffect(isInterviewing) {
        while (isInterviewing && elapsedSeconds < maxSeconds) {
            delay(1000)
            elapsedSeconds++
        }
        if (elapsedSeconds >= maxSeconds && isInterviewing) {
            endInterview(timeUp = true)
        }
    }

    BackHandler {
        if (isInterviewing) {
            Toast.makeText(ctx, "Please end the interview first.", Toast.LENGTH_SHORT).show()
        } else if (isConcluded) {
            showExitDialog = true
        } else {
            onBack()
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Scorecard?") },
            text = { Text("Are you sure you want to leave? Your scorecard will not be saved.") },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onBack() }) { Text("Exit", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Cancel", color = Color(0xFF38BDF8)) }
            }
        )
    }

    fun speak(text: String) {
        isAITalking = true
        isMicHot = false
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LiveInterviewTTS_${System.currentTimeMillis()}")
    }

    fun sendAudioToGemini(finalizedText: String, isSystemTrigger: Boolean = false) {
        if (finalizedText.isBlank() && !isSystemTrigger) {
            startContinuousListening()
            return
        }

        isMicHot = false
        if (!isSystemTrigger) {
            transcript = transcript + ChatTurn("Candidate", finalizedText)
        }
        currentTranscript = ""
        isAIThinking = true

        scope.launch {
            try {
                val reqObj = LiveInterviewRequest(
                    targetRole = userProfile?.targetRole ?: "Software Engineer",
                    jobDescription = "General tech interview based on profile.",
                    vaultData = vaultData,
                    chatHistory = Gson().toJson(transcript),
                    userAudioText = finalizedText,
                    elapsedSeconds = elapsedSeconds
                )

                val reqBuilder = Request.Builder()
                    .url(apiBaseUrl.trimEnd('/') + "/v1/ai/live-interview")
                    .post(Gson().toJson(reqObj).toRequestBody("application/json".toMediaType()))
                    .build()

                val response = withContext(Dispatchers.IO) { httpClient.newCall(reqBuilder).execute() }
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val aiRes = Gson().fromJson(body, LiveInterviewResponse::class.java)
                    transcript = transcript + ChatTurn("Interviewer", aiRes.aiReply)
                    isAIThinking = false

                    if (aiRes.isConcluded || elapsedSeconds >= maxSeconds) {
                        endInterview(timeUp = false)
                    } else {
                        speak(aiRes.aiReply)
                    }
                } else {
                    isAIThinking = false
                    if (!isSystemTrigger) transcript = transcript + ChatTurn("System", "Connection error. Retrying...")
                    startContinuousListening()
                }
            } catch (e: Exception) {
                isAIThinking = false
                val err = e.localizedMessage ?: "Unknown Error"
                Toast.makeText(ctx, "Network Error: Server taking too long to respond. Try again.", Toast.LENGTH_LONG).show()
                startContinuousListening()
            }
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isMicHot = false }
            override fun onError(error: Int) {
                isMicHot = false
                if (isInterviewing && !isAITalking && !isAIThinking) {
                    startContinuousListening()
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty() && matches[0].isNotBlank()) {
                    sendAudioToGemini(matches[0])
                } else {
                    if (isInterviewing && !isAITalking && !isAIThinking) startContinuousListening()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) currentTranscript = matches[0]
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { }
    }

    fun startInterview() {
        if (!hasMicPermission) { permLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        isStarted = true; isInterviewing = true; isConcluded = false; elapsedSeconds = 0
        transcript = emptyList(); currentTranscript = ""

        val sysPrompt = "[SYSTEM INITIALIZATION: The interview is starting. Give a brief, welcoming opening statement. Address the candidate as Rehan. Do not wait for a response, just greet and ask the first question.]"
        sendAudioToGemini(sysPrompt, isSystemTrigger = true)
    }

    val luxuryBgColor = Color(0xFF030303)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isConcluded) "Interview Complete" else "MasterR Technical Interview", color = Color.White, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    if (!isInterviewing) IconButton(onClick = {
                        if (isConcluded) showExitDialog = true else onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    if (isInterviewing) {
                        IconButton(onClick = { endInterview(timeUp = false) }) { Icon(Icons.Filled.Close, "End Early", tint = Color.Gray) }
                    }
                }
            )
        },
        containerColor = luxuryBgColor
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            StarfieldBackground()

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isStarted && !isConcluded) {
                    Spacer(Modifier.weight(1f))
                    Box(modifier = Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
                        ParticleBlobOrb(isThinking = false, isTalking = false, isListening = false)
                    }
                    Spacer(Modifier.height(48.dp))
                    Text("Ready to begin?", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Light)
                    Spacer(Modifier.height(16.dp))
                    Text("Hands-free mode active.\nThe AI will evaluate you like a Senior Recruiter.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(Modifier.height(48.dp))
                    Button(
                        onClick = { startInterview() },
                        modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Initiate Session", style = MaterialTheme.typography.titleSmall, color = Color.White, letterSpacing = 1.5.sp)
                    }
                    Spacer(Modifier.weight(1f))
                } else if (isConcluded) {
                    Column(Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = postInterviewTab,
                            containerColor = Color.Transparent,
                            contentColor = Color.White,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[postInterviewTab]),
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        ) {
                            Tab(selected = postInterviewTab == 0, onClick = { postInterviewTab = 0 }, text = { Text("AI Scorecard") })
                            Tab(selected = postInterviewTab == 1, onClick = { postInterviewTab = 1 }, text = { Text("Full Transcript") })
                        }

                        if (postInterviewTab == 0) {
                            if (isFetchingFeedback) {
                                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator(color = Color(0xFF475569))
                                    Spacer(Modifier.height(16.dp))
                                    Text("Generating SROM Feedback Report...", color = Color.Gray)
                                }
                            } else if (feedbackData != null) {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f))
                                        ) {
                                            Column(Modifier.padding(24.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF10B981))
                                                    Spacer(Modifier.width(12.dp))
                                                    Text("Hireability Assessment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                                Spacer(Modifier.height(8.dp))
                                                Text(feedbackData!!.hireability, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                                HorizontalDivider(Modifier.padding(vertical = 16.dp), color = Color.DarkGray)

                                                Text("Communication Analysis", fontWeight = FontWeight.Bold, color = Color.LightGray)
                                                Spacer(Modifier.height(4.dp))
                                                Text(feedbackData!!.communicationFeedback, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                                Spacer(Modifier.height(16.dp))

                                                Text("Technical Accuracy", fontWeight = FontWeight.Bold, color = Color.LightGray)
                                                Spacer(Modifier.height(4.dp))
                                                Text(feedbackData!!.technicalFeedback, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                                Spacer(Modifier.height(24.dp))

                                                Text("Critical Areas for Improvement", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                                feedbackData!!.improvementAreas.forEach { point ->
                                                    Row(modifier = Modifier.padding(top = 8.dp)) {
                                                        Icon(Icons.Filled.Warning, null, modifier = Modifier.size(16.dp).padding(top=2.dp), tint = Color(0xFFEF4444))
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(point, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                                    }
                                                }

                                                if (feedbackData!!.vaultCorrections.isNotEmpty()) {
                                                    Spacer(Modifier.height(24.dp))
                                                    Text("Vault Corrections Detected", fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                                                    Text("The AI noted these factual corrections during your chat:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                                    feedbackData!!.vaultCorrections.forEach { point ->
                                                        Row(modifier = Modifier.padding(top = 8.dp)) {
                                                            Icon(Icons.Filled.Memory, null, modifier = Modifier.size(16.dp).padding(top=2.dp), tint = Color(0xFFD4AF37))
                                                            Spacer(Modifier.width(8.dp))
                                                            Text(point, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(32.dp))
                                        Button(
                                            onClick = { showExitDialog = true },
                                            modifier = Modifier.fillMaxWidth().height(50.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                        ) {
                                            Text("Return to Vault", color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                                items(transcript) { msg ->
                                    val isCandidate = msg.role == "Candidate"
                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = if (isCandidate) Alignment.End else Alignment.Start) {
                                        Text(if (isCandidate) "YOU" else "MASTERR", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.5.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = if (isCandidate) Color(0xFF334155) else Color(0xFF1E293B)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.padding(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {

                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (elapsedSeconds > 240) MaterialTheme.colorScheme.error else Color.Gray,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(Modifier.weight(1f))

                    Box(modifier = Modifier.fillMaxWidth().height(380.dp), contentAlignment = Alignment.Center) {
                        ParticleBlobOrb(isThinking = isAIThinking, isTalking = isAITalking, isListening = isMicHot)
                    }

                    Spacer(Modifier.weight(1f))

                    val statusText = when {
                        isMicHot -> "MASTERR IS LISTENING..."
                        isAIThinking -> "ANALYZING VECTORS..."
                        isAITalking -> "SYNTHESIZING RESPONSE..."
                        else -> "SYSTEM INITIALIZING..."
                    }
                    val statusColor = when {
                        isMicHot -> Color(0xFF10B981)
                        isAIThinking -> Color(0xFFD4AF37)
                        isAITalking -> Color(0xFF38BDF8)
                        else -> Color.DarkGray
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Box(
                        modifier = Modifier
                            .padding(bottom = 56.dp)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isMicHot) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isMicHot) Color(0xFF10B981) else Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                            Icon(if (isMicHot) Icons.Filled.Mic else Icons.Filled.Close, contentDescription = "Mic Status", tint = if (isMicHot) Color.Black else Color.Gray, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarfieldBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), repeatMode = RepeatMode.Restart)
    )

    val stars = remember {
        List(150) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { index, star ->
            val (x, y, starSize) = star
            val drift = (time * 0.01f * (starSize + 0.5f)) % 1f
            val actualY = (y + drift) % 1f
            val twinkle = (sin((time * 0.1f + index).toDouble()).toFloat() + 1f) / 2f

            drawCircle(
                color = Color.White.copy(alpha = 0.1f + (twinkle * 0.3f)),
                radius = starSize * 2.5f,
                center = Offset(x * size.width, actualY * size.height)
            )
        }
    }
}

@Composable
fun ParticleBlobOrb(isThinking: Boolean, isTalking: Boolean, isListening: Boolean) {

    val targetColor = when {
        isListening -> Color(0xFF10B981)
        isThinking -> Color(0xFFD4AF37)
        isTalking -> Color(0xFF38BDF8)
        else -> Color(0xFF1E3A8A)
    }

    val coreColor by animateColorAsState(targetValue = targetColor, animationSpec = tween(1500))

    var rotX by remember { mutableFloatStateOf(0f) }
    var rotY by remember { mutableFloatStateOf(0f) }
    var rotZ by remember { mutableFloatStateOf(0f) }
    var waveTime by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (true) {
            val currentTime = withFrameNanos { it }
            val deltaMs = (currentTime - lastTime) / 1_000_000f
            lastTime = currentTime

            rotY += deltaMs * 0.0004f
            rotX += deltaMs * 0.0002f
            rotZ += deltaMs * 0.0003f
            waveTime += deltaMs * 0.003f
        }
    }

    val targetBounce = if (isTalking) 0.15f else if (isListening) 0.08f else 0.02f
    val currentBounce by animateFloatAsState(targetValue = targetBounce, animationSpec = tween(400, easing = FastOutSlowInEasing))

    Canvas(modifier = Modifier.fillMaxSize()) {
        val numPoints = 800
        val goldenRatio = (1.0 + sqrt(5.0)) / 2.0
        val angleIncrement = Math.PI * 2.0 * goldenRatio

        val radius = size.width / 3.4f
        val center = Offset(size.width / 2, size.height / 2)
        val focalLength = radius * 3.0f

        val glowRadius = radius * 2.5f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(coreColor.copy(alpha = 0.25f), Color.Transparent),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )

        data class Point3D(val z: Float, val screenX: Float, val screenY: Float, val pointRadius: Float, val alpha: Float)
        val projectedPoints = mutableListOf<Point3D>()

        val rotZ_D = rotZ.toDouble()
        val rotX_D = rotX.toDouble()
        val rotY_D = rotY.toDouble()

        for (i in 0 until numPoints) {
            val t = i.toDouble() / numPoints.toDouble()
            val phi = acos(1.0 - 2.0 * t)
            val theta = angleIncrement * i

            val startX = (sin(phi) * cos(theta)).toFloat()
            val startY = (sin(phi) * sin(theta)).toFloat()
            val startZ = (cos(phi)).toFloat()

            val x1 = (startX * cos(rotZ_D) - startY * sin(rotZ_D)).toFloat()
            val y1 = (startX * sin(rotZ_D) + startY * cos(rotZ_D)).toFloat()
            val y2 = (y1 * cos(rotX_D) - startZ * sin(rotX_D)).toFloat()
            val z2 = (y1 * sin(rotX_D) + startZ * cos(rotX_D)).toFloat()
            val finalX = (x1 * cos(rotY_D) - z2 * sin(rotY_D)).toFloat()
            val finalZ = (x1 * sin(rotY_D) + z2 * cos(rotY_D)).toFloat()

            val waveSpeed = if (isTalking) 8f else 3f
            val wTime = waveTime * waveSpeed

            val waveX = sin((finalX * 4f + wTime).toDouble()).toFloat()
            val waveY = cos((y2 * 4f - wTime * 0.8f).toDouble()).toFloat()
            val waveZ = sin((finalZ * 4f + wTime * 1.2f).toDouble()).toFloat()

            val organicDeformation = (waveX + waveY + waveZ) / 3f
            val currentRadius = radius * (1f + (organicDeformation * currentBounce))

            val pulseX = finalX * currentRadius
            val pulseY = y2 * currentRadius
            val pulseZ = finalZ * currentRadius

            val perspectiveScale = focalLength / (focalLength - pulseZ)
            val screenX = center.x + (pulseX * perspectiveScale)
            val screenY = center.y + (pulseY * perspectiveScale)

            val pointRadius = 2.0f * perspectiveScale
            val depthRatio = (pulseZ + radius) / (radius * 2f)

            var alpha = depthRatio.coerceIn(0.3f, 1.0f)
            if (pulseZ < 0) alpha *= 0.7f

            projectedPoints.add(Point3D(pulseZ, screenX, screenY, pointRadius, alpha))
        }

        projectedPoints.sortBy { it.z }

        for (pt in projectedPoints) {
            drawCircle(
                color = coreColor.copy(alpha = pt.alpha),
                radius = pt.pointRadius,
                center = Offset(pt.screenX, pt.screenY)
            )
        }
    }
}