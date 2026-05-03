package com.rehanu04.resumematchv2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SkillStandingsScreen(
    onBack: () -> Unit,
    // Defaulting to 0f to ensure no false data is shown[cite: 16]
    techScore: Float = 0f,
    sustainabilityIndex: Float = 0f,
    stabilityIndex: Float = 0f
) {
    val backgroundColor = Color(0xFF0C0C0C)
    val cardColor = Color(0xFF18181B)
    val accentCyan = Color(0xFF22D3EE)

    val hasEvaluations = techScore > 0f || sustainabilityIndex > 0f || stabilityIndex > 0f

    Scaffold(containerColor = backgroundColor) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text("Your Professional Standings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "Real-time evaluation based on your completed trajectories.",
                fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            if (!hasEvaluations) {
                // --- EMPTY STANDINGS STATE ---
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Assessment, null, modifier = Modifier.size(64.dp), tint = Color.DarkGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Performance Data", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Complete your first Gauntlet round to initialize your skill rubric.",
                            color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp).padding(top = 8.dp)
                        )
                    }
                }
            } else {
                // --- THE 2026 METRIC RUBRIC ---
                MetricCard("Execution Correctness", techScore, "40% Weight", accentCyan, cardColor)
                Spacer(modifier = Modifier.height(16.dp))
                MetricCard("Sustainability Index", sustainabilityIndex, "35% Weight", Color.Green, cardColor)
                Spacer(modifier = Modifier.height(16.dp))
                MetricCard("Agent Stability", stabilityIndex, "25% Weight", Color.Magenta, cardColor)
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentCyan), shape = RoundedCornerShape(12.dp)
            ) {
                Text("RETURN TO HUB", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, score: Float, weight: String, color: Color, cardColor: Color) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(weight, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { score },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color, trackColor = Color.DarkGray
            )
            Text("${(score * 100).toInt()}% Proficiency", color = color, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}