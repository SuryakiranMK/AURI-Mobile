/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.shared.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Vertical scrim from [from] to [to] using smoothstep easing.
 *
 * Smoothstep easing ensures that the curve's derivative is zero at both ends,
 * preventing visible sharp edges or banding where the gradient begins and ends.
 * Interpolating colors directly prevents Skia un-premultiplied alpha grey bands.
 */
fun smoothScrimBrush(
    from: Color,
    to: Color,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    steps: Int = 24,
): Brush =
    Brush.verticalGradient(
        colorStops =
            Array(steps + 1) { i ->
                val t = i / steps.toFloat()
                val position = startFraction + (endFraction - startFraction) * t
                val eased = t * t * (3f - 2f * t)
                position to lerp(from, to, eased)
            },
    )

/**
 * Artwork scrim brush that smoothly transitions from transparent to [color] (default CosmicPureBlack #05030E).
 */
fun artworkScrimBrush(
    color: Color = CosmicPureBlack,
    steps: Int = 24,
): Brush = smoothScrimBrush(from = color.copy(alpha = 0f), to = color, steps = steps)
