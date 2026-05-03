@file:OptIn(ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogHistoryScreen(
    onBack: () -> Unit,
    logs: List<LogEntry> = emptyList()
) {
    val bgColor = Color(0xFF030303)
    val cardBg = Color(0xFF0C0C0C)
    val accentCyan = Color(0xFF22D3EE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity & Audit Ledger", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = bgColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = accentCyan, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "This ledger tracks your credit utilization and session integrity in real-time.",
                        color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp
                    )
                }
            }

            Text("Recent Trajectories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            // --- REAL-TIME DATA LOGIC ---
            if (logs.isEmpty()) {
                EmptyLogsState(accentCyan) // Shows only if no real data exists[cite: 15]
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(logs) { log -> LogItem(log = log, accentColor = accentCyan) }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun EmptyLogsState(accentColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.DarkGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Activity Logged", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Complete a technical round or interview to see your activity history here.",
            color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp).padding(top = 8.dp)
        )
    }
}

@Composable
fun LogItem(log: LogEntry, accentColor: Color) {
    val statusColor = when (log.status) {
        "COMPLETED", "SUCCESS" -> Color.Green
        "INTERRUPTED" -> Color.Yellow
        else -> Color.Red
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0C0C0C),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(text = log.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "${log.date} • Status: ${log.status}", color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(text = "${log.credits} CR", color = accentColor, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = log.description, color = Color.Gray, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

data class LogEntry(val date: String, val title: String, val status: String, val credits: Int, val description: String)