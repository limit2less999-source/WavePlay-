package com.example.util

import android.content.Context
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object SampleAudioGenerator {

    suspend fun generateInitialTracksIfEmpty(context: Context): List<TrackEntity> = withContext(Dispatchers.IO) {
        val musicDir = File(context.filesDir, "offline_music")
        if (!musicDir.exists()) {
            musicDir.mkdirs()
        }

        val track1File = File(musicDir, "neon_horizon_synthwave.wav")
        val track2File = File(musicDir, "starlight_lofi_chill.wav")
        val track3File = File(musicDir, "cyber_pulse_electronic.wav")

        val generatedTracks = mutableListOf<TrackEntity>()

        if (!track1File.exists() || track1File.length() < 1000) {
            writeSynthesizedAudio(
                file = track1File,
                durationSeconds = 24,
                baseFreq = 130.81, // C3
                scaleType = ScaleType.SYNTHWAVE
            )
        }
        generatedTracks.add(
            TrackEntity(
                id = "demo_track_1",
                title = "Neon Horizon (Synthwave)",
                artist = "AuraTune Studios",
                album = "Cybernetic Nights",
                durationMs = 24_000L,
                mediaUri = track1File.absolutePath,
                isVideo = false,
                isFavorite = true,
                fileSizeBytes = track1File.length()
            )
        )

        if (!track2File.exists() || track2File.length() < 1000) {
            writeSynthesizedAudio(
                file = track2File,
                durationSeconds = 24,
                baseFreq = 146.83, // D3
                scaleType = ScaleType.LOFI
            )
        }
        generatedTracks.add(
            TrackEntity(
                id = "demo_track_2",
                title = "Starlight Ambient (Lo-Fi Chill)",
                artist = "Luna Acoustic",
                album = "Midnight Reverie",
                durationMs = 24_000L,
                mediaUri = track2File.absolutePath,
                isVideo = false,
                isFavorite = false,
                fileSizeBytes = track2File.length()
            )
        )

        if (!track3File.exists() || track3File.length() < 1000) {
            writeSynthesizedAudio(
                file = track3File,
                durationSeconds = 20,
                baseFreq = 110.0, // A2
                scaleType = ScaleType.ELECTRONIC
            )
        }
        generatedTracks.add(
            TrackEntity(
                id = "demo_track_3",
                title = "Cyber Pulse (Bass & Beats)",
                artist = "Echo District",
                album = "Voltage Protocol",
                durationMs = 20_000L,
                mediaUri = track3File.absolutePath,
                isVideo = false,
                isFavorite = false,
                fileSizeBytes = track3File.length()
            )
        )

        generatedTracks
    }

    private enum class ScaleType { SYNTHWAVE, LOFI, ELECTRONIC }

    private fun writeSynthesizedAudio(
        file: File,
        durationSeconds: Int,
        baseFreq: Double,
        scaleType: ScaleType
    ) {
        val sampleRate = 22050
        val numChannels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSeconds
        val bytesPerSample = bitsPerSample / 8
        val dataSize = totalSamples * numChannels * bytesPerSample

        FileOutputStream(file).use { fos ->
            // Write temporary 44-byte WAV header
            val header = ByteArray(44)
            fos.write(header)

            val bufferSize = 4096
            val buffer = ByteBuffer.allocate(bufferSize * numChannels * bytesPerSample)
            buffer.order(ByteOrder.LITTLE_ENDIAN)

            val chords = when (scaleType) {
                ScaleType.SYNTHWAVE -> listOf(
                    listOf(baseFreq, baseFreq * 1.2599, baseFreq * 1.4983),       // Major triad
                    listOf(baseFreq * 0.8909, baseFreq * 1.1225, baseFreq * 1.3348),
                    listOf(baseFreq * 0.7937, baseFreq, baseFreq * 1.1892),
                    listOf(baseFreq * 0.8909, baseFreq * 1.1225, baseFreq * 1.3348)
                )
                ScaleType.LOFI -> listOf(
                    listOf(baseFreq, baseFreq * 1.1892, baseFreq * 1.4983, baseFreq * 1.7818), // Min7
                    listOf(baseFreq * 1.1225, baseFreq * 1.3348, baseFreq * 1.6818, baseFreq * 2.0),
                    listOf(baseFreq * 0.8909, baseFreq * 1.1225, baseFreq * 1.4142, baseFreq * 1.6818),
                    listOf(baseFreq * 0.7492, baseFreq * 0.8909, baseFreq * 1.1225, baseFreq * 1.3348)
                )
                ScaleType.ELECTRONIC -> listOf(
                    listOf(baseFreq, baseFreq * 1.5, baseFreq * 2.0),
                    listOf(baseFreq * 1.0595, baseFreq * 1.4142, baseFreq * 1.8877),
                    listOf(baseFreq * 0.8909, baseFreq * 1.3348, baseFreq * 1.7818),
                    listOf(baseFreq * 1.1892, baseFreq * 1.5874, baseFreq * 2.1189)
                )
            }

            val chordDuration = sampleRate * 3 // Change chord every 3 seconds
            var sampleIndex = 0

            while (sampleIndex < totalSamples) {
                buffer.clear()
                val chunk = minOf(bufferSize, totalSamples - sampleIndex)

                for (i in 0 until chunk) {
                    val currentSample = sampleIndex + i
                    val chordIdx = (currentSample / chordDuration) % chords.size
                    val activeNotes = chords[chordIdx]

                    val t = currentSample.toDouble() / sampleRate

                    // Bass & Chord synthesis
                    var signalLeft = 0.0
                    var signalRight = 0.0

                    // Chord voices
                    for ((noteIdx, freq) in activeNotes.withIndex()) {
                        val pan = if (noteIdx % 2 == 0) 0.7 else 0.3
                        val tone = sin(2 * PI * freq * t) * 0.22
                        val harmonic = sin(4 * PI * freq * t) * 0.08
                        signalLeft += (tone + harmonic) * pan
                        signalRight += (tone + harmonic) * (1.0 - pan)
                    }

                    // Bass tone
                    val bassFreq = activeNotes.first() * 0.5
                    val bass = sin(2 * PI * bassFreq * t) * 0.35
                    signalLeft += bass
                    signalRight += bass

                    // Rhythmic kick / percussion beat
                    val beatPos = (currentSample % (sampleRate / 2)).toDouble() / (sampleRate / 2)
                    val kickEnvelope = (1.0 - beatPos).coerceIn(0.0, 1.0)
                    val kick = sin(2 * PI * (60.0 + 80.0 * kickEnvelope) * t) * (kickEnvelope * kickEnvelope) * 0.35
                    signalLeft += kick
                    signalRight += kick

                    // Soft master fade in / out
                    val fadeIn = (currentSample.toDouble() / (sampleRate * 2)).coerceIn(0.0, 1.0)
                    val fadeOut = ((totalSamples - currentSample).toDouble() / (sampleRate * 2)).coerceIn(0.0, 1.0)
                    val masterVol = 0.75 * fadeIn * fadeOut

                    val sampleL = ((signalLeft * masterVol).coerceIn(-0.95, 0.95) * 32767).toInt().toShort()
                    val sampleR = ((signalRight * masterVol).coerceIn(-0.95, 0.95) * 32767).toInt().toShort()

                    buffer.putShort(sampleL)
                    buffer.putShort(sampleR)
                }

                fos.write(buffer.array(), 0, chunk * numChannels * bytesPerSample)
                sampleIndex += chunk
            }
        }

        // Fill real WAV header
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(0)
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            val byteRate = sampleRate * numChannels * bytesPerSample
            val blockAlign = numChannels * bytesPerSample

            header.put("RIFF".toByteArray())
            header.putInt(dataSize + 36) // Total file length - 8
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // Subchunk1Size for PCM
            header.putShort(1) // AudioFormat 1 = PCM
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign.toShort())
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray())
            header.putInt(dataSize)

            raf.write(header.array())
        }
    }
}
