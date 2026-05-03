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
 * HireSphere v5 - Master Home System
 * Visual Identity: Atmospheric Kinetic Lamp (Calibrated 3:5)
 * Persona: [Night/Cyan/Charcoal] vs [Vivid/Gold/Bronze]
 * Engineered by MasterR Labs
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
    isDark: Boolean, // isDark = Cyan/Night | !isDark = Gold/Vibrant
    onToggleTheme: (Boolean) -> Unit,
    onNavigateToAnalyze: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToInterviewHub: () -> Unit,
    onNavigateToGauntlet: () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Icons always white on black background
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // --- LUXURY PERSONA PALETTE ---
    val bgColor = Color(0xFF030303)
    val cardColor = Color(0xFF0C0C0C)

    // Adaptive Material Design:
    // In Gold mode, top is warm champagne, bottom is deep bronze to make the LED line stand out.
    val accentColor = if (isDark) Color(0xFF22D3EE) else Color(0xFFFFD700)
    val headColorTop = if (isDark) Color(0xFF4A5568) else Color(0xFFE6D2A5)
    val headColorBottom = if (isDark) Color(0xFF1A1A1B) else Color(0xFF3E2723)

    val animatedAccentColor by animateColorAsState(targetValue = accentColor, animationSpec = tween(1000))
    val animHeadTop by animateColorAsState(targetValue = headColorTop, animationSpec = tween(1000))
    val animHeadBottom by animateColorAsState(targetValue = headColorBottom, animationSpec = tween(1000))

    val features = remember {
        listOf(
            OrbitalFeature(0, "Analyze Engine", "Intelligent JD Alignment & ATS Verification.", Icons.Filled.AutoAwesome, onNavigateToAnalyze),
            OrbitalFeature(1, "Live Interview", "Voice-adaptive technical simulations.", Icons.Filled.Mic, onNavigateToInterviewHub),
            OrbitalFeature(2, "Resume Builder", "Dynamic Vault-to-PDF generation.", Icons.Default.Edit, onNavigateToCreate),
            OrbitalFeature(3, "Master Vault", "Central decentralized skill repository.", Icons.Filled.Storage, onNavigateToVault),
            OrbitalFeature(4, "2026 Gauntlet", "Start 2026 Recruitment Gauntlet.", Icons.Filled.Security, onNavigateToGauntlet)
        )
    }

    var activeFeatureId by remember { mutableStateOf<Int?>(null) }
    var manualRotateOffset by remember { mutableStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "core_engine")
    val swingRotation by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(4200, easing = SineEaseInOut), RepeatMode.Reverse),
        label = "swing"
    )

    val autoOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(55000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
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
        // --- 1. ATMOSPHERIC FADE GLOW (STATIC DEPTH) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        animatedAccentColor.copy(alpha = if (isDark) 0.08f else 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2, 180.dp.toPx()),
                    radius = size.height * 1.2f
                )
            )
        }

        // --- 2. ADAPTIVE KINETIC LAMP ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .graphicsLayer {
                    rotationZ = swingRotation
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f)
                }
        ) {
            AdaptiveStudioLamp(
                accentColor = animatedAccentColor,
                headTop = animHeadTop,
                headBottom = animHeadBottom,
                isDark = isDark
            )
        }

        // --- 3. FIXED BRANDING (ELEVATED TO 160.dp) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "HireSphere",
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
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
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // --- 4. RADIAL ORBITAL ENGINE (ELEVATED 310.dp) ---
        val orbitRadiusDp = 140.dp
        val density = LocalDensity.current
        val orbitRadiusPx = with(density) { orbitRadiusDp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 310.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        manualRotateOffset += (dragAmount.x / 4.5f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(orbitRadiusDp * 2),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, animatedAccentColor.copy(alpha = 0.08f))
            ) {}

            Box(
                modifier = Modifier.size(80.dp).scale(corePulse),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = CircleShape,
                    color = cardColor,
                    border = BorderStroke(1.5.dp, animatedAccentColor.copy(alpha = 0.5f)),
                    shadowElevation = 12.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SettingsSystemDaydream,
                            contentDescription = null,
                            tint = animatedAccentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Orbital Features with Manual Control
            features.forEachIndexed { index, feature ->
                val isActive = activeFeatureId == feature.id
                val isDimmed = activeFeatureId != null && !isActive

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val baseAngle = (index * (360f / features.size))
                            val currentAngleRad = Math.toRadians((baseAngle + autoOrbitAngle + manualRotateOffset).toDouble())

                            translationX = (cos(currentAngleRad) * orbitRadiusPx).toFloat()
                            translationY = (sin(currentAngleRad) * orbitRadiusPx).toFloat()

                            scaleX = if (isActive) 1.2f else 1f
                            scaleY = if (isActive) 1.2f else 1f
                            alpha = if (isDimmed) 0.5f else 1f
                        }
                        .size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) {
                            activeFeatureId = if (isActive) null else feature.id
                        }
                    ) {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape,
                            color = if (isActive) animatedAccentColor else cardColor,
                            border = BorderStroke(1.2.dp, animatedAccentColor.copy(alpha = 0.4f)),
                            shadowElevation = if (isActive) 12.dp else 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = feature.icon,
                                    contentDescription = null,
                                    tint = if (isActive) Color.Black else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (!isDimmed) {
                            Spacer(modifier = Modifier.height(6.dp))
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

        // --- 5. BOTTOM ACTION INTERFACE ---
        AnimatedVisibility(
            visible = activeFeatureId != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .navigationBarsPadding()
        ) {
            val feature = features.find { it.id == activeFeatureId }
            if (feature != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, animatedAccentColor.copy(alpha = 0.2f)),
                    shadowElevation = 32.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = animatedAccentColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(feature.icon, null, tint = animatedAccentColor, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = feature.title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = feature.description,
                            color = Color.Gray,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Button(
                            onClick = {
                                activeFeatureId = null
                                feature.onClick()
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = animatedAccentColor)
                        ) {
                            Text("INITIALIZE SEQUENCE", color = Color.Black, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // --- 6. THE PHYSICS PULL SWITCH ---
        LampPullChain(isDark = isDark, onToggleTheme = onToggleTheme, accentColor = animatedAccentColor)
    }
}

// -----------------------------
// ADAPTIVE KINETIC LAMP
// -----------------------------
@Composable
private fun AdaptiveStudioLamp(accentColor: Color, headTop: Color, headBottom: Color, isDark: Boolean) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val wireTopY = 0f

        val lampHeadTopY = 80.dp.toPx()
        val headWidthTop = 91.dp.toPx()
        val headWidthBottom = w * 0.48f
        val headHeight = 96.dp.toPx()

        // 1. Hanging Wire
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(centerX, wireTopY),
            end = Offset(centerX, lampHeadTopY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 2. Head (Adaptive Metallic Fading Gradient)
        val headPath = Path().apply {
            moveTo(centerX - headWidthTop / 2, lampHeadTopY)
            lineTo(centerX + headWidthTop / 2, lampHeadTopY)
            lineTo(centerX + headWidthBottom / 2, lampHeadTopY + headHeight)
            lineTo(centerX - headWidthBottom / 2, lampHeadTopY + headHeight)
            close()
        }
        drawPath(
            path = headPath,
            brush = Brush.verticalGradient(
                colors = listOf(headTop, headBottom), // headBottom is much darker now
                startY = lampHeadTopY,
                endY = lampHeadTopY + headHeight
            )
        )

        // 3. MULTI-LAYER LED LINE (Distinguishable Visibility)
        val lineY = lampHeadTopY + headHeight
        // Layer A: Wide Soft Bloom
        drawLine(
            accentColor.copy(alpha = 0.5f),
            Offset(centerX - (headWidthBottom / 2) + 6f, lineY),
            Offset(centerX + (headWidthBottom / 2) - 6f, lineY),
            10.dp.toPx(),
            StrokeCap.Round
        )
        // Layer B: Sharp Neon Accent
        drawLine(
            accentColor,
            Offset(centerX - (headWidthBottom / 2) + 12f, lineY),
            Offset(centerX + (headWidthBottom / 2) - 12f, lineY),
            4.dp.toPx(),
            StrokeCap.Round
        )
        // Layer C: High-Energy White Core (Cuts through the Gold)
        drawLine(
            Color.White.copy(alpha = 0.8f),
            Offset(centerX - (headWidthBottom / 2) + 20f, lineY),
            Offset(centerX + (headWidthBottom / 2) - 20f, lineY),
            1.5.dp.toPx(),
            StrokeCap.Round
        )

        // 4. Parabolic Light Cone
        val beamPath = Path().apply {
            moveTo(centerX - headWidthBottom / 2 + 15f, lineY)
            lineTo(centerX + headWidthBottom / 2 - 15f, lineY)
            lineTo(centerX + w * 0.45f, h * 1.5f)
            lineTo(centerX - w * 0.45f, h * 1.5f)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor.copy(alpha = if (isDark) 0.15f else 0.22f), Color.Transparent),
                startY = lineY,
                endY = h * 1.2f
            )
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
        drawLine(linkColor, Offset(anchorX, anchorY), Offset(anchorX + pullOffset.value.x, anchorY + restLength + pullOffset.value.y), 2.5.dp.toPx(), StrokeCap.Round)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .offset { IntOffset((anchorX + pullOffset.value.x - 24.dp.toPx()).roundToInt(), (anchorY + restLength + pullOffset.value.y - 24.dp.toPx()).roundToInt()) }
                .size(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { hasToggled = false; coroutineScope.launch { pullOffset.animateTo(Offset.Zero, spring(0.35f, Spring.StiffnessLow)) } },
                        onDragCancel = { hasToggled = false; coroutineScope.launch { pullOffset.animateTo(Offset.Zero, spring(0.35f, Spring.StiffnessLow)) } },
                        onDrag = { _, amt ->
                            coroutineScope.launch {
                                val next = pullOffset.value + amt; pullOffset.snapTo(next)
                                if (next.getDistance() > 160f && !hasToggled) { currentOnToggle(!currentIsDark); hasToggled = true }
                            }
                        }
                    )
                },
            shape = CircleShape, color = Color(0xFF151515), border = BorderStroke(1.2.dp, linkColor), shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (isDark) Icons.Default.DarkMode else Icons.Default.Lightbulb, null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}

private val SineEaseInOut = Easing { fraction -> -0.5f * (cos(Math.PI * fraction.toDouble()) - 1f).toFloat() }