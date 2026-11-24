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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel(
    application: Application,
    private val repository: AuriRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, DefaultAuriRepository())

    private val playerController = AuriPlayerController(application, repository, viewModelScope)

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    val quickPicks: List<Song> = repository.getQuickPicks()
    val trending: List<Song> = repository.getTrending()
    val categories: List<String> = repository.getMoodCategories()

    val likedSongs: StateFlow<List<Song>> = repository.getLikedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playSong(song: Song, queue: List<Song> = listOf(song)) {
        playerController.playSong(song, queue)
    }

    fun togglePlayPause() {
        playerController.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun skipNext() {
        playerController.skipNext()
    }

    fun skipPrevious() {
        playerController.skipPrevious()
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

    fun getLyrics(songId: String): Lyrics {
        return repository.getLyrics(songId)
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}
