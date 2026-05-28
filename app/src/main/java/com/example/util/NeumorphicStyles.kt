package com.example.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A beautiful, authentic Neumorphic modifier that draws realistic soft bevels.
 * Works perfectly for both Raised (convex) and Pressed (concave) states in both dark and light modes.
 */
@Composable
fun Modifier.neumorphic(
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 4.dp,
    isPressed: Boolean = false,
    accentColor: Color? = null
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // Choose highly calibrated neumorphic shadow colors for perfect contrast
    val darkShadow = if (isDark) {
        Color(0xFF090A0E).copy(alpha = 0.8f)
    } else {
        Color(0xFFA3B1C6).copy(alpha = 0.6f)
    }
    
    val lightShadow = if (isDark) {
        Color(0xFF2E3240).copy(alpha = 0.5f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 1.0f)
    }

    val baseColor = MaterialTheme.colorScheme.surface

    return this.drawBehind {
        val radiusPx = cornerRadius.toPx()
        val elevPx = elevation.toPx()
        
        if (isPressed) {
            // Drawn as pressed (sunken) Neumorphic surface
            drawRoundRect(
                color = accentColor ?: baseColor.copy(alpha = 0.9f),
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
            
            // Inner shadow representation
            drawRoundRect(
                color = darkShadow.copy(alpha = 0.25f),
                topLeft = Offset(2f, 2f),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 5f)
            )
            drawRoundRect(
                color = lightShadow.copy(alpha = 0.35f),
                topLeft = Offset(-1f, -1f),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 4f)
            )
        } else {
            // Drawn as elevated (raised) Neumorphic surface
            val steps = 5
            for (i in 1..steps) {
                val alphaMultiplier = (steps - i + 1).toFloat() / steps
                val currentOffset = (elevPx * i) / steps
                
                // Dark shadow down and to the right
                drawRoundRect(
                    color = darkShadow.copy(alpha = darkShadow.alpha * 0.18f * alphaMultiplier),
                    topLeft = Offset(currentOffset, currentOffset),
                    size = this.size,
                    cornerRadius = CornerRadius(radiusPx + currentOffset / 2, radiusPx + currentOffset / 2)
                )
                
                // Light shadow up and to the left
                drawRoundRect(
                    color = lightShadow.copy(alpha = lightShadow.alpha * 0.25f * alphaMultiplier),
                    topLeft = Offset(-currentOffset, -currentOffset),
                    size = this.size,
                    cornerRadius = CornerRadius(radiusPx + currentOffset / 2, radiusPx + currentOffset / 2)
                )
            }
            
            // Draw original surface shape
            drawRoundRect(
                color = accentColor ?: baseColor,
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        }
    }
}
