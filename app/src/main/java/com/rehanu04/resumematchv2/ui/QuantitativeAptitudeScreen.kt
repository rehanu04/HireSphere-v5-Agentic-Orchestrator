@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.pdf.PdfDocument
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehanu04.resumematchv2.data.LogEntry
import com.rehanu04.resumematchv2.data.UserProfileStore
import com.rehanu04.resumematchv2.ui.viewmodel.ActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Local Question Bank ───────────────────────────────────────────────────────

private data class LocalQuestion(
    val id: String,
    val stem: String,
    val type: QuestionType,
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val difficulty: LocalDiff
)

private enum class QuestionType { MCQ, NUMERIC }
private enum class LocalDiff { EASY, MEDIUM, HARD }

private val LOCAL_BANK: List<LocalQuestion> = listOf(
    // EASY
    LocalQuestion("q1", "What is the time complexity of binary search on a sorted array of n elements?",
        QuestionType.MCQ, listOf("O(log n)", "O(n)", "O(n log n)", "O(1)"), difficulty = LocalDiff.EASY),
    LocalQuestion("q2", "A train travels 240 km in 4 hours. What is its speed in km/h? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "60", difficulty = LocalDiff.EASY),
    LocalQuestion("q3", "Which data structure follows the LIFO principle?",
        QuestionType.MCQ, listOf("Stack", "Queue", "Deque", "Heap"), difficulty = LocalDiff.EASY),
    LocalQuestion("q4", "If 5 workers complete a task in 8 days, how many days will 10 workers take? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "4", difficulty = LocalDiff.EASY),
    LocalQuestion("q5", "Which sorting algorithm has O(n log n) average-case complexity?",
        QuestionType.MCQ, listOf("Merge Sort", "Bubble Sort", "Selection Sort", "Insertion Sort"), difficulty = LocalDiff.EASY),
    LocalQuestion("q6", "A pipe fills a tank in 6 hours. What fraction of the tank is filled in 2 hours? Enter numerator only (over 3). [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "1", difficulty = LocalDiff.EASY),
    LocalQuestion("q7", "Which HTTP method is idempotent and used to retrieve data?",
        QuestionType.MCQ, listOf("GET", "POST", "DELETE", "PATCH"), difficulty = LocalDiff.EASY),
    LocalQuestion("q8", "A rectangle has length 12 cm and width 5 cm. What is its perimeter in cm? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "34", difficulty = LocalDiff.EASY),
    // MEDIUM
    LocalQuestion("q9", "In a group of 60 people, 40% are engineers. How many are NOT engineers? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "36", difficulty = LocalDiff.MEDIUM),
    LocalQuestion("q10", "Which design pattern ensures a class has only one instance?",
        QuestionType.MCQ, listOf("Singleton", "Factory", "Observer", "Decorator"), difficulty = LocalDiff.MEDIUM),
    LocalQuestion("q11", "A sum of Rs 5000 earns 10% simple interest per year. What is the interest after 3 years? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "1500", difficulty = LocalDiff.MEDIUM),
    LocalQuestion("q12", "Which SQL clause filters results after aggregation?",
        QuestionType.MCQ, listOf("HAVING", "WHERE", "GROUP BY", "ORDER BY"), difficulty = LocalDiff.MEDIUM),
    LocalQuestion("q13", "Two pipes fill a tank in 3 hours and 6 hours respectively. Together, how many hours do they take? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "2", difficulty = LocalDiff.MEDIUM),
    LocalQuestion("q14", "Which layer in the OSI model handles end-to-end communication?",
        QuestionType.MCQ, listOf("Transport", "Network", "Session", "Data Link"), difficulty = LocalDiff.MEDIUM),
    LocalQuestion("q15", "A car depreciates 20% per year. If its value is Rs 100000 now, what is its value after 2 years? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "64000", difficulty = LocalDiff.MEDIUM),
    LocalQuestion("q16", "Which algorithm is used in Dijkstra's shortest path implementation efficiently?",
        QuestionType.MCQ, listOf("Min Heap (Priority Queue)", "Stack", "Queue (BFS)", "Union-Find"), difficulty = LocalDiff.MEDIUM),
    // HARD
    LocalQuestion("q17", "In IRT 2-PL model, which parameter controls item discrimination?",
        QuestionType.MCQ, listOf("Parameter 'a'", "Parameter 'b'", "Parameter 'c'", "Parameter 'd'"), difficulty = LocalDiff.HARD),
    LocalQuestion("q18", "A 12-bit addressing system can address how many unique memory locations? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "4096", difficulty = LocalDiff.HARD),
    LocalQuestion("q19", "Which NP-complete problem is solved by dynamic programming via the Held-Karp algorithm?",
        QuestionType.MCQ, listOf("Travelling Salesman Problem", "Knapsack Problem", "Graph Coloring", "Subset Sum"), difficulty = LocalDiff.HARD),
    LocalQuestion("q20", "If f(x) = 3x² + 2x − 5, what is f(3)? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "28", difficulty = LocalDiff.HARD),
    LocalQuestion("q21", "Which consistency model do distributed databases use when prioritizing availability over consistency?",
        QuestionType.MCQ, listOf("Eventual Consistency", "Strong Consistency", "Linearizability", "Serializability"), difficulty = LocalDiff.HARD),
    LocalQuestion("q22", "A binary tree has 7 levels. What is the maximum number of nodes it can contain? [NUMERIC]",
        QuestionType.NUMERIC, correctAnswer = "127", difficulty = LocalDiff.HARD),
    LocalQuestion("q23", "Which theorem states that you cannot simultaneously have Consistency, Availability, and Partition Tolerance?",
        QuestionType.MCQ, listOf("CAP Theorem", "Paxos Theorem", "ACID Theorem", "BASE Theorem"), difficulty = LocalDiff.HARD),
    LocalQuestion("q24", "In Big-O notation, what is the space complexity of merge sort? [NUMERIC — answer: 1 for O(n), 2 for O(log n), 3 for O(1)]",
        QuestionType.NUMERIC, correctAnswer = "1", difficulty = LocalDiff.HARD),
    LocalQuestion("q25", "Which technique resolves hash collisions by storing multiple entries in the same bucket as a linked list?",
        QuestionType.MCQ, listOf("Chaining", "Open Addressing", "Robin Hood Hashing", "Cuckoo Hashing"), difficulty = LocalDiff.HARD)
)

private val ROLE_TRACKS = listOf(
    "Software Engineer", "Android Developer", "iOS Developer", "Data Scientist",
    "Machine Learning Engineer", "Data Engineer", "Backend Engineer", "Frontend Engineer",
    "Full Stack Developer", "DevOps Engineer", "Cloud Architect", "Site Reliability Engineer",
    "Cybersecurity Analyst", "Product Manager", "Business Analyst", "QA Engineer",
    "Embedded Systems Engineer", "Systems Programmer", "Database Administrator", "AI Research Scientist"
)

private enum class AptitudePhase { CONFIG, RUNNING, SCORECARD }
private enum class Difficulty(val label: String, val seedTheta: Float, val tag: String) {
    EASY("Easy", -1.0f, "easy"), MEDIUM("Medium", 0.0f, "medium"), HARD("Hard", 0.5f, "hard")
}
private enum class AnswerState { BLANK, VISITED_SKIPPED, FLAGGED, CONFIRMED }

private data class ActiveQuestion(
    val id: String, val text: String, val isNumeric: Boolean, val options: List<String>,
    val correctIndex: Int, val localCorrectAnswer: String, val localDiff: LocalDiff
)

private data class ItemState(
    val question: ActiveQuestion, var state: AnswerState = AnswerState.BLANK,
    var userAnswerText: String = "", var userAnswerIndex: Int = -1
) {
    fun isCorrect(): Boolean {
        if (state != AnswerState.CONFIRMED) return false
        return if (question.isNumeric) {
            userAnswerText.trim().equals(question.localCorrectAnswer.trim(), ignoreCase = true)
        } else {
            userAnswerIndex == question.correctIndex
        }
    }
}

private fun generateEvaluationMatrix(difficulty: Difficulty, seenIds: Set<String>): List<ActiveQuestion> {
    var easyPool   = LOCAL_BANK.filter { it.difficulty == LocalDiff.EASY && it.id !in seenIds }.shuffled()
    var mediumPool = LOCAL_BANK.filter { it.difficulty == LocalDiff.MEDIUM && it.id !in seenIds }.shuffled()
    var hardPool   = LOCAL_BANK.filter { it.difficulty == LocalDiff.HARD && it.id !in seenIds }.shuffled()

    val easyNeeded = when (difficulty) {
        Difficulty.EASY   -> 6
        Difficulty.MEDIUM -> 3
        Difficulty.HARD   -> 1
    }
    val mediumNeeded = when (difficulty) {
        Difficulty.EASY   -> 3
        Difficulty.MEDIUM -> 4
        Difficulty.HARD   -> 3
    }
    val hardNeeded = when (difficulty) {
        Difficulty.EASY   -> 1
        Difficulty.MEDIUM -> 3
        Difficulty.HARD   -> 5
    }

    // Fallback if not enough questions are left in easyPool
    if (easyPool.size < easyNeeded) {
        easyPool = LOCAL_BANK.filter { it.difficulty == LocalDiff.EASY }.shuffled()
    }
    // Fallback if not enough questions are left in mediumPool
    if (mediumPool.size < mediumNeeded) {
        mediumPool = LOCAL_BANK.filter { it.difficulty == LocalDiff.MEDIUM }.shuffled()
    }
    // Fallback if not enough questions are left in hardPool
    if (hardPool.size < hardNeeded) {
        hardPool = LOCAL_BANK.filter { it.difficulty == LocalDiff.HARD }.shuffled()
    }

    val dist = when (difficulty) {
        Difficulty.EASY   -> listOf(easyPool.take(easyNeeded), mediumPool.take(mediumNeeded), hardPool.take(hardNeeded))
        Difficulty.MEDIUM -> listOf(easyPool.take(easyNeeded), mediumPool.take(mediumNeeded), hardPool.take(hardNeeded))
        Difficulty.HARD   -> listOf(easyPool.take(easyNeeded), mediumPool.take(mediumNeeded), hardPool.take(hardNeeded))
    }
    return dist.flatten().map { q ->
        if (q.type == QuestionType.NUMERIC) {
            ActiveQuestion(q.id, q.stem, true, emptyList(), -1, q.correctAnswer, q.difficulty)
        } else {
            val correctOpt = q.options[0]
            val shuffled   = q.options.shuffled()
            val newIdx     = shuffled.indexOf(correctOpt)
            ActiveQuestion(q.id, q.stem, false, shuffled, newIdx, "", q.difficulty)
        }
    }
}

private fun getSeenQuestionIds(context: Context): Set<String> {
    val seen = mutableSetOf<String>()
    try {
        val file = File(context.filesDir, "aptitude_history.json")
        if (file.exists()) {
            val content = file.readText()
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val session = array.getJSONObject(i)
                val questions = session.optJSONArray("questions") ?: continue
                for (j in 0 until questions.length()) {
                    val q = questions.getJSONObject(j)
                    val qId = q.optString("id")
                    if (!qId.isNullOrEmpty()) {
                        seen.add(qId)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return seen
}

private suspend fun saveSessionToHistory(
    context: Context,
    matrix: List<ItemState>,
    score: Int,
    theta: Float,
    b: Float,
    a: Float,
    role: String,
    difficulty: String
): Set<String> = withContext(Dispatchers.IO) {
    val newSeen = mutableSetOf<String>()
    try {
        val file = File(context.filesDir, "aptitude_history.json")
        val array = if (file.exists()) {
            try {
                JSONArray(file.readText())
            } catch (e: Exception) {
                JSONArray()
            }
        } else {
            JSONArray()
        }

        val sessionObj = JSONObject()
        sessionObj.put("score", score)
        sessionObj.put("timestamp", System.currentTimeMillis())
        sessionObj.put("theta", theta)
        sessionObj.put("b", b)
        sessionObj.put("a", a)
        sessionObj.put("role", role)
        sessionObj.put("difficulty", difficulty)

        val questionsArray = JSONArray()
        for (item in matrix) {
            val qObj = JSONObject()
            qObj.put("id", item.question.id)
            qObj.put("title", item.question.text)
            val userAns = if (item.question.isNumeric) item.userAnswerText else {
                if (item.userAnswerIndex >= 0) item.question.options.getOrNull(item.userAnswerIndex) ?: "" else ""
            }
            qObj.put("userAnswer", userAns)
            qObj.put("correct", item.isCorrect())
            questionsArray.put(qObj)
            
            newSeen.add(item.question.id)
        }
        sessionObj.put("questions", questionsArray)
        array.put(sessionObj)

        file.writeText(array.toString(2))
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext newSeen
}

// ── Native PDF Export Engine ──────────────────────────────────────────────────

private suspend fun generatePdfTranscript(
    context: Context,
    matrix: List<ItemState>,
    finalTheta: Float,
    finalB: Float,
    finalA: Float,
    correctCount: Int
) {
    withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 18f
                isFakeBoldText = true
            }
            val subPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 14f
            }
            val textPaint = TextPaint().apply {
                color = android.graphics.Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val redPaint = TextPaint().apply {
                color = android.graphics.Color.parseColor("#EF4444")
                textSize = 12f
                isAntiAlias = true
            }
            val greenPaint = TextPaint().apply {
                color = android.graphics.Color.parseColor("#10B981")
                textSize = 12f
                isAntiAlias = true
            }

            var currentY = 50f
            canvas.drawText("HireSphere Academic Ledger - Quantitative Aptitude Report", 50f, currentY, titlePaint)
            currentY += 30f
            canvas.drawText("Total Score: $correctCount / ${matrix.size}", 50f, currentY, subPaint)
            currentY += 20f
            canvas.drawText("IRT Metrics: θ: ${"%.2f".format(finalTheta)}, b: ${"%.2f".format(finalB)}, a: ${"%.2f".format(finalA)}", 50f, currentY, subPaint)
            currentY += 40f

            for ((i, state) in matrix.withIndex()) {
                val qText = "${i + 1}. ${state.question.text}"
                val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(qText, 0, qText.length, textPaint, 495).build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(qText, textPaint, 495, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
                }

                if (currentY + staticLayout.height + 100f > 800f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 50f
                }

                canvas.save()
                canvas.translate(50f, currentY)
                staticLayout.draw(canvas)
                canvas.restore()
                currentY += staticLayout.height + 10f

                val isCorrect = state.isCorrect()
                if (state.question.isNumeric) {
                    canvas.drawText("Your Answer: ${state.userAnswerText.ifBlank { "None" }}", 50f, currentY, if (isCorrect) greenPaint else redPaint)
                    currentY += 16f
                    canvas.drawText("Correct Answer: ${state.question.localCorrectAnswer}", 50f, currentY, greenPaint)
                    currentY += 24f
                } else {
                    val userAnsText = if (state.userAnswerIndex >= 0) state.question.options.getOrNull(state.userAnswerIndex) ?: "None" else "None"
                    val correctAnsText = state.question.options.getOrNull(state.question.correctIndex) ?: "Unknown"
                    canvas.drawText("Your Answer: $userAnsText", 50f, currentY, if (isCorrect) greenPaint else redPaint)
                    currentY += 16f
                    if (!isCorrect) {
                        canvas.drawText("Correct Answer: $correctAnsText", 50f, currentY, greenPaint)
                        currentY += 16f
                    }
                    currentY += 8f
                }
            }
            pdfDocument.finishPage(page)

            val filename = "Aptitude_Transcript_${System.currentTimeMillis()}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val pdfFile = File(downloadsDir, filename)
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            MediaScannerConnection.scanFile(
                context,
                arrayOf(pdfFile.absolutePath),
                arrayOf("application/pdf"),
                null
            )

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Transcript saved to Downloads: $filename", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save transcript: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
fun QuantitativeAptitudeScreen(
    isDark: Boolean,
    apiBaseUrl: String,
    activityViewModel: ActivityViewModel?,
    userProfileStore: UserProfileStore?,
    onBack: () -> Unit
) {
    val accent    = Color(0xFF22D3EE)
    val textColor = if (isDark) Color.White else Color(0xFF111827)
    val surface   = if (isDark) Color(0xFF18181B) else Color.White
    val cardGlass = if (isDark) Color(0xFF1F2937).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.9f)
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()

    var phase by remember { mutableStateOf(AptitudePhase.CONFIG) }
    var selectedRole       by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.MEDIUM) }
    var matrix          by remember { mutableStateOf<List<ItemState>>(emptyList()) }
    var currentIndex    by remember { mutableIntStateOf(0) }
    var timeLeft        by remember { mutableIntStateOf(600) }

    var finalTheta by remember { mutableFloatStateOf(0f) }
    var finalB     by remember { mutableFloatStateOf(0f) }
    var finalA     by remember { mutableFloatStateOf(0f) }

    var seenIds by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(Unit) {
        seenIds = getSeenQuestionIds(context)
    }

    LaunchedEffect(userProfileStore) {
        userProfileStore?.userProfileFlow?.first()?.let { profile ->
            if (profile.targetRole.isNotBlank()) selectedRole = profile.targetRole
        }
    }

    LaunchedEffect(timeLeft, phase) {
        if (phase == AptitudePhase.RUNNING && timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    fun finishExam() {
        val correctCount = matrix.count { it.isCorrect() }
        val scorePercent = (correctCount.toFloat() / matrix.size) * 100f
        finalTheta = -2.0f + (scorePercent / 100f) * 4.0f
        finalB = matrix.sumOf {
            when (it.question.localDiff) { LocalDiff.EASY -> -1.0; LocalDiff.MEDIUM -> 0.0; LocalDiff.HARD -> 1.0 }
        }.toFloat() / matrix.size
        finalA = 0.8f + (Math.random().toFloat() * 0.7f)

        val tag = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date()).uppercase()
        activityViewModel?.addLog(LogEntry(tag, "Quantitative Aptitude",
            if (scorePercent >= 50f) "PASSED" else "FAILED", scorePercent.toInt(),
            "Bulk Grid Eval | Theta: ${"%.2f".format(finalTheta)} | ${selectedDifficulty.label} | $selectedRole"))

        scope.launch {
            val newSeen = saveSessionToHistory(
                context = context,
                matrix = matrix,
                score = correctCount,
                theta = finalTheta,
                b = finalB,
                a = finalA,
                role = selectedRole,
                difficulty = selectedDifficulty.name
            )
            seenIds = seenIds + newSeen
        }

        phase = AptitudePhase.SCORECARD
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(
                colors = if (isDark) listOf(Color(0xFF0C0C0C), Color(0xFF0F172A))
                else listOf(Color(0xFFF9FAFB), Color(0xFFEFF6FF))
            ))
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("HIRESPHERE", style = MaterialTheme.typography.labelSmall,
                                color = accent, letterSpacing = 2.sp, fontSize = 10.sp)
                            Text("Quantitative Aptitude", style = MaterialTheme.typography.titleMedium,
                                color = textColor, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        if (phase == AptitudePhase.SCORECARD) {
                            IconButton(onClick = {
                                scope.launch {
                                    generatePdfTranscript(context, matrix, finalTheta, finalB, finalA, matrix.count { it.isCorrect() })
                                }
                            }) {
                                Icon(Icons.Default.Download, null, tint = accent)
                            }
                        }
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, null, tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            AnimatedContent(
                targetState = phase,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(300)) },
                label = "phase",
                modifier = Modifier.fillMaxSize().padding(padding)
            ) { currentPhase ->
                when (currentPhase) {
                    AptitudePhase.CONFIG -> ConfigGate(
                        isDark = isDark, accent = accent, textColor = textColor, cardGlass = cardGlass,
                        selectedRole = selectedRole, onRoleChange = { selectedRole = it },
                        selectedDifficulty = selectedDifficulty, onDifficultyChange = { selectedDifficulty = it },
                        onLaunch = {
                            matrix = generateEvaluationMatrix(selectedDifficulty, seenIds).map { ItemState(it) }
                            currentIndex = 0; timeLeft = 600; phase = AptitudePhase.RUNNING
                        }
                    )
                    AptitudePhase.RUNNING -> RunningPhase(
                        isDark = isDark, accent = accent, textColor = textColor, surface = surface,
                        timeLeft = timeLeft, matrix = matrix, currentIndex = currentIndex,
                        onIndexChange = { newIdx ->
                            if (matrix[currentIndex].state == AnswerState.BLANK) {
                                matrix = matrix.toMutableList().apply { this[currentIndex] = this[currentIndex].copy(state = AnswerState.VISITED_SKIPPED) }
                            }
                            currentIndex = newIdx
                        },
                        onAnswerUpdate = { updatedState -> matrix = matrix.toMutableList().apply { this[currentIndex] = updatedState } },
                        onFinish = { finishExam() }
                    )
                    AptitudePhase.SCORECARD -> ScorecardPhase(
                        isDark = isDark, accent = accent, textColor = textColor, surface = surface,
                        finalTheta = finalTheta, finalB = finalB, finalA = finalA, matrix = matrix,
                        onRetake = {
                            matrix = generateEvaluationMatrix(selectedDifficulty, seenIds).map { ItemState(it) }
                            currentIndex = 0
                            timeLeft = 600
                            phase = AptitudePhase.RUNNING
                        },
                        onExit = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigGate(
    isDark: Boolean, accent: Color, textColor: Color, cardGlass: Color,
    selectedRole: String, onRoleChange: (String) -> Unit,
    selectedDifficulty: Difficulty, onDifficultyChange: (Difficulty) -> Unit, onLaunch: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue(selectedRole)) }
    var dropdownOpen by remember { mutableStateOf(false) }

    val suggestions = remember(searchQuery.text) {
        if (searchQuery.text.isBlank()) ROLE_TRACKS else ROLE_TRACKS.filter { it.contains(searchQuery.text, ignoreCase = true) }
    }

    LaunchedEffect(selectedRole) {
        if (selectedRole != searchQuery.text) {
            searchQuery = TextFieldValue(selectedRole, selection = TextRange(selectedRole.length))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Psychology, null, tint = accent, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(12.dp))
        Text("Evaluation Setup", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
        Text("Configure your bulk non-linear aptitude session.", fontSize = 13.sp, color = if (isDark) Color(0xFF9CA3AF) else Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardGlass),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Target Role Track", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newValue ->
                        searchQuery = newValue
                        dropdownOpen = newValue.text.isNotBlank()
                        onRoleChange(newValue.text)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search role…", color = Color.Gray, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.text.isNotBlank()) {
                            IconButton(onClick = { searchQuery = TextFieldValue(""); dropdownOpen = false; onRoleChange("") }) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        } else Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = Color.Gray.copy(0.4f),
                        focusedTextColor = textColor, unfocusedTextColor = textColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                AnimatedVisibility(visible = dropdownOpen && suggestions.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        LazyColumn {
                            items(suggestions) { role ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        searchQuery = TextFieldValue(role, selection = TextRange(role.length))
                                        dropdownOpen = false; onRoleChange(role)
                                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) { Text(role, fontSize = 14.sp, color = textColor) }
                                if (suggestions.last() != role) HorizontalDivider(color = Color.Gray.copy(0.15f))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Difficulty Strategy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = accent)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Difficulty.values().forEach { d ->
                        val selected = d == selectedDifficulty
                        val chipScale by animateFloatAsState(targetValue = if (selected) 1.04f else 1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f), label = "chipScale")
                        Surface(
                            modifier = Modifier.weight(1f).scale(chipScale).clip(RoundedCornerShape(12.dp)).clickable { onDifficultyChange(d) },
                            color = if (selected) accent else Color.Transparent, shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selected) accent else Color.Gray.copy(0.4f))
                        ) {
                            Column(modifier = Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(d.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.Black else textColor)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onLaunch, enabled = searchQuery.text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent, disabledContainerColor = accent.copy(alpha = 0.35f))
        ) { Text("LAUNCH GRID EVALUATION", fontWeight = FontWeight.ExtraBold, color = if (searchQuery.text.isNotBlank()) Color.Black else Color.Gray, letterSpacing = 1.sp) }
        Spacer(Modifier.height(8.dp))
        if (searchQuery.text.isBlank()) Text("Select a role track to enable the evaluation.", fontSize = 12.sp, color = Color(0xFFEF4444), textAlign = TextAlign.Center)
    }
}

@Composable
private fun RunningPhase(
    isDark: Boolean, accent: Color, textColor: Color, surface: Color, timeLeft: Int,
    matrix: List<ItemState>, currentIndex: Int, onIndexChange: (Int) -> Unit, onAnswerUpdate: (ItemState) -> Unit, onFinish: () -> Unit
) {
    val currentItem = matrix[currentIndex]
    val q = currentItem.question

    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 0.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Item ${currentIndex + 1} / ${matrix.size}", color = Color.Gray, fontSize = 13.sp)
            val minutes = timeLeft / 60; val seconds = timeLeft % 60
            Text("⏱ %02d:%02d".format(minutes, seconds), color = if (timeLeft <= 60) Color(0xFFEF4444) else Color.Gray, fontSize = 14.sp, fontWeight = if (timeLeft <= 60) FontWeight.Bold else FontWeight.Normal)
        }
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surface), elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(if (q.isNumeric) "Numeric Reasoning (${q.localDiff})" else "Multiple Choice (${q.localDiff})", fontWeight = FontWeight.Bold, color = accent, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(q.text, color = textColor, fontSize = 16.sp, lineHeight = 24.sp)
                Spacer(Modifier.height(24.dp))

                if (q.isNumeric) NumericInputGrid(accent = accent, textColor = textColor, itemState = currentItem, onStateUpdate = onAnswerUpdate)
                else McqInputGrid(accent = accent, textColor = textColor, itemState = currentItem, onStateUpdate = onAnswerUpdate)

                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isFlagged = currentItem.state == AnswerState.FLAGGED
                    OutlinedButton(
                        onClick = { onAnswerUpdate(currentItem.copy(state = if (isFlagged) AnswerState.VISITED_SKIPPED else AnswerState.FLAGGED)) },
                        modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isFlagged) Color(0xFFFBBF24).copy(0.1f) else Color.Transparent),
                        border = BorderStroke(1.dp, if (isFlagged) Color(0xFFFBBF24) else Color.Gray.copy(0.4f))
                    ) {
                        Icon(Icons.Default.Flag, null, tint = if (isFlagged) Color(0xFFFBBF24) else textColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Review", color = if (isFlagged) Color(0xFFFBBF24) else textColor, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onAnswerUpdate(currentItem.copy(state = AnswerState.CONFIRMED))
                            val nextUnanswered = matrix.indexOfFirst { it.state == AnswerState.BLANK }
                            if (nextUnanswered != -1) onIndexChange(nextUnanswered)
                            else if (currentIndex < matrix.size - 1) onIndexChange(currentIndex + 1)
                        },
                        modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        enabled = currentItem.userAnswerText.isNotBlank() || currentItem.userAnswerIndex != -1
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
            items(matrix.size) { idx ->
                val state = matrix[idx].state
                val isSelected = idx == currentIndex
                val (bgColor, contentColor, borderColor) = when (state) {
                    AnswerState.CONFIRMED -> Triple(Color(0xFF10B981).copy(0.2f), Color(0xFF10B981), Color(0xFF10B981))
                    AnswerState.FLAGGED -> Triple(Color(0xFFFBBF24).copy(0.2f), Color(0xFFFBBF24), Color(0xFFFBBF24))
                    AnswerState.VISITED_SKIPPED -> Triple(Color(0xFFEF4444).copy(0.1f), Color(0xFFEF4444).copy(0.7f), Color(0xFFEF4444).copy(0.3f))
                    AnswerState.BLANK -> Triple(surface, textColor, Color.Gray.copy(0.3f))
                }
                Surface(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable { onIndexChange(idx) },
                    color = if (isSelected) accent else bgColor, shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) textColor else borderColor)
                ) { Box(contentAlignment = Alignment.Center) { Text(text = (idx + 1).toString(), fontWeight = FontWeight.Bold, color = if (isSelected) Color.Black else contentColor, fontSize = 16.sp) } }
            }
        }
        Button(
            onClick = onFinish, modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB))
        ) { Text("FINISH EXAM", color = textColor, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun NumericInputGrid(accent: Color, textColor: Color, itemState: ItemState, onStateUpdate: (ItemState) -> Unit) {
    OutlinedTextField(
        value = itemState.userAnswerText,
        onValueChange = { onStateUpdate(itemState.copy(userAnswerText = it, state = if (itemState.state == AnswerState.CONFIRMED) AnswerState.VISITED_SKIPPED else itemState.state)) },
        modifier = Modifier.fillMaxWidth(), label = { Text("Enter numeric value", color = Color.Gray, fontSize = 13.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, unfocusedBorderColor = Color.Gray.copy(0.4f), focusedTextColor = textColor, unfocusedTextColor = textColor),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun McqInputGrid(accent: Color, textColor: Color, itemState: ItemState, onStateUpdate: (ItemState) -> Unit) {
    itemState.question.options.forEachIndexed { index, option ->
        val selected = itemState.userAnswerIndex == index
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable {
                onStateUpdate(itemState.copy(userAnswerIndex = index, userAnswerText = option, state = if (itemState.state == AnswerState.CONFIRMED) AnswerState.VISITED_SKIPPED else itemState.state))
            },
            shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = if (selected) accent.copy(alpha = 0.15f) else Color.Transparent),
            border = BorderStroke(1.dp, if (selected) accent else Color.Gray.copy(0.35f))
        ) { Text(option, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), color = if (selected) accent else textColor, fontSize = 14.sp) }
    }
}

// ── Scorecard Phase ───────────────────────────────────────────────────────────

@Composable
private fun ScorecardPhase(
    isDark: Boolean, accent: Color, textColor: Color, surface: Color,
    finalTheta: Float, finalB: Float, finalA: Float, matrix: List<ItemState>,
    onRetake: () -> Unit,
    onExit: () -> Unit
) {
    val correctCount = matrix.count { it.isCorrect() }
    val totalCount = matrix.size

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp)
    ) {
        item {
            Icon(Icons.Default.Speed, null, tint = accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(16.dp))
            Text("Recruiter Analytics", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text("IRT 2-PL Evaluation Model Results", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(32.dp))

            val percentage = (correctCount.toFloat() / totalCount * 100).toInt()
            Text("$percentage%", fontSize = 56.sp, fontWeight = FontWeight.Black, color = if (percentage >= 50) Color(0xFF10B981) else Color(0xFFEF4444))
            Text("$correctCount of $totalCount Correct", fontSize = 16.sp, color = textColor)
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Mathematical Weights", fontWeight = FontWeight.Bold, color = accent, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    IrtMetricRow("Terminal Ability Trait (θ)", "%.2f".format(finalTheta), "Determines baseline candidate aptitude.")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(0.15f))
                    IrtMetricRow("Avg Difficulty Threshold (b)", "%.2f".format(finalB), "The probability baseline required to pass.")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(0.15f))
                    IrtMetricRow("Discrimination Vector (a)", "%.2f".format(finalA), "Item accuracy stability index.")
                }
            }
            Spacer(Modifier.height(32.dp))
            Text("Review Matrix", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            Spacer(Modifier.height(16.dp))
        }

        itemsIndexed(matrix) { idx, state ->
            ReviewCard(isDark = isDark, surface = surface, accent = accent, textColor = textColor, index = idx, itemState = state)
            Spacer(Modifier.height(16.dp))
        }

        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, accent)
                ) {
                    Text("RETAKE EXAM", color = textColor, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onExit,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text("RETURN TO HUB", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(isDark: Boolean, surface: Color, accent: Color, textColor: Color, index: Int, itemState: ItemState) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Question ${index + 1}", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(itemState.question.text, fontSize = 14.sp, color = textColor, lineHeight = 20.sp)
            Spacer(Modifier.height(16.dp))

            val isCorrect = itemState.isCorrect()
            val cyanGreen = Color(0xFF10B981)
            val mutedRed = Color(0xFFEF4444)

            if (itemState.question.isNumeric) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Your Answer:", fontSize = 12.sp, color = Color.Gray)
                        Text(itemState.userAnswerText.ifBlank { "None" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isCorrect) cyanGreen else mutedRed)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Expected Key:", fontSize = 12.sp, color = Color.Gray)
                        Text(itemState.question.localCorrectAnswer, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cyanGreen)
                    }
                }
            } else {
                itemState.question.options.forEachIndexed { optIdx, option ->
                    val isSelected = optIdx == itemState.userAnswerIndex
                    val isActualCorrect = optIdx == itemState.question.correctIndex

                    val bgColor = when {
                        isActualCorrect -> cyanGreen.copy(0.15f)
                        isSelected && !isActualCorrect -> mutedRed.copy(0.1f)
                        else -> Color.Transparent
                    }
                    val borderColor = when {
                        isActualCorrect -> cyanGreen
                        isSelected && !isActualCorrect -> mutedRed.copy(0.5f)
                        else -> Color.Gray.copy(0.2f)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor), border = BorderStroke(1.dp, borderColor)
                    ) { Text(option, modifier = Modifier.padding(12.dp), color = textColor, fontSize = 13.sp) }
                }
            }
        }
    }
}

@Composable
private fun IrtMetricRow(title: String, value: String, description: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.TrendingUp, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF22D3EE))
    }
}
