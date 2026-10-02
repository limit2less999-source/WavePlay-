package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TrackEntity
import com.example.download.DownloadTask
import com.example.util.TimeUtils

enum class MediaDownloadType(val label: String) {
    AUTO("Auto Detect"),
    AUDIO("Audio (MP3)"),
    VIDEO("Video (MP4)")
}

@Composable
fun DirectLinkDownloaderCard(
    modifier: Modifier = Modifier,
    activeDownloads: List<DownloadTask>,
    onStartDownload: (url: String, customName: String?, forceVideo: Boolean?) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDismissTask: (String) -> Unit,
    onPlayDownloadedFile: ((TrackEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var selectedMediaType by remember { mutableStateOf(MediaDownloadType.AUTO) }
    var clipboardCandidate by remember { mutableStateOf<String?>(null) }

    // Check clipboard on launch
    LaunchedEffect(Unit) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()?.trim() ?: ""
                if ((text.startsWith("http://") || text.startsWith("https://")) && !text.contains(" ")) {
                    clipboardCandidate = text
                }
            }
        } catch (_: Exception) {}
    }

    val sampleLinks = listOf(
        Triple("Big Buck Bunny (MP4)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", MediaDownloadType.VIDEO),
        Triple("Elephants Dream (MP4)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", MediaDownloadType.VIDEO),
        Triple("Lossless Beats (MP3)", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", MediaDownloadType.AUDIO),
        Triple("Acoustic Melody (MP3)", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", MediaDownloadType.AUDIO)
    )

    val isValidUrl = urlInput.trim().startsWith("http://") || urlInput.trim().startsWith("https://")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("direct_link_downloader_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131E30) // Deep modern navy container
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title and Storage Target
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Direct Link Downloader",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Saves to Device Storage (Music/Videos)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF34D399),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Clipboard Quick Suggestion Chip
            if (clipboardCandidate != null && urlInput.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                        .clickable {
                            urlInput = clipboardCandidate ?: ""
                            clipboardCandidate = null
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("clipboard_paste_chip")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paste from clipboard: ${clipboardCandidate?.take(32)}...",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Direct URL Input Field
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("direct_url_input"),
                label = { Text("Direct Media URL (HTTP/HTTPS)") },
                placeholder = { Text("https://example.com/audio.mp3 or video.mp4") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Link",
                        tint = if (isValidUrl) Color(0xFF34D399) else Color(0xFF94A3B8)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (urlInput.isNotBlank()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        urlInput = clip.getItemAt(0).text?.toString() ?: ""
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.testTag("paste_icon_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Media Type Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaDownloadType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedMediaType == type,
                        onClick = { selectedMediaType = type },
                        label = { Text(type.label, fontSize = 12.sp) },
                        leadingIcon = {
                            val icon = when (type) {
                                MediaDownloadType.AUTO -> Icons.Default.CloudDownload
                                MediaDownloadType.AUDIO -> Icons.Default.Audiotrack
                                MediaDownloadType.VIDEO -> Icons.Default.Videocam
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Optional Custom Title Field
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("direct_title_input"),
                label = { Text("Custom Title / File Name (Optional)") },
                placeholder = { Text("e.g. My Favorite Song") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedLabelColor = Color(0xFF38BDF8),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Download Action Button
            Button(
                onClick = {
                    if (isValidUrl) {
                        val forceVideo = when (selectedMediaType) {
                            MediaDownloadType.AUTO -> null
                            MediaDownloadType.AUDIO -> false
                            MediaDownloadType.VIDEO -> true
                        }
                        onStartDownload(
                            urlInput.trim(),
                            titleInput.trim().ifBlank { null },
                            forceVideo
                        )
                        urlInput = ""
                        titleInput = ""
                    }
                },
                enabled = isValidUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_direct_download_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    disabledContainerColor = Color(0xFF1E293B)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Download Directly to Device Storage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick 1-Tap Sample Presets
            Text(
                text = "TRY 1-TAP SAMPLE DIRECT LINKS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleLinks) { (label, url, type) ->
                    SuggestionChip(
                        onClick = {
                            urlInput = url
                            titleInput = label
                            selectedMediaType = type
                        },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        )
                    )
                }
            }

            // Active / Recent Downloads Section inside the component
            if (activeDownloads.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "DOWNLOAD PROGRESS (${activeDownloads.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeDownloads.take(4).forEach { task ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("active_task_${task.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (task.isVideo) Icons.Default.Videocam else Icons.Default.Audiotrack,
                                        contentDescription = null,
                                        tint = if (task.isVideo) Color(0xFFC084FC) else Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = task.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (!task.isFinished) {
                                        IconButton(
                                            onClick = { onCancelDownload(task.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = "Cancel",
                                                tint = Color(0xFFF43F5E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { onDismissTask(task.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (!task.isFinished) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { task.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(2.5.dp)),
                                        color = Color(0xFF0284C7),
                                        trackColor = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(task.progress * 100).toInt()}% • ${TimeUtils.formatBytes(task.downloadedBytes)} / ${TimeUtils.formatBytes(task.totalBytes)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                        if (task.speedBytesPerSec > 0) {
                                            Text(
                                                text = "${TimeUtils.formatBytes(task.speedBytesPerSec)}/s",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF38BDF8),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else if (task.error != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = task.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFF43F5E),
                                        fontSize = 11.sp
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Saved to device storage (${TimeUtils.formatBytes(task.downloadedBytes)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectLinkDownloaderModalSheet(
    activeDownloads: List<DownloadTask>,
    onStartDownload: (url: String, customName: String?, forceVideo: Boolean?) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDismissTask: (String) -> Unit,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            DirectLinkDownloaderCard(
                activeDownloads = activeDownloads,
                onStartDownload = { url, name, forceVideo ->
                    onStartDownload(url, name, forceVideo)
                },
                onCancelDownload = onCancelDownload,
                onDismissTask = onDismissTask
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
