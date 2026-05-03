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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    onNavigateToJobSimulation: () -> Unit,
    techPassed: Boolean = false,
    aptitudePassed: Boolean = false
) {
    // HireSphere Monochrome Luxury Palette
    val backgroundColor = if (isDark) Color(0xFF0C0C0C) else Color(0xFFF9FAFB)
    val cardBg = if (isDark) Color(0xFF18181B) else Color.White
    val accentColor = Color(0xFF22D3EE)
    val primaryText = if (isDark) Color.White else Color.Black
    val secondaryText = if (isDark) Color(0xFFD1D5DB) else Color.DarkGray

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Interviews & Prep", color = primaryText, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        // FIX: Using AutoMirrored to resolve deprecation warning
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Master the Interview Gauntlet",
                fontSize = 24.sp,
                color = primaryText,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Select a module to begin simulation. Real-time feedback enabled.",
                fontSize = 14.sp,
                color = secondaryText,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // 1. Live Interview
            HubCard(
                title = "Live Interview",
                subtitle = "Voice-based technical and behavioral simulation.",
                icon = Icons.Default.Mic,
                accentColor = accentColor,
                isDark = isDark,
                onClick = onNavigateToLiveVoice
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Technical Turnaround
            HubCard(
                title = "Technical Turnaround",
                subtitle = "Algorithmic challenges and TG-A1 to TG-A5 gates.",
                icon = Icons.Default.Code,
                accentColor = accentColor,
                isDark = isDark,
                onClick = onNavigateToTechnical
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Quantitative Aptitude
            HubCard(
                title = "Quantitative Aptitude",
                subtitle = "Adaptive reasoning (IRT 2-PL logic).",
                icon = Icons.Default.Psychology,
                accentColor = accentColor,
                isDark = isDark,
                enabled = techPassed, // Unlock logic preserved
                onClick = onNavigateToAptitude
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Group Discussion
            HubCard(
                title = "Group Discussion",
                subtitle = "AI Agent personas: Skeptic, Visionary, PM.",
                icon = Icons.Default.Groups,
                accentColor = accentColor,
                isDark = isDark,
                enabled = aptitudePassed, // Unlock logic preserved
                onClick = onNavigateToGroupDiscussion
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Immersive Job Simulation
            HubCard(
                title = "Job Simulation",
                subtitle = "Full immersive workflow with HITL elements.",
                icon = Icons.Default.Work,
                accentColor = accentColor,
                isDark = isDark,
                onClick = onNavigateToJobSimulation
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun HubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isDark: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val cardBg = if (isDark) Color(0xFF18181B) else Color.White
    val iconBg = if (isDark) accentColor.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.1f)
    val alpha = if (enabled) 1.0f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled) { onClick() }
            .graphicsLayer { this.alpha = alpha },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}