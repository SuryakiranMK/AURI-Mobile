/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.Image
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Precision
import com.metrolist.shared.ui.theme.CosmicPureBlack
import com.metrolist.shared.ui.utils.nativeBlurCompat

internal data class LayerArt(
    val painter: Painter,
    val pixelWidth: Float?,
    val pixelHeight: Float?,
    val image: Image? = null,
)

@Composable
internal expect fun PlatformLiquidBackgroundCanvas(
    modifier: Modifier,
    activeArt: LayerArt,
    previousArt: LayerArt?,
    crossfadeAlpha: () -> Float,
    rotationTimeMs: () -> Long,
    isLightTheme: Boolean,
)

@Composable
fun LiquidBackground(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    isLyricsVisible: Boolean = false,
    animate: Boolean = true,
    isPlaying: Boolean = true,
    visible: Boolean = true,
) {
    val isLightTheme = MaterialTheme.colorScheme.surface.luminance() >= 0.5f
    val context = LocalPlatformContext.current

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .clipToBounds(),
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            Crossfade(
                targetState = artworkUrl,
                animationSpec = tween(600),
                modifier = Modifier.fillMaxSize(),
            ) { url ->
                val trackRequest =
                    remember(url, context) {
                        ImageRequest
                            .Builder(context)
                            .data(url)
                            .size(128, 128)
                            .precision(Precision.INEXACT)
                            .build()
                    }
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = trackRequest,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .nativeBlurCompat(radiusPx = 100f)
                                .blur(radius = 48.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            // Cosmic vignette scrim overlay (Color(0xFF05030E) fade)
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops =
                                    arrayOf(
                                        0.0f to CosmicPureBlack.copy(alpha = if (isLightTheme) 0.3f else 0.60f),
                                        0.25f to CosmicPureBlack.copy(alpha = if (isLightTheme) 0.1f else 0.20f),
                                        0.65f to CosmicPureBlack.copy(alpha = if (isLightTheme) 0.3f else 0.50f),
                                        1.0f to CosmicPureBlack.copy(alpha = if (isLightTheme) 0.6f else 0.95f),
                                    ),
                            ),
                        ),
            )

            // Radial cosmic vignette edge fade into OLED void
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        CosmicPureBlack.copy(alpha = if (isLightTheme) 0.15f else 0.35f),
                                        CosmicPureBlack.copy(alpha = if (isLightTheme) 0.45f else 0.75f),
                                        CosmicPureBlack,
                                    ),
                            ),
                        ),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(if (isLightTheme) Color.White else CosmicPureBlack),
            )
        }
    }
}
