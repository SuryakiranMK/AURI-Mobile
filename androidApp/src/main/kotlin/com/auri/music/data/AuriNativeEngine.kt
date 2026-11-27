package com.auri.music.data

import android.util.Log
import java.nio.ByteBuffer

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

    val isAvailable: Boolean get() = isLoaded

    external fun nativeGetVersion(): String

    external fun nativeCalculateLoudnessGain(loudnessDb: Double): Double

    external fun nativeSearch(query: String): String

    external fun nativeResolveStream(videoId: String): String

    external fun nativeResolveStreamWithLang(
        videoId: String,
        preferredLang: String,
    ): String

    external fun nativeGetLyrics(
        videoId: String,
        title: String,
        artist: String,
        durationSec: Long,
    ): String

    external fun nativeDspCreate(sampleRate: Int): Long

    external fun nativeDspDestroy(handle: Long)

    external fun nativeDspReset(handle: Long)

    external fun nativeDspConfigure(
        handle: Long,
        eqGains: FloatArray,
        preampDb: Float,
        autoHeadroom: Boolean,
        loudnessGainDb: Double,
        normalizerEnabled: Boolean,
    )

    external fun nativeDspProcessPcm16(
        handle: Long,
        directBuf: ByteBuffer,
        byteOffset: Int,
        byteLen: Int,
        channels: Int,
        sampleRate: Int,
    )

    external fun nativeDspProcessPcmFloat(
        handle: Long,
        directBuf: ByteBuffer,
        byteOffset: Int,
        byteLen: Int,
        channels: Int,
        sampleRate: Int,
    )

    fun dspCreate(sampleRate: Int): Long =
        if (isLoaded) {
            try {
                nativeDspCreate(sampleRate)
            } catch (_: Throwable) {
                0L
            }
        } else {
            0L
        }

    fun dspDestroy(handle: Long) {
        if (isLoaded && handle != 0L) {
            try {
                nativeDspDestroy(handle)
            } catch (_: Throwable) {
            }
        }
    }

    fun dspReset(handle: Long) {
        if (isLoaded && handle != 0L) {
            try {
                nativeDspReset(handle)
            } catch (_: Throwable) {
            }
        }
    }

    fun dspConfigure(
        handle: Long,
        eqGains: FloatArray,
        preampDb: Float,
        autoHeadroom: Boolean,
        loudnessGainDb: Double,
        normalizerEnabled: Boolean,
    ) {
        if (isLoaded && handle != 0L) {
            try {
                nativeDspConfigure(handle, eqGains, preampDb, autoHeadroom, loudnessGainDb, normalizerEnabled)
            } catch (_: Throwable) {
            }
        }
    }

    fun dspProcessPcm16(
        handle: Long,
        buf: ByteBuffer,
        offset: Int,
        len: Int,
        channels: Int,
        sampleRate: Int,
    ) {
        if (isLoaded && handle != 0L && buf.isDirect) {
            try {
                nativeDspProcessPcm16(handle, buf, offset, len, channels, sampleRate)
            } catch (_: Throwable) {
            }
        }
    }

    fun dspProcessPcmFloat(
        handle: Long,
        buf: ByteBuffer,
        offset: Int,
        len: Int,
        channels: Int,
        sampleRate: Int,
    ) {
        if (isLoaded && handle != 0L && buf.isDirect) {
            try {
                nativeDspProcessPcmFloat(handle, buf, offset, len, channels, sampleRate)
            } catch (_: Throwable) {
            }
        }
    }

    fun getVersion(): String =
        if (isLoaded) {
            try {
                nativeGetVersion()
            } catch (e: Throwable) {
                "AURI Core v2.5.2 (Rust / Innertube / -14.0 LUFS)"
            }
        } else {
            "AURI Core (Embedded Architecture)"
        }

    fun calculateLoudnessGain(loudnessDb: Double?): Double =
        if (isLoaded) {
            try {
                nativeCalculateLoudnessGain(loudnessDb ?: Double.NaN)
            } catch (e: Throwable) {
                fallbackLoudness(loudnessDb)
            }
        } else {
            fallbackLoudness(loudnessDb)
        }

    private fun fallbackLoudness(loudnessDb: Double?): Double =
        when {
            loudnessDb != null && loudnessDb.isFinite() -> (-loudnessDb).coerceIn(-24.0, 3.0)
            else -> -4.5
        }
}
