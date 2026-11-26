package com.auri.music.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auri.music.AuriApplication
import com.auri.music.data.AuriRepository
import com.auri.music.data.cache.NextTrackPreloadTrigger
import com.auri.music.data.sponsorblock.SponsorSegment
import com.auri.music.model.EqPreset
import com.auri.music.model.PlaybackState
import com.auri.music.model.RepeatMode
import com.auri.music.model.Song
import com.google.common.util.concurrent.ListenableFuture
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

@OptIn(UnstableApi::class)
class AuriPlayerController(
    private val context: Context,
    private val repository: AuriRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {
    private var exoPlayer: ExoPlayer? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var progressJob: Job? = null
    private var playResolutionJob: Job? = null
    private var btMonitor: BluetoothA2dpMonitor? = null
    private var preloadTrigger: NextTrackPreloadTrigger? = null

    private var activeSegments: List<SponsorSegment> = emptyList()
    private var lastSkippedEndMs: Long = -1L
    private var accumulatedPlayMs: Long = 0L
    private var lastReportedSongId: String? = null
    private var lastStallCheckPos: Long = -1L
    private var stallTicks: Int = 0

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackStateInt: Int) {
            when (playbackStateInt) {
                Player.STATE_BUFFERING -> _playbackState.update { it.copy(isBuffering = true) }
                Player.STATE_READY -> {
                    _playbackState.update {
                        it.copy(
                            isBuffering = false,
                            durationMs = exoPlayer?.duration?.coerceAtLeast(0L) ?: it.durationMs
                        )
                    }
                    applyDspAndOffload()
                }
                Player.STATE_ENDED -> onTrackEnded()
                Player.STATE_IDLE -> _playbackState.update { it.copy(isBuffering = false) }
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
    }

    init {
        initPlayer()
    }

    private fun initPlayer() {
        try {
            val app = context.applicationContext as? AuriApplication
            if (app != null) {
                preloadTrigger = NextTrackPreloadTrigger(repository, app.cacheManager)
            }
            val player = AuriPlayerHolder.getOrCreatePlayer(context)
            player.removeListener(playerListener)
            player.addListener(playerListener)
            exoPlayer = player

            btMonitor = BluetoothA2dpMonitor(context) {
                if (_playbackState.value.isPlaying) {
                    exoPlayer?.pause()
                }
            }.apply { start() }

            val token = SessionToken(context, ComponentName(context, AuriPlaybackService::class.java))
            controllerFuture = MediaController.Builder(context, token).buildAsync()
        } catch (_: Exception) {
            // Fallback for JVM unit test environments
        }
    }

    private fun applyDspAndOffload() {
        val state = _playbackState.value
        AuriPlayerHolder.dspProcessor?.configureDsp(
            gains = state.eqPreset.gains,
            preampDb = state.preampDb,
            autoHeadroom = state.autoHeadroomEnabled,
            loudnessGainDb = state.loudnessGainDb,
            normalizerEnabled = state.isLoudnessNormalizerEnabled
        )

        val player = exoPlayer ?: return
        player.volume = state.volume.coerceIn(0f, 1f)

        val eligibility = AudioOffloadEligibility(
            eqPreset = state.eqPreset,
            preampDb = state.preampDb,
            normalizerEnabled = state.isLoudnessNormalizerEnabled,
            crossfadeActive = state.crossfadeEnabled
        )
        val offloadMode = if (eligibility.isEligible) {
            AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
        } else {
            AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
        }
        val params = player.trackSelectionParameters.buildUpon()
            .setAudioOffloadPreferences(
                AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(offloadMode)
                    .build()
            )
            .build()
        player.trackSelectionParameters = params
        _playbackState.update { it.copy(isOffloadActive = eligibility.isEligible) }
    }

    private fun startForegroundPlaybackService() {
        try {
            val intent = Intent(context, AuriPlaybackService::class.java)
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Throwable) {}
    }

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        repository.registerSong(song)
        val index = queue.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        val liked = repository.isSongLiked(song.id) || song.isLiked
        val initialSong = song.copy(isLiked = liked)
        val gain = repository.calculateLoudnessGain(initialSong.loudnessDb)

        activeSegments = emptyList()
        lastSkippedEndMs = -1L
        accumulatedPlayMs = 0L
        lastReportedSongId = null

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

            val app = context.applicationContext as? AuriApplication
            if (app?.preferences?.settings?.value?.sponsorBlockEnabled == true) {
                launch {
                    activeSegments = app.sponsorBlock.getSkipSegments(resolvedSong.id)
                }
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
                        applyDspAndOffload()
                        player.play()
                    }
                } catch (_: Exception) {}
            }

            preloadTrigger?.preloadNext(updatedQueue, index)
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

        if (newPlaying) startProgressTracker() else stopProgressTracker()
    }

    fun seekTo(positionMs: Long) {
        val duration = _playbackState.value.durationMs
        val clamped = positionMs.coerceIn(0L, duration.coerceAtLeast(1L))
        _playbackState.update { it.copy(currentPositionMs = clamped) }
        exoPlayer?.seekTo(clamped)
    }

    fun seekBy(deltaMs: Long) {
        val current = exoPlayer?.currentPosition ?: _playbackState.value.currentPositionMs
        seekTo(current + deltaMs)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        _playbackState.update { state ->
            val list = state.queue.toMutableList()
            if (fromIndex !in list.indices || toIndex !in list.indices) return@update state
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            val newIndex = when (state.queueIndex) {
                fromIndex -> toIndex
                in (toIndex until fromIndex) -> state.queueIndex + 1
                in ((fromIndex + 1)..toIndex) -> state.queueIndex - 1
                else -> state.queueIndex
            }
            state.copy(queue = list, queueIndex = newIndex)
        }
    }

    fun removeQueueItem(index: Int) {
        _playbackState.update { state ->
            val list = state.queue.toMutableList()
            if (index !in list.indices || list.size <= 1) return@update state
            list.removeAt(index)
            val newIndex = when {
                index < state.queueIndex -> state.queueIndex - 1
                state.queueIndex >= list.size -> list.lastIndex
                else -> state.queueIndex
            }
            state.copy(queue = list, queueIndex = newIndex)
        }
    }

    fun setPreferredAudioDevice(device: AudioDeviceInfo?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { exoPlayer?.setPreferredAudioDevice(device) }
        }
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
        applyDspAndOffload()
    }

    fun setEqPreset(preset: EqPreset) {
        _playbackState.update { it.copy(eqPreset = preset) }
        applyDspAndOffload()
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _playbackState.update { it.copy(volume = clamped) }
        applyDspAndOffload()
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
            RepeatMode.ALL -> skipNext()
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
                        val pos = player.currentPosition
                        val state = _playbackState.value
                        _playbackState.update {
                            it.copy(
                                currentPositionMs = pos,
                                bufferedPositionMs = player.bufferedPosition,
                                durationMs = if (player.duration > 0) player.duration else it.durationMs
                            )
                        }

                        // SponsorBlock check
                        if (activeSegments.isNotEmpty()) {
                            val hit = activeSegments.firstOrNull { pos in it.startMs until (it.endMs - 250L) }
                            if (hit != null && hit.endMs != lastSkippedEndMs) {
                                lastSkippedEndMs = hit.endMs
                                seekTo(hit.endMs)
                            }
                        }

                        // Offload stall detection (2s stuck while playing)
                        if (state.isOffloadActive) {
                            if (pos == lastStallCheckPos) {
                                stallTicks++
                                if (stallTicks >= 8) {
                                    stallTicks = 0
                                    // Recover by disabling offload
                                    val params = player.trackSelectionParameters.buildUpon()
                                        .setAudioOffloadPreferences(
                                            AudioOffloadPreferences.Builder()
                                                .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED)
                                                .build()
                                        )
                                        .build()
                                    player.trackSelectionParameters = params
                                    _playbackState.update { it.copy(isOffloadActive = false) }
                                }
                            } else {
                                stallTicks = 0
                            }
                            lastStallCheckPos = pos
                        }

                        // Record play event once >= 30s accumulated
                        accumulatedPlayMs += 250L
                        val currentSong = state.currentSong
                        if (accumulatedPlayMs >= 30_000L && currentSong != null && lastReportedSongId != currentSong.id) {
                            lastReportedSongId = currentSong.id
                            launch { repository.recordPlayEvent(currentSong, accumulatedPlayMs) }
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
        playResolutionJob?.cancel()
        preloadTrigger?.cancel()
        stopProgressTracker()
        btMonitor?.stop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        exoPlayer?.removeListener(playerListener)
        exoPlayer = null
    }
}
