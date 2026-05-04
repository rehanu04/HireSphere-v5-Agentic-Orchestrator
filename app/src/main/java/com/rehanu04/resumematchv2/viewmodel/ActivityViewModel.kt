package com.rehanu04.resumematchv2.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.rehanu04.resumematchv2.data.LogEntry // Ensure this is accessible

class ActivityViewModel : ViewModel() {
    // Real-time stream of logs
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    // Real-time proficiency metrics[cite: 16]
    var techScore = MutableStateFlow(0f)
    var sustainabilityIndex = MutableStateFlow(0f)
    var stabilityIndex = MutableStateFlow(0f)

    fun addLog(entry: LogEntry) {
        _logs.value = _logs.value + entry
    }

    fun updateMetrics(tech: Float, sustain: Float, stable: Float) {
        techScore.value = tech
        sustainabilityIndex.value = sustain
        stabilityIndex.value = stable
    }
}