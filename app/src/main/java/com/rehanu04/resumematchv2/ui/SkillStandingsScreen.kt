package com.rehanu04.resumematchv2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SkillStandingsScreen(
    onBack: () -> Unit,
    // These values would eventually be pulled from your Supabase backend
    techScore: Float = 0.75f,
    sustainabilityIndex: Float = 0.65f,
    stabilityIndex: Float = 0.80f
) {
    val backgroundColor = Color(0xFF0C0C0C)
    val cardColor = Color(0xFF18181B)
    val accentCyan = Color(0xFF22D3EE)

    Scaffold(containerColor = backgroundColor) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "Your Professional Standings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Real-time evaluation based on your Gauntlet trajectories.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // --- THE 2026 METRIC RUBRIC ---
            MetricCard("Execution Correctness", techScore, "40% Weight", accentCyan, cardColor)
            Spacer(modifier = Modifier.height(16.dp))
            MetricCard("Sustainability Index", sustainabilityIndex, "35% Weight", Color.Green, cardColor)
            Spacer(modifier = Modifier.height(16.dp))
            MetricCard("Agent Stability", stabilityIndex, "25% Weight", Color.Magenta, cardColor)

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RETURN TO HUB", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, score: Float, weight: String, color: Color, cardColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(weight, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Progress Bar representing the score
            LinearProgressIndicator(
                progress = score,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = color,
                trackColor = Color.DarkGray
            )
            Text(
                "${(score * 100).toInt()}% Proficiency",
                color = color,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}