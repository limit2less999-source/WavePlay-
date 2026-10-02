package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DirectLinkDownloaderModalSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.components.WavePlayProSheet
import com.example.ui.screens.AiMusicGeneratorScreen
import com.example.ui.screens.DownloaderScreen
import com.example.ui.screens.DragToArrangeScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.NowPlayingSheet
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.VideoListScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.theme.AuraTuneTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraTuneApp()
        }
    }
}

@Composable
fun AuraTuneApp(viewModel: MainViewModel = viewModel()) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val isArrangeMode by viewModel.isArrangeMode.collectAsStateWithLifecycle()
    val isDirectDownloaderVisible by viewModel.isDirectDownloaderVisible.collectAsStateWithLifecycle()
    val activeVideo by viewModel.activeVideoPlayer.collectAsStateWithLifecycle()

    val audioTracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    val videoTracks by viewModel.videoTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val sleepTimerSeconds by viewModel.sleepTimerSeconds.collectAsStateWithLifecycle()
    val equalizerSettings by viewModel.equalizerSettings.collectAsStateWithLifecycle()
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    val isGeneratingAiMusic by viewModel.isGeneratingAiMusic.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val isProSheetVisible by viewModel.isProSheetVisible.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    AuraTuneTheme(themeMode = currentTheme) {
        if (isArrangeMode) {
            // "Drag to arrange" mode (as shown in reference image 3)
            DragToArrangeScreen(
                tracks = audioTracks,
                onApply = { viewModel.updateTracksOrder(it) },
                onClose = { viewModel.setArrangeMode(false) }
            )
        } else if (activeVideo != null) {
            // Full Screen Video Player with double-tap ±10s skip
            VideoPlayerScreen(
                video = activeVideo!!,
                playbackManager = viewModel.playbackManager,
                playbackState = playbackState,
                onClose = { viewModel.closeVideoPlayer() }
            )
        } else if (isNowPlayingExpanded && playbackState.currentTrack != null) {
            // Full Screen Play Control Section (as shown in reference image 2)
            NowPlayingSheet(
                playbackState = playbackState,
                playlists = playlists,
                sleepTimerSeconds = sleepTimerSeconds,
                currentTheme = currentTheme,
                onThemeChange = { viewModel.setTheme(it) },
                onCollapse = { viewModel.setNowPlayingExpanded(false) },
                onTogglePlay = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeekTo = { viewModel.seekTo(it) },
                onToggleFavorite = {
                    playbackState.currentTrack?.let { viewModel.toggleFavorite(it) }
                },
                onOpenEqualizer = {
                    viewModel.setNowPlayingExpanded(false)
                    viewModel.navigateTo(Screen.Equalizer)
                },
                onAddToPlaylist = { playlistId ->
                    playbackState.currentTrack?.let { viewModel.addTrackToPlaylist(playlistId, it.id) }
                },
                onDeleteTrack = {
                    playbackState.currentTrack?.let { viewModel.deleteTrack(it) }
                },
                onRenameTrack = { title, color ->
                    playbackState.currentTrack?.let { viewModel.updateTrackTitleAndCover(it.id, title, color) }
                },
                onStartSleepTimer = { viewModel.startSleepTimer(it) },
                onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                onOpenQueue = {
                    viewModel.setNowPlayingExpanded(false)
                    viewModel.navigateTo(Screen.Playlists)
                }
            )
        } else {
            // Main App View: Home Screen (as shown in reference image 1) + Mini Player
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    // Mini Player shown when a song is playing/paused (reference image 1)
                    if (playbackState.currentTrack != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            MiniPlayer(
                                playbackState = playbackState,
                                onExpand = { viewModel.setNowPlayingExpanded(true) },
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onCycleRepeat = { viewModel.cycleRepeatMode() }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        Screen.Music -> {
                            MusicScreen(
                                tracks = audioTracks,
                                playbackState = playbackState,
                                playlists = playlists,
                                currentTheme = currentTheme,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onThemeChange = { viewModel.setTheme(it) },
                                onNavigateCategory = { viewModel.navigateTo(it) },
                                onStartArrangeMode = { viewModel.setArrangeMode(true) },
                                onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onDeleteTrack = { viewModel.deleteTrack(it) },
                                onAddTrackToPlaylist = { plId, trId -> viewModel.addTrackToPlaylist(plId, trId) },
                                onImportAudio = { uri, name -> viewModel.importLocalMedia(uri, false, name) },
                                isProUser = isProUser,
                                onOpenPro = { viewModel.setProSheetVisible(true) }
                            )
                        }
                        Screen.Videos -> {
                            VideoListScreen(
                                videoTracks = videoTracks,
                                onPlayVideo = { viewModel.openVideoPlayer(it) },
                                onDeleteVideo = { viewModel.deleteTrack(it) },
                                onImportVideo = { uri, name -> viewModel.importLocalMedia(uri, true, name) },
                                onNavigateToDownloader = { viewModel.navigateTo(Screen.Downloader) }
                            )
                        }
                        Screen.Playlists -> {
                            PlaylistsScreen(
                                playlists = playlists,
                                selectedPlaylist = selectedPlaylist,
                                onSelectPlaylist = { viewModel.selectPlaylist(it) },
                                onCreatePlaylist = { name, desc, color -> viewModel.createPlaylist(name, desc, color) },
                                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                                onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                                onRemoveTrackFromPlaylist = { plId, trId -> viewModel.removeTrackFromPlaylist(plId, trId) },
                                getPlaylistTracks = { viewModel.getPlaylistTracks(it) }
                            )
                        }
                        Screen.Equalizer -> {
                            EqualizerScreen(
                                settings = equalizerSettings,
                                onBack = { viewModel.navigateTo(Screen.Music) },
                                onToggleEnabled = { viewModel.toggleEqualizer(it) },
                                onBandLevelChange = { idx, lvl -> viewModel.setEqualizerBandLevel(idx, lvl) },
                                onBassBoostChange = { viewModel.setBassBoost(it) },
                                onVirtualizerChange = { viewModel.setVirtualizer(it) },
                                onApplyPreset = { viewModel.applyEqualizerPreset(it) }
                            )
                        }
                        Screen.Downloader -> {
                            DownloaderScreen(
                                activeDownloads = activeDownloads,
                                onStartDownload = { url, customTitle, forceVideo ->
                                    viewModel.startDownload(url, customTitle, forceVideo)
                                },
                                onCancelDownload = { viewModel.cancelDownload(it) },
                                onDismissTask = { viewModel.dismissDownloadTask(it) }
                            )
                        }
                        Screen.AiMusic -> {
                            val aiTracks = audioTracks.filter {
                                it.artist.contains("Gemini", ignoreCase = true) ||
                                it.sourceUrl?.startsWith("gemini://") == true ||
                                it.album.contains("Collection", ignoreCase = true)
                            }
                            AiMusicGeneratorScreen(
                                isGenerating = isGeneratingAiMusic,
                                recentGeneratedTracks = aiTracks,
                                onGenerate = { params, key ->
                                    viewModel.generateAiMusic(params, key) { newTrack ->
                                        if (newTrack != null) {
                                            viewModel.playTrack(newTrack, listOf(newTrack) + audioTracks)
                                        }
                                    }
                                },
                                onPlayTrack = { track ->
                                    viewModel.playTrack(track, audioTracks)
                                },
                                onBack = { viewModel.navigateTo(Screen.Music) }
                            )
                        }
                    }
                }
            }

            // Direct Link Downloader Modal Sheet (if opened)
            if (isDirectDownloaderVisible) {
                DirectLinkDownloaderModalSheet(
                    activeDownloads = activeDownloads,
                    onStartDownload = { url, customTitle, forceVideo ->
                        viewModel.startDownload(url, customTitle, forceVideo)
                    },
                    onCancelDownload = { viewModel.cancelDownload(it) },
                    onDismissTask = { viewModel.dismissDownloadTask(it) },
                    onClose = { viewModel.setDirectDownloaderVisible(false) }
                )
            }

            // WavePlay Pro Paywall Sheet (if opened)
            if (isProSheetVisible) {
                WavePlayProSheet(
                    isProUser = isProUser,
                    onDismiss = { viewModel.setProSheetVisible(false) },
                    onUpgradeSuccess = {
                        viewModel.setProUser(true)
                        viewModel.setProSheetVisible(false)
                    },
                    onToggleTestPro = {
                        viewModel.toggleProUser()
                    }
                )
            }
        }
    }
}
