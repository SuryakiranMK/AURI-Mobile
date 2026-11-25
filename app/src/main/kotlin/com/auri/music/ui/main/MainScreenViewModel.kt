package com.auri.music.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auri.music.data.AuriRepository
import com.auri.music.data.DefaultAuriRepository
import com.auri.music.model.EqPreset
import com.auri.music.model.Lyrics
import com.auri.music.model.PlaybackState
import com.auri.music.model.Song
import com.auri.music.player.AuriPlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(
    application: Application,
    private val repository: AuriRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, DefaultAuriRepository(application))

    private val playerController = AuriPlayerController(application, repository, viewModelScope)

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    val quickPicks: List<Song> = repository.getQuickPicks()
    val trending: List<Song> = repository.getTrending()
    val categories: List<String> = repository.getMoodCategories()

    val likedSongs: StateFlow<List<Song>> = repository.getLikedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentLyrics = MutableStateFlow(Lyrics())
    val currentLyrics: StateFlow<Lyrics> = _currentLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private var lyricsJob: Job? = null
    private var lastLoadedLyricsSongId: String? = null

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        playerController.playSong(song, queue)
        loadLyricsForSong(song)
    }

    fun loadLyricsForSong(song: Song) {
        if (lastLoadedLyricsSongId == song.id && (_currentLyrics.value.syncedLines.isNotEmpty() || _currentLyrics.value.plainText.isNotEmpty())) {
            return
        }
        lastLoadedLyricsSongId = song.id
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _isLyricsLoading.value = true
            _currentLyrics.value = Lyrics(source = "Fetching lyrics via Rust Innertube / LRCLIB...")
            val fetched = repository.getLyrics(song)
            _currentLyrics.value = fetched
            _isLyricsLoading.value = false
        }
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun skipNext() {
        playerController.skipNext()
        playbackState.value.currentSong?.let { loadLyricsForSong(it) }
    }

    fun skipPrevious() {
        playerController.skipPrevious()
        playbackState.value.currentSong?.let { loadLyricsForSong(it) }
    }

    fun toggleShuffle() {
        playerController.toggleShuffle()
    }

    fun toggleRepeat() {
        playerController.toggleRepeat()
    }

    fun toggleNormalizer() {
        playerController.toggleLoudnessNormalizer()
    }

    fun selectEqPreset(preset: EqPreset) {
        playerController.setEqPreset(preset)
    }

    fun toggleLike() {
        playerController.toggleLikeCurrentSong()
    }

    fun search(query: String): List<Song> {
        return repository.search(query)
    }

    suspend fun searchOnline(query: String): List<Song> {
        return repository.searchOnline(query)
    }

    fun getLyrics(songId: String): Lyrics {
        return repository.getLyrics(songId)
    }

    suspend fun getLyrics(song: Song): Lyrics {
        return repository.getLyrics(song)
    }

    override fun onCleared() {
        super.onCleared()
        lyricsJob?.cancel()
        playerController.release()
    }
}
