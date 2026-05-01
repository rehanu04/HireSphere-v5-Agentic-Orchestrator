@file:OptIn(ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InterviewHubScreen(
    isDark: Boolean,
    onBack: () -> Unit,
    onNavigateToLiveVoice: () -> Unit,
    onNavigateToTechnical: () -> Unit,
    onNavigateToAptitude: () -> Unit,
    onNavigateToGroupDiscussion: () -> Unit,
    onNavigateToJobSimulation: () -> Unit
) {
    val scrollState = rememberScrollState()
    val bgColor = if (isDark) Color(0xFF030303) else MaterialTheme.colorScheme.background
    val textColor = if (isDark) Color.White else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interviews & Prep", fontWeight = FontWeight.Bold, color = textColor) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            HubCard("MasterR Voice Interview", "A fully autonomous, real-time voice interview evaluating your Master Vault data.", Icons.Default.Mic, Color(0xFF10B981), isDark, onNavigateToLiveVoice)
            HubCard("Technical Written Round", "Answer 4 highly specific technical and behavioral questions generated from your projects.", Icons.Default.Psychology, Color(0xFF38BDF8), isDark, onNavigateToTechnical)
            HubCard("Quantitative Aptitude", "Test your logic, mathematics, and problem-solving speed under pressure.", Icons.Default.Quiz, Color(0xFFD4AF37), isDark, onNavigateToAptitude)
            HubCard("Group Discussion Simulator", "Practice collaborative problem-solving and articulation in a simulated multi-agent environment.", Icons.Default.Groups, Color(0xFF8B5CF6), isDark, onNavigateToGroupDiscussion)
            HubCard("Immersive Job Simulation", "Step into a day in the life. Resolve PR comments, debug outages, and navigate system design trade-offs.", Icons.Default.Work, Color(0xFFF59E0B), isDark, onNavigateToJobSimulation)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun HubCard(title: String, subtitle: String, icon: ImageVector, accentColor: Color, isDark: Boolean, onClick: () -> Unit) {
    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
    val iconBg = if (isDark) accentColor.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.1f)

    Card(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.LightGray else Color.DarkGray, lineHeight = 18.sp)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Color.Gray)
        }
    }
}