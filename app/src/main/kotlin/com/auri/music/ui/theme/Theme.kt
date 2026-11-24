package com.auri.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    primaryContainer = primaryContainerDark,
    secondary = secondaryDark,
    tertiary = tertiaryDark,
    background = backgroundDark,
    surface = surfaceDark,
    surfaceVariant = AuriGlassSurfaceElevated,
    onPrimary = AuriTextPrimary,
    onBackground = AuriTextPrimary,
    onSurface = AuriTextPrimary,
    onSurfaceVariant = AuriTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    primaryContainer = primaryContainerLight,
    secondary = secondaryLight,
    tertiary = tertiaryLight
)

@Composable
fun AURITheme(
    darkTheme: Boolean = true, // AURI is dark-first by design
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
