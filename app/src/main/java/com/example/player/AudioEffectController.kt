package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEffectController {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentAudioSessionId: Int = 0

    private val _settings = MutableStateFlow(EqualizerSettings())
    val settings: StateFlow<EqualizerSettings> = _settings.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentAudioSessionId) return
        release()
        currentAudioSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _settings.value.isEnabled
            }
            equalizer = eq

            val minLevel = eq.bandLevelRange[0].toInt()
            val maxLevel = eq.bandLevelRange[1].toInt()
            val numBands = eq.numberOfBands.toInt()

            val bands = mutableListOf<EqualizerBand>()
            for (i in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // mHz to Hz
                val currentLevel = eq.getBandLevel(i.toShort()).toInt()
                bands.add(
                    EqualizerBand(
                        index = i,
                        centerFreqHz = centerFreq,
                        minLevelMilliBels = minLevel,
                        maxLevelMilliBels = maxLevel,
                        currentLevelMilliBels = currentLevel
                    )
                )
            }

            try {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = _settings.value.isEnabled
                    setStrength(_settings.value.bassBoostStrength.toShort())
                }
            } catch (e: Exception) {
                Log.w("AudioEffect", "BassBoost unavailable: ${e.message}")
            }

            try {
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = _settings.value.isEnabled
                    setStrength(_settings.value.virtualizerStrength.toShort())
                }
            } catch (e: Exception) {
                Log.w("AudioEffect", "Virtualizer unavailable: ${e.message}")
            }

            _settings.value = _settings.value.copy(bands = bands)
        } catch (e: Exception) {
            Log.w("AudioEffect", "Equalizer init error: ${e.message}")
            // Fallback default 5 bands if hardware equalizer couldn't be initialized
            val fallbackBands = listOf(
                EqualizerBand(0, 60, -1500, 1500, 0),
                EqualizerBand(1, 230, -1500, 1500, 0),
                EqualizerBand(2, 910, -1500, 1500, 0),
                EqualizerBand(3, 3600, -1500, 1500, 0),
                EqualizerBand(4, 14000, -1500, 1500, 0)
            )
            _settings.value = _settings.value.copy(bands = fallbackBands)
        }
    }

    fun setEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.w("AudioEffect", "Failed to toggle enabled: ${e.message}")
        }
    }

    fun setBandLevel(bandIndex: Int, levelMilliBels: Int) {
        val currentBands = _settings.value.bands.toMutableList()
        if (bandIndex in currentBands.indices) {
            val updated = currentBands[bandIndex].copy(currentLevelMilliBels = levelMilliBels)
            currentBands[bandIndex] = updated
            _settings.value = _settings.value.copy(
                bands = currentBands,
                activePresetName = "Custom"
            )
            try {
                equalizer?.setBandLevel(bandIndex.toShort(), levelMilliBels.toShort())
            } catch (e: Exception) {
                Log.w("AudioEffect", "Failed to set band level: ${e.message}")
            }
        }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _settings.value = _settings.value.copy(bassBoostStrength = clamped)
        try {
            bassBoost?.setStrength(clamped.toShort())
        } catch (e: Exception) {
            Log.w("AudioEffect", "Failed to set bass boost: ${e.message}")
        }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _settings.value = _settings.value.copy(virtualizerStrength = clamped)
        try {
            virtualizer?.setStrength(clamped.toShort())
        } catch (e: Exception) {
            Log.w("AudioEffect", "Failed to set virtualizer: ${e.message}")
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        val currentBands = _settings.value.bands.toMutableList()
        val updatedBands = currentBands.mapIndexed { idx, band ->
            val presetLevel = preset.bandLevels.getOrNull(idx) ?: 0
            val clampedLevel = presetLevel.coerceIn(band.minLevelMilliBels, band.maxLevelMilliBels)
            try {
                equalizer?.setBandLevel(idx.toShort(), clampedLevel.toShort())
            } catch (_: Exception) {}
            band.copy(currentLevelMilliBels = clampedLevel)
        }
        _settings.value = _settings.value.copy(
            bands = updatedBands,
            activePresetName = preset.name
        )
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentAudioSessionId = 0
    }
}
