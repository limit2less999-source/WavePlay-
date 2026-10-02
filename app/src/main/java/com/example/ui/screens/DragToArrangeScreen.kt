package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.local.TrackEntity

enum class SmartSortOption(val label: String) {
    NAME_ASC("A to Z"),
    NAME_DESC("Z to A"),
    DURATION("Duration"),
    REVERSE("Reverse")
}

@Composable
fun DragToArrangeScreen(
    tracks: List<TrackEntity>,
    playlists: List<com.example.data.local.PlaylistEntity> = emptyList(),
    onApply: (List<TrackEntity>) -> Unit,
    onDeleteSelected: (List<TrackEntity>) -> Unit = {},
    onAddSelectedToPlaylist: (Long, List<String>) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    BackHandler {
        onClose()
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val reorderedTracks = remember(tracks) {
        mutableStateListOf<TrackEntity>().apply { addAll(tracks) }
    }
    val selectedIds = remember { mutableStateListOf<String>() }
    var showPlaylistChooserDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("drag_to_arrange_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header: Back arrow, "Drag to arrange", "Apply" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(48.dp).testTag("arrange_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Drag to arrange",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        onApply(reorderedTracks.toList())
                        onClose()
                    },
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("arrange_apply_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E40AF), // Deep royal blue pill from screenshot
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = "Apply",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Smart Sorting Presets Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Text(
                        text = "Smart Sort:",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    val isAllSelected = selectedIds.size == reorderedTracks.size && reorderedTracks.isNotEmpty()
                    FilterChip(
                        selected = isAllSelected,
                        onClick = {
                            if (isAllSelected) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(reorderedTracks.map { it.id })
                            }
                        },
                        label = { Text(if (isAllSelected) "Deselect All" else "Select All (${selectedIds.size})", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isAllSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                            labelColor = if (isAllSelected) Color.Black else Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val sorted = reorderedTracks.sortedBy { it.title.lowercase() }
                            reorderedTracks.clear()
                            reorderedTracks.addAll(sorted)
                        },
                        label = { Text("A → Z", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val sorted = reorderedTracks.sortedByDescending { it.title.lowercase() }
                            reorderedTracks.clear()
                            reorderedTracks.addAll(sorted)
                        },
                        label = { Text("Z → A", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val sorted = reorderedTracks.sortedByDescending { it.durationMs }
                            reorderedTracks.clear()
                            reorderedTracks.addAll(sorted)
                        },
                        label = { Text("Duration", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val reversed = reorderedTracks.reversed()
                            reorderedTracks.clear()
                            reorderedTracks.addAll(reversed)
                        },
                        label = { Text("Reverse", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Batch Selection Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedIds.isNotEmpty()) "${selectedIds.size} of ${reorderedTracks.size} selected" else "${reorderedTracks.size} tracks",
                    color = if (selectedIds.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )

                Row {
                    TextButton(
                        onClick = {
                            if (selectedIds.size == reorderedTracks.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(reorderedTracks.map { it.id })
                            }
                        }
                    ) {
                        Text(
                            text = if (selectedIds.size == reorderedTracks.size) "Deselect All" else "Select All",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Track list with selection radio, squircle, title, subtitle, and drag handle
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(reorderedTracks, key = { _, item -> item.id }) { index, track ->
                    val isSelected = selectedIds.contains(track.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isSelected) selectedIds.remove(track.id)
                                else selectedIds.add(track.id)
                            }
                            .testTag("arrange_item_${track.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D2E)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Radio/Select circle
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (isSelected) "Selected" else "Unselected",
                                tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF64748B),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        if (isSelected) selectedIds.remove(track.id)
                                        else selectedIds.add(track.id)
                                    }
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            // Squircle Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Title & Subtitle
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
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

                            // Quick Move Up / Move Down Arrow Buttons
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val item = reorderedTracks.removeAt(index)
                                            reorderedTracks.add(index - 1, item)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (index < reorderedTracks.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val item = reorderedTracks.removeAt(index)
                                            reorderedTracks.add(index + 1, item)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 2/3 Lines Reorder Drag Handle (matching reference image 3)
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = Color(0xFF64748B),
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("drag_handle_${track.id}")
                            )
                        }
                    }
                }
            }

            // Bottom action bar: "Move to top" and "Move to bottom" (matching image 3)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                color = Color(0xFF0F2342), // Deep royal navy bottom bar
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share selected
                    IconButton(
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                val selectedTracks = reorderedTracks.filter { selectedIds.contains(it.id) }
                                val text = selectedTracks.joinToString("\n") { "${it.title} - ${it.artist}" }
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, "Shared from WavePlay:\n$text")
                                    type = "text/plain"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Selected Songs"))
                            }
                        },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                        )
                    }

                    // Add to Playlist
                    IconButton(
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                showPlaylistChooserDialog = true
                            }
                        },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Add to playlist",
                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                        )
                    }

                    // Delete Selected
                    IconButton(
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                val selectedTracks = reorderedTracks.filter { selectedIds.contains(it.id) }
                                onDeleteSelected(selectedTracks)
                                reorderedTracks.removeAll(selectedTracks)
                                selectedIds.clear()
                            }
                        },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error else Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    // Move to top
                    IconButton(
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                val selectedTracks = reorderedTracks.filter { selectedIds.contains(it.id) }
                                reorderedTracks.removeAll(selectedTracks)
                                reorderedTracks.addAll(0, selectedTracks)
                            }
                        },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignTop,
                            contentDescription = "Move to top",
                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                        )
                    }

                    // Move to bottom
                    IconButton(
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                val selectedTracks = reorderedTracks.filter { selectedIds.contains(it.id) }
                                reorderedTracks.removeAll(selectedTracks)
                                reorderedTracks.addAll(selectedTracks)
                            }
                        },
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignBottom,
                            contentDescription = "Move to bottom",
                            tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // Add to Playlist Chooser Dialog
        if (showPlaylistChooserDialog) {
            AlertDialog(
                onDismissRequest = { showPlaylistChooserDialog = false },
                title = { Text("Add ${selectedIds.size} songs to Playlist", fontWeight = FontWeight.Bold) },
                text = {
                    if (playlists.isEmpty()) {
                        Text("No playlists found. Create a playlist first in the Playlists section.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(playlists) { pl ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddSelectedToPlaylist(pl.id, selectedIds.toList())
                                            showPlaylistChooserDialog = false
                                            selectedIds.clear()
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlaylistAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(pl.name, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPlaylistChooserDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
