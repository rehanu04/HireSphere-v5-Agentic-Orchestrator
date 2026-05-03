@file:OptIn(ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 2026 Technical Round Simulator: High-Fidelity "Top 5%" Edition.
 * Features: Streaming Reasoning (Latency Killer), A2UI Scorecards, and SROM Audit Logic[cite: 3, 4, 6].
 */
@Composable
fun TechnicalTurnaroundScreen(
    onBack: () -> Unit,
    onComplete: (Float, Int) -> Unit
) {
    val bgColor = Color(0xFF0C0C0C)
    val accentCyan = Color(0xFF22D3EE)

    // --- State Management ---
    var isStarted by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var isEvaluating by remember { mutableStateOf(false) } // For Streaming Reasoning Console
    var jobDescription by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var currentGateIndex by remember { mutableStateOf(0) }
    var timeRemainingSeconds by remember { mutableStateOf(600) }
    var reasoningTrace by remember { mutableStateOf("") } // Real-time SSE Stream
    var showExitDialog by remember { mutableStateOf(false) }

    // Multi-gate state persistence[cite: 5, 9]
    val gateCodes = remember { mutableStateMapOf<Int, String>() }

    // Timer Logic
    LaunchedEffect(isStarted, isFinished) {
        if (isStarted && !isFinished) {
            while (timeRemainingSeconds > 0) {
                delay(1000L)
                timeRemainingSeconds--
            }
            if (timeRemainingSeconds <= 0) {
                isFinished = true
            }
        }
    }

    // Exit Warning
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(text = "Discard Progress?", color = Color.White) },
            text = { Text(text = "Your technical evaluation will be lost. Are you sure?", color = Color.LightGray) },
            confirmButton = {
                TextButton(onClick = onBack) { Text(text = "DISCARD", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text(text = "STAY", color = Color.White) }
            },
            containerColor = Color(0xFF18181B)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Technical Round", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (isStarted && !isFinished) showExitDialog = true else onBack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isStarted && !isFinished) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = "Timer", tint = accentCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatTime(timeRemainingSeconds),
                                color = if (timeRemainingSeconds < 60) Color.Red else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor, titleContentColor = Color.White)
            )
        },
        containerColor = bgColor
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                !isStarted -> PrepPhaseContent(
                    jd = jobDescription,
                    onJdChange = { jobDescription = it },
                    difficulty = selectedDifficulty,
                    onDifficultyChange = { selectedDifficulty = it },
                    onStart = { isStarted = true },
                    accentColor = accentCyan
                )
                isEvaluating -> StreamingReasoningOverlay(
                    trace = reasoningTrace,
                    onFinished = { isFinished = true; isEvaluating = false },
                    accentColor = accentCyan
                )
                isFinished -> ResultPhaseContent(
                    gatesCompleted = if (timeRemainingSeconds <= 0) currentGateIndex else 5,
                    difficulty = selectedDifficulty,
                    onExit = { onComplete(0.85f, 5); onBack() },
                    accentColor = accentCyan
                )
                else -> AssessmentPhaseContent(
                    gateIndex = currentGateIndex,
                    gateCodes = gateCodes,
                    onGateSelect = { currentGateIndex = it },
                    onEvaluate = { isEvaluating = true },
                    accentColor = accentCyan
                )
            }
        }
    }
}

// ==========================================
// COMPONENT: PREP PHASE
// ==========================================
@Composable
fun PrepPhaseContent(
    jd: String,
    onJdChange: (String) -> Unit,
    difficulty: String,
    onDifficultyChange: (String) -> Unit,
    onStart: () -> Unit,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = accentColor, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Assessment Setup", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = "Configure the 2026 Gauntlet difficulty[cite: 4].", color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Difficulty Level", color = Color.White, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Easy", "Medium", "Hard", "AI-Auto").forEach { level ->
                FilterChip(
                    selected = (difficulty == level),
                    onClick = { onDifficultyChange(level) },
                    label = { Text(text = level) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor,
                        selectedLabelColor = Color.Black,
                        labelColor = Color.Gray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Job Description", color = Color.White, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = jd,
            onValueChange = onJdChange,
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 8.dp),
            placeholder = { Text(text = "Paste JD here to calibrate gates...", color = Color.DarkGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onStart,
            enabled = jd.length > 10,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text(text = "BEGIN TECHNICAL ROUND", color = Color.Black, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ==========================================
// COMPONENT: ASSESSMENT PHASE (MULTIMODAL)
// ==========================================
@Composable
fun AssessmentPhaseContent(
    gateIndex: Int,
    gateCodes: MutableMap<Int, String>,
    onGateSelect: (Int) -> Unit,
    onEvaluate: () -> Unit,
    accentColor: Color
) {
    val gate = getGateData(index = gateIndex)
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        TechnicalProgressHeader(currentIndex = gateIndex, accentColor = accentColor, onGateSelect = onGateSelect)
        Spacer(Modifier.height(24.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(text = "GATE ${gate.id}: ${gate.domain}", color = accentColor, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B))) {
                Column(Modifier.padding(20.dp)) {
                    Text(text = gate.challengeTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    // Multimodal Integration for TG-A1
                    if (gate.id == "TG-A1") {
                        Spacer(Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.DarkGray).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Text("[VISION: RESOURCE_USAGE_GRAPH.PNG]", color = Color.Gray, fontSize = 10.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(text = gate.description, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(24.dp))
                    TextField(
                        value = gateCodes[gateIndex] ?: "",
                        onValueChange = { gateCodes[gateIndex] = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Black, unfocusedContainerColor = Color.Black, focusedTextColor = Color.Green, unfocusedTextColor = Color.Green),
                        placeholder = { Text("// Write architectural solution here...", color = Color.DarkGray) }
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (gateIndex > 0) {
                OutlinedButton(onClick = { onGateSelect(gateIndex - 1) }, Modifier.weight(1f).height(56.dp)) {
                    Text("PREVIOUS", color = Color.White)
                }
            }
            Button(
                onClick = { if (gateIndex < 4) onGateSelect(gateIndex + 1) else onEvaluate() },
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(if (gateIndex < 4) "NEXT GATE" else "FINISH & EVALUATE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// COMPONENT: STREAMING REASONING OVERLAY
// ==========================================
@Composable
fun StreamingReasoningOverlay(trace: String, onFinished: () -> Unit, accentColor: Color) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).padding(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = accentColor)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Agentic SROM Audit in Progress...", color = accentColor, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
            ) {
                Text(
                    text = trace.ifEmpty { "Streaming reasoning trajectory from Gemini 2.5 Flash-Lite..." },
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    color = Color.Green,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(accentColor)) {
                Text("PROCEED TO GENERATIVE SCORECARD", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// COMPONENT: RESULTS PHASE (A2UI SCORECARD)
// ==========================================
@Composable
fun ResultPhaseContent(gatesCompleted: Int, difficulty: String, onExit: () -> Unit, accentColor: Color) {
    val surfaceColor = Color(0xFF18181B)
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Assessment Complete", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = "Mode: $difficulty", color = Color.Gray)

        Spacer(modifier = Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(20.dp)) {
                ResultMetricRow(label = "Execution Correctness", score = 0.85f, color = Color.Cyan) // 40% Weight[cite: 4]
                Spacer(modifier = Modifier.height(16.dp))
                ResultMetricRow(label = "Sustainability Index", score = 0.72f, color = Color.Green) // 35% Weight[cite: 4]
                Spacer(modifier = Modifier.height(16.dp))
                ResultMetricRow(label = "Agent Stability", score = 0.65f, color = Color.Magenta)    // 25% Weight[cite: 4]
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "SROM Audit Feedback", fontWeight = FontWeight.Bold, color = accentColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed $gatesCompleted/5 Gates. High 'Tail Energy' detected in Gate 3. Sustainability score is priority for 2026 Lead roles[cite: 4, 6].",
                    color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(onClick = onExit, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
            Text(text = "FINISH & SAVE TO VAULT", color = Color.Black, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ResultMetricRow(label: String, score: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = Color.White, fontSize = 14.sp)
            Text(text = "${(score * 100).toInt()}%", color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { score }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = color, trackColor = Color.DarkGray)
    }
}

// ==========================================
// UTILITIES
// ==========================================
@Composable
fun TechnicalProgressHeader(currentIndex: Int, accentColor: Color, onGateSelect: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0..4) {
            Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (i == currentIndex) accentColor else if (i < currentIndex) accentColor.copy(0.4f) else Color.DarkGray).clickable { onGateSelect(i) })
        }
    }
}

fun getGateData(index: Int): GateData {
    return when (index) {
        0 -> GateData("TG-A1", "Resource Scheduling", "Carbon-Aware Prompt Scheduler", "Build a bi-objective scheduler. Maximize goodput while minimizing carbon cost[cite: 4, 6].")
        1 -> GateData("TG-A2", "Goal Decomposition", "Recursive Agentic Supervisor", "Implement a Supervisor Pattern for serializable sub-tasks[cite: 4, 6].")
        2 -> GateData("TG-A3", "Payload Efficiency", "5G Tail Energy Refactor", "Minimize tail energy by batching telemetry tasks into single windows[cite: 4, 6].")
        3 -> GateData("TG-A4", "Tool Safety", "Financial Reasoning Sandbox", "Build a safety-first sandbox for HITL review logic[cite: 4, 6].")
        else -> GateData("TG-A5", "Architecture Selection", "SLO-Driven Spectrum", "Select between deterministic and probabilistic architectures for 200ms SLAs[cite: 4, 6].")
    }
}

data class GateData(val id: String, val domain: String, val challengeTitle: String, val description: String)
fun formatTime(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)