package com.example

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    private var isInPipMode by mutableStateOf(false)

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val pipParams = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(pipParams)
            } catch (_: Exception) {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WavePlayApp(isInPipMode = isInPipMode)
        }
    }
}

@Composable
fun WavePlayApp(
    viewModel: MainViewModel = viewModel(),
    isInPipMode: Boolean = false
) {
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
    val isPlayTogether by viewModel.isPlayTogether.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    // Request permissions for local storage and notifications, then scan device media
    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.scanDeviceMedia()
    }

    val requestPermissionsAndScan = {
        permissionLauncher.launch(permissionsToRequest)
        viewModel.scanDeviceMedia()
    }

    LaunchedEffect(Unit) {
        requestPermissionsAndScan()
    }

    // System Back Press handling
    BackHandler(enabled = isNowPlayingExpanded) {
        viewModel.setNowPlayingExpanded(false)
    }
    BackHandler(enabled = isArrangeMode) {
        viewModel.setArrangeMode(false)
    }
    BackHandler(enabled = currentScreen != Screen.Music && activeVideo == null && !isNowPlayingExpanded && !isArrangeMode) {
        viewModel.navigateTo(Screen.Music)
    }

    AuraTuneTheme(themeMode = currentTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (activeVideo != null) {
                // Full Screen Video Player with double-tap ±10s skip and PiP
                VideoPlayerScreen(
                    video = activeVideo!!,
                    playbackManager = viewModel.playbackManager,
                    playbackState = playbackState,
                    isInPipMode = isInPipMode,
                    onClose = { viewModel.closeVideoPlayer() }
                )
            } else if (isArrangeMode) {
                // Drag to arrange mode with batch actions (share, delete, add to playlist)
                DragToArrangeScreen(
                    tracks = audioTracks,
                    playlists = playlists,
                    onApply = { viewModel.updateTracksOrder(it) },
                    onDeleteSelected = { viewModel.deleteTracks(it) },
                    onAddSelectedToPlaylist = { plId, ids -> viewModel.addTracksToPlaylist(plId, ids) },
                    onClose = { viewModel.setArrangeMode(false) }
                )
            } else {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        // Mini Player shown when a song is playing/paused
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
                                    onOpenPro = { viewModel.setProSheetVisible(true) },
                                    onPlayNext = { viewModel.playNextInQueue(it) },
                                    onScanDevice = requestPermissionsAndScan,
                                    onToggleShuffle = { viewModel.toggleShuffle() },
                                    onCycleRepeatMode = { viewModel.cycleRepeatMode() },
                                    isPlayTogether = isPlayTogether,
                                    onTogglePlayTogether = { viewModel.togglePlayTogether() }
                                )
                            }
                            Screen.Videos -> {
                                VideoListScreen(
                                    videoTracks = videoTracks,
                                    onPlayVideo = { viewModel.openVideoPlayer(it) },
                                    onDeleteVideo = { viewModel.deleteTrack(it) },
                                    onImportVideo = { uri, name -> viewModel.importLocalMedia(uri, true, name) },
                                    onNavigateToDownloader = { viewModel.navigateTo(Screen.Downloader) },
                                    onBack = { viewModel.navigateTo(Screen.Music) },
                                    onScanVideos = requestPermissionsAndScan
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
                                    getPlaylistTracks = { viewModel.getPlaylistTracks(it) },
                                    availableTracks = audioTracks,
                                    onAddTrackToPlaylist = { plId, trId -> viewModel.addTrackToPlaylist(plId, trId) },
                                    onBackToMusic = { viewModel.navigateTo(Screen.Music) }
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
                                    onDismissTask = { viewModel.dismissDownloadTask(it) },
                                    onBack = { viewModel.navigateTo(Screen.Music) }
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
                                    onGenerate = { params, customApiKey ->
                                        viewModel.generateAiMusic(params, customApiKey)
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
            }

            // Smooth Animated Now Playing Screen
            AnimatedVisibility(
                visible = isNowPlayingExpanded && playbackState.currentTrack != null,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(200)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(180))
            ) {
                NowPlayingSheet(
                    playbackState = playbackState,
                    playlists = playlists,
                    sleepTimerSeconds = sleepTimerSeconds,
                    currentTheme = currentTheme,
                    isPlayTogether = isPlayTogether,
                    onThemeChange = { viewModel.setTheme(it) },
                    onCollapse = { viewModel.setNowPlayingExpanded(false) },
                    onTogglePlay = { viewModel.togglePlayPause() },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleFavorite = {
                        playbackState.currentTrack?.let { viewModel.toggleFavorite(it) }
                    },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onCycleRepeatMode = { viewModel.cycleRepeatMode() },
                    onTogglePlayTogether = { viewModel.togglePlayTogether() },
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
                        viewModel.setArrangeMode(true)
                    }
                )
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
