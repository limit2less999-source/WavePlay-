package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveformScrubber(
    modifier: Modifier = Modifier,
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val validDuration = durationMs.coerceAtLeast(1000L)
    val playbackFraction = (currentPositionMs.toFloat() / validDuration).coerceIn(0f, 1f)
    val displayFraction = if (isDragging) dragFraction else playbackFraction

    val primaryColor = MaterialTheme.colorScheme.primary
    val activeTrackColor = MaterialTheme.colorScheme.primary
    val inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val thumbColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("waveform_scrubber")
            .pointerInput(validDuration) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((frac * validDuration).toLong())
                }
            }
            .pointerInput(validDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek((dragFraction * validDuration).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(54.dp)) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f

            val barCount = 48
            val barSpacing = width / barCount
            val barWidth = (barSpacing * 0.62f).coerceAtLeast(2f)

            for (i in 0 until barCount) {
                val barFraction = i.toFloat() / barCount
                val x = i * barSpacing + (barSpacing - barWidth) / 2f

                // Generate pseudo-waveform heights based on sine harmonics
                val waveFactor1 = sin((i * 0.35f))
                val waveFactor2 = sin((i * 0.85f))
                val normalizedHeight = (0.25f + 0.35f * waveFactor1 * waveFactor1 + 0.4f * waveFactor2 * waveFactor2)
                    .coerceIn(0.15f, 0.95f)

                val barHeight = height * normalizedHeight * (if (isDragging) 0.9f else 0.8f)
                val top = centerY - barHeight / 2f

                val isPlayed = barFraction <= displayFraction
                val color = if (isPlayed) activeTrackColor else inactiveTrackColor

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }

            // Draggable Glowing Scrubber Indicator Thumb
            val thumbX = width * displayFraction
            val thumbRadius = if (isDragging) 11.dp.toPx() else 8.dp.toPx()

            // Outer glow
            drawCircle(
                color = primaryColor.copy(alpha = if (isDragging) 0.45f else 0.25f),
                radius = thumbRadius + 5.dp.toPx(),
                center = Offset(thumbX, centerY)
            )

            // Main thumb circle
            drawCircle(
                color = primaryColor,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )

            // Inner center pin
            drawCircle(
                color = thumbColor,
                radius = thumbRadius * 0.45f,
                center = Offset(thumbX, centerY)
            )
        }
    }
}
