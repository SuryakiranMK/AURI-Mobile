package com.auri.music.ui.main

import android.app.Application
import android.media.AudioDeviceInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auri.music.AuriApplication
import com.auri.music.data.AuriPreferences
import com.auri.music.data.AuriRepository
import com.auri.music.data.DefaultAuriRepository
import com.auri.music.data.LibrarySortMode
import com.auri.music.data.LibraryViewMode
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

    constructor(application: Application) : this(
        application,
        (application as? AuriApplication)?.repository ?: DefaultAuriRepository(application)
    )

    private val preferences: AuriPreferences? = (application as? AuriApplication)?.preferences
    private val playerController = AuriPlayerController(application, repository, viewModelScope)

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    val quickPicks: List<Song> = repository.getQuickPicks()
    val trending: List<Song> = repository.getTrending()
    val categories: List<String> = repository.getMoodCategories()

    val likedSongs: StateFlow<List<Song>> = repository.getLikedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cachedSongs: StateFlow<List<Song>> = repository.getCachedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedSongs: StateFlow<List<Song>> = repository.getDownloadedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _keepListening = MutableStateFlow<List<Song>>(emptyList())
    val keepListening: StateFlow<List<Song>> = _keepListening.asStateFlow()

    private val _forgottenFavorites = MutableStateFlow<List<Song>>(emptyList())
    val forgottenFavorites: StateFlow<List<Song>> = _forgottenFavorites.asStateFlow()

    private val _lyricsOffsetMs = MutableStateFlow(0L)
    val lyricsOffsetMs: StateFlow<Long> = _lyricsOffsetMs.asStateFlow()

    private val _librarySortMode = MutableStateFlow(LibrarySortMode.RECENT)
    val librarySortMode: StateFlow<LibrarySortMode> = _librarySortMode.asStateFlow()

    private val _libraryViewMode = MutableStateFlow(LibraryViewMode.LIST)
    val libraryViewMode: StateFlow<LibraryViewMode> = _libraryViewMode.asStateFlow()

    private val _currentLyrics = MutableStateFlow(Lyrics())
    val currentLyrics: StateFlow<Lyrics> = _currentLyrics.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    private var lyricsJob: Job? = null
    private var lastLoadedLyricsSongId: String? = null

    init {
        viewModelScope.launch {
            _keepListening.value = repository.getKeepListening(10)
            _forgottenFavorites.value = repository.getForgottenFavorites(10)
            preferences?.settings?.collect { snap ->
                _lyricsOffsetMs.value = snap.globalLyricsOffsetMs
                _librarySortMode.value = snap.librarySortMode
                _libraryViewMode.value = snap.libraryViewMode
            }
        }
    }

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
            _currentLyrics.value = Lyrics(source = "Fetching lyrics...")
            val fetched = repository.getLyrics(song)
            _currentLyrics.value = fetched
            _isLyricsLoading.value = false
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun seekBy(deltaMs: Long) = playerController.seekBy(deltaMs)
    fun skipNext() {
        playerController.skipNext()
        playbackState.value.currentSong?.let { loadLyricsForSong(it) }
    }
    fun skipPrevious() {
        playerController.skipPrevious()
        playbackState.value.currentSong?.let { loadLyricsForSong(it) }
    }
    fun toggleShuffle() = playerController.toggleShuffle()
    fun toggleRepeat() = playerController.toggleRepeat()
    fun toggleNormalizer() = playerController.toggleLoudnessNormalizer()
    fun selectEqPreset(preset: EqPreset) {
        playerController.setEqPreset(preset)
        viewModelScope.launch { preferences?.setEqPreset(preset) }
    }
    fun setVolume(volume: Float) = playerController.setVolume(volume)
    fun setPreferredAudioDevice(device: AudioDeviceInfo?) = playerController.setPreferredAudioDevice(device)
    fun moveQueueItem(from: Int, to: Int) = playerController.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = playerController.removeQueueItem(index)
    fun toggleLike() = playerController.toggleLikeCurrentSong()

    fun adjustLyricsOffset(deltaMs: Long) {
        val updated = (_lyricsOffsetMs.value + deltaMs).coerceIn(-10000L, 10000L)
        _lyricsOffsetMs.value = updated
        viewModelScope.launch { preferences?.setGlobalLyricsOffsetMs(updated) }
    }

    fun resetLyricsOffset() {
        _lyricsOffsetMs.value = 0L
        viewModelScope.launch { preferences?.setGlobalLyricsOffsetMs(0L) }
    }

    fun setLibrarySortMode(mode: LibrarySortMode) {
        _librarySortMode.value = mode
        viewModelScope.launch { preferences?.setLibrarySortMode(mode) }
    }

    fun setLibraryViewMode(mode: LibraryViewMode) {
        _libraryViewMode.value = mode
        viewModelScope.launch { preferences?.setLibraryViewMode(mode) }
    }

    fun search(query: String): List<Song> = repository.search(query)
    suspend fun searchOnline(query: String): List<Song> = repository.searchOnline(query)
    fun getLyrics(songId: String): Lyrics = repository.getLyrics(songId)
    suspend fun getLyrics(song: Song): Lyrics = repository.getLyrics(song)

    override fun onCleared() {
        super.onCleared()
        lyricsJob?.cancel()
        playerController.release()
    }
}
