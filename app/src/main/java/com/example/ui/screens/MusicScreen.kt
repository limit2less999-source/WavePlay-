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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WorkspacePremium
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
    onOpenPro: () -> Unit = {}
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

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        color = Color(0xFF0F1724) // Deep slate background matching reference image 1
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
                    color = Color.White
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            tint = if (isSearchExpanded || searchQuery.isNotBlank()) Color(0xFF38BDF8) else Color(0xFF94A3B8),
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
                            tint = Color(0xFF38BDF8),
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
                            tint = Color(0xFF94A3B8),
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
                                .background(Color(0xFF1E293B))
                                .clickable { onNavigateCategory(Screen.Videos) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("category_pill_videos"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Videos",
                                    color = Color.White,
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
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                    }

                    // 2. [Songs] pill (Active selected pill)
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF0284C7)) // Bright vibrant cyan-blue pill
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                                .testTag("category_pill_songs"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Songs",
                                color = Color.White,
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
                                .background(Color(0xFF1E293B))
                                .clickable { onNavigateCategory(Screen.Playlists) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_pill_playlists"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Playlists",
                                color = Color(0xFF94A3B8),
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
                                .background(Color(0xFF1E293B))
                                .clickable { onNavigateCategory(Screen.Downloader) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_pill_downloader"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Downloads",
                                color = Color(0xFF94A3B8),
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
                                .background(Color(0xFF7C3AED).copy(alpha = 0.25f))
                                .clickable { onNavigateCategory(Screen.AiMusic) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("category_pill_ai_music"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "✨ AI Studio",
                                    color = Color(0xFFA78BFA),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Search Bar (when search is toggled) OR "Shuffle playback" row (Reference Image 1)
            if (isSearchExpanded || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("music_search_input"),
                    placeholder = { Text("Search songs, artists...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8))
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) onSearchQueryChange("")
                            else isSearchExpanded = false
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Close search", tint = Color(0xFF94A3B8))
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFF131D2E),
                        unfocusedContainerColor = Color(0xFF131D2E)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // Shuffle Playback Row matching Reference Image 1
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
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Shuffle playback (${tracks.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }

                    Text(
                        text = "Long-press to arrange",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
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
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Long-press to arrange",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tracks List matching Reference Image 1:
            // Squircle on left with music note, title in bold white, subtitle in gray
            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No songs found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
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
                                    if (isPlaying) Color(0xFF1E293B) else Color.Transparent
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
                                // Squircle Thumbnail with Music Note (matching reference image 1)
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF182234)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (isPlaying) Color(0xFF38BDF8) else Color(0xFF64748B),
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
                                        color = if (isPlaying) Color(0xFF38BDF8) else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (track.album.isNotBlank()) track.album else "Download",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
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
                                            tint = Color(0xFF64748B),
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
