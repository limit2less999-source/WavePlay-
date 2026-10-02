package com.example.player

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val minLevelMilliBels: Int = -1500, // -15 dB
    val maxLevelMilliBels: Int = 1500,  // +15 dB
    val currentLevelMilliBels: Int = 0  // 0 dB
) {
    val displayFreq: String
        get() = if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000}k"
        } else {
            "${centerFreqHz}Hz"
        }

    val displayDb: String
        get() = "${if (currentLevelMilliBels > 0) "+" else ""}${currentLevelMilliBels / 100} dB"
}

data class EqualizerPreset(
    val name: String,
    val bandLevels: List<Int> // dB values or milliBels
)

data class EqualizerSettings(
    val isEnabled: Boolean = true,
    val bands: List<EqualizerBand> = emptyList(),
    val bassBoostStrength: Int = 300, // 0 to 1000
    val virtualizerStrength: Int = 200, // 0 to 1000
    val activePresetName: String = "Flat"
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            EqualizerPreset("Flat", listOf(0, 0, 0, 0, 0)),
            EqualizerPreset("Bass Boost", listOf(800, 600, 200, 0, 0)),
            EqualizerPreset("Vocal Booster", listOf(-200, 300, 800, 600, 200)),
            EqualizerPreset("Rock", listOf(600, 300, -100, 400, 700)),
            EqualizerPreset("Pop", listOf(-100, 400, 700, 300, -100)),
            EqualizerPreset("Electronic", listOf(700, 500, 0, 400, 800)),
            EqualizerPreset("Jazz", listOf(300, 200, -200, 300, 500)),
            EqualizerPreset("Acoustic", listOf(400, 300, 200, 500, 400))
        )
    }
}
