package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.local.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RepeatMode { OFF, ALL, ONE }

data class PlaybackState(
    val currentTrack: TrackEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val queue: List<TrackEntity> = emptyList(),
    val queueIndex: Int = -1
)

@OptIn(UnstableApi::class)
class PlaybackManager(private val context: Context) {

    val audioEffectController = AudioEffectController()
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _sleepTimerSeconds = MutableStateFlow(0L)
    val sleepTimerSeconds: StateFlow<Long> = _sleepTimerSeconds.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                    audioEffectController.attachAudioSession(exoPlayer.audioSessionId)
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                val isBuffering = state == Player.STATE_BUFFERING
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _playbackState.value.currentTrack?.durationMs ?: 0L
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = isBuffering,
                    durationMs = duration
                )

                if (state == Player.STATE_READY) {
                    audioEffectController.attachAudioSession(exoPlayer.audioSessionId)
                } else if (state == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = exoPlayer.currentPosition
                )
            }
        })
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = if (exoPlayer.duration > 0) exoPlayer.duration else _playbackState.value.currentTrack?.durationMs ?: 0L
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur
                    )
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun playTrack(track: TrackEntity, playlist: List<TrackEntity> = listOf(track)) {
        val index = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = playlist,
            queueIndex = index,
            currentPositionMs = 0L,
            durationMs = track.durationMs
        )

        val uri = if (track.mediaUri.startsWith("http://") || track.mediaUri.startsWith("https://")) {
            Uri.parse(track.mediaUri)
        } else {
            Uri.fromFile(File(track.mediaUri))
        }

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _playbackState.value.currentTrack != null) {
                _playbackState.value.currentTrack?.let { playTrack(it, _playbackState.value.queue) }
            } else {
                exoPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(1000L))
        _playbackState.value = _playbackState.value.copy(currentPositionMs = clamped)
        exoPlayer.seekTo(clamped)
    }

    fun seekBy(deltaMs: Long) {
        val newPos = (_playbackState.value.currentPositionMs + deltaMs).coerceIn(
            0L,
            _playbackState.value.durationMs.coerceAtLeast(1000L)
        )
        seekTo(newPos)
    }

    fun playNext() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        var nextIndex = state.queueIndex + 1
        if (nextIndex >= state.queue.size) {
            if (state.repeatMode == RepeatMode.ALL) {
                nextIndex = 0
            } else {
                return
            }
        }
        val nextTrack = state.queue[nextIndex]
        playTrack(nextTrack, state.queue)
    }

    fun playPrevious() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        // If played more than 3 seconds, replay current song first
        if (exoPlayer.currentPosition > 3000) {
            seekTo(0)
            return
        }

        var prevIndex = state.queueIndex - 1
        if (prevIndex < 0) {
            prevIndex = if (state.repeatMode == RepeatMode.ALL) state.queue.size - 1 else 0
        }
        val prevTrack = state.queue[prevIndex]
        playTrack(prevTrack, state.queue)
    }

    private fun handleTrackEnded() {
        val state = _playbackState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                exoPlayer.play()
            }
            RepeatMode.ALL -> playNext()
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    playNext()
                } else {
                    exoPlayer.pause()
                    seekTo(0)
                }
            }
        }
    }

    fun toggleShuffle() {
        val current = _playbackState.value.isShuffle
        val newShuffle = !current
        val state = _playbackState.value
        val newQueue = if (newShuffle) {
            state.queue.shuffled()
        } else {
            state.queue
        }
        val newIndex = state.currentTrack?.let { curr -> newQueue.indexOfFirst { it.id == curr.id } } ?: 0
        _playbackState.value = state.copy(
            isShuffle = newShuffle,
            queue = newQueue,
            queueIndex = newIndex
        )
    }

    fun cycleRepeatMode() {
        val current = _playbackState.value.repeatMode
        val next = when (current) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = next)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 2.5f)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = clamped)
        exoPlayer.playbackParameters = PlaybackParameters(clamped)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _playbackState.value = _playbackState.value.copy(volume = clamped)
        exoPlayer.volume = clamped
    }

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return
        var remaining = minutes * 60L
        _sleepTimerSeconds.value = remaining
        sleepTimerJob = scope.launch {
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerSeconds.value = remaining
            }
            if (remaining <= 0) {
                exoPlayer.pause()
                _sleepTimerSeconds.value = 0L
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSeconds.value = 0L
    }

    fun release() {
        cancelSleepTimer()
        stopProgressTracker()
        audioEffectController.release()
        exoPlayer.release()
    }
}
