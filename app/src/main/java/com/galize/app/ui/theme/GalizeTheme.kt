package com.galize.app.ui.theme

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Aurora palette ──────────────────────────────────────────
val AuroraPurple = Color(0xFF8B5CF6)
val AuroraViolet = Color(0xFFA78BFA)
val AuroraCyan = Color(0xFF22D3EE)
val AuroraPink = Color(0xFFF472B6)
val AuroraGreen = Color(0xFF34D399)

// Background layers
val NightSky = Color(0xFF0F0B1E)
val NightSurface = Color(0xFF1A1333)
val NightElevated = Color(0xFF251D42)

// Choice colors (semantic)
val PureHeartGreen = Color(0xFF34D399)
val ChaosRed = Color(0xFFFB7185)
val PhilosopherPurple = Color(0xFFC084FC)

// Legacy aliases for backward compatibility
val CyberPurple = AuroraPurple
val CyberPurpleLight = AuroraViolet
val CyberPink = AuroraPink
val CyberDark = NightSky
val CyberSurface = NightSurface

// ── Color Scheme ────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = AuroraPurple,
    onPrimary = Color.White,
    primaryContainer = NightElevated,
    onPrimaryContainer = AuroraViolet,
    secondary = AuroraCyan,
    onSecondary = NightSky,
    secondaryContainer = Color(0xFF0E3A3F),
    onSecondaryContainer = AuroraCyan,
    tertiary = AuroraPink,
    onTertiary = NightSky,
    tertiaryContainer = Color(0xFF3D1A2E),
    onTertiaryContainer = AuroraPink,
    background = NightSky,
    onBackground = Color(0xFFE8E0F0),
    surface = NightSurface,
    onSurface = Color(0xFFE8E0F0),
    surfaceVariant = NightElevated,
    onSurfaceVariant = Color(0xFFBDB3CC),
    outline = Color(0xFF4A3D66),
    outlineVariant = Color(0xFF332952),
)

private val LightColorScheme = lightColorScheme(
    primary = AuroraPurple,
    onPrimary = Color.White,
    secondary = AuroraCyan,
    tertiary = AuroraPink,
    background = Color(0xFFF8F5FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF0EBFA),
    onSurfaceVariant = Color(0xFF4A3D66),
)

// ── Typography ──────────────────────────────────────────────
val GalizeTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ── Shapes ──────────────────────────────────────────────────
val GalizeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ── Gradient Utilities ──────────────────────────────────────

/** Standard aurora linear gradient (purple → cyan → pink). */
fun auroraBrush(
    start: Offset = Offset.Zero,
    end: Offset = Offset.Infinite,
): Brush = Brush.linearGradient(
    colors = listOf(AuroraPurple, AuroraCyan, AuroraPink),
    start = start,
    end = end,
)

/** Horizontal aurora gradient — handy for text or bars. */
fun auroraHorizontalBrush(): Brush = Brush.horizontalGradient(
    colors = listOf(AuroraPurple, AuroraCyan, AuroraPink),
)

/** Sweep (circular) aurora gradient — good for borders/bubbles. */
fun auroraSweepBrush(center: Offset = Offset.Unspecified): Brush = Brush.sweepGradient(
    colors = listOf(AuroraPurple, AuroraCyan, AuroraPink, AuroraPurple),
    center = center,
)

/** Glass-card style modifier: semi-transparent elevated surface + subtle border. */
fun Modifier.glassCard(): Modifier = this
    .background(
        color = NightElevated.copy(alpha = 0.55f),
        shape = GalizeShapes.medium,
    )
    .drawBehind {
        // subtle inner glow at top edge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    AuroraPurple.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.35f,
            )
        )
    }

// ── Theme Composable ────────────────────────────────────────

@Composable
fun GalizeTheme(
    darkTheme: Boolean = true, // default dark
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GalizeTypography,
        shapes = GalizeShapes,
        content = content,
    )
}
