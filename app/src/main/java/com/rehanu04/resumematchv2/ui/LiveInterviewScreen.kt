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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rehanu04.resumematchv2.data.UserProfileStore
import com.rehanu04.resumematchv2.util.isOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class VoiceChatMessage(val role: String, val text: String)

data class InterviewFeedback(
    val hireability: String,
    val communicationFeedback: String,
    val technicalFeedback: String,
    val improvementAreas: List<String>
)

@Composable
fun LiveInterviewScreen(
    onBack: () -> Unit,
    userProfileStore: UserProfileStore,
    apiBaseUrl: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    var isSetupPhase by remember { mutableStateOf(true) }
    var targetRole by remember { mutableStateOf(userProfile.targetRole) }
    var jobDescription by remember { mutableStateOf("") }

    var roleMenuExpanded by remember { mutableStateOf(false) }
    val commonRoles = listOf("Software Engineer", "Backend Developer", "Frontend Developer", "Full Stack Engineer", "Data Scientist", "Machine Learning Engineer", "Product Manager", "Android Developer")

    var hasMicPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var isListening by remember { mutableStateOf(false) }
    var isThinking by remember { mutableStateOf(false) }
    var isAiSpeaking by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf(mutableListOf<VoiceChatMessage>()) }
    var partialSpeech by remember { mutableStateOf("") }

    var isConcluded by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }

    var isFetchingFeedback by remember { mutableStateOf(false) }
    var feedbackData by remember { mutableStateOf<InterviewFeedback?>(null) }
    var postInterviewTab by remember { mutableStateOf(0) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) Toast.makeText(context, "Microphone permission is required.", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(isSetupPhase, isConcluded) {
        if (!isSetupPhase && !isConcluded) {
            while (true) {
                delay(1000)
                timerSeconds++
            }
        }
    }
    val timeString = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60)

    // --- Infinite Continuous Time for 3D Math ---
    var continuousTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                continuousTime += (frameTime - lastFrame) / 1_000_000_000f
                lastFrame = frameTime
            }
        }
    }

    fun fetchFeedbackReport(historyString: String) {
        isFetchingFeedback = true
        scope.launch {
            try {
                val client = OkHttpClient.Builder().connectTimeout(120, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
                val jsonBody = JSONObject().apply {
                    put("target_role", targetRole)
                    put("chat_history", historyString)
                }.toString()

                val req = Request.Builder()
                    .url(apiBaseUrl.trimEnd('/') + "/v1/ai/interview-feedback")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val responseStr = withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use { response -> if (response.isSuccessful) response.body?.string() else null }
                }

                if (responseStr != null) {
                    val parsed = JSONObject(responseStr)
                    val areasArray = parsed.optJSONArray("improvement_areas")
                    val areasList = mutableListOf<String>()
                    if (areasArray != null) {
                        for (i in 0 until areasArray.length()) areasList.add(areasArray.getString(i))
                    }
                    feedbackData = InterviewFeedback(
                        hireability = parsed.optString("hireability", "Unknown"),
                        communicationFeedback = parsed.optString("communication_feedback", ""),
                        technicalFeedback = parsed.optString("technical_feedback", ""),
                        improvementAreas = areasList
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load feedback.", Toast.LENGTH_SHORT).show()
            } finally {
                isFetchingFeedback = false
            }
        }
    }

    fun sendToAiBackend(userText: String) {
        if (!isOnline(context)) { Toast.makeText(context, "Offline!", Toast.LENGTH_SHORT).show(); return }
        isThinking = true
        scope.launch {
            try {
                val client = OkHttpClient.Builder().connectTimeout(120, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).writeTimeout(120, TimeUnit.SECONDS).build()
                val vaultDataStr = "Skills: ${userProfile.savedSkillsJson}\nExp: ${userProfile.savedExperienceJson}\nProjects: ${userProfile.savedProjectsJson}"
                val historyString = transcript.joinToString("\n") { "${it.role}: ${it.text}" }

                val jsonBody = JSONObject().apply {
                    put("target_role", targetRole.ifBlank { "Software Engineer" })
                    put("job_description", jobDescription)
                    put("vault_data", vaultDataStr)
                    put("chat_history", historyString)
                    put("user_audio_text", userText)
                    put("elapsed_seconds", timerSeconds)
                }.toString()

                val req = Request.Builder().url(apiBaseUrl.trimEnd('/') + "/v1/ai/live-interview").post(jsonBody.toRequestBody("application/json".toMediaType())).build()

                val responseStr = withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use { response ->
                        val body = response.body?.string()
                        if (response.isSuccessful) body else throw Exception("HTTP ${response.code}: $body")
                    }
                }

                if (responseStr != null) {
                    val parsed = JSONObject(responseStr)
                    val aiReply = parsed.optString("ai_reply", "I'm sorry, I didn't catch that.")
                    val aiConcluded = parsed.optBoolean("is_concluded", false)

                    transcript = (transcript + VoiceChatMessage("Interviewer", aiReply)).toMutableList()
                    isAiSpeaking = true
                    tts?.speak(aiReply, TextToSpeech.QUEUE_FLUSH, null, "AI_REPLY")

                    if (aiConcluded) {
                        isConcluded = true
                        fetchFeedbackReport(transcript.joinToString("\n") { "${it.role}: ${it.text}" })
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isThinking = false
            }
        }
    }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US }
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { isAiSpeaking = true }
            override fun onDone(utteranceId: String?) { isAiSpeaking = false }
            @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { isAiSpeaking = false }
        })

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false; partialSpeech = "" }
            override fun onPartialResults(partialResults: Bundle?) {
                val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) partialSpeech = data[0]
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                isListening = false
                partialSpeech = ""
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) {
                    val spokenText = data[0]
                    transcript = (transcript + VoiceChatMessage("Candidate", spokenText)).toMutableList()
                    sendToAiBackend(spokenText)
                }
            }
        }
        speechRecognizer?.setRecognitionListener(listener)

        onDispose { tts?.stop(); tts?.shutdown(); speechRecognizer?.destroy() }
    }

    LaunchedEffect(transcript.size, partialSpeech, isFetchingFeedback, feedbackData) {
        if (transcript.isNotEmpty()) listState.animateScrollToItem(transcript.size + 2)
    }

    fun startInterview() {
        if (targetRole.isBlank() || jobDescription.isBlank()) { Toast.makeText(context, "Enter Role & JD.", Toast.LENGTH_SHORT).show(); return }
        focusManager.clearFocus()
        isSetupPhase = false
        timerSeconds = 0

        val intro = "Hello! I am your AI Interviewer. I have reviewed your profile and the job description. Whenever you are ready, tap the microphone to say hello and we can begin."
        transcript = mutableListOf(VoiceChatMessage("Interviewer", intro))
        isAiSpeaking = true
        tts?.speak(intro, TextToSpeech.QUEUE_FLUSH, null, "INTRO")
    }

    fun toggleListening() {
        if (!hasMicPermission) { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return }
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        } else {
            tts?.stop()
            isAiSpeaking = false
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
            isListening = true
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (isSetupPhase) "Interview Setup" else if (isConcluded) "Interview Complete" else "Live Interview", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { tts?.stop(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (!isSetupPhase && !isConcluded) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(timeString, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (!isSetupPhase && !isConcluded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp).navigationBarsPadding()
                    ) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = if (isListening) 1.2f else 1f, animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse))

                        Text(if (isListening) "Listening..." else if (isThinking) "Thinking..." else if (isAiSpeaking) "AI is speaking..." else "Tap to Speak", color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))

                        FloatingActionButton(
                            onClick = { toggleListening() },
                            containerColor = if (isListening) MaterialTheme.colorScheme.error else if (isThinking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp).scale(scale),
                            shape = CircleShape
                        ) {
                            Icon(if (isListening) Icons.Filled.Stop else Icons.Filled.Mic, "Mic", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (isSetupPhase) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Target Role", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        ExposedDropdownMenuBox(
                            expanded = roleMenuExpanded,
                            onExpandedChange = { roleMenuExpanded = !roleMenuExpanded },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = targetRole, onValueChange = { targetRole = it },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true, placeholder = { Text("e.g. Backend Engineer") },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                                commonRoles.forEach { selection ->
                                    DropdownMenuItem(text = { Text(selection) }, onClick = { targetRole = selection; roleMenuExpanded = false })
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Job Description", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = jobDescription, onValueChange = { jobDescription = it },
                            placeholder = { Text("Paste the JD here...") }, modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { startInterview() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Start Virtual Interview")
                        }
                    }
                }
            } else if (!isConcluded) {
                // 🟢 3D POINT-CLOUD SPHERE (ZARA STYLE) - FIXED MATH TYPES
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f

                            val baseRadius = size.minDimension / 2.5f

                            val rotY = continuousTime * 1.2f
                            val rotX = continuousTime * 0.4f

                            val numLatitudes = 18
                            val numLongitudes = 24

                            val baseColor = if (isListening) Color(0xFF00E676) else if (isAiSpeaking) Color(0xFF00B0FF) else Color(0xFF1DE9B6)

                            for (i in 0..numLatitudes) {
                                val lat = (PI * i / numLatitudes).toFloat()
                                for (j in 0..numLongitudes) {
                                    val lon = (2 * PI * j / numLongitudes).toFloat()

                                    var x = baseRadius * sin(lat) * cos(lon)
                                    var y = baseRadius * cos(lat)
                                    var z = baseRadius * sin(lat) * sin(lon)

                                    val distAmp = if (isListening) 20f else if (isAiSpeaking) 12f else 3f
                                    val distSpeed = if (isListening) 5f else 2f
                                    val distortion = sin(lat * 3f + continuousTime * distSpeed) * cos(lon * 4f + continuousTime * distSpeed) * distAmp

                                    val rAdjusted = baseRadius + distortion
                                    x = rAdjusted * sin(lat) * cos(lon)
                                    y = rAdjusted * cos(lat)
                                    z = rAdjusted * sin(lat) * sin(lon)

                                    val yRot = y * cos(rotX) - z * sin(rotX)
                                    val zRot = y * sin(rotX) + z * cos(rotX)
                                    y = yRot
                                    z = zRot

                                    val xRot2 = x * cos(rotY) + z * sin(rotY)
                                    val zRot2 = -x * sin(rotY) + z * cos(rotY)
                                    x = xRot2
                                    z = zRot2

                                    val projX = centerX + x
                                    val projY = centerY + y

                                    val depth = (z + baseRadius) / (2f * baseRadius)
                                    val alpha = (0.15f + 0.85f * depth).coerceIn(0.1f, 1f)
                                    val dotSize = (1f + 3.5f * depth).coerceIn(1f, 4.5f)

                                    drawCircle(
                                        color = baseColor.copy(alpha = alpha),
                                        radius = dotSize,
                                        center = Offset(projX, projY)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(60.dp))
                    if (partialSpeech.isNotBlank()) {
                        Text(partialSpeech, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 32.dp))
                    } else if (isThinking) {
                        Text("Analyzing your answer...", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Live Interview Active", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // 📊 POST-INTERVIEW DASHBOARD
                Column(Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = postInterviewTab) {
                        Tab(selected = postInterviewTab == 0, onClick = { postInterviewTab = 0 }, text = { Text("Feedback Scorecard") })
                        Tab(selected = postInterviewTab == 1, onClick = { postInterviewTab = 1 }, text = { Text("Transcript") })
                    }

                    if (postInterviewTab == 0) {
                        if (isFetchingFeedback) {
                            Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(); Spacer(Modifier.height(16.dp)); Text("Recruiter is writing your feedback...", color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (feedbackData != null) {
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                item {
                                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                                        Column(Modifier.padding(24.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                                Spacer(Modifier.width(12.dp))
                                                Text("Hireability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(feedbackData!!.hireability, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                            Divider(Modifier.padding(vertical = 16.dp))

                                            Text("Communication Skills", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                            Text(feedbackData!!.communicationFeedback, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(Modifier.height(16.dp))

                                            Text("Technical Accuracy", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                            Text(feedbackData!!.technicalFeedback, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(Modifier.height(24.dp))

                                            Text("Crucial Areas to Improve", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                            feedbackData!!.improvementAreas.forEach { point ->
                                                Row(modifier = Modifier.padding(top = 8.dp)) {
                                                    Icon(Icons.Filled.Warning, null, modifier = Modifier.size(16.dp).padding(top=2.dp), tint = MaterialTheme.colorScheme.error)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(point, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                            items(transcript) { msg ->
                                val isCandidate = msg.role == "Candidate"
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = if (isCandidate) Arrangement.End else Arrangement.Start) {
                                    Box(modifier = Modifier.fillMaxWidth(0.85f).clip(RoundedCornerShape(16.dp, 16.dp, if(isCandidate) 0.dp else 16.dp, if(isCandidate) 16.dp else 0.dp)).background(if (isCandidate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer).padding(16.dp)) {
                                        Column {
                                            Text(msg.role, style = MaterialTheme.typography.labelSmall, color = if (isCandidate) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                            Spacer(Modifier.height(4.dp))
                                            Text(msg.text, style = MaterialTheme.typography.bodyLarge, color = if (isCandidate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}