@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import java.util.concurrent.TimeUnit

data class InterviewQuestion(val question: String, val explanation: String)

@Composable
fun MockInterviewScreen(
    onBack: () -> Unit,
    userProfileStore: UserProfileStore,
    apiBaseUrl: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())

    var targetRole by remember { mutableStateOf(userProfile.targetRole) }
    var jobDescription by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var questions by remember { mutableStateOf<List<InterviewQuestion>>(emptyList()) }
    var expandedIndex by remember { mutableIntStateOf(-1) }

    // ✨ NEW: Controls whether the large input form is visible
    var showInputForm by remember { mutableStateOf(true) }

    fun generateQuestions() {
        if (!isOnline(context)) {
            Toast.makeText(context, "You are offline!", Toast.LENGTH_SHORT).show()
            return
        }
        if (targetRole.isBlank() || jobDescription.isBlank()) {
            Toast.makeText(context, "Please enter a role and job description.", Toast.LENGTH_SHORT).show()
            return
        }

        isGenerating = true
        focusManager.clearFocus()
        questions = emptyList()
        expandedIndex = -1

        scope.launch {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val vaultDataStr = "Skills: ${userProfile.savedSkillsJson}\nExp: ${userProfile.savedExperienceJson}\nProjects: ${userProfile.savedProjectsJson}"

                val jsonBody = JSONObject().apply {
                    put("target_role", targetRole)
                    put("job_description", jobDescription)
                    put("vault_data", vaultDataStr)
                }.toString()

                val req = Request.Builder()
                    .url(apiBaseUrl.trimEnd('/') + "/v1/ai/generate-interview")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val responseStr = withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use { response ->
                        val body = response.body?.string()
                        if (response.isSuccessful) body else throw Exception("HTTP ${response.code}: $body")
                    }
                }

                if (responseStr != null) {
                    val parsed = JSONObject(responseStr)
                    val qArray = parsed.optJSONArray("questions")
                    val parsedList = mutableListOf<InterviewQuestion>()

                    if (qArray != null) {
                        for (i in 0 until qArray.length()) {
                            val obj = qArray.getJSONObject(i)
                            parsedList.add(InterviewQuestion(
                                question = obj.optString("question", ""),
                                explanation = obj.optString("explanation", "")
                            ))
                        }
                    }
                    questions = parsedList
                    showInputForm = false // ✨ NEW: Hide the giant input form on success!
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isGenerating = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("AI Interview Simulator", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ✨ NEW: Animated wrapper to smoothly hide/show the input block
            AnimatedVisibility(
                visible = showInputForm,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Target Role", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = targetRole,
                            onValueChange = { targetRole = it },
                            placeholder = { Text("e.g. Backend Engineer") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Job Description", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            placeholder = { Text("Paste the JD here...") },
                            modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 8.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { generateQuestions() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Generating Questions...")
                            } else {
                                Icon(Icons.Filled.AutoAwesome, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Mock Interview")
                            }
                        }
                    }
                }
            }

            // RESULTS SECTION
            if (questions.isNotEmpty()) {

                // ✨ NEW: A small header that lets them reopen the input form if they want to change the JD
                if (!showInputForm) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Your Custom Interview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showInputForm = true }) {
                            Icon(Icons.Filled.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit JD")
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(questions) { index, item ->
                        val isExpanded = expandedIndex == index

                        Card(
                            // ✨ FIXED: Tapping toggles the exact index on and off
                            modifier = Modifier.fillMaxWidth().animateContentSize().clickable {
                                expandedIndex = if (isExpanded) -1 else index
                            },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Spacer(Modifier.width(8.dp))
                                    // ✨ FIXED: Added visual Chevron so the user knows it can collapse/expand
                                    Icon(
                                        if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Toggle",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isExpanded) {
                                    Divider(Modifier.padding(vertical = 12.dp))
                                    Column(Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(12.dp).fillMaxWidth()) {
                                        Text("Interviewer's Secret Objective:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.height(4.dp))
                                        Text(item.explanation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    Text("Tap to reveal the interviewer's objective...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, start = 36.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}