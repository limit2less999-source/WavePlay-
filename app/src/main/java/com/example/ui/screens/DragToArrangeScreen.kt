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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
    onApply: (List<TrackEntity>) -> Unit,
    onClose: () -> Unit
) {
    BackHandler {
        onClose()
    }

    val reorderedTracks = remember(tracks) {
        mutableStateListOf<TrackEntity>().apply { addAll(tracks) }
    }
    val selectedIds = remember { mutableStateListOf<String>() }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("drag_to_arrange_screen"),
        color = Color(0xFF0C1420) // Deep navy matching reference image 3
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
                    // Move to top
                    Row(
                        modifier = Modifier
                            .clickable(enabled = selectedIds.isNotEmpty()) {
                                if (selectedIds.isNotEmpty()) {
                                    val selectedTracks = reorderedTracks.filter { selectedIds.contains(it.id) }
                                    reorderedTracks.removeAll(selectedTracks)
                                    reorderedTracks.addAll(0, selectedTracks)
                                }
                            }
                            .padding(8.dp)
                            .testTag("move_to_top_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignTop,
                            contentDescription = "Move to top",
                            tint = if (selectedIds.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Move to top",
                            color = if (selectedIds.isNotEmpty()) Color.White else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    // Move to bottom
                    Row(
                        modifier = Modifier
                            .clickable(enabled = selectedIds.isNotEmpty()) {
                                if (selectedIds.isNotEmpty()) {
                                    val selectedTracks = reorderedTracks.filter { selectedIds.contains(it.id) }
                                    reorderedTracks.removeAll(selectedTracks)
                                    reorderedTracks.addAll(selectedTracks)
                                }
                            }
                            .padding(8.dp)
                            .testTag("move_to_bottom_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerticalAlignBottom,
                            contentDescription = "Move to bottom",
                            tint = if (selectedIds.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Move to bottom",
                            color = if (selectedIds.isNotEmpty()) Color.White else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
