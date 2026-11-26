package com.auri.music.player

import com.auri.music.model.RepeatMode
import com.auri.music.model.Song

/**
 * Quintic S-curve crossfade helper (modeled after Metrolist) that preserves gapless same-album transitions.
 */
object CrossfadeManager {

    /**
     * Quintic S-curve: t^3 * (t * (6t - 15) + 10) for smooth equal-loudness transitions.
     */
    fun quinticCurve(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * x * (x * (6f * x - 15f) + 10f)
    }

    fun shouldCrossfade(
        crossfadeEnabled: Boolean,
        currentSong: Song?,
        nextSong: Song?,
        repeatMode: RepeatMode
    ): Boolean {
        if (!crossfadeEnabled) return false
        if (repeatMode == RepeatMode.ONE) return false
        if (currentSong == null || nextSong == null) return false
        // Preserve gapless playback when both tracks share the same non-empty album
        if (currentSong.album.isNotBlank() && currentSong.album == nextSong.album) return false
        return true
    }
}
