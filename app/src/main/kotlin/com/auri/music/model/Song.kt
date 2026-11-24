package com.auri.music.model

import java.io.Serializable

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: String,
    val durationMs: Long,
    val thumbnailUrl: String,
    val streamUrl: String = "",
    val isOpus: Boolean = true,
    val audioQuality: String = "OPUS 160 kbps • HIGH RES",
    val loudnessDb: Double? = null,
    val isLiked: Boolean = false
) : Serializable

enum class RepeatMode {
    OFF, ALL, ONE
}

data class LyricsLine(
    val timestampMs: Long,
    val text: String
)

data class Lyrics(
    val syncedLines: List<LyricsLine> = emptyList(),
    val plainText: String = "",
    val source: String = "YouTube Music / LRCLIB"
)

enum class EqPreset(val label: String, val gains: FloatArray) {
    FLAT("Audiophile Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    BASS_PUNCH("Deep Bass Punch", floatArrayOf(5.5f, 6.0f, 4.0f, 2.0f, 0.5f, -0.5f, 0.0f, 1.0f, 2.0f, 2.0f)),
    VOCAL_CLARITY("Vocal Clarity", floatArrayOf(-2.0f, -2.5f, -1.0f, 1.0f, 2.5f, 4.5f, 3.5f, 2.0f, 1.0f, 0.5f)),
    CINEMATIC("Cinematic Dynamic", floatArrayOf(6.0f, 4.5f, 2.0f, 0.0f, -1.0f, 0.5f, 2.0f, 3.5f, 4.5f, 4.0f)),
    CUSTOM("Custom Profile", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f));

    override fun toString(): String = label
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val isBuffering: Boolean = false,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val volume: Float = 1.0f,
    val isLoudnessNormalizerEnabled: Boolean = true,
    val loudnessGainDb: Double = -7.0,
    val eqPreset: EqPreset = EqPreset.FLAT,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1
)
