package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.data.local.TrackEntity
import com.example.util.TimeUtils
import java.io.File

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "waveplay_media_playback"
        const val NOTIFICATION_ID = 1010
        const val ACTION_PLAY = "com.example.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.ACTION_NEXT"
        const val ACTION_PREV = "com.example.ACTION_PREV"
        const val ACTION_STOP = "com.example.ACTION_STOP"

        var activeSession: MediaSession? = null
        var activePlaybackManager: PlaybackManager? = null
        var activeService: PlaybackService? = null

        fun start(context: Context) {
            val intent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (_: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    var mediaSessionCompat: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        activeService = this
        createNotificationChannel()
        acquireWakeLock()
        initMediaSessionCompat()
    }

    private fun initMediaSessionCompat() {
        try {
            mediaSessionCompat = MediaSessionCompat(this, "WavePlaySession").apply {
                isActive = true
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        activePlaybackManager?.exoPlayer?.play()
                        updateNotification()
                    }

                    override fun onPause() {
                        activePlaybackManager?.exoPlayer?.pause()
                        updateNotification()
                    }

                    override fun onSkipToNext() {
                        activePlaybackManager?.playNext()
                        updateNotification()
                    }

                    override fun onSkipToPrevious() {
                        activePlaybackManager?.playPrevious()
                        updateNotification()
                    }

                    override fun onSeekTo(pos: Long) {
                        activePlaybackManager?.seekTo(pos)
                        updateNotification()
                    }

                    override fun onStop() {
                        activePlaybackManager?.exoPlayer?.stop()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                })
            }
        } catch (_: Exception) {}
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WavePlay::PlaybackWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WavePlay Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback and lock-screen playback controls with timeline"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val manager = activePlaybackManager

        if (action != null && manager != null) {
            when (action) {
                ACTION_PLAY -> manager.exoPlayer.play()
                ACTION_PAUSE -> manager.exoPlayer.pause()
                ACTION_NEXT -> manager.playNext()
                ACTION_PREV -> manager.playPrevious()
                ACTION_STOP -> {
                    manager.exoPlayer.stop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }

        updateNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    fun updateNotification() {
        val manager = activePlaybackManager ?: return
        val track = manager.playbackState.value.currentTrack
        val isPlaying = manager.playbackState.value.isPlaying
        val rawDurationMs = if (manager.exoPlayer.duration > 0) {
            manager.exoPlayer.duration
        } else {
            manager.playbackState.value.durationMs.coerceAtLeast(track?.durationMs ?: 0L)
        }
        val durationMs = if (rawDurationMs > 0) rawDurationMs else 180_000L
        val currentPositionMs = manager.exoPlayer.currentPosition.coerceAtLeast(0L).coerceAtMost(durationMs)

        val title = track?.title ?: "WavePlay"
        val artist = track?.artist ?: "Offline Music Player"
        val album = track?.album ?: "WavePlay Music"

        // Generate or fetch vibrant artwork so system notification doesn't fallback to "wired headphone" icon
        val artworkBitmap = getTrackArtworkBitmap(track, title)

        // 1. Sync state & duration with MediaSessionCompat for Android System Media Controller
        try {
            val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            val actions = PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_STOP

            val playbackState = PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, currentPositionMs, if (isPlaying) 1.0f else 0.0f, SystemClock.elapsedRealtime())
                .build()
            mediaSessionCompat?.setPlaybackState(playbackState)

            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artworkBitmap)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artworkBitmap)
                .build()
            mediaSessionCompat?.setMetadata(metadata)
        } catch (_: Exception) {}

        // 2. PendingIntents for actions
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_PREV }
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(this, PlaybackService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }
        val playPausePendingIntent = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, PlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // 3. Build Notification with Android MediaStyle
        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        mediaSessionCompat?.sessionToken?.let { token ->
            mediaStyle.setMediaSession(token)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(artworkBitmap)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("${TimeUtils.formatMs(currentPositionMs)} / ${TimeUtils.formatMs(durationMs)}")
            .setContentInfo(TimeUtils.formatMs(durationMs))
            .setProgress(durationMs.toInt(), currentPositionMs.toInt(), false)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", stopPendingIntent)
            .setStyle(mediaStyle)

        val notification = builder.build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {}
    }

    private fun getTrackArtworkBitmap(track: TrackEntity?, title: String): Bitmap {
        // Try decoding existing artUri if present
        if (!track?.artUri.isNullOrBlank()) {
            try {
                if (track!!.artUri.startsWith("content://")) {
                    contentResolver.openInputStream(Uri.parse(track.artUri))?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) return bmp
                    }
                } else {
                    val file = File(track!!.artUri)
                    if (file.exists()) {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) return bmp
                    }
                }
            } catch (_: Exception) {}
        }

        // Generate a vibrant studio album artwork bitmap (256x256)
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val baseColor = (track?.coverColorHex?.toInt() ?: 0xFF6366F1.toInt())
        val darkColor = AndroidColor.argb(255, 19, 16, 28)

        val shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            baseColor, darkColor, Shader.TileMode.CLAMP
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 32f, 32f, bgPaint)

        // Draw initial letter or music waveform emblem
        val initial = title.firstOrNull()?.uppercaseChar()?.toString() ?: "W"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            textSize = 96f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, yPos, textPaint)

        return bitmap
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return activeSession
    }

    override fun onDestroy() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
        try {
            mediaSessionCompat?.release()
        } catch (_: Exception) {}
        mediaSessionCompat = null
        activeSession = null
        activePlaybackManager = null
        activeService = null
        super.onDestroy()
    }
}
