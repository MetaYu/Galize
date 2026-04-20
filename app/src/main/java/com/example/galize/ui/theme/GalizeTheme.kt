package com.example.galize.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Cyberpunk Purple palette
val CyberPurple = Color(0xFF7B2FFF)
val CyberPurpleLight = Color(0xFFB388FF)
val CyberPink = Color(0xFFFF4081)
val CyberDark = Color(0xFF1A1025)
val CyberSurface = Color(0xFF2D1B47)

// Choice colors
val PureHeartGreen = Color(0xFF4CAF50)
val ChaosRed = Color(0xFFFF1744)
val PhilosopherPurple = Color(0xFF9C27B0)

private val DarkColorScheme = darkColorScheme(
    primary = CyberPurple,
    secondary = CyberPink,
    tertiary = CyberPurpleLight,
    background = CyberDark,
    surface = CyberSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = CyberPurple,
    secondary = CyberPink,
    tertiary = CyberPurpleLight,
)

@Composable
fun GalizeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
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
        content = content
    )
}
