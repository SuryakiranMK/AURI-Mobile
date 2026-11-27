/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.metrolist.shared.ui.theme.CosmicElectricViolet
import com.metrolist.shared.ui.theme.CosmicPureBlack
import com.metrolist.shared.ui.theme.artworkScrimBrush
import com.metrolist.shared.ui.utils.nativeBlurCompat

/**
 * Hardware-accelerated frosted ambient artwork blur rendered at the top of the Home screen.
 *
 * Samples the playing song's thumbnail (or top recommendation), renders a downscaled 128x128
 * texture blurred with hardware acceleration (nativeBlurCompat + blur) and smooth crossfade,
 * and seamlessly blends into pure OLED Black (#05030E) via a smoothstep vertical scrim.
 */
@Composable
fun HomeFrostedArtworkBlur(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current

    Box(
        modifier =
            modifier
                .clipToBounds(),
    ) {
        // Fallback celestial ambient glow when artwork is not yet available or loading
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    CosmicElectricViolet.copy(alpha = 0.40f),
                                    Color(0xFF0E0B1F).copy(alpha = 0.65f),
                                    CosmicPureBlack,
                                ),
                        ),
                    ),
        )

        // Frosted artwork blur with smooth crossfade
        if (!thumbnailUrl.isNullOrBlank()) {
            Crossfade(
                targetState = thumbnailUrl,
                animationSpec = tween(600),
                modifier = Modifier.fillMaxSize(),
            ) { url ->
                val request =
                    remember(url, context) {
                        ImageRequest
                            .Builder(context)
                            .data(url)
                            .size(128, 128)
                            .precision(Precision.INEXACT)
                            .crossfade(500)
                            .build()
                    }

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nativeBlurCompat(radiusPx = 80f, offscreen = true)
                            .blur(radius = 42.dp),
                ) {
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Ambient tone-down overlay: soft gradient that preserves top artwork brilliance while ensuring card legibility
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops =
                                arrayOf(
                                    0.0f to Color.Transparent,
                                    0.45f to CosmicPureBlack.copy(alpha = 0.40f),
                                    0.80f to CosmicPureBlack.copy(alpha = 0.85f),
                                    1.0f to CosmicPureBlack,
                                ),
                        ),
                    ),
        )

        // Smoothstep vertical scrim at the bottom fading 100% cleanly into OLED Black #05030E
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .background(artworkScrimBrush(CosmicPureBlack)),
        )
    }
}
