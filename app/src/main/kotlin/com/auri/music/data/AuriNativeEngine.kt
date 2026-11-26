package com.auri.music.data

import android.util.Log
import com.auri.music.model.Lyrics
import com.auri.music.model.LyricsLine
import com.auri.music.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer

data class ResolvedStream(
    val url: String,
    val durationMs: Long,
    val isOpus: Boolean,
    val loudnessDb: Double?,
    val audioQuality: String,
    val itag: Int = 0,
    val mimeType: String = "",
    val codecs: String = "",
    val bitrate: Long = 0L,
    val sampleRate: Int = 48000,
    val contentLength: Long = 0L,
    val expiresInSeconds: Long = 21600L
)

object AuriNativeEngine {
    private const val TAG = "AuriNativeEngine"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("auri_core")
            isLoaded = true
            logInfo("Native AURI Rust core loaded successfully: ${nativeGetVersion()}")
        } catch (e: UnsatisfiedLinkError) {
            logWarn("Native libauri_core not loaded on current platform: ${e.message}")
            isLoaded = false
        } catch (t: Throwable) {
            logWarn("Unexpected error loading native library: ${t.message}")
            isLoaded = false
        }
    }

    private fun logInfo(msg: String) {
        try {
            Log.i(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun logWarn(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (_: Throwable) {
            System.err.println("[$TAG] $msg")
        }
    }

    private fun logError(msg: String, t: Throwable? = null) {
        try {
            Log.e(TAG, msg, t)
        } catch (_: Throwable) {
            System.err.println("[$TAG] $msg: ${t?.message}")
        }
    }

    val isAvailable: Boolean get() = isLoaded

    external fun nativeGetVersion(): String
    external fun nativeCalculateLoudnessGain(loudnessDb: Double): Double
    external fun nativeSearch(query: String): String
    external fun nativeResolveStream(videoId: String): String
    external fun nativeResolveStreamWithLang(videoId: String, preferredLang: String): String
    external fun nativeGetLyrics(videoId: String, title: String, artist: String, durationSec: Long): String
    external fun nativeDspCreate(sampleRate: Int): Long
    external fun nativeDspDestroy(handle: Long)
    external fun nativeDspReset(handle: Long)
    external fun nativeDspConfigure(
        handle: Long,
        eqGains: FloatArray,
        preampDb: Float,
        autoHeadroom: Boolean,
        loudnessGainDb: Double,
        normalizerEnabled: Boolean
    )
    external fun nativeDspProcessPcm16(
        handle: Long,
        directBuf: ByteBuffer,
        byteOffset: Int,
        byteLen: Int,
        channels: Int,
        sampleRate: Int
    )
    external fun nativeDspProcessPcmFloat(
        handle: Long,
        directBuf: ByteBuffer,
        byteOffset: Int,
        byteLen: Int,
        channels: Int,
        sampleRate: Int
    )

    fun dspCreate(sampleRate: Int): Long = if (isLoaded) try { nativeDspCreate(sampleRate) } catch (_: Throwable) { 0L } else 0L
    fun dspDestroy(handle: Long) { if (isLoaded && handle != 0L) try { nativeDspDestroy(handle) } catch (_: Throwable) {} }
    fun dspReset(handle: Long) { if (isLoaded && handle != 0L) try { nativeDspReset(handle) } catch (_: Throwable) {} }
    fun dspConfigure(
        handle: Long,
        eqGains: FloatArray,
        preampDb: Float,
        autoHeadroom: Boolean,
        loudnessGainDb: Double,
        normalizerEnabled: Boolean
    ) {
        if (isLoaded && handle != 0L) {
            try {
                nativeDspConfigure(handle, eqGains, preampDb, autoHeadroom, loudnessGainDb, normalizerEnabled)
            } catch (_: Throwable) {}
        }
    }

    fun dspProcessPcm16(handle: Long, buf: ByteBuffer, offset: Int, len: Int, channels: Int, sampleRate: Int) {
        if (isLoaded && handle != 0L && buf.isDirect) {
            try {
                nativeDspProcessPcm16(handle, buf, offset, len, channels, sampleRate)
            } catch (_: Throwable) {}
        }
    }

    fun dspProcessPcmFloat(handle: Long, buf: ByteBuffer, offset: Int, len: Int, channels: Int, sampleRate: Int) {
        if (isLoaded && handle != 0L && buf.isDirect) {
            try {
                nativeDspProcessPcmFloat(handle, buf, offset, len, channels, sampleRate)
            } catch (_: Throwable) {}
        }
    }

    fun getVersion(): String {
        return if (isLoaded) {
            try {
                nativeGetVersion()
            } catch (e: Throwable) {
                "AURI Core v2.5.2 (Rust / Innertube / -14.0 LUFS)"
            }
        } else {
            "AURI Core (Embedded Architecture)"
        }
    }

    fun calculateLoudnessGain(loudnessDb: Double?): Double {
        return if (isLoaded) {
            try {
                nativeCalculateLoudnessGain(loudnessDb ?: Double.NaN)
            } catch (e: Throwable) {
                fallbackLoudness(loudnessDb)
            }
        } else {
            fallbackLoudness(loudnessDb)
        }
    }

    private fun fallbackLoudness(loudnessDb: Double?): Double {
        return when {
            loudnessDb != null && loudnessDb.isFinite() -> (-loudnessDb).coerceIn(-24.0, 3.0)
            else -> -4.5
        }
    }

    fun search(query: String): List<Song> {
        if (!isLoaded) return emptyList()
        return try {
            val jsonStr = nativeSearch(query)
            val jsonArray = JSONArray(jsonStr)
            val results = ArrayList<Song>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "")
                if (id.isEmpty()) continue
                val durStr = obj.optString("duration", "3:30")
                val durMs = obj.optLong("duration_ms", 210000L)
                results.add(
                    Song(
                        id = id,
                        title = obj.optString("title", "Unknown Title"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", "YouTube Music"),
                        duration = durStr,
                        durationMs = durMs,
                        thumbnailUrl = obj.optString("thumbnail_url", ""),
                        streamUrl = "",
                        isOpus = true,
                        audioQuality = "OPUS 160 kbps • 48 kHz",
                        loudnessDb = null,
                        isLiked = false
                    )
                )
            }
            results
        } catch (e: Throwable) {
            logError("Error executing native search for query: $query", e)
            emptyList()
        }
    }

    fun resolveStream(videoId: String, preferredLang: String = ""): ResolvedStream? {
        if (!isLoaded) return null
        return try {
            val jsonStr = if (preferredLang.isBlank()) {
                nativeResolveStream(videoId)
            } else {
                nativeResolveStreamWithLang(videoId, preferredLang)
            }
            val obj = JSONObject(jsonStr)
            if (obj.has("url")) {
                ResolvedStream(
                    url = obj.getString("url"),
                    durationMs = obj.optLong("duration_ms", 0L),
                    isOpus = obj.optBoolean("is_opus", true),
                    loudnessDb = if (obj.has("loudness_db") && !obj.isNull("loudness_db")) obj.getDouble("loudness_db") else null,
                    audioQuality = obj.optString("audio_quality", "OPUS 160 kbps • 48 kHz"),
                    itag = obj.optInt("itag", 0),
                    mimeType = obj.optString("mime_type", ""),
                    codecs = obj.optString("codecs", ""),
                    bitrate = obj.optLong("bitrate", 0L),
                    sampleRate = obj.optInt("sample_rate", 48000),
                    contentLength = obj.optLong("content_length", 0L),
                    expiresInSeconds = obj.optLong("expires_in_seconds", 21600L)
                )
            } else {
                null
            }
        } catch (e: Throwable) {
            logError("Error resolving stream for $videoId", e)
            null
        }
    }

    fun getLyrics(videoId: String, title: String, artist: String, durationSec: Long): Lyrics? {
        if (!isLoaded) return null
        return try {
            val jsonStr = nativeGetLyrics(videoId, title, artist, durationSec)
            val obj = JSONObject(jsonStr)
            if (obj.has("synced")) {
                val syncedArray = obj.getJSONArray("synced")
                val lines = ArrayList<LyricsLine>()
                for (i in 0 until syncedArray.length()) {
                    val lineObj = syncedArray.getJSONObject(i)
                    lines.add(
                        LyricsLine(
                            timestampMs = lineObj.getLong("timestamp_ms"),
                            text = lineObj.getString("text")
                        )
                    )
                }
                Lyrics(
                    syncedLines = lines,
                    plainText = obj.optString("plain", ""),
                    source = obj.optString("source", "AURI Rust Engine")
                )
            } else null
        } catch (e: Throwable) {
            logError("Error fetching lyrics for $videoId", e)
            null
        }
    }
}
