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
    themeMode: ThemeMode = ThemeMode.DARK,
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
            background = Color(0xFF09090C),      // High-contrast pitch OLED black
            surface = Color(0xFF121217),         // Layered premium dark card surface
            onBackground = Color(0xFFECECF1),
            onSurface = Color(0xFFECECF1),
            secondary = accentColor.copy(alpha = 0.8f),
            onSecondary = Color.White,
            surfaceVariant = Color(0xFF1D1D24),
            onSurfaceVariant = Color(0xFFC5C5CE),
            outline = Color(0xFF2E2E38)
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            onPrimary = Color.White,
            primaryContainer = accentColor.copy(alpha = 0.12f),
            onPrimaryContainer = accentColor,
            background = Color(0xFFF9F9FB),      // Elegant cream off-white
            surface = Color(0xFFFFFFFF),         // Clean paper white cards
            onBackground = Color(0xFF101014),
            onSurface = Color(0xFF101014),
            secondary = accentColor.copy(alpha = 0.8f),
            onSecondary = Color.White,
            surfaceVariant = Color(0xFFF2F2F6),
            onSurfaceVariant = Color(0xFF60606A),
            outline = Color(0xFFDFDFE5)
        )
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

