package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

enum class VisualizerMode(val label: String) {
    SPECTRUM("Spectrum Bars"),
    NEON_WAVE("Oscilloscope"),
    AURA_PULSE("Aura Pulse"),
    VU_METER("VU Meter")
}

@Composable
fun AudioVisualizerView(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    initialMode: VisualizerMode = VisualizerMode.SPECTRUM,
    showControls: Boolean = true
) {
    var mode by remember { mutableStateOf(initialMode) }

    val infiniteTransition = rememberInfiniteTransition(label = "audio_visualizer")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val beatPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beatPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_visualizer_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visualizer Canvas Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                VisualizerMode.SPECTRUM -> {
                    SpectrumBarsCanvas(
                        isPlaying = isPlaying,
                        phase = phase,
                        beat = beatPulse,
                        primaryColor = accentColor,
                        secondaryColor = secondaryColor
                    )
                }
                VisualizerMode.NEON_WAVE -> {
                    SineWaveCanvas(
                        isPlaying = isPlaying,
                        phase = phase,
                        beat = beatPulse,
                        waveColor = accentColor
                    )
                }
                VisualizerMode.AURA_PULSE -> {
                    AuraPulseCanvas(
                        isPlaying = isPlaying,
                        beat = beatPulse,
                        pulseColor = accentColor,
                        centerColor = secondaryColor
                    )
                }
                VisualizerMode.VU_METER -> {
                    VuMeterCanvas(
                        isPlaying = isPlaying,
                        phase = phase,
                        meterColor = accentColor
                    )
                }
            }
        }

        if (showControls) {
            Spacer(modifier = Modifier.height(10.dp))
            // Mode Selector Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VisualizerMode.entries.forEach { item ->
                    val isSelected = mode == item
                    FilterChip(
                        selected = isSelected,
                        onClick = { mode = item },
                        label = { Text(item.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.25f),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SpectrumBarsCanvas(
    isPlaying: Boolean,
    phase: Float,
    beat: Float,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().testTag("canvas_spectrum")) {
        val barCount = 28
        val spacing = 4.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3f)

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount
            val wave = if (isPlaying) {
                val f1 = sin(phase + normalizedIndex * 4 * PI.toFloat())
                val f2 = sin(phase * 1.5f + normalizedIndex * 8 * PI.toFloat())
                val dynamicHeight = (0.25f + 0.35f * (f1 + 1f) / 2f + 0.4f * (f2 + 1f) / 2f) * beat
                dynamicHeight.coerceIn(0.1f, 1f)
            } else {
                0.08f
            }

            val barHeight = size.height * 0.85f * wave
            val x = i * (barWidth + spacing)
            val y = size.height - barHeight

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor)
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Top peak dot
            if (isPlaying && barHeight > 10f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = (barWidth / 2f).coerceAtMost(3.dp.toPx()),
                    center = Offset(x + barWidth / 2f, y - 4.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun SineWaveCanvas(
    isPlaying: Boolean,
    phase: Float,
    beat: Float,
    waveColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().testTag("canvas_sinewave")) {
        val midY = size.height / 2f
        val path = Path()
        val path2 = Path()

        val steps = 100
        val stepX = size.width / steps

        for (i in 0..steps) {
            val x = i * stepX
            val progress = i.toFloat() / steps
            val amp = if (isPlaying) (size.height * 0.35f * beat * sin(progress * PI.toFloat())) else (size.height * 0.05f)

            val y1 = midY + amp * sin(phase * 1.8f + progress * 6 * PI.toFloat())
            val y2 = midY + (amp * 0.7f) * sin(-phase * 1.2f + progress * 4 * PI.toFloat())

            if (i == 0) {
                path.moveTo(x, y1)
                path2.moveTo(x, y2)
            } else {
                path.lineTo(x, y1)
                path2.lineTo(x, y2)
            }
        }

        // Secondary subtle wave
        drawPath(
            path = path2,
            color = waveColor.copy(alpha = 0.35f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Main bright wave
        drawPath(
            path = path,
            color = waveColor,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AuraPulseCanvas(
    isPlaying: Boolean,
    beat: Float,
    pulseColor: Color,
    centerColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().testTag("canvas_aura")) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = (size.height.coerceAtMost(size.width) / 2f) * 0.9f

        val rings = 4
        for (r in rings downTo 1) {
            val ringProgress = r.toFloat() / rings
            val radius = maxRadius * ringProgress * if (isPlaying) beat else 0.5f
            val alpha = (1f - ringProgress * 0.7f) * if (isPlaying) 0.5f else 0.2f

            drawCircle(
                color = pulseColor.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Glowing center core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, centerColor, Color.Transparent),
                center = center,
                radius = 28.dp.toPx() * if (isPlaying) beat else 0.8f
            ),
            radius = 28.dp.toPx() * if (isPlaying) beat else 0.8f,
            center = center
        )
    }
}

@Composable
private fun VuMeterCanvas(
    isPlaying: Boolean,
    phase: Float,
    meterColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().testTag("canvas_vumeter")) {
        val channelHeight = (size.height - 24.dp.toPx()) / 2f
        val totalLeds = 20
        val ledSpacing = 4.dp.toPx()
        val totalLedSpacing = ledSpacing * (totalLeds - 1)
        val ledWidth = (size.width - totalLedSpacing) / totalLeds

        val levelL = if (isPlaying) (0.4f + 0.55f * sin(phase * 1.5f).coerceAtLeast(0f)) else 0.05f
        val levelR = if (isPlaying) (0.35f + 0.6f * sin(phase * 1.8f + 1f).coerceAtLeast(0f)) else 0.05f

        listOf(Pair("L", levelL), Pair("R", levelR)).forEachIndexed { chIdx, (_, level) ->
            val topY = if (chIdx == 0) 10.dp.toPx() else 10.dp.toPx() + channelHeight + 8.dp.toPx()
            val litCount = (totalLeds * level).toInt()

            for (led in 0 until totalLeds) {
                val isLit = led <= litCount
                val ledColor = when {
                    led > totalLeds * 0.8f -> Color(0xFFEF4444) // Red peak
                    led > totalLeds * 0.55f -> Color(0xFFF59E0B) // Amber
                    else -> Color(0xFF10B981) // Green
                }

                val x = led * (ledWidth + ledSpacing)
                drawRoundRect(
                    color = if (isLit) ledColor else ledColor.copy(alpha = 0.15f),
                    topLeft = Offset(x, topY),
                    size = Size(ledWidth, channelHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
    }
}
