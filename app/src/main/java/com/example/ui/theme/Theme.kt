package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String, val previewColor: Color) {
    CYBER_NEON("Cyber Neon", CyberPrimary),
    DEEP_VELVET("Deep Velvet", VelvetPrimary),
    SUNSET_GROOVE("Sunset Groove", SunsetPrimary),
    EMERALD_BASS("Emerald Bass", EmeraldPrimary),
    AMOLED_DARK("AMOLED Pure", AmoledPrimary)
}

fun getAuraColorScheme(themeMode: AppThemeMode): ColorScheme {
    return when (themeMode) {
        AppThemeMode.CYBER_NEON -> darkColorScheme(
            primary = CyberPrimary,
            onPrimary = Color.Black,
            secondary = CyberSecondary,
            onSecondary = Color.White,
            tertiary = CyberTertiary,
            background = CyberBackground,
            onBackground = Color(0xFFF1F5F9),
            surface = CyberSurface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = CyberSurfaceVariant,
            onSurfaceVariant = Color(0xFFCBD5E1)
        )
        AppThemeMode.DEEP_VELVET -> darkColorScheme(
            primary = VelvetPrimary,
            onPrimary = Color.Black,
            secondary = VelvetSecondary,
            onSecondary = Color.Black,
            tertiary = VelvetTertiary,
            background = VelvetBackground,
            onBackground = Color(0xFFFAF5FF),
            surface = VelvetSurface,
            onSurface = Color(0xFFFDF4FF),
            surfaceVariant = VelvetSurfaceVariant,
            onSurfaceVariant = Color(0xFFE9D5FF)
        )
        AppThemeMode.SUNSET_GROOVE -> darkColorScheme(
            primary = SunsetPrimary,
            onPrimary = Color.Black,
            secondary = SunsetSecondary,
            onSecondary = Color.White,
            tertiary = SunsetTertiary,
            background = SunsetBackground,
            onBackground = Color(0xFFFFF7ED),
            surface = SunsetSurface,
            onSurface = Color(0xFFFFEDD5),
            surfaceVariant = SunsetSurfaceVariant,
            onSurfaceVariant = Color(0xFFFED7AA)
        )
        AppThemeMode.EMERALD_BASS -> darkColorScheme(
            primary = EmeraldPrimary,
            onPrimary = Color.Black,
            secondary = EmeraldSecondary,
            onSecondary = Color.Black,
            tertiary = EmeraldTertiary,
            background = EmeraldBackground,
            onBackground = Color(0xFFECFDF5),
            surface = EmeraldSurface,
            onSurface = Color(0xFFD1FAE5),
            surfaceVariant = EmeraldSurfaceVariant,
            onSurfaceVariant = Color(0xFFA7F3D0)
        )
        AppThemeMode.AMOLED_DARK -> darkColorScheme(
            primary = AmoledPrimary,
            onPrimary = Color.Black,
            secondary = AmoledSecondary,
            onSecondary = Color.White,
            tertiary = AmoledTertiary,
            background = AmoledBackground,
            onBackground = Color(0xFFFFFFFF),
            surface = AmoledSurface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = AmoledSurfaceVariant,
            onSurfaceVariant = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun AuraTuneTheme(
    themeMode: AppThemeMode = AppThemeMode.CYBER_NEON,
    content: @Composable () -> Unit
) {
    val colorScheme = getAuraColorScheme(themeMode)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
