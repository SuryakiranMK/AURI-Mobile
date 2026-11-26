package com.auri.music.player

import com.auri.music.model.EqPreset
import kotlin.math.abs

/**
 * Hardware audio offload saves battery when no software DSP/EQ/crossfade processing is needed.
 */
data class AudioOffloadEligibility(
    val eqPreset: EqPreset,
    val customGains: FloatArray = FloatArray(10),
    val preampDb: Float = 0f,
    val normalizerEnabled: Boolean = true,
    val crossfadeActive: Boolean = false,
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f
) {
    val eqIsFlat: Boolean
        get() = eqPreset == EqPreset.FLAT && abs(preampDb) < 0.05f &&
            customGains.all { abs(it) < 0.05f }

    val isEligible: Boolean
        get() = eqIsFlat && !normalizerEnabled && !crossfadeActive &&
            abs(speed - 1.0f) < 0.01f && abs(pitch - 1.0f) < 0.01f
}
