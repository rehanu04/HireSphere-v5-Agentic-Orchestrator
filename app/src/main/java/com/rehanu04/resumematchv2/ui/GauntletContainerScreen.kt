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
    onExit: () -> Unit
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
                            accentColor = accentColor
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
fun AptitudeRoundFragment(onComplete: () -> Unit, surfaceColor: Color, accentColor: Color) {
    val options = listOf("12.5g CO2", "26.8g CO2", "41.2g CO2", "55.0g CO2")
    var selectedIndex by remember { mutableStateOf(-1) }
    Column {
        Text("Stage 2: IRT Calibration (APT-Q2)", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = surfaceColor)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Numerical Reasoning", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Calculate the probable carbon intensity forecasting for a GPU cluster with b=2.10 and a=1.60.", color = Color.LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(20.dp))

                options.forEachIndexed { index, option ->
                    OutlinedButton(
                        onClick = { selectedIndex = index },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedIndex == index) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (selectedIndex == index) accentColor else Color.White
                        )
                    ) { Text(option) }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onComplete,
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