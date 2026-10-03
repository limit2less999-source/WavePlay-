package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WorkspacePremium
import com.example.player.RepeatMode
import com.example.ui.components.AdMobBannerView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.player.PlaybackState
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MusicScreen(
    modifier: Modifier = Modifier,
    tracks: List<TrackEntity>,
    playbackState: PlaybackState,
    playlists: List<PlaylistEntity>,
    currentTheme: AppThemeMode,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onThemeChange: (AppThemeMode) -> Unit = {},
    onNavigateCategory: (Screen) -> Unit,
    onStartArrangeMode: () -> Unit,
    onPlayTrack: (TrackEntity, List<TrackEntity>) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onDeleteTrack: (TrackEntity) -> Unit,
    onAddTrackToPlaylist: (playlistId: Long, trackId: String) -> Unit,
    onImportAudio: (Uri, String?) -> Unit,
    isProUser: Boolean = false,
    onOpenPro: () -> Unit = {},
    onPlayNext: (TrackEntity) -> Unit = {},
    onScanDevice: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onCycleRepeatMode: () -> Unit = {},
    isPlayTogether: Boolean = false,
    onTogglePlayTogether: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenVault: () -> Unit = {},
    onHideTrackToVault: (TrackEntity) -> Unit = {}
) {
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportAudio(uri, "Imported Audio")
        }
    }

    var trackForPlaylistDialog by remember { mutableStateOf<TrackEntity?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Top App Bar with Branding and Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WavePlay",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Scan Device Media / Refresh Button
                    IconButton(
                        onClick = onScanDevice,
                        modifier = Modifier.size(38.dp).testTag("home_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Device Audio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Pro Crown / VIP Status Button
                    IconButton(
                        onClick = onOpenPro,
                        modifier = Modifier.size(38.dp).testTag("home_pro_btn")
                    ) {
                        if (isProUser) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF59E0B))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "WavePlay Pro",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Search toggle icon
                    IconButton(
                        onClick = { isSearchExpanded = !isSearchExpanded },
                        modifier = Modifier.size(38.dp).testTag("home_search_toggle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearchExpanded || searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Palette Theme Switcher
                    IconButton(
                        onClick = { showThemePicker = true },
                        modifier = Modifier.size(38.dp).testTag("home_theme_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // App Settings (Play together, Vault, Video speed)
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(38.dp).testTag("home_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Arrange mode button
                    IconButton(
                        onClick = onStartArrangeMode,
                        modifier = Modifier.size(38.dp).testTag("home_arrange_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Arrange files",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Pills Bar (Videos | Songs Playlists Downloads AI Studio)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. [▶ Videos] pill
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onNavigateCategory(Screen.Videos) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("category_pill_videos"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Videos",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Divider `|`
                    item {
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                        )
                    }

                    // 2. [Songs] pill (Active selected pill)
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                                .testTag("category_pill_songs"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Songs",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 3. [Playlists] pill
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onNavigateCategory(Screen.Playlists) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_pill_playlists"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Playlists",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 4. [Downloader] pill
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onNavigateCategory(Screen.Downloader) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_pill_downloader"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Downloads",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 5. [✨ AI Studio] pill
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                                .clickable { onNavigateCategory(Screen.AiMusic) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_pill_ai_music"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "✨ AI Studio",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Search Bar (when search is toggled) OR "Shuffle playback" row
            if (isSearchExpanded || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("music_search_input"),
                    placeholder = { Text("Search songs, artists...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) onSearchQueryChange("")
                            else isSearchExpanded = false
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Close search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Shuffle & Repeat Playback Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (tracks.isNotEmpty()) {
                                    val shuffled = tracks.shuffled()
                                    onPlayTrack(shuffled.first(), shuffled)
                                    onToggleShuffle()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playbackState.isShuffle) Icons.Default.Shuffle else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (playbackState.isShuffle) "Shuffled (${tracks.size})" else "Shuffle playback (${tracks.size})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        )
                    }

                    // Quick Repeat Mode Toggle (OFF -> ALL -> ONE)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onCycleRepeatMode() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (repIcon, repLabel) = when (playbackState.repeatMode) {
                            RepeatMode.ONE -> Pair(Icons.Default.RepeatOne, "Repeat One")
                            RepeatMode.ALL -> Pair(Icons.Default.Repeat, "Repeat All")
                            RepeatMode.OFF -> Pair(Icons.Default.Repeat, "Loop Off")
                        }
                        Icon(
                            imageVector = repIcon,
                            contentDescription = repLabel,
                            tint = if (playbackState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = repLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (playbackState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // AdMob Ready Banner (Auto-hidden for Pro users)
            AdMobBannerView(
                isProUser = isProUser,
                onOpenPro = onOpenPro
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Hint for user
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${tracks.size} Songs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap 3-dots for Play Next & Share",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tracks List with Empty State Card
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Scan Device Audio Files",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Grant permission to automatically load all your phone's songs, downloads, and audio tracks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Button(
                                onClick = onScanDevice,
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Device Audio (Allow Permission)", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedButton(
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pick Songs From Storage", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        val isPlaying = playbackState.currentTrack?.id == track.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .combinedClickable(
                                    onClick = { onPlayTrack(track, tracks) },
                                    onLongClick = { onStartArrangeMode() }
                                )
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("track_row_${track.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Squircle Thumbnail with Music Note
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                // Title and Subtitle ("Download" or album)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val isAiTrack = track.artist == "Generated" || track.sourceUrl?.startsWith("gemini") == true
                                    Text(
                                        text = if (isAiTrack) "Generated" else (if (track.artist.isNotBlank()) track.artist else if (track.album.isNotBlank()) track.album else "Local Audio"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAiTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isAiTrack) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Quick more menu
                                var showTrackMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(
                                        onClick = { showTrackMenu = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showTrackMenu,
                                        onDismissRequest = { showTrackMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Arrange order") },
                                            leadingIcon = {
                                                Icon(Icons.Default.SwapVert, contentDescription = null)
                                            },
                                            onClick = {
                                                showTrackMenu = false
                                                onStartArrangeMode()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Play next") },
                                            leadingIcon = {
                                                Icon(Icons.Default.SkipNext, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            },
                                            onClick = {
                                                showTrackMenu = false
                                                onPlayNext(track)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            },
                                            onClick = {
                                                showTrackMenu = false
                                                val sendIntent = android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    if (track.mediaUri.startsWith("content://")) {
                                                        putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(track.mediaUri))
                                                        type = "audio/*"
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    } else {
                                                        val file = java.io.File(track.mediaUri)
                                                        if (file.exists()) {
                                                            try {
                                                                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                                                    context,
                                                                    "${context.packageName}.fileprovider",
                                                                    file
                                                                )
                                                                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                                                                type = "audio/*"
                                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            } catch (_: Exception) {
                                                                putExtra(android.content.Intent.EXTRA_TEXT, "Listening to ${track.title} on WavePlay")
                                                                type = "text/plain"
                                                            }
                                                        } else {
                                                            putExtra(android.content.Intent.EXTRA_TEXT, "Listening to ${track.title} on WavePlay")
                                                            type = "text/plain"
                                                        }
                                                    }
                                                }
                                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Track"))
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Move to Safe Vault") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981))
                                            },
                                            onClick = {
                                                showTrackMenu = false
                                                onHideTrackToVault(track)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Add to playlist") },
                                            leadingIcon = {
                                                Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                                            },
                                            onClick = {
                                                showTrackMenu = false
                                                trackForPlaylistDialog = track
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete track", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            },
                                            onClick = {
                                                showTrackMenu = false
                                                onDeleteTrack(track)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Theme Picker Dialog (Feature 7)
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text("App Theme & Aesthetics", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { theme ->
                        val isSelected = currentTheme == theme
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(theme)
                                    showThemePicker = false
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

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "PLAYBACK & SYSTEM BEHAVIOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Play Together With Other Apps
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePlayTogether() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Play Together With Other Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Don't pause when Instagram, Games, or Camera play audio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            androidx.compose.material3.Switch(
                                checked = isPlayTogether,
                                onCheckedChange = { onTogglePlayTogether() }
                            )
                        }
                    }

                    // Display Over Other Apps / Floating Player
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    if (!android.provider.Settings.canDrawOverlays(context)) {
                                        val intent = android.content.Intent(
                                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Display Over Other Apps", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                val hasOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    android.provider.Settings.canDrawOverlays(context)
                                } else true
                                Text(
                                    if (hasOverlay) "Permission Allowed • Video PiP & Floating Window Active" else "Tap to Grant System Overlay Permission",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemePicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add To Playlist Dialog
    trackForPlaylistDialog?.let { track ->
        AlertDialog(
            onDismissRequest = { trackForPlaylistDialog = null },
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists yet. Create a playlist first in the Playlists tab.")
                } else {
                    Column {
                        playlists.forEach { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onAddTrackToPlaylist(playlist.id, track.id)
                                        trackForPlaylistDialog = null
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.padding(12.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { trackForPlaylistDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}
