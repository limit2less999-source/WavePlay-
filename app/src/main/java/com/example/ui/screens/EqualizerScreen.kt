package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.EqualizerBand
import com.example.player.EqualizerPreset
import com.example.player.EqualizerSettings

@Composable
fun EqualizerScreen(
    modifier: Modifier = Modifier,
    settings: EqualizerSettings,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onBandLevelChange: (bandIndex: Int, levelMilliBels: Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onApplyPreset: (EqualizerPreset) -> Unit
) {
    BackHandler {
        onBack()
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val bands = settings.bands

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("equalizer_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Back Arrow, Title, Master Power Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("equalizer_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Equalizer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (settings.isEnabled) "Hardware DSP Active" else "Bypassed",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.isEnabled) Color(0xFF38BDF8) else Color(0xFF64748B)
                    )
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { onToggleEnabled(it) },
                    modifier = Modifier.testTag("equalizer_master_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Studio Frequency Response Curve with Gradient Fill
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF131D2E)
                )
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("equalizer_curve_canvas")
                ) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f

                    // Draw reference grid lines: +12dB, 0dB, -12dB
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, h * 0.15f),
                        end = Offset(w, h * 0.15f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(0f, centerY),
                        end = Offset(w, centerY),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, h * 0.85f),
                        end = Offset(w, h * 0.85f),
                        strokeWidth = 1.dp.toPx()
                    )

                    if (bands.isNotEmpty()) {
                        val path = Path()
                        val fillPath = Path()
                        val step = if (bands.size > 1) w / (bands.size - 1) else w

                        fillPath.moveTo(0f, h)

                        bands.forEachIndexed { i, band ->
                            val range = (band.maxLevelMilliBels - band.minLevelMilliBels).toFloat().coerceAtLeast(1f)
                            val normalized = if (settings.isEnabled) {
                                (band.currentLevelMilliBels - band.minLevelMilliBels) / range
                            } else {
                                0.5f // flat 0 dB when disabled
                            }
                            val y = h - (normalized * h)
                            val x = i * step

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (i - 1) * step
                                val prevRange = (bands[i - 1].maxLevelMilliBels - bands[i - 1].minLevelMilliBels).toFloat().coerceAtLeast(1f)
                                val prevNorm = if (settings.isEnabled) {
                                    (bands[i - 1].currentLevelMilliBels - bands[i - 1].minLevelMilliBels) / prevRange
                                } else 0.5f
                                val prevY = h - (prevNorm * h)
                                val controlX = (prevX + x) / 2f
                                path.cubicTo(controlX, prevY, controlX, y, x, y)
                                fillPath.cubicTo(controlX, prevY, controlX, y, x, y)
                            }

                            // Glowing band node point
                            drawCircle(
                                color = if (settings.isEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                                radius = 4.5.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }

                        fillPath.lineTo(w, h)
                        fillPath.close()

                        // Gradient Area Fill under the curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = if (settings.isEnabled) 0.35f else 0.05f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Main Glowing Spline
                        drawPath(
                            path = path,
                            color = if (settings.isEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets Bar (with Custom and Reset)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRESET SOUND PROFILES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                TextButton(
                    onClick = {
                        val flatPreset = EqualizerSettings.DEFAULT_PRESETS.firstOrNull { it.name == "Flat" }
                            ?: EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0))
                        onApplyPreset(flatPreset)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Flat", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Presets Horizontal Slider
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "Custom" chip
                item {
                    val isCustom = settings.activePresetName == "Custom"
                    FilterChip(
                        selected = isCustom,
                        onClick = {
                            // User clicked custom; keeps current faders
                        },
                        label = { Text("Custom", fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7C3AED),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }

                items(EqualizerSettings.DEFAULT_PRESETS) { preset ->
                    val isSelected = settings.activePresetName == preset.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { onApplyPreset(preset) },
                        label = { Text(preset.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Studio Vertical Graphic Faders (Option to adjust custom Equalizer!)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUSTOM GRAPHIC EQUALIZER",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Slide any fader to customize",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bands.forEach { band ->
                        StudioVerticalFader(
                            band = band,
                            isEnabled = settings.isEnabled,
                            onLevelChanged = { newMilliBels ->
                                onBandLevelChange(band.index, newMilliBels)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dual Studio Dials: Bass Boost & 3D Spatial Virtualizer
            Text(
                text = "DSP AUDIO ENHANCEMENTS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bass Boost",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${settings.bassBoostStrength / 10}%",
                            color = Color(0xFF38BDF8),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = settings.bassBoostStrength.toFloat(),
                            onValueChange = { onBassBoostChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = settings.isEnabled,
                            modifier = Modifier.testTag("bass_boost_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF0284C7),
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }

                // 3D Spatial Virtualizer Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SurroundSound,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3D Virtualizer",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${settings.virtualizerStrength / 10}%",
                            color = Color(0xFFA78BFA),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Slider(
                            value = settings.virtualizerStrength.toFloat(),
                            onValueChange = { onVirtualizerChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            enabled = settings.isEnabled,
                            modifier = Modifier.testTag("virtualizer_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFA78BFA),
                                activeTrackColor = Color(0xFF7C3AED),
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Modern tactile vertical studio fader with real-time dB text and frequency readout.
 */
@Composable
private fun StudioVerticalFader(
    band: EqualizerBand,
    isEnabled: Boolean,
    onLevelChanged: (Int) -> Unit
) {
    val totalRange = (band.maxLevelMilliBels - band.minLevelMilliBels).toFloat().coerceAtLeast(1f)
    val currentNorm = if (isEnabled) {
        ((band.currentLevelMilliBels - band.minLevelMilliBels) / totalRange).coerceIn(0f, 1f)
    } else 0.5f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(58.dp)
    ) {
        // Gain readout (e.g. +4 dB, 0 dB, -2 dB)
        val dbVal = band.currentLevelMilliBels / 100
        val dbColor = when {
            !isEnabled -> Color(0xFF64748B)
            dbVal > 0 -> Color(0xFF38BDF8)
            dbVal < 0 -> Color(0xFFF472B6)
            else -> Color(0xFF94A3B8)
        }

        Text(
            text = if (dbVal > 0) "+$dbVal" else "$dbVal",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = dbColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Vertical Track Canvas with tactile thumb
        Box(
            modifier = Modifier
                .height(150.dp)
                .width(28.dp)
                .testTag("eq_vertical_fader_${band.index}")
                .pointerInput(isEnabled) {
                    if (!isEnabled) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging UP decreases Y, so it increases gain
                        val delta = -dragAmount.y / 150f
                        val newNorm = (currentNorm + delta).coerceIn(0f, 1f)
                        val newMilliBels = (band.minLevelMilliBels + (newNorm * totalRange)).toInt()
                        onLevelChanged(newMilliBels)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val trackWidth = 6.dp.toPx()
                val centerX = w / 2f
                val centerY = h / 2f

                // Track Slot (background)
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(centerX - trackWidth / 2f, 0f),
                    size = Size(trackWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth / 2f)
                )

                // Zero Center Notch Line
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(centerX - 8.dp.toPx(), centerY),
                    end = Offset(centerX + 8.dp.toPx(), centerY),
                    strokeWidth = 2.dp.toPx()
                )

                // Active level bar from center to current position
                val thumbY = h - (currentNorm * h)
                val activeStart = if (thumbY <= centerY) thumbY else centerY
                val activeHeight = kotlin.math.abs(thumbY - centerY)

                if (isEnabled) {
                    drawRoundRect(
                        color = if (thumbY <= centerY) Color(0xFF38BDF8) else Color(0xFFF472B6),
                        topLeft = Offset(centerX - trackWidth / 2f, activeStart),
                        size = Size(trackWidth, activeHeight.coerceAtLeast(1f)),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackWidth / 2f)
                    )
                }

                // Tactile Fader Thumb
                val thumbHeight = 18.dp.toPx()
                val thumbWidth = 22.dp.toPx()
                drawRoundRect(
                    color = if (isEnabled) Color.White else Color(0xFF64748B),
                    topLeft = Offset(centerX - thumbWidth / 2f, thumbY - thumbHeight / 2f),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                )

                // Thumb Center Ridge
                drawLine(
                    color = Color.Black.copy(alpha = 0.5f),
                    start = Offset(centerX - 6.dp.toPx(), thumbY),
                    end = Offset(centerX + 6.dp.toPx(), thumbY),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Frequency Label (e.g. 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
        Text(
            text = band.displayFreq,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF94A3B8)
        )
    }
}
