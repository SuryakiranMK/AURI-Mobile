package com.metrolist.shared.playback

actual typealias VideoFrame = Any

@Suppress("UNUSED_PARAMETER")
actual class VideoPlayer actual constructor(
    private val onFrame: (VideoFrame) -> Unit,
    private val onEnded: () -> Unit,
    private val onSeekLanded: ((Long) -> Unit)?,
    private val onVideoSizeChanged: (width: Int, height: Int) -> Unit,
    private val onRenderedFirstFrame: () -> Unit,
    private val looping: Boolean,
) {
    actual val hasOwnAudio: Boolean = false

    actual var audioPtsProvider: (() -> Long?)? = null

    actual fun getStats(): VideoPlayerStats = VideoPlayerStats()

    actual fun load(
        url: String,
        headers: Map<String, String>,
        positionProvider: () -> Long,
    ): Boolean = false

    actual fun load(
        source: SeekableMediaSource,
        positionProvider: () -> Long,
    ): Boolean = false

    actual fun play(startPaused: Boolean) = Unit

    actual fun pause() = Unit

    actual fun resume() = Unit

    actual fun seekTo(positionMs: Long) {
        onSeekLanded?.invoke(positionMs)
    }

    actual fun setVolume(volume: Float) = Unit

    actual fun setSurface(surface: Any?) = Unit

    actual fun close() = Unit

    fun setTargetSize(width: Int, height: Int, fillViewport: Boolean = false) = Unit

    fun dispose() = close()

    fun disposeAsync(): kotlinx.coroutines.Job = kotlinx.coroutines.Job().apply { complete() }
}
