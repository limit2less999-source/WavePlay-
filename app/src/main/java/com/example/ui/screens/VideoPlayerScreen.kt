package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.local.TrackEntity
import com.example.player.PlaybackManager
import com.example.player.PlaybackState
import com.example.ui.components.DoubleTapVideoGestureOverlay
import com.example.util.TimeUtils
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    video: TrackEntity,
    playbackManager: PlaybackManager,
    playbackState: PlaybackState,
    isInPipMode: Boolean = false,
    onClose: () -> Unit
) {
    BackHandler {
        onClose()
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var areControlsVisible by remember { mutableStateOf(true) }
    var isControlsLocked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    // Auto-hide controls after 4 seconds of inactivity if playing and not locked
    LaunchedEffect(areControlsVisible, playbackState.isPlaying, isControlsLocked) {
        if (areControlsVisible && playbackState.isPlaying && !isControlsLocked) {
            delay(4000)
            areControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_fullscreen")
    ) {
        // Double-tap & swipe gesture wrapper with Media3 PlayerView inside
        DoubleTapVideoGestureOverlay(
            modifier = Modifier.fillMaxSize(),
            isLocked = isControlsLocked,
            onSingleTap = {
                if (!isControlsLocked) {
                    areControlsVisible = !areControlsVisible
                } else {
                    areControlsVisible = true
                }
            },
            onDoubleTapLeft = {
                // Double tap left: rewind 10s
                playbackManager.seekBy(-10_000L)
            },
            onDoubleTapRight = {
                // Double tap right: forward 10s
                playbackManager.seekBy(10_000L)
            }
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playbackManager.exoPlayer
                        useController = false // Use our custom Jetpack Compose controls
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.resizeMode = resizeMode
                    }
                },
                update = { playerView ->
                    playerView.player = playbackManager.exoPlayer
                    playerView.resizeMode = resizeMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering Indicator
        if (playbackState.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }

        // Overlay Controls (Animated Visibility)
        AnimatedVisibility(
            visible = areControlsVisible && !isControlsLocked && !isInPipMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp).testTag("video_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Picture-in-Picture Button
                    IconButton(
                        onClick = {
                            val activity = context as? android.app.Activity
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && activity != null) {
                                val pipParams = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(16, 9))
                                    .build()
                                activity.enterPictureInPictureMode(pipParams)
                            }
                        },
                        modifier = Modifier.size(48.dp).testTag("video_pip_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Picture in Picture",
                            tint = Color.White
                        )
                    }

                    // Aspect Ratio Button
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White
                        )
                    }

                    // Speed Selector Menu
                    Box {
                        IconButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                DropdownMenuItem(
                                    text = { Text("${spd}x") },
                                    onClick = {
                                        playbackManager.setPlaybackSpeed(spd)
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Lock Button
                    IconButton(
                        onClick = { isControlsLocked = true },
                        modifier = Modifier.size(48.dp).testTag("video_lock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Lock Controls",
                            tint = Color.White
                        )
                    }
                }

                // Center Play/Pause & Quick Skip Buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s Button
                    IconButton(
                        onClick = { playbackManager.seekBy(-10_000L) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("video_rewind_10_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause Big Center Button
                    FilledIconButton(
                        onClick = { playbackManager.togglePlayPause() },
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("video_play_pause_center"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Fast-Forward 10s Button
                    IconButton(
                        onClick = { playbackManager.seekBy(10_000L) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("video_forward_10_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10 seconds",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Timeline Scrubber & Times
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    val duration = playbackState.durationMs.coerceAtLeast(1000L)
                    val pos = playbackState.currentPositionMs.coerceIn(0L, duration)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = TimeUtils.formatMs(pos),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = TimeUtils.formatMs(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Slider(
                        value = pos.toFloat(),
                        onValueChange = { playbackManager.seekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_timeline_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // When locked: Small Unlock Icon Button in top left corner
        if (isControlsLocked) {
            IconButton(
                onClick = { isControlsLocked = false },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .testTag("video_unlock_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Unlock Controls",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
