package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.GeminiMusicService
import com.example.ai.MusicGenerationParams
import com.example.player.EqualizerSettings
import com.example.player.PlaybackState
import com.example.player.RepeatMode
import com.example.util.TimeUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WavePlay", appName)
    }

    @Test
    fun `time utils formatting tests`() {
        assertEquals("0:00", TimeUtils.formatMs(0L))
        assertEquals("1:15", TimeUtils.formatMs(75_000L))
        assertEquals("1:00:05", TimeUtils.formatMs(3_605_000L))
        assertTrue(TimeUtils.formatBytes(1024 * 1024 * 5).contains("MB"))
    }

    @Test
    fun `equalizer default presets are available`() {
        val presets = EqualizerSettings.DEFAULT_PRESETS
        assertTrue(presets.isNotEmpty())
        assertNotNull(presets.find { it.name == "Bass Boost" })
        assertNotNull(presets.find { it.name == "Rock" })
        assertNotNull(presets.find { it.name == "Flat" })
    }

    @Test
    fun `playback state defaults are correct`() {
        val state = PlaybackState()
        assertEquals(false, state.isPlaying)
        assertEquals(RepeatMode.OFF, state.repeatMode)
        assertEquals(1.0f, state.playbackSpeed, 0.01f)
    }

    @Test
    fun `gemini music service generates playable offline audio file`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var savedTrack: com.example.data.local.TrackEntity? = null
        val service = GeminiMusicService(context) { track ->
            savedTrack = track
        }

        val params = MusicGenerationParams(
            prompt = "Cyberpunk Synthwave Beat",
            genre = "Cyberpunk Synth",
            mood = "Energetic",
            bpm = 120,
            durationSeconds = 12,
            customTitle = "Neon Genesis"
        )

        val result = service.generateMusic(params)
        assertTrue(result.isSuccess)
        val track = result.getOrNull()
        assertNotNull(track)
        assertEquals("Neon Genesis", track?.title)
        assertNotNull(track?.mediaUri)
        val file = File(track!!.mediaUri)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
}
