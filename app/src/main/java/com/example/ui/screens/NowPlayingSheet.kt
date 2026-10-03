package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.player.PlaybackState
import com.example.ui.components.AudioVisualizerView
import com.example.ui.components.VisualizerMode
import com.example.ui.theme.AppThemeMode
import com.example.util.TimeUtils
import java.io.File

@Composable
fun NowPlayingSheet(
    modifier: Modifier = Modifier,
    playbackState: PlaybackState,
    playlists: List<PlaylistEntity>,
    sleepTimerSeconds: Long,
    currentTheme: AppThemeMode = AppThemeMode.DEEP_VELVET,
    onThemeChange: (AppThemeMode) -> Unit = {},
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onAddToPlaylist: (playlistId: Long) -> Unit,
    onDeleteTrack: () -> Unit,
    onRenameTrack: (newTitle: String, newCoverColor: Long) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenQueue: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    onTogglePlayTogether: () -> Unit = {},
    isPlayTogether: Boolean = false
) {
    BackHandler {
        onCollapse()
    }

    val track = playbackState.currentTrack ?: return
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showRenameCoverDialog by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var isVisualizerActive by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("now_playing_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top collapse indicator & visualizer toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(48.dp).testTag("now_playing_collapse")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Sleep timer badge if active
                if (sleepTimerSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFF472B6),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = TimeUtils.formatMs(sleepTimerSeconds * 1000),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Toggle Audio Visualizer Mode Button
                IconButton(
                    onClick = { isVisualizerActive = !isVisualizerActive },
                    modifier = Modifier.size(48.dp).testTag("toggle_visualizer_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Visualizer",
                        tint = if (isVisualizerActive) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Center Display: Large squircle artwork OR Real-Time Audio Visualizer (Feature 7)
            AnimatedContent(
                targetState = isVisualizerActive,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "visualizer_swap"
            ) { visualizerOn ->
                if (visualizerOn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF13101C))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioVisualizerView(
                            isPlaying = playbackState.isPlaying,
                            accentColor = Color(track.coverColorHex),
                            secondaryColor = Color(0xFF38BDF8),
                            initialMode = VisualizerMode.SPECTRUM,
                            showControls = true
                        )
                    }
                } else {
                    // Large squircle album art / music container (as shown in reference image 2)
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(Color(0xFF2E1F34)), // Dark squircle container
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle watermark branding like "Lark" in reference image
                        Text(
                            text = "WavePlay",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.05f),
                            letterSpacing = 2.sp
                        )

                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(track.coverColorHex).copy(alpha = 0.55f),
                            modifier = Modifier.size(140.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Track Title (bold, modern typography)
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Heart & Three Dots Row (matching reference image 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heart / Favorite Icon
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(48.dp).testTag("now_playing_fav_btn")
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Three Dots Menu Icon
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(48.dp).testTag("now_playing_more_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Three dots dropdown menu: Delete, Sleep timer, Add to playlist, Rename & cover, Share, Change Theme
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        // 1. Delete
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )

                        // 2. Sleep timer
                        DropdownMenuItem(
                            text = { Text("Sleep timer") },
                            leadingIcon = {
                                Icon(Icons.Default.Alarm, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                showSleepTimerDialog = true
                            }
                        )

                        // 3. Add to playlist
                        DropdownMenuItem(
                            text = { Text("Add to playlist") },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                showAddToPlaylistDialog = true
                            }
                        )

                        // 4. Rename & cover
                        DropdownMenuItem(
                            text = { Text("Rename & cover") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                showRenameCoverDialog = true
                            }
                        )

                        // 5. Share
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                shareMedia(context, track)
                            }
                        )

                        // 6. Change Theme
                        DropdownMenuItem(
                            text = { Text("Change Theme") },
                            leadingIcon = {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showMenu = false
                                showThemeDialog = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ultra-Sleek Modern Audio Timeline & High-Precision Scrubber
            var localSeekingPosition by remember { mutableStateOf<Float?>(null) }
            val duration = playbackState.durationMs.coerceAtLeast(1000L)
            val position = playbackState.currentPositionMs.coerceIn(0L, duration)
            val currentSliderVal = localSeekingPosition ?: position.toFloat()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Slider(
                    value = currentSliderVal,
                    onValueChange = { localSeekingPosition = it },
                    onValueChangeFinished = {
                        localSeekingPosition?.let { onSeekTo(it.toLong()) }
                        localSeekingPosition = null
                    },
                    valueRange = 0f..duration.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .testTag("now_playing_seekbar"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimeUtils.formatMs((localSeekingPosition?.toLong() ?: position)),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Remaining time
                    val remainingMs = (duration - (localSeekingPosition?.toLong() ?: position)).coerceAtLeast(0L)
                    Text(
                        text = "-${TimeUtils.formatMs(remainingMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Playback Controls: Shuffle | Previous | Play/Pause | Next | Repeat Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button with visual active indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.size(48.dp).testTag("now_playing_shuffle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = if (playbackState.isShuffle) "SHUFFLE" else "OFF",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (playbackState.isShuffle) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                }

                // Previous button
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(54.dp).testTag("now_playing_prev")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Center Play/Pause button with glowing circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onTogglePlay() }
                        .testTag("now_playing_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Next button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(54.dp).testTag("now_playing_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Repeat Mode Button (Cycle OFF -> ALL -> ONE)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onCycleRepeatMode,
                        modifier = Modifier.size(48.dp).testTag("now_playing_repeat")
                    ) {
                        val repeatIcon = if (playbackState.repeatMode == com.example.player.RepeatMode.ONE) {
                            Icons.Default.RepeatOne
                        } else {
                            Icons.Default.Repeat
                        }
                        val repeatTint = if (playbackState.repeatMode != com.example.player.RepeatMode.OFF) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = "Repeat Mode",
                            tint = repeatTint,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    val loopText = when (playbackState.repeatMode) {
                        com.example.player.RepeatMode.ONE -> "ONE"
                        com.example.player.RepeatMode.ALL -> "LOOP"
                        com.example.player.RepeatMode.OFF -> ""
                    }
                    Text(
                        text = loopText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom action row: Equalizer on left, "Lyrics" pill in center, Queue on right (reference image 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Equalizer / Audio FX icon
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier.size(48.dp).testTag("now_playing_equalizer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Center "Lyrics" Pill button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { showLyricsDialog = true }
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                        .testTag("now_playing_lyrics_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lyrics",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Queue / Playlist icon on right
                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier.size(48.dp).testTag("now_playing_queue")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Playlists",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- 1. Delete Confirmation Dialog ---
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Track") },
            text = { Text("Are you sure you want to permanently delete \"${track.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteTrack()
                        onCollapse()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- 2. Sleep Timer Dialog ---
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    val timerOptions = listOf(15, 30, 45, 60, 90)
                    timerOptions.forEach { minutes ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    onStartSleepTimer(minutes)
                                    showSleepTimerDialog = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "$minutes minutes",
                                modifier = Modifier.padding(14.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (sleepTimerSeconds > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onCancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn Off Timer")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- 3. Add to Playlist Dialog ---
    if (showAddToPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists yet. Go to Playlists tab to create one.")
                } else {
                    Column {
                        playlists.forEach { pl ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onAddToPlaylist(pl.id)
                                        showAddToPlaylistDialog = false
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = pl.name,
                                    modifier = Modifier.padding(14.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- 4. Rename & Cover Dialog ---
    if (showRenameCoverDialog) {
        var editTitle by remember { mutableStateOf(track.title) }
        var selectedCoverColor by remember { mutableLongStateOf(track.coverColorHex) }

        val coverColors = listOf(
            0xFF6366F1, // Indigo
            0xFFA855F7, // Purple
            0xFFEC4899, // Pink
            0xFFF43F5E, // Rose
            0xFFFB923C, // Orange
            0xFF10B981, // Emerald
            0xFF06B6D4  // Cyan
        )

        AlertDialog(
            onDismissRequest = { showRenameCoverDialog = false },
            title = { Text("Rename & Cover", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Track Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Cover Accent Color:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        coverColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .clickable { selectedCoverColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedCoverColor == hex) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            onRenameTrack(editTitle.trim(), selectedCoverColor)
                            showRenameCoverDialog = false
                        }
                    },
                    enabled = editTitle.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameCoverDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- 5. Dynamic Theme Selector Dialog (Feature 7) ---
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme & Aesthetics", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { theme ->
                        val isSelected = currentTheme == theme
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(theme)
                                    showThemeDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(theme.previewColor)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = theme.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- Lyrics Preview Dialog ---
    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsDialog = false },
            title = { Text("Lyrics: ${track.title}", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "♪ [Melody playing in harmony]\n♪ Offline crystal audio stream\n♪ Pure waves vibrating\n♪ High fidelity studio sound\n\n(Enjoy offline playback with hardware Equalizer)",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 24.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun shareMedia(context: Context, track: TrackEntity) {
    try {
        val file = File(track.mediaUri)
        if (file.exists()) {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (track.isVideo) "video/*" else "audio/*"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, track.title)
                putExtra(Intent.EXTRA_TEXT, "Playing ${track.title} on WavePlay")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Media File"))
        } else {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Listening to ${track.title} by ${track.artist} on WavePlay")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Track"))
        }
    } catch (_: Exception) {}
}
