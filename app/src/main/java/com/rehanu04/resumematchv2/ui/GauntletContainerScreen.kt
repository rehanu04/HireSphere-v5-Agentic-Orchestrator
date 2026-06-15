@file:OptIn(ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehanu04.resumematchv2.ui.viewmodel.ActivityViewModel
import com.rehanu04.resumematchv2.data.LogEntry
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlin.random.Random

data class AptitudeRequest(
    @SerializedName("current_theta") val currentTheta: Float,
    @SerializedName("responses") val responses: List<Map<String, String>>
)

data class AptitudeResponse(
    @SerializedName("next_question") val nextQuestion: String,
    @SerializedName("difficulty_b") val difficultyB: Float,
    @SerializedName("discrimination_a") val discriminationA: Float,
    @SerializedName("information_gain") val informationGain: Float
)

data class FinalScoreRequest(
    @SerializedName("execution_correctness") val executionCorrectness: Float,
    @SerializedName("sustainability_index") val sustainabilityIndex: Float,
    @SerializedName("agent_stability") val agentStability: Float
)

data class FinalScoreResponse(
    @SerializedName("final_score") val finalScore: Float,
    @SerializedName("passed") val passed: Boolean
)

/**
 * Your Vision: The Immersive Job Simulation.
 * This mimics the scenario where the user has been selected and must navigate the
 * final technical and operational hurdles of the role.
 */
enum class GauntletStage(val displayName: String, val step: Int) {
    TECH("Technical Round", 1),
    APTITUDE("Quantitative Aptitude", 2),
    GD("Group Discussion", 3),
    JOB_SIM("Operational Simulation", 4)
}

@Composable
fun GauntletContainerScreen(
    isDark: Boolean,
    startStage: String = "TECH",
    onExit: () -> Unit,
    apiBaseUrl: String = "",
    activityViewModel: ActivityViewModel? = null
) {
    // Branding & Theme Consistency
    val bgColor = if (isDark) Color(0xFF0C0C0C) else Color(0xFFF9FAFB)
    val accentColor = Color(0xFF22D3EE)
    val textColor = if (isDark) Color.White else Color.Black
    val surfaceColor = if (isDark) Color(0xFF18181B) else Color(0xFFFFFFFF)

    var currentStage by remember {
        mutableStateOf(
            try { GauntletStage.valueOf(startStage) } catch (e: Exception) { GauntletStage.TECH }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HIRESPHERE SIMULATION", style = MaterialTheme.typography.labelSmall, color = accentColor, letterSpacing = 2.sp)
                        Text(currentStage.displayName, style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold)
                    }
                },
                actions = { IconButton(onClick = onExit) { Icon(Icons.Default.Close, null, tint = textColor) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp)) {
            // The Consistent Progress Bar
            GauntletProgressHeader(currentStep = currentStage.step, accentColor = accentColor, isDark = isDark)

            Spacer(modifier = Modifier.height(24.dp))

            // The Content Engine: Swapping only the "Inside" of the TV
            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = currentStage, label = "stage_transition") { stage ->
                    when (stage) {
                        GauntletStage.TECH -> TechnicalRoundFragment(
                            onComplete = { currentStage = GauntletStage.APTITUDE },
                            surfaceColor = surfaceColor,
                            accentColor = accentColor
                        )
                        GauntletStage.APTITUDE -> AptitudeRoundFragment(
                            onComplete = { currentStage = GauntletStage.GD },
                            surfaceColor = surfaceColor,
                            accentColor = accentColor,
                            apiBaseUrl = apiBaseUrl,
                            activityViewModel = activityViewModel
                        )
                        GauntletStage.GD -> GDRoundFragment(
                            onComplete = { currentStage = GauntletStage.JOB_SIM },
                            surfaceColor = surfaceColor,
                            accentColor = accentColor
                        )
                        GauntletStage.JOB_SIM -> FinalJobSimulationFragment(
                            onComplete = onExit,
                            surfaceColor = surfaceColor,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GauntletProgressHeader(currentStep: Int, accentColor: Color, isDark: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..4) {
            val barColor = when {
                i == currentStep -> accentColor
                i < currentStep -> accentColor.copy(alpha = 0.4f)
                else -> if (isDark) Color(0xFF2D2D30) else Color(0xFFE5E7EB)
            }
            Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(barColor))
        }
    }
}

// --- 1. TECHNICAL ROUND: THE RESOURCE CHALLENGE ---
@Composable
fun TechnicalRoundFragment(onComplete: () -> Unit, surfaceColor: Color, accentColor: Color) {
    var codeInput by remember { mutableStateOf("") }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Stage 1: Resource Scheduling (TG-A1)", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Carbon-Aware Scheduler", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Formula: Reward = (w1 * Goodput) - (w2 * CarbonCost)", color = Color.LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Active code entry for the Technical Simulation
                TextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedTextColor = Color.Green,
                        unfocusedTextColor = Color.Green
                    ),
                    placeholder = { Text("// Write your Green Coding logic here...", color = Color.DarkGray) }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onComplete,
                    enabled = codeInput.length > 5,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("SUBMIT FOR SROM AUDIT", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// --- 2. APTITUDE ROUND: THE 2-PL BRAIN TEST ---
@Composable
fun AptitudeRoundFragment(onComplete: () -> Unit, surfaceColor: Color, accentColor: Color, apiBaseUrl: String, activityViewModel: ActivityViewModel?) {
    val scope = rememberCoroutineScope()
    var currentTheta by remember { mutableFloatStateOf(0.0f) }
    val responses = remember { mutableStateListOf<Map<String, String>>() }
    var iteration by remember { mutableIntStateOf(0) }
    val maxIterations = 10
    
    var questionText by remember { mutableStateOf("Initializing Adaptive Engine...") }
    var difficultyB by remember { mutableFloatStateOf(0f) }
    var discriminationA by remember { mutableFloatStateOf(0f) }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    
    // Timer
    var timeLeft by remember { mutableIntStateOf(60) }
    
    val httpClient = remember { OkHttpClient() }
    val gson = remember { Gson() }

    fun fetchNextQuestion() {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val req = AptitudeRequest(currentTheta, responses.toList())
                val body = gson.toJson(req).toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url("$apiBaseUrl/v1/gauntlet/aptitude_item").post(body).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val resText = response.body?.string() ?: ""
                    val aptRes = gson.fromJson(resText, AptitudeResponse::class.java)
                    withContext(Dispatchers.Main) {
                        questionText = aptRes.nextQuestion
                        difficultyB = aptRes.difficultyB
                        discriminationA = aptRes.discriminationA
                        timeLeft = 60
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    questionText = "Connection Error. Retrying..."
                    delay(2000)
                    fetchNextQuestion()
                }
            }
        }
    }
    
    fun submitFinalScore() {
        isSubmitting = true
        scope.launch(Dispatchers.IO) {
            try {
                val req = FinalScoreRequest(currentTheta * 10 + 50f, 0.8f, 0.9f)
                val body = gson.toJson(req).toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url("$apiBaseUrl/v1/gauntlet/final_score").post(body).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val resText = response.body?.string() ?: ""
                    val finalRes = gson.fromJson(resText, FinalScoreResponse::class.java)
                    withContext(Dispatchers.Main) {
                        activityViewModel?.addLog(LogEntry("MAY 19", "Aptitude Gauntlet", if(finalRes.passed) "PASSED" else "FAILED", finalRes.finalScore.toInt(), "Completed 10 iterations. Theta: $currentTheta"))
                        onComplete()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        activityViewModel?.addLog(LogEntry("MAY 19", "Aptitude Gauntlet", "COMPLETED", 75, "Fallback score. Theta: $currentTheta"))
                        onComplete()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    activityViewModel?.addLog(LogEntry("MAY 19", "Aptitude Gauntlet", "ERROR", 0, "Failed to reach final score endpoint."))
                    onComplete()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchNextQuestion()
    }
    
    LaunchedEffect(timeLeft, isLoading, isSubmitting) {
        if (!isLoading && !isSubmitting && timeLeft > 0) {
            delay(1000)
            timeLeft -= 1
        }
    }

    Column {
        Text("Stage 2: IRT Calibration (APT-Q2) - Question ${iteration + 1}/$maxIterations", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        
        // Progress Bar
        LinearProgressIndicator(
            progress = { timeLeft / 60f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = accentColor,
            trackColor = surfaceColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(questionText, color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (isSubmitting) {
                    CircularProgressIndicator(color = accentColor, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Calculating Final Score...", color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Text("Numerical Reasoning", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(questionText, color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    val isNumeric = questionText.contains("calculate", ignoreCase = true) || questionText.contains("formula", ignoreCase = true) || questionText.contains("number", ignoreCase = true)
                    
                    if (isNumeric) {
                        var numericInput by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = numericInput,
                            onValueChange = { numericInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            label = { Text("Numeric Value", color = Color.Gray) }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val isCorrect = numericInput.isNotBlank() && Random.nextBoolean() // dummy check
                                val score = if (isCorrect) 1.0f else 0.0f
                                currentTheta += discriminationA * (score - 0.5f)
                                responses.add(mapOf("question" to questionText, "score" to score.toString()))
                                iteration++
                                if (iteration >= maxIterations) submitFinalScore() else fetchNextQuestion()
                            },
                            enabled = numericInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("CONFIRM ANSWER", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    } else {
                        val options = listOf("Increase b-factor", "Decrease b-factor", "Normalize a-factor", "Hold constant")
                        var selectedIndex by remember { mutableStateOf(-1) }
                        options.forEachIndexed { index, option ->
                            val scale by animateFloatAsState(
                                targetValue = if (selectedIndex == index) 1.04f else 1f,
                                animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).scale(scale).clickable { selectedIndex = index },
                                colors = CardDefaults.cardColors(containerColor = if (selectedIndex == index) accentColor.copy(alpha = 0.2f) else Color.Transparent),
                                border = BorderStroke(1.dp, if (selectedIndex == index) accentColor else Color.DarkGray)
                            ) {
                                Text(option, modifier = Modifier.padding(16.dp), color = if (selectedIndex == index) accentColor else Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val isCorrect = Random.nextBoolean() // dummy check
                                val score = if (isCorrect) 1.0f else 0.0f
                                currentTheta += discriminationA * (score - 0.5f)
                                responses.add(mapOf("question" to questionText, "score" to score.toString()))
                                iteration++
                                if (iteration >= maxIterations) submitFinalScore() else fetchNextQuestion()
                            },
                            enabled = selectedIndex != -1,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("CONFIRM ANSWER", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

// --- 3. GROUP DISCUSSION: THE AI PERSONA BATTLE ---
@Composable
fun GDRoundFragment(onComplete: () -> Unit, surfaceColor: Color, accentColor: Color) {
    Column {
        Text("Stage 3: Multi-Agent Consensus", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(surfaceColor, RoundedCornerShape(16.dp)).padding(16.dp)) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Skeptic: Your HNSW index is too energy intensive for 2026 standards.", color = Color.Cyan, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Visionary: But the complexity O(d * log N) is critical for our orchestration!", color = Color.Magenta, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("PM: We need to pivot. Should we sacrifice latency for sustainability?", color = Color.Yellow, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Human Action Required: Influence the agents toward a 'Sober' decision.", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
            Text("PIVOT DISCUSSION", color = Color.Black, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// --- 4. JOB SIMULATION: THE FINAL CRISIS ---
@Composable
fun FinalJobSimulationFragment(onComplete: () -> Unit, surfaceColor: Color, accentColor: Color) {
    val protocols = listOf("Apply TG-A4 Sandbox Guardrail", "Trigger HITL Escalation", "Execute Tiered Fallback")
    var selectedProtocol by remember { mutableStateOf(-1) }
    Column {
        Text("Final Stage: System Trade-offs", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Operational Crisis", fontWeight = FontWeight.Bold, color = Color.White)
                Text("A tool call hallucination is spiking latency. You are now the Lead Architect. Choose the recovery protocol.", color = Color.LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(20.dp))

                protocols.forEachIndexed { index, action ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedProtocol = index }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedProtocol == index),
                            onClick = { selectedProtocol = index },
                            colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                        )
                        Text(action, color = Color.White, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onComplete,
                    enabled = selectedProtocol != -1,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("FINISH SIMULATION", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}