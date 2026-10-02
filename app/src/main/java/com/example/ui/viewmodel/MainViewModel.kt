package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.MusicGenerationParams
import com.example.data.local.PlaylistEntity
import com.example.data.local.TrackEntity
import com.example.data.repository.MediaRepository
import com.example.download.DownloadTask
import com.example.player.EqualizerPreset
import com.example.player.EqualizerSettings
import com.example.player.PlaybackManager
import com.example.player.PlaybackState
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen(val title: String) {
    data object Music : Screen("Music")
    data object Videos : Screen("Videos")
    data object Playlists : Screen("Playlists")
    data object Equalizer : Screen("Equalizer")
    data object Downloader : Screen("Downloader")
    data object AiMusic : Screen("AI Music")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    val playbackManager = PlaybackManager(application)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Music)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTheme = MutableStateFlow(AppThemeMode.CYBER_NEON)
    val currentTheme: StateFlow<AppThemeMode> = _currentTheme.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _activeVideoPlayer = MutableStateFlow<TrackEntity?>(null)
    val activeVideoPlayer: StateFlow<TrackEntity?> = _activeVideoPlayer.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    val audioTracks: StateFlow<List<TrackEntity>> = combine(repository.audioTracks, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoTracks: StateFlow<List<TrackEntity>> = combine(repository.videoTracks, _searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter {
            it.title.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackState> = playbackManager.playbackState

    val equalizerSettings: StateFlow<EqualizerSettings> = playbackManager.audioEffectController.settings

    val activeDownloads: StateFlow<List<DownloadTask>> = repository.downloader.activeDownloads

    val sleepTimerSeconds: StateFlow<Long> = playbackManager.sleepTimerSeconds

    private val _isArrangeMode = MutableStateFlow(false)
    val isArrangeMode: StateFlow<Boolean> = _isArrangeMode.asStateFlow()

    private val _isDirectDownloaderVisible = MutableStateFlow(false)
    val isDirectDownloaderVisible: StateFlow<Boolean> = _isDirectDownloaderVisible.asStateFlow()

    private val _isGeneratingAiMusic = MutableStateFlow(false)
    val isGeneratingAiMusic: StateFlow<Boolean> = _isGeneratingAiMusic.asStateFlow()

    private val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _isProSheetVisible = MutableStateFlow(false)
    val isProSheetVisible: StateFlow<Boolean> = _isProSheetVisible.asStateFlow()

    fun setProSheetVisible(visible: Boolean) {
        _isProSheetVisible.value = visible
    }

    fun setProUser(isPro: Boolean) {
        _isProUser.value = isPro
    }

    fun toggleProUser() {
        _isProUser.value = !_isProUser.value
    }

    init {
        viewModelScope.launch {
            repository.initializeOfflineSamplesIfEmpty()
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setTheme(mode: AppThemeMode) {
        _currentTheme.value = mode
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun openVideoPlayer(video: TrackEntity) {
        _activeVideoPlayer.value = video
        playbackManager.playTrack(video, listOf(video))
    }

    fun closeVideoPlayer() {
        _activeVideoPlayer.value = null
        playbackManager.exoPlayer.pause()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playTrack(track: TrackEntity, playlist: List<TrackEntity> = audioTracks.value) {
        if (track.isVideo) {
            openVideoPlayer(track)
        } else {
            playbackManager.playTrack(track, playlist)
        }
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun seekBy(deltaMs: Long) {
        playbackManager.seekBy(deltaMs)
    }

    fun playNext() {
        playbackManager.playNext()
    }

    fun playPrevious() {
        playbackManager.playPrevious()
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playbackManager.cycleRepeatMode()
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackManager.setPlaybackSpeed(speed)
    }

    fun setVolume(vol: Float) {
        playbackManager.setVolume(vol)
    }

    // Equalizer controls
    fun toggleEqualizer(enabled: Boolean) {
        playbackManager.audioEffectController.setEnabled(enabled)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBels: Int) {
        playbackManager.audioEffectController.setBandLevel(bandIndex, levelMilliBels)
    }

    fun setBassBoost(strength: Int) {
        playbackManager.audioEffectController.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Int) {
        playbackManager.audioEffectController.setVirtualizer(strength)
    }

    fun applyEqualizerPreset(preset: EqualizerPreset) {
        playbackManager.audioEffectController.applyPreset(preset)
    }

    fun setDirectDownloaderVisible(visible: Boolean) {
        _isDirectDownloaderVisible.value = visible
    }

    // Downloader controls
    fun startDownload(url: String, customName: String? = null, forceVideo: Boolean? = null) {
        viewModelScope.launch {
            repository.downloader.startDownload(url, customName, forceVideo)
        }
    }

    fun cancelDownload(id: String) {
        repository.downloader.cancelTask(id)
    }

    fun dismissDownloadTask(id: String) {
        repository.downloader.dismissTask(id)
    }

    // Gemini AI Music Generation
    fun generateAiMusic(
        params: MusicGenerationParams,
        customApiKey: String? = null,
        onComplete: ((TrackEntity?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isGeneratingAiMusic.value = true
            val result = repository.geminiMusicService.generateMusic(params, customApiKey)
            _isGeneratingAiMusic.value = false
            if (result.isSuccess) {
                val track = result.getOrNull()
                onComplete?.invoke(track)
            }
        }
    }

    // Playlist controls
    fun createPlaylist(name: String, description: String = "", colorHex: Long = 0xFF6366F1) {
        viewModelScope.launch {
            repository.createPlaylist(name, description, colorHex)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
            repository.deletePlaylist(playlistId)
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
    }

    fun getPlaylistTracks(playlistId: Long) = repository.getTracksForPlaylist(playlistId)

    fun addTrackToPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(track.id, track.isFavorite)
        }
    }

    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            if (playbackState.value.currentTrack?.id == track.id) {
                playbackManager.exoPlayer.stop()
            }
            repository.deleteTrack(track)
        }
    }

    fun importLocalMedia(uri: Uri, isVideo: Boolean, fileName: String?) {
        viewModelScope.launch {
            try {
                repository.importLocalMedia(uri, isVideo, fileName)
            } catch (_: Exception) {}
        }
    }

    fun setArrangeMode(active: Boolean) {
        _isArrangeMode.value = active
    }

    fun startSleepTimer(minutes: Int) {
        playbackManager.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playbackManager.cancelSleepTimer()
    }

    fun updateTracksOrder(orderedTracks: List<TrackEntity>) {
        viewModelScope.launch {
            repository.updateTracksOrder(orderedTracks)
        }
    }

    fun updateTrackTitleAndCover(trackId: String, newTitle: String, newCoverColor: Long) {
        viewModelScope.launch {
            repository.updateTrackTitleAndCover(trackId, newTitle, newCoverColor)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
