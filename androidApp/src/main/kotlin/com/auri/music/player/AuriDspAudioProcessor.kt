package com.auri.music.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.auri.music.data.AuriNativeEngine
import java.nio.ByteBuffer

/**
 * Media3 AudioProcessor routing decoded PCM directly into AURI's Rust AudioPipeline
 * (10-band ISO EQ + Auto-Headroom + -14.0 LUFS Loudness Normalizer + SoftLimiter).
 */
@OptIn(UnstableApi::class)
class AuriDspAudioProcessor : BaseAudioProcessor() {
    @Volatile
    private var nativeHandle: Long = 0L

    @Volatile
    private var eqGains: FloatArray = FloatArray(10)

    @Volatile
    private var preampDb: Float = 0f

    @Volatile
    private var autoHeadroomEnabled: Boolean = true

    @Volatile
    private var loudnessGainDb: Double = -4.5

    @Volatile
    private var normalizerEnabled: Boolean = true

    fun configureDsp(
        gains: FloatArray,
        preampDb: Float,
        autoHeadroom: Boolean,
        loudnessGainDb: Double,
        normalizerEnabled: Boolean,
    ) {
        this.eqGains = gains.copyOf(10)
        this.preampDb = preampDb
        this.autoHeadroomEnabled = autoHeadroom
        this.loudnessGainDb = loudnessGainDb
        this.normalizerEnabled = normalizerEnabled
        pushConfigToNative()
    }

    private fun pushConfigToNative() {
        val h = nativeHandle
        if (h != 0L) {
            AuriNativeEngine.dspConfigure(
                handle = h,
                eqGains = eqGains,
                preampDb = preampDb,
                autoHeadroom = autoHeadroomEnabled,
                loudnessGainDb = loudnessGainDb,
                normalizerEnabled = normalizerEnabled,
            )
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (nativeHandle == 0L) {
            nativeHandle = AuriNativeEngine.dspCreate(inputAudioFormat.sampleRate)
        }
        pushConfigToNative()
        return inputAudioFormat
    }

    override fun isActive(): Boolean = super.isActive()

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()

        val h = nativeHandle
        val isProcessingNeeded = normalizerEnabled || preampDb != 0f || eqGains.any { kotlin.math.abs(it) >= 0.05f }
        if (h != 0L && outputBuffer.isDirect && isProcessingNeeded) {
            val channels = inputAudioFormat.channelCount
            val sampleRate = inputAudioFormat.sampleRate
            when (inputAudioFormat.encoding) {
                C.ENCODING_PCM_16BIT -> {
                    AuriNativeEngine.dspProcessPcm16(h, outputBuffer, 0, remaining, channels, sampleRate)
                }

                C.ENCODING_PCM_FLOAT -> {
                    AuriNativeEngine.dspProcessPcmFloat(h, outputBuffer, 0, remaining, channels, sampleRate)
                }
            }
        }
    }

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        if (nativeHandle != 0L) {
            AuriNativeEngine.dspReset(nativeHandle)
        }
    }

    override fun onReset() {
        if (nativeHandle != 0L) {
            AuriNativeEngine.dspDestroy(nativeHandle)
            nativeHandle = 0L
        }
    }
}
