@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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
import kotlin.math.cos
import kotlin.math.sin

data class InterviewQuestion(val question: String, val explanation: String)

@Composable
fun MockInterviewScreen(
    isDark: Boolean,
    onBack: () -> Unit,
    userProfileStore: UserProfileStore,
    apiBaseUrl: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val userProfile by userProfileStore.userProfileFlow.collectAsState(initial = com.rehanu04.resumematchv2.data.UserProfile())
    val view = LocalView.current

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = Color.Transparent.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
    }

    val galacticCyan = Color(0xFF22D3EE)
    val twilightIndigo = Color(0xFF6366F1)
    val bgColor = if (isDark) Color(0xFF030303) else Color(0xFFE2E8F0)
    val cardBg = if (isDark) Color(0xFF0C0C0C) else Color(0xFFFFFFFF)
    val accentColor = if (isDark) galacticCyan else twilightIndigo
    val animatedAccent by animateColorAsState(targetValue = accentColor, animationSpec = tween(1000))

    var targetRole by remember { mutableStateOf(userProfile.targetRole) }
    var jobDescription by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var questions by remember { mutableStateOf<List<InterviewQuestion>>(emptyList()) }
    var expandedIndex by remember { mutableIntStateOf(-1) }
    var showInputForm by remember { mutableStateOf(true) }

    fun generateQuestions() {
        if (!isOnline(context)) {
            Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
            return
        }
        if (targetRole.isBlank() || jobDescription.isBlank()) {
            Toast.makeText(context, "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        isGenerating = true
        focusManager.clearFocus()
        questions = emptyList()
        expandedIndex = -1

        scope.launch {
            try {
                val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
                val vaultDataStr = "Skills: ${userProfile.savedSkillsJson}\nExp: ${userProfile.savedExperienceJson}"
                val jsonBody = JSONObject().apply {
                    put("target_role", targetRole)
                    put("job_description", jobDescription)
                    put("vault_data", vaultDataStr)
                }.toString()

                val req = Request.Builder().url(apiBaseUrl.trimEnd('/') + "/v1/ai/generate-interview").post(jsonBody.toRequestBody("application/json".toMediaType())).build()

                val responseStr = withContext(Dispatchers.IO) {
                    client.newCall(req).execute().use { if (it.isSuccessful) it.body?.string() else null }
                }

                if (responseStr != null) {
                    val qArray = JSONObject(responseStr).optJSONArray("questions")
                    val parsedList = mutableListOf<InterviewQuestion>()
                    qArray?.let { for (i in 0 until it.length()) { val obj = it.getJSONObject(i); parsedList.add(InterviewQuestion(obj.optString("question", ""), obj.optString("explanation", ""))) } }
                    questions = parsedList
                    showInputForm = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "AI Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally { isGenerating = false }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        InterviewAtmosphere(accentColor = animatedAccent, isDark = isDark)

        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                TopAppBar(
                    title = { Text("Interview Simulation", fontWeight = FontWeight.Black) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = if (isDark) Color.White else Color.Black, navigationIconContentColor = if (isDark) Color.White else Color.Black)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                AnimatedVisibility(visible = showInputForm, enter = expandVertically(), exit = shrinkVertically()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.85f)),
                        border = BorderStroke(1.dp, (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f))
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, null, tint = animatedAccent, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Simulation Core", color = if (isDark) Color.White else Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(value = targetRole, onValueChange = { targetRole = it }, label = { Text("Position") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = animatedAccent, focusedTextColor = if (isDark) Color.White else Color.Black, unfocusedTextColor = if (isDark) Color.White else Color.Black), singleLine = true)
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(value = jobDescription, onValueChange = { jobDescription = it }, label = { Text("JD Details") }, modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = animatedAccent, focusedTextColor = if (isDark) Color.White else Color.Black, unfocusedTextColor = if (isDark) Color.White else Color.Black))
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = { generateQuestions() }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = !isGenerating, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = animatedAccent)) {
                                if (isGenerating) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = if (isDark) Color.Black else Color.White, strokeWidth = 2.5.dp)
                                else { Icon(Icons.Filled.AutoAwesome, null, tint = if (isDark) Color.Black else Color.White); Spacer(Modifier.width(10.dp)); Text("START GAUNTLET", color = if (isDark) Color.Black else Color.White, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }

                if (questions.isNotEmpty()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(questions) { index, item ->
                            val isExpanded = expandedIndex == index
                            Surface(modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expandedIndex = if (isExpanded) -1 else index }, shape = RoundedCornerShape(16.dp), color = cardBg.copy(alpha = 0.9f), border = BorderStroke(1.dp, (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f))) {
                                Column(Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Filled.Lightbulb, null, tint = animatedAccent, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(14.dp))
                                        Text(text = item.question, color = if (isDark) Color.White else Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Icon(imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                                    }
                                    if (isExpanded) {
                                        Spacer(Modifier.height(18.dp))
                                        Column(modifier = Modifier.background(animatedAccent.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(16.dp).fillMaxWidth()) {
                                            Text("STRATEGY", fontWeight = FontWeight.Black, color = animatedAccent, fontSize = 11.sp)
                                            Spacer(Modifier.height(8.dp))
                                            Text(text = item.explanation, color = if (isDark) Color.LightGray else Color.DarkGray, fontSize = 13.sp)
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

@Composable
private fun InterviewAtmosphere(accentColor: Color, isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wash")
    val drift by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 2f * Math.PI.toFloat(), animationSpec = infiniteRepeatable(animation = tween(40000, easing = LinearEasing)), label = "drift")
    val bgColor = if (isDark) Color(0xFF010103) else Color(0xFFE2E8F0)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = bgColor)
        val xShift = (sin(drift.toDouble()).toFloat() * 0.2f) + 0.5f
        val yShift = (cos(drift.toDouble() * 0.7).toFloat() * 0.15f) + 0.5f
        drawRect(brush = Brush.linearGradient(colors = listOf(accentColor.copy(alpha = 0.12f), Color.Transparent), start = Offset(size.width * xShift, 0f), end = Offset(size.width * (1f - xShift), size.height)))
        drawRect(brush = Brush.linearGradient(colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.06f), Color.Transparent), start = Offset(0f, size.height * yShift), end = Offset(size.width, size.height * (1f - yShift))))
    }
}