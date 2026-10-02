package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DoubleTapVideoGestureOverlay(
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onSingleTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    var showLeftIndicator by remember { mutableStateOf(false) }
    var showRightIndicator by remember { mutableStateOf(false) }

    // Brightness state (0.0f .. 1.0f)
    var brightnessLevel by remember { mutableFloatStateOf(0.7f) }
    var showBrightnessHud by remember { mutableStateOf(false) }

    // Volume state (0.0f .. 1.0f)
    var volumeLevel by remember {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf(current.toFloat() / max)
    }
    var showVolumeHud by remember { mutableStateOf(false) }

    LaunchedEffect(showLeftIndicator) {
        if (showLeftIndicator) {
            delay(650)
            showLeftIndicator = false
        }
    }

    LaunchedEffect(showRightIndicator) {
        if (showRightIndicator) {
            delay(650)
            showRightIndicator = false
        }
    }

    LaunchedEffect(showBrightnessHud) {
        if (showBrightnessHud) {
            delay(1200)
            showBrightnessHud = false
        }
    }

    LaunchedEffect(showVolumeHud) {
        if (showVolumeHud) {
            delay(1200)
            showVolumeHud = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (!isLocked) {
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT 50%: Tap, Double Tap to Rewind, Vertical Drag for Brightness
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("video_left_gesture_area")
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onSingleTap() },
                                onDoubleTap = {
                                    showLeftIndicator = true
                                    onDoubleTapLeft()
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { showBrightnessHud = true },
                                onDragEnd = { /* auto-dismissed via LaunchedEffect */ },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // Vertical drag: negative dragAmount.y = swipe UP = increase brightness
                                    val delta = -dragAmount.y / 600f
                                    brightnessLevel = (brightnessLevel + delta).coerceIn(0.05f, 1.0f)
                                    showBrightnessHud = true

                                    // Apply to Android Window
                                    (context as? Activity)?.window?.let { window ->
                                        val lp = window.attributes
                                        lp.screenBrightness = brightnessLevel
                                        window.attributes = lp
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Double Tap Left Badge
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showLeftIndicator,
                        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f),
                        exit = fadeOut(tween(250)) + scaleOut(targetScale = 1.1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "-10s",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // RIGHT 50%: Tap, Double Tap to Fast Forward, Vertical Drag for Volume
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("video_right_gesture_area")
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onSingleTap() },
                                onDoubleTap = {
                                    showRightIndicator = true
                                    onDoubleTapRight()
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { showVolumeHud = true },
                                onDragEnd = { /* auto-dismissed via LaunchedEffect */ },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // Vertical drag: negative dragAmount.y = swipe UP = increase volume
                                    val delta = -dragAmount.y / 600f
                                    volumeLevel = (volumeLevel + delta).coerceIn(0f, 1.0f)
                                    showVolumeHud = true

                                    // Apply to Android AudioManager
                                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val targetVol = (volumeLevel * max).toInt().coerceIn(0, max)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Double Tap Right Badge
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showRightIndicator,
                        enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f),
                        exit = fadeOut(tween(250)) + scaleOut(targetScale = 1.1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "+10s",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // HUD OVERLAY: Left Side Brightness HUD Indicator
            AnimatedVisibility(
                visible = showBrightnessHud,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (brightnessLevel > 0.5f) Icons.Default.BrightnessHigh else Icons.Default.BrightnessMedium,
                            contentDescription = "Brightness",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .height(80.dp)
                                .width(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(brightnessLevel)
                                    .background(Color(0xFFFBBF24))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(brightnessLevel * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // HUD OVERLAY: Right Side Volume HUD Indicator
            AnimatedVisibility(
                visible = showVolumeHud,
                enter = fadeIn() + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + scaleOut(targetScale = 0.9f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (volumeLevel > 0.05f) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Volume",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .height(80.dp)
                                .width(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(volumeLevel)
                                    .background(Color(0xFF38BDF8))
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(volumeLevel * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // When locked, tapping anywhere triggers single tap (which can show the unlock button)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onSingleTap() })
                    }
            )
        }
    }
}
