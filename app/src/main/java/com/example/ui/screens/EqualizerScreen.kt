package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.MusicPlayerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val enabled by viewModel.equalizerEnabled.collectAsState()
    val bands by viewModel.equalizerBands.collectAsState()
    val activePreset by viewModel.equalizerPreset.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()

    val bandFrequencies = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
    val presets = listOf("Normal", "Bass Booster", "Rock", "Pop", "Classical", "Jazz", "Vocal Booster", "Flat")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Row
        Text(
            text = "Equalizer",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Fine-tune frequencies for high-definition outputs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("eq_toggle_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Master DSP Equalizer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (enabled) "Status: Enabled & Active" else "Status: Bypassed",
                        fontSize = 12.sp,
                        color = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { viewModel.toggleEqualizer(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("eq_master_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Equalizer Spline Plotting Curve
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("eq_spline_chart"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                EqualizerCurveCanvas(bands = bands, enabled = enabled, accentColor = accentColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets Chips
        Text(
            text = "Acoustic Presets",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = activePreset == preset
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.surface
                )
                val chipTextBy by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    onClick = { viewModel.applyPreset(preset) },
                    modifier = Modifier.testTag("eq_preset_$preset"),
                    shape = RoundedCornerShape(14.dp),
                    color = chipBg,
                    contentColor = chipTextBy,
                    tonalElevation = if (isSelected) 4.dp else 0.dp,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                ) {
                    Text(
                        text = preset,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Frequency Sliders
        Text(
            text = "Frequency Band Level Controllers",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        bands.forEachIndexed { idx, gainVal ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("eq_slider_card_$idx"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Band Label
                    Column(modifier = Modifier.width(72.dp)) {
                        Text(
                            text = bandFrequencies[idx],
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) accentColor else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (gainVal > 0) "+${String.format("%.1f", gainVal)} dB" else "${String.format("%.1f", gainVal)} dB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Slider
                    Slider(
                        value = gainVal,
                        onValueChange = { viewModel.updateEqualizerBand(idx, it) },
                        valueRange = -15f..15f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(
                            activeTrackColor = accentColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            thumbColor = accentColor,
                            disabledThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            disabledActiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("eq_band_slider_$idx")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(130.dp))
    }
}

@Composable
fun EqualizerCurveCanvas(
    bands: FloatArray,
    enabled: Boolean,
    accentColor: Color
) {
    val curveColor = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw horizontal grid lines (0dB, +10dB, -10dB)
        drawLine(
            color = gridColor,
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = gridColor.copy(alpha = 0.15f),
            start = Offset(0f, h * 0.15f),
            end = Offset(w, h * 0.15f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = gridColor.copy(alpha = 0.15f),
            start = Offset(0f, h * 0.85f),
            end = Offset(w, h * 0.85f),
            strokeWidth = 1.dp.toPx()
        )

        // Plot the 5 band coordinates
        val points = mutableListOf<Offset>()
        val paddingFactor = 0.12f
        val startX = w * paddingFactor
        val spacing = (w * (1f - 2 * paddingFactor)) / 4f

        for (i in bands.indices) {
            val x = startX + i * spacing
            // Map slider value range [-15, 15] to drawing canvas height [0.85h, 0.15h]
            val normGain = (bands[i] + 15f) / 30f // [0f, 1f]
            val y = h * (0.85f - normGain * 0.7f)
            points.add(Offset(x, y))

            // Draw frequency ticks
            drawCircle(
                color = curveColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Draw a smooth Bezier Spline connecting points
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(0f, h / 2f)
            path.lineTo(points.first().x / 2f, h / 2f)
            path.quadraticTo(
                points.first().x * 0.75f,
                h / 2f,
                points.first().x,
                points.first().y
            )

            // Dynamic interpolation between band nodes
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i+1]
                val controlX = (p1.x + p2.x) / 2f
                path.cubicTo(
                    controlX, p1.y,
                    controlX, p2.y,
                    p2.x, p2.y
                )
            }

            val lastP = points.last()
            path.quadraticTo(
                (lastP.x + w) / 2f,
                lastP.y,
                (lastP.x + w) / 2f,
                h / 2f
            )
            path.lineTo(w, h / 2f)

            // Draw filled gradient area under curve
            val fillPath = Path().apply {
                addPath(path)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(curveColor.copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
