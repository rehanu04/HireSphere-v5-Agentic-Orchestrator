@file:OptIn(ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.Manifest
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rehanu04.resumematchv2.data.UserProfileStore
import com.rehanu04.resumematchv2.util.isOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ChatMessage(val text: String, val isUser: Boolean, val isLoading: Boolean = false)
data class AiExperience(val company: String = "", val role: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")
data class AiProject(val name: String = "", val startMonth: String = "", val startYear: String = "", val endMonth: String = "", val endYear: String = "", val bullets: String = "")

@Composable
fun AiAssistantScreen(
    onBack: () -> Unit,
    userProfileStore: UserProfileStore,
    apiBaseUrl: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val listState = rememberLazyListState()

    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isInitialized by remember { mutableStateOf(false) }
    var hasInternet by remember { mutableStateOf(isOnline(context)) }

    LaunchedEffect(userProfile.chatHistoryJson) {
        if (!isInitialized) {
            try {
                val type = object : TypeToken<List<ChatMessage>>() {}.type
                val savedRaw: List<ChatMessage>? = gson.fromJson(userProfile.chatHistoryJson, type)

                val saved = savedRaw?.filterNotNull()?.map {
                    ChatMessage(it.text ?: "", it.isUser, it.isLoading)
                }

                messages = if (saved.isNullOrEmpty()) {
                    listOf(ChatMessage("Hi! I'm your AI Career Coach. How are you doing today? Tell me about your recent projects, or tell me your name and a bit about yourself!", false))
                } else saved
            } catch (e: Exception) {
                messages = listOf(ChatMessage("Hi! I'm your AI Career Coach. Let's start fresh. Tell me about your experience!", false))
            }
            isInitialized = true
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    var currentInput by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.getOrNull(0)
            if (!spokenText.isNullOrBlank()) currentInput = spokenText
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            isListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell me about your experience...")
            }
            try { speechLauncher.launch(intent) } catch (e: Exception) { isListening = false; Toast.makeText(context, "Speech not available", Toast.LENGTH_SHORT).show() }
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMessage() {
        hasInternet = isOnline(context)
        if (!hasInternet) { Toast.makeText(context, "You are currently offline!", Toast.LENGTH_SHORT).show(); return }
        if (currentInput.isBlank()) return
        val transcript = currentInput
        currentInput = ""

        messages = messages + ChatMessage(transcript, true) + ChatMessage("Analyzing...", false, isLoading = true)

        scope.launch {
            try {
                // INCREASED TIMEOUT TO 120s FOR RENDER COLD STARTS
                val client = OkHttpClient.Builder()
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .build()

                val jsonBody = JSONObject().apply { put("transcript", transcript) }.toString()
                val safeBaseUrl = if (apiBaseUrl.isNotBlank()) apiBaseUrl else "http://192.168.1.6:8000"

                val req = Request.Builder().url(safeBaseUrl.trimEnd('/') + "/v1/ai/parse-dump").post(jsonBody.toRequestBody("application/json".toMediaType())).build()

                val (isSuccess, responseStr) = withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use { response -> response.isSuccessful to response.body?.string() }
                }

                if (isSuccess && !responseStr.isNullOrBlank()) {
                    try {
                        val parsedObj = JSONObject(responseStr)
                        val aiReply = parsedObj.optString("reply", "I've saved your details to the Vault!")

                        val aiFirstName = parsedObj.optString("first_name", "").trim()
                        val aiLastName = parsedObj.optString("last_name", "").trim()
                        val aiRole = parsedObj.optString("target_role", "").trim()
                        val aiSummary = parsedObj.optString("summary", "").trim()

                        val newFirstName = if (aiFirstName.isNotBlank()) aiFirstName else userProfile.firstName
                        val newLastName = if (aiLastName.isNotBlank()) aiLastName else userProfile.lastName
                        val newTargetRole = if (aiRole.isNotBlank()) aiRole else userProfile.targetRole
                        val newSummary = if (aiSummary.isNotBlank()) {
                            if (userProfile.summary.isBlank()) aiSummary else "${userProfile.summary}\n$aiSummary"
                        } else userProfile.summary

                        val newProjectsJson = parsedObj.optJSONArray("projects")?.toString() ?: "[]"
                        val newExperienceJson = parsedObj.optJSONArray("experience")?.toString() ?: "[]"
                        val newSkillsJson = parsedObj.optJSONArray("skills_suggested")?.toString() ?: "[]"

                        val listTypeProj = object : TypeToken<List<AiProject>>() {}.type
                        val listTypeExp = object : TypeToken<List<AiExperience>>() {}.type
                        val listTypeSkill = object : TypeToken<List<String>>() {}.type

                        val existingProjectsRaw: List<AiProject>? = try { gson.fromJson(if (userProfile.savedProjectsJson.isBlank()) "[]" else userProfile.savedProjectsJson, listTypeProj) } catch (e: Exception) { emptyList() }
                        val existingProjects = existingProjectsRaw?.filterNotNull()?.map { AiProject(it.name ?: "Unknown Project", it.startMonth ?: "", it.startYear ?: "", it.endMonth ?: "", it.endYear ?: "", it.bullets ?: "") } ?: emptyList()

                        val existingExpRaw: List<AiExperience>? = try { gson.fromJson(if (userProfile.savedExperienceJson.isBlank()) "[]" else userProfile.savedExperienceJson, listTypeExp) } catch (e: Exception) { emptyList() }
                        val existingExp = existingExpRaw?.filterNotNull()?.map { AiExperience(it.company ?: "Unknown Company", it.role ?: "Unknown Role", it.startMonth ?: "", it.startYear ?: "", it.endMonth ?: "", it.endYear ?: "", it.bullets ?: "") } ?: emptyList()

                        val existingSkillsRaw: List<String>? = try { gson.fromJson(if (userProfile.savedSkillsJson.isBlank()) "[]" else userProfile.savedSkillsJson, listTypeSkill) } catch (e: Exception) { emptyList() }
                        val existingSkills = existingSkillsRaw?.filterNotNull() ?: emptyList()

                        val parsedProjectsRaw: List<AiProject>? = try { gson.fromJson(newProjectsJson, listTypeProj) } catch (e: Exception) { emptyList() }
                        val parsedProjects = parsedProjectsRaw?.filterNotNull()?.map { AiProject(it.name ?: "Unknown Project", it.startMonth ?: "", it.startYear ?: "", it.endMonth ?: "", it.endYear ?: "", it.bullets ?: "") } ?: emptyList()

                        val parsedExpRaw: List<AiExperience>? = try { gson.fromJson(newExperienceJson, listTypeExp) } catch (e: Exception) { emptyList() }
                        val parsedExp = parsedExpRaw?.filterNotNull()?.map { AiExperience(it.company ?: "Unknown Company", it.role ?: "Unknown Role", it.startMonth ?: "", it.startYear ?: "", it.endMonth ?: "", it.endYear ?: "", it.bullets ?: "") } ?: emptyList()

                        val parsedSkillsRaw: List<String>? = try { gson.fromJson(newSkillsJson, listTypeSkill) } catch (e: Exception) { emptyList() }
                        val parsedSkills = parsedSkillsRaw?.filterNotNull() ?: emptyList()

                        val mergedProjects = (existingProjects + parsedProjects).groupBy { it.name.lowercase().trim() }.map { it.value.last() }
                        val mergedExp = (existingExp + parsedExp).groupBy { it.company.lowercase().trim() }.map { it.value.last() }
                        val mergedSkills = (existingSkills + parsedSkills).map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }

                        val finalMessages = messages.dropLast(1) + ChatMessage(aiReply, false)
                        messages = finalMessages

                        userProfileStore.saveUserProfile(
                            userProfile.copy(
                                firstName = newFirstName,
                                lastName = newLastName,
                                targetRole = newTargetRole,
                                summary = newSummary,
                                savedProjectsJson = gson.toJson(mergedProjects),
                                savedExperienceJson = gson.toJson(mergedExp),
                                savedSkillsJson = gson.toJson(mergedSkills),
                                chatHistoryJson = gson.toJson(finalMessages)
                            )
                        )
                    } catch (e: Exception) {
                        messages = messages.dropLast(1) + ChatMessage("Data parsing error: Could not read AI response.", false)
                    }
                } else {
                    messages = messages.dropLast(1) + ChatMessage("Backend Error: Server returned a failure status.", false)
                }
            } catch (e: Exception) {
                messages = messages.dropLast(1) + ChatMessage("Connection Error: Request timed out while waiting for server to wake up.", false)
            }
        }
    }

    val luxuryBgColor = Color(0xFF030303)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Master Vault Assistant", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        if (!hasInternet) Text("Offline Mode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) } },
                actions = {
                    TextButton(onClick = {
                        scope.launch {
                            messages = emptyList()
                            userProfileStore.saveUserProfile(userProfile.copy(chatHistoryJson = "[]"))
                        }
                    }) { Text("Clear", color = Color.Gray) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = luxuryBgColor
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                val glareRadius = size.width * 1.5f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width / 2, 0f),
                        radius = glareRadius
                    ),
                    radius = glareRadius,
                    center = Offset(size.width / 2, 0f)
                )
            }

            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages) { msg -> ChatBubble(msg) }
                }

                Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, luxuryBgColor)))) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                            .imePadding()
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = currentInput,
                            onValueChange = { currentInput = it },
                            modifier = Modifier.weight(1f),
                            enabled = hasInternet,
                            placeholder = { Text(if (isListening) "Listening..." else if(!hasInternet) "Waiting for connection..." else "Type message...", color = Color.Gray) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF38BDF8)
                            ),
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(if (hasInternet) Color(0xFF38BDF8) else Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = {
                                    hasInternet = isOnline(context)
                                    if (hasInternet) {
                                        if (currentInput.isNotBlank()) sendMessage()
                                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        Toast.makeText(context, "Offline - Please connect to Wi-Fi", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (!hasInternet) Icons.Filled.MicOff else if (currentInput.isBlank()) Icons.Filled.Mic else Icons.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    val bgColor = if (isUser) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF1E293B).copy(alpha = 0.6f)
    val borderColor = if (isUser) Color(0xFF38BDF8).copy(alpha = 0.3f) else Color.Transparent
    val textColor = if (isUser) Color.White else Color(0xFFE2E8F0)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 290.dp),
            border = if (isUser) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
        ) {
            Row(
                modifier = Modifier.padding(16.dp).animateContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (msg.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = msg.text, color = Color(0xFF38BDF8), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                } else {
                    Text(text = msg.text, color = textColor, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                }
            }
        }
    }
}