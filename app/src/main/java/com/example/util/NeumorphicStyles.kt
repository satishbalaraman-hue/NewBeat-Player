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
        Color(0xFF030305).copy(alpha = 0.95f)
    } else {
        Color(0xFFA3B1C6).copy(alpha = 1.0f) // Specified in design.md at 100% opacity
    }
    
    val lightShadow = if (isDark) {
        Color(0xFF383C4D).copy(alpha = 0.85f)
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
            
            // Inner shadow representation with higher density
            drawRoundRect(
                color = darkShadow.copy(alpha = 0.5f),
                topLeft = Offset(3f, 3f),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 8f)
            )
            drawRoundRect(
                color = lightShadow.copy(alpha = 0.6f),
                topLeft = Offset(-1.5f, -1.5f),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 6f)
            )
        } else {
            // Drawn as elevated (raised) Neumorphic surface
            val steps = 6
            for (i in 1..steps) {
                val alphaMultiplier = (steps - i + 1).toFloat() / steps
                val currentOffset = (elevPx * i) / steps
                
                // Dark shadow down and to the right - increased alpha
                drawRoundRect(
                    color = darkShadow.copy(alpha = darkShadow.alpha * 0.32f * alphaMultiplier),
                    topLeft = Offset(currentOffset, currentOffset),
                    size = this.size,
                    cornerRadius = CornerRadius(radiusPx + currentOffset / 2, radiusPx + currentOffset / 2)
                )
                
                // Light shadow up and to the left - increased alpha
                drawRoundRect(
                    color = lightShadow.copy(alpha = lightShadow.alpha * 0.45f * alphaMultiplier),
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

            // Add a beautiful subtle dual highlight stroke (bevel outline)
            drawRoundRect(
                color = lightShadow.copy(alpha = if (isDark) 0.15f else 0.4f),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
                style = Stroke(width = 1.5f)
            )
        }
    }
}
