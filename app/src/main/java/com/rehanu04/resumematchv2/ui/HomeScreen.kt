@file:OptIn(ExperimentalMaterial3Api::class)

package com.rehanu04.resumematchv2.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * HireSphere v5 - Master System UI
 * Persona Architecture: [Cyan/Black] vs [Vivid Gold/Black]
 */

private data class OrbitalFeature(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    isDark: Boolean, // Night/Cyan (true) | Gold/Black (false)
    onToggleTheme: (Boolean) -> Unit,
    onNavigateToAnalyze: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToInterviewHub: () -> Unit
) {
    // --- 1. SYSTEM UI ADJUSTMENT ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Icons always light because background is always dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // --- 2. LUXURY PERSONA PALETTE ---
    val bgColor = Color(0xFF030303) // Pitch Black Luxury
    val cardColor = Color(0xFF0B0B0B) // Subtle Card Elevation
    val textColor = Color.White

    // Theme Accents: Cyan vs High-Saturation Gold
    val accentColor = if (isDark) Color(0xFF22D3EE) else Color(0xFFFFD700)
    val tubeColor = if (isDark) accentColor else Color(0xFFFFE066)
    val rayAlpha = if (isDark) 0.18f else 0.30f

    val animatedAccentColor by animateColorAsState(targetValue = accentColor, animationSpec = tween(1000), label = "accent")
    val animatedTubeColor by animateColorAsState(targetValue = tubeColor, animationSpec = tween(1000), label = "tube")

    // --- 3. SYSTEM MODULES ---
    val features = remember {
        listOf(
            OrbitalFeature(0, "Analyze Engine", "Intelligent JD Alignment & ATS Verification.", Icons.Filled.AutoAwesome, onNavigateToAnalyze),
            OrbitalFeature(1, "Live Interview", "Voice-adaptive technical stress-test simulation.", Icons.Filled.Mic, onNavigateToInterviewHub),
            OrbitalFeature(2, "Resume Builder", "Dynamic Vault-to-PDF generation sequence.", Icons.Default.Edit, onNavigateToCreate),
            OrbitalFeature(3, "Master Vault", "Central decentralized skill repository.", Icons.Filled.Storage, onNavigateToVault)
        )
    }

    var activeFeatureId by remember { mutableStateOf<Int?>(null) }

    // --- 4. ENGINE ANIMATION KINETICS ---
    val infiniteTransition = rememberInfiniteTransition(label = "core_engine")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(50000, easing = LinearEasing), // Slow, cinematic orbit
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                activeFeatureId = null
            }
    ) {
        // --- 5. ENLARGED GEOMETRIC LAMP (Anchored below subtitle) ---
        // Length tuned to exceed the "HireSphere" text width (approx 65% of screen)
        StudioSpotlightCanvas(
            accentColor = animatedAccentColor,
            tubeColor = animatedTubeColor,
            rayAlpha = rayAlpha
        )

        // --- 6. POSITIONED EXECUTIVE HEADER ---
        // Shifted further down as requested to balance with the lamp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 60.dp), // Increased vertical padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HireSphere",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = textColor,
                letterSpacing = (-2).sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "SYSTEM ORCHESTRATOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = animatedAccentColor,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // --- 7. RADIAL ORBITAL ENGINE ---
        val orbitRadiusDp = 150.dp
        val density = LocalDensity.current
        val orbitRadiusPx = with(density) { orbitRadiusDp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 160.dp), // Pushed down to clear the header area
            contentAlignment = Alignment.Center
        ) {
            // Mechanical Track
            Surface(
                modifier = Modifier.size(orbitRadiusDp * 2),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, animatedAccentColor.copy(alpha = 0.08f))
            ) {}

            // Pulsing AI Hub
            Box(
                modifier = Modifier
                    .size(95.dp)
                    .scale(corePulse),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = cardColor,
                    border = BorderStroke(1.5.dp, animatedAccentColor.copy(alpha = 0.6f)),
                    shadowElevation = 20.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SettingsSystemDaydream,
                            contentDescription = null,
                            tint = animatedAccentColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            // Satellite Features
            features.forEachIndexed { index, feature ->
                val isActive = activeFeatureId == feature.id
                val isDimmed = activeFeatureId != null && !isActive

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val baseAngle = (index * (360f / features.size))
                            val currentAngleRad = Math.toRadians((baseAngle + orbitAngle).toDouble())

                            translationX = (cos(currentAngleRad) * orbitRadiusPx).toFloat()
                            translationY = (sin(currentAngleRad) * orbitRadiusPx).toFloat()

                            scaleX = if (isActive) 1.25f else 1f
                            scaleY = if (isActive) 1.25f else 1f
                            alpha = if (isDimmed) 0.5f else 1f
                        }
                        .size(85.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) {
                            activeFeatureId = if (isActive) null else feature.id
                        }
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = if (isActive) animatedAccentColor else cardColor,
                            border = BorderStroke(1.2.dp, animatedAccentColor.copy(alpha = 0.4f)),
                            shadowElevation = if (isActive) 16.dp else 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = if (isActive) Color.Black else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (!isDimmed) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = feature.title,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- 8.bottom ACTION INTERFACE ---
        AnimatedVisibility(
            visible = activeFeatureId != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            val currentFeature = features.find { it.id == activeFeatureId }
            if (currentFeature != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, animatedAccentColor.copy(alpha = 0.25f)),
                    shadowElevation = 32.dp
                ) {
                    Column(modifier = Modifier.padding(28.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = animatedAccentColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(currentFeature.icon, null, tint = animatedAccentColor, modifier = Modifier.size(26.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = currentFeature.title,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = currentFeature.description,
                            color = Color.Gray,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )

                        Button(
                            onClick = {
                                activeFeatureId = null
                                currentFeature.onClick()
                            },
                            modifier = Modifier.fillMaxWidth().height(62.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = animatedAccentColor),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                        ) {
                            Text("INITIALIZE SEQUENCE", color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // --- 9. THE PHYSICS PULL SWITCH ---
        LampPullChain(isDark = isDark, onToggleTheme = onToggleTheme, accentColor = animatedAccentColor)
    }
}

// -----------------------------
// WIDE GEOMETRIC SPOTLIGHT
// -----------------------------
@Composable
private fun StudioSpotlightCanvas(accentColor: Color, tubeColor: Color, rayAlpha: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Anchored 180dp down (below the title/subtitle group)
        val tubeY = 520f
        val tubeWidth = w * 0.65f // Spans wider than the title "HireSphere"
        val tubeStartX = (w - tubeWidth) / 2f
        val tubeEndX = tubeStartX + tubeWidth

        // 1. Draw physical Tube
        drawLine(
            color = tubeColor,
            start = Offset(tubeStartX, tubeY),
            end = Offset(tubeEndX, tubeY),
            strokeWidth = 4.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 2. Parabolic/Angular Ray Spread
        val beamPath = Path().apply {
            moveTo(tubeStartX + 12f, tubeY)
            lineTo(tubeEndX - 12f, tubeY)
            // Properly spreads downward to engulf the orbital engine
            lineTo(w * 0.95f, h * 0.8f)
            lineTo(w * 0.05f, h * 0.8f)
            close()
        }

        drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = rayAlpha), Color.Transparent),
                startY = tubeY,
                endY = h * 0.8f
            )
        )

        // 3. Wide Focus Glow
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(accentColor.copy(alpha = rayAlpha * 0.6f), Color.Transparent),
                center = Offset(w / 2, tubeY),
                radius = w * 0.7f
            ),
            size = size
        )
    }
}

// -----------------------------
// PHYSICS PULL CHAIN
// -----------------------------
@Composable
private fun LampPullChain(isDark: Boolean, onToggleTheme: (Boolean) -> Unit, accentColor: Color) {
    val coroutineScope = rememberCoroutineScope()
    val pullOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var hasToggled by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val currentIsDark by rememberUpdatedState(isDark)
    val currentOnToggle by rememberUpdatedState(onToggleTheme)

    val anchorX = with(density) { (LocalConfiguration.current.screenWidthDp.dp - 44.dp).toPx() }
    val anchorY = with(density) { (-10.dp).toPx() }
    val restLength = with(density) { 130.dp.toPx() }
    val linkColor = Color.White.copy(alpha = 0.35f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(
            linkColor,
            Offset(anchorX, anchorY),
            Offset(anchorX + pullOffset.value.x, anchorY + restLength + pullOffset.value.y),
            2.5.dp.toPx(),
            StrokeCap.Round
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (anchorX + pullOffset.value.x - 24.dp.toPx()).roundToInt(),
                        (anchorY + restLength + pullOffset.value.y - 24.dp.toPx()).roundToInt()
                    )
                }
                .size(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            hasToggled = false
                            coroutineScope.launch { pullOffset.animateTo(Offset.Zero, spring(0.35f, Spring.StiffnessLow)) }
                        },
                        onDragCancel = {
                            hasToggled = false
                            coroutineScope.launch { pullOffset.animateTo(Offset.Zero, spring(0.35f, Spring.StiffnessLow)) }
                        },
                        onDrag = { _, amt ->
                            coroutineScope.launch {
                                val next = pullOffset.value + amt
                                pullOffset.snapTo(next)
                                if (next.getDistance() > 160f && !hasToggled) {
                                    currentOnToggle(!currentIsDark)
                                    hasToggled = true
                                }
                            }
                        }
                    )
                },
            shape = CircleShape,
            color = Color(0xFF151515),
            border = BorderStroke(1.2.dp, linkColor),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}