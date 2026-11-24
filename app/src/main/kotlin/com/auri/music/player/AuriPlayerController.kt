package com.auri.music.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

class AuriPlayerController(
    private val context: Context,
    private val repository: AuriRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        initPlayer()
    }

    private fun initPlayer() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            exoPlayer = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackStateInt: Int) {
                            when (playbackStateInt) {
                                Player.STATE_BUFFERING -> {
                                    _playbackState.update { it.copy(isBuffering = true) }
                                }
                                Player.STATE_READY -> {
                                    _playbackState.update {
                                        it.copy(
                                            isBuffering = false,
                                            durationMs = duration.coerceAtLeast(0L)
                                        )
                                    }
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
                    })
                }
        } catch (_: Exception) {
            // Fallback for preview or mock test environments
        }
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        val index = queue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        val gain = repository.calculateLoudnessGain(song.loudnessDb)

        _playbackState.update {
            it.copy(
                currentSong = song,
                queue = queue,
                queueIndex = index,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = song.durationMs,
                loudnessGainDb = gain
            )
        }

        exoPlayer?.let { player ->
            try {
                if (song.streamUrl.isNotEmpty()) {
                    val mediaItem = MediaItem.fromUri(song.streamUrl)
                    player.setMediaItem(mediaItem)
                    player.prepare()
                    player.play()
                }
            } catch (_: Exception) {}
        }

        startProgressTracker()
    }

    fun togglePlayPause() {
        val currentState = _playbackState.value
        val newPlaying = !currentState.isPlaying

        _playbackState.update { it.copy(isPlaying = newPlaying) }

        exoPlayer?.let { player ->
            if (newPlaying) {
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

        val nextIndex = if (state.isShuffle) {
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

        // If played more than 3 seconds, restart current track
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
    }

    fun setEqPreset(preset: EqPreset) {
        _playbackState.update { it.copy(eqPreset = preset) }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _playbackState.update { it.copy(volume = clamped) }
        exoPlayer?.volume = clamped
    }

    fun toggleLikeCurrentSong() {
        val current = _playbackState.value.currentSong ?: return
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

    fun release() {
        stopProgressTracker()
        exoPlayer?.release()
        exoPlayer = null
    }
}
