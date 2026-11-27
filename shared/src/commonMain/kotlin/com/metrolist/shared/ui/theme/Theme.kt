package com.metrolist.shared.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.metrolist.shared.ui.haptics.MetrolistHapticsProvider

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF7C3AED),
        onPrimary = Color.White,
        secondary = Color(0xFF0891B2),
        onSecondary = Color.White,
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color.White,
        onSurface = Color(0xFF0F172A),
        error = Color(0xFFEF4444),
        onError = Color.White,
    )

private val DarkColors =
    darkColorScheme(
        primary = CosmicElectricViolet,
        onPrimary = Color.White,
        primaryContainer = CosmicDarkObsidianElevated,
        onPrimaryContainer = CosmicElectricViolet,
        secondary = CosmicCyberCyan,
        onSecondary = Color.Black,
        secondaryContainer = CosmicDarkObsidianElevated,
        onSecondaryContainer = CosmicCyberCyan,
        tertiary = CosmicCyberCyan,
        onTertiary = Color.Black,
        background = CosmicPureBlack,
        onBackground = CosmicOnVoid,
        surface = CosmicPureBlack,
        onSurface = CosmicOnVoid,
        surfaceDim = CosmicPureBlack,
        surfaceBright = CosmicDarkObsidian,
        surfaceContainerLowest = CosmicPureBlack,
        surfaceContainerLow = Color(0xFF080614),
        surfaceContainer = CosmicDarkObsidian,
        surfaceContainerHigh = CosmicDarkObsidianElevated,
        surfaceContainerHighest = CosmicDarkObsidianHighest,
        surfaceVariant = CosmicDarkObsidian,
        onSurfaceVariant = CosmicOnObsidianVariant,
        outline = Color(0xFF332A55),
        outlineVariant = CosmicFrostedBorder,
        error = Color(0xFFF87171),
        onError = Color.Black,
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MetrolistTheme(
    useDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) DarkColors else LightColors

    val motionScheme = remember { MotionScheme.expressive() }
    MaterialExpressiveTheme(
        colorScheme = animateColorScheme(colors, motionScheme),
        motionScheme = motionScheme,
        typography = Typography(),
        content = { MetrolistHapticsProvider(content) },
    )
}
