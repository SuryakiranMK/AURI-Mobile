package com.auri.music.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.auri.music.data.AuriRepository
import com.auri.music.model.EqPreset
import com.auri.music.model.PlaybackState
import com.auri.music.model.RepeatMode
import com.auri.music.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

class AuriPlayerController(
    private val context: Context,
    private val repository: AuriRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {
    private var exoPlayer: ExoPlayer? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: Job? = null
    private var playResolutionJob: Job? = null
    private var hardwareEqualizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackStateInt: Int) {
            when (playbackStateInt) {
                Player.STATE_BUFFERING -> {
                    _playbackState.update { it.copy(isBuffering = true) }
                }
                Player.STATE_READY -> {
                    _playbackState.update {
                        it.copy(
                            isBuffering = false,
                            durationMs = exoPlayer?.duration?.coerceAtLeast(0L) ?: it.durationMs
                        )
                    }
                    applyDspEffects()
                }
                Player.STATE_ENDED -> {
                    onTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _playbackState.update { it.copy(isBuffering = false) }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachAudioEffects(audioSessionId)
        }
    }

    init {
        initPlayer()
    }

    private fun initPlayer() {
        try {
            val player = AuriPlayerHolder.getOrCreatePlayer(context)
            player.removeListener(playerListener)
            player.addListener(playerListener)
            exoPlayer = player
            val sessionId = player.audioSessionId
            if (sessionId != 0) {
                attachAudioEffects(sessionId)
            }
            connectMediaSessionService()
        } catch (_: Exception) {
            // Fallback for JVM unit test environments
        }
    }

    private fun connectMediaSessionService() {
        if (controllerFuture != null) return
        try {
            val sessionToken = SessionToken(
                context.applicationContext,
                ComponentName(context.applicationContext, AuriPlaybackService::class.java)
            )
            val future = MediaController.Builder(context.applicationContext, sessionToken).buildAsync()
            controllerFuture = future
            future.addListener(
                {
                    try {
                        mediaController = future.get()
                    } catch (t: Throwable) {
                        Log.w(TAG, "MediaController connection error: ${t.message}")
                    }
                },
                ContextCompat.getMainExecutor(context.applicationContext)
            )
        } catch (_: Throwable) {
            // Ignored in JVM unit test environments
        }
    }

    private fun attachAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        releaseAudioEffects()
        try {
            hardwareEqualizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Hardware Equalizer unavailable on session $audioSessionId: ${t.message}")
            hardwareEqualizer = null
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                enabled = true
            }
        } catch (t: Throwable) {
            Log.w(TAG, "LoudnessEnhancer unavailable on session $audioSessionId: ${t.message}")
            loudnessEnhancer = null
        }

        applyDspEffects()
    }

    private fun applyDspEffects() {
        val state = _playbackState.value
        applyEqPresetToHardware(state.eqPreset)
        applyLoudnessToHardware(
            enabled = state.isLoudnessNormalizerEnabled,
            gainDb = state.loudnessGainDb,
            userVolume = state.volume
        )
    }

    private fun applyEqPresetToHardware(preset: EqPreset) {
        val eq = hardwareEqualizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            val isFlat = preset == EqPreset.FLAT
            eq.enabled = !isFlat

            if (!isFlat && numBands > 0) {
                for (band in 0 until numBands) {
                    val centerHz = eq.getCenterFreq(band.toShort()) / 1000.0
                    val interpolatedDb = interpolateIsoBandDb(centerHz, preset.gains)
                    val targetMb = (interpolatedDb * 100.0).toInt().coerceIn(minMb, maxMb)
                    eq.setBandLevel(band.toShort(), targetMb.toShort())
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed applying EQ preset ${preset.label}: ${t.message}")
        }
    }

    private fun interpolateIsoBandDb(freqHz: Double, gains: FloatArray): Double {
        if (freqHz <= ISO_CENTER_FREQS_HZ.first()) return gains.first().toDouble()
        if (freqHz >= ISO_CENTER_FREQS_HZ.last()) return gains.last().toDouble()

        for (i in 0 until ISO_CENTER_FREQS_HZ.size - 1) {
            val f0 = ISO_CENTER_FREQS_HZ[i]
            val f1 = ISO_CENTER_FREQS_HZ[i + 1]
            if (freqHz in f0..f1) {
                val t = (freqHz - f0) / (f1 - f0)
                return gains[i] + t * (gains[i + 1] - gains[i])
            }
        }
        return 0.0
    }

    private fun applyLoudnessToHardware(enabled: Boolean, gainDb: Double, userVolume: Float) {
        val player = exoPlayer
        if (!enabled) {
            player?.volume = userVolume.coerceIn(0f, 1f)
            try {
                loudnessEnhancer?.setTargetGain(0)
                loudnessEnhancer?.enabled = false
            } catch (_: Throwable) {}
            return
        }

        if (gainDb <= 0.0) {
            // Attenuate cleanly via ExoPlayer volume multiplier
            val linear = 10.0.pow(gainDb / 20.0).toFloat().coerceIn(0.05f, 1.0f)
            player?.volume = (userVolume * linear).coerceIn(0f, 1f)
            try {
                loudnessEnhancer?.setTargetGain(0)
                loudnessEnhancer?.enabled = false
            } catch (_: Throwable) {}
        } else {
            // Positive gain for quiet masters via LoudnessEnhancer
            player?.volume = userVolume.coerceIn(0f, 1f)
            try {
                loudnessEnhancer?.enabled = true
                loudnessEnhancer?.setTargetGain((gainDb * 100.0).toInt().coerceIn(0, 1000))
            } catch (_: Throwable) {}
        }
    }

    private fun startForegroundPlaybackService() {
        try {
            val intent = Intent(context, AuriPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Throwable) {
            // Ignored in JVM unit test environments
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        repository.registerSong(song)
        val index = queue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        val liked = repository.isSongLiked(song.id) || song.isLiked
        val initialSong = song.copy(isLiked = liked)
        val gain = repository.calculateLoudnessGain(initialSong.loudnessDb)

        _playbackState.update {
            it.copy(
                currentSong = initialSong,
                queue = queue,
                queueIndex = index,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = initialSong.durationMs,
                loudnessGainDb = gain,
                isBuffering = initialSong.streamUrl.isEmpty()
            )
        }

        playResolutionJob?.cancel()
        playResolutionJob = scope.launch {
            val resolvedSong = if (initialSong.streamUrl.isEmpty()) {
                repository.resolveStream(initialSong)
            } else {
                initialSong
            }

            val resolvedGain = repository.calculateLoudnessGain(resolvedSong.loudnessDb)
            val updatedQueue = queue.map { if (it.id == song.id) resolvedSong else it }

            _playbackState.update {
                it.copy(
                    currentSong = resolvedSong,
                    queue = updatedQueue,
                    durationMs = resolvedSong.durationMs,
                    loudnessGainDb = resolvedGain,
                    isBuffering = false
                )
            }

            exoPlayer?.let { player ->
                try {
                    if (resolvedSong.streamUrl.isNotEmpty()) {
                        startForegroundPlaybackService()
                        val metadata = MediaMetadata.Builder()
                            .setTitle(resolvedSong.title)
                            .setArtist(resolvedSong.artist)
                            .setAlbumTitle(resolvedSong.album)
                            .apply {
                                if (resolvedSong.thumbnailUrl.isNotBlank()) {
                                    setArtworkUri(Uri.parse(resolvedSong.thumbnailUrl))
                                }
                            }
                            .build()
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(resolvedSong.id)
                            .setUri(resolvedSong.streamUrl)
                            .setMediaMetadata(metadata)
                            .build()
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        applyDspEffects()
                        player.play()
                    }
                } catch (_: Exception) {}
            }

            startProgressTracker()
        }
    }

    fun togglePlayPause() {
        val currentState = _playbackState.value
        val newPlaying = !currentState.isPlaying

        _playbackState.update { it.copy(isPlaying = newPlaying) }

        exoPlayer?.let { player ->
            if (newPlaying) {
                startForegroundPlaybackService()
                if (player.playbackState == Player.STATE_IDLE && currentState.currentSong != null) {
                    currentState.currentSong.streamUrl.takeIf { it.isNotEmpty() }?.let { url ->
                        player.setMediaItem(MediaItem.fromUri(url))
                        player.prepare()
                    }
                }
                player.play()
            } else {
                player.pause()
            }
        }

        if (newPlaying) {
            startProgressTracker()
        } else {
            stopProgressTracker()
        }
    }

    fun seekTo(positionMs: Long) {
        val duration = _playbackState.value.durationMs
        val clamped = positionMs.coerceIn(0L, duration.coerceAtLeast(1L))
        _playbackState.update { it.copy(currentPositionMs = clamped) }
        exoPlayer?.seekTo(clamped)
    }

    fun skipNext() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffle && state.queue.size > 1) {
            (state.queue.indices - state.queueIndex).randomOrNull() ?: 0
        } else {
            (state.queueIndex + 1) % state.queue.size
        }

        val nextSong = state.queue.getOrNull(nextIndex) ?: return
        playSong(nextSong, state.queue)
    }

    fun skipPrevious() {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return

        if (state.currentPositionMs > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.queueIndex - 1 < 0) state.queue.size - 1 else state.queueIndex - 1
        val prevSong = state.queue.getOrNull(prevIndex) ?: return
        playSong(prevSong, state.queue)
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _playbackState.update {
            val next = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = next)
        }
    }

    fun toggleLoudnessNormalizer() {
        _playbackState.update {
            val enabled = !it.isLoudnessNormalizerEnabled
            val gain = if (enabled) repository.calculateLoudnessGain(it.currentSong?.loudnessDb) else 0.0
            it.copy(isLoudnessNormalizerEnabled = enabled, loudnessGainDb = gain)
        }
        applyDspEffects()
    }

    fun setEqPreset(preset: EqPreset) {
        _playbackState.update { it.copy(eqPreset = preset) }
        applyEqPresetToHardware(preset)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _playbackState.update { it.copy(volume = clamped) }
        applyDspEffects()
    }

    fun toggleLikeCurrentSong() {
        val current = _playbackState.value.currentSong ?: return
        repository.registerSong(current)
        val newLiked = repository.toggleLike(current.id)
        val updatedSong = current.copy(isLiked = newLiked)
        val updatedQueue = _playbackState.value.queue.map {
            if (it.id == current.id) updatedSong else it
        }
        _playbackState.update {
            it.copy(currentSong = updatedSong, queue = updatedQueue)
        }
    }

    private fun onTrackEnded() {
        when (_playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0L)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                val state = _playbackState.value
                if (state.queueIndex < state.queue.size - 1) {
                    skipNext()
                } else {
                    _playbackState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackState.update {
                            it.copy(
                                currentPositionMs = player.currentPosition,
                                bufferedPositionMs = player.bufferedPosition,
                                durationMs = if (player.duration > 0) player.duration else it.durationMs
                            )
                        }
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releaseAudioEffects() {
        try {
            hardwareEqualizer?.release()
        } catch (_: Throwable) {}
        hardwareEqualizer = null
        try {
            loudnessEnhancer?.release()
        } catch (_: Throwable) {}
        loudnessEnhancer = null
    }

    fun release() {
        playResolutionJob?.cancel()
        stopProgressTracker()
        releaseAudioEffects()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        exoPlayer?.removeListener(playerListener)
        exoPlayer = null
    }

    companion object {
        private const val TAG = "AuriPlayerController"
        private val ISO_CENTER_FREQS_HZ = doubleArrayOf(
            31.0, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0
        )
    }
}
