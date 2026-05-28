package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.viewmodel.ColorPresets
import com.example.viewmodel.ThemeMode

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accentColor: Color = ColorPresets.RoyalAmethyst,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.2f),
            onPrimaryContainer = accentColor,
            background = Color(0xFF1B1C24),      // Softer dark background for Neumorphism shadows
            surface = Color(0xFF1B1C24),         // Matching surface for extruded neumorphic shapes
            onBackground = Color(0xFFECECF1),
            onSurface = Color(0xFFECECF1),
            secondary = accentColor.copy(alpha = 0.8f),
            onSecondary = Color.White,
            surfaceVariant = Color(0xFF252631),
            onSurfaceVariant = Color(0xFFC5C5CE),
            outline = Color(0xFF323445)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.12f),
            onPrimaryContainer = accentColor,
            background = Color(0xFFE6EBF2),      // Calibrated soft neumorphic light gray background
            surface = Color(0xFFE6EBF2),         // Matching surface for extruded shapes
            onBackground = Color(0xFF101014),
            onSurface = Color(0xFF101014),
            secondary = accentColor.copy(alpha = 0.8f),
            onSecondary = Color.White,
            surfaceVariant = Color(0xFFD6DBE4),
            onSurfaceVariant = Color(0xFF60606A),
            outline = Color(0xFFBCC2CD)
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

