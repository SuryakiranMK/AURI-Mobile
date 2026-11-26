package com.auri.music.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.auri.music.ui.components.ExpressiveMiniPlayerHeight
import com.auri.music.ui.components.MiniPlayer
import com.auri.music.ui.components.MorphingPlayerSheet
import com.auri.music.ui.components.NowPlayingSheet
import com.auri.music.ui.components.rememberAuriBottomSheetState
import com.auri.music.ui.navigation.FloatingNavItem
import com.auri.music.ui.navigation.MetrolistFloatingNavigation
import com.auri.music.ui.screens.HomeScreen
import com.auri.music.ui.screens.LibraryScreen
import com.auri.music.ui.screens.SearchScreen
import com.auri.music.ui.theme.AuriObsidianBackground

/**
 * Main application shell featuring Metrolist UI foundations:
 * - Floating capsule navigation bar with fluid spring physics
 * - Expressive MiniPlayer floating above navigation
 * - Morphing PlayerSheet transitioning seamlessly to full-bleed NowPlayingSheet
 */
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    val cachedSongs by viewModel.cachedSongs.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsStateWithLifecycle()
    val lyricsOffsetMs by viewModel.lyricsOffsetMs.collectAsStateWithLifecycle()
    val librarySortMode by viewModel.librarySortMode.collectAsStateWithLifecycle()
    val libraryViewMode by viewModel.libraryViewMode.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val playerSheetState = rememberAuriBottomSheetState()

    val navItems = remember {
        listOf(
            FloatingNavItem(0, "Home", Icons.Outlined.Home, Icons.Filled.Home),
            FloatingNavItem(2, "Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic)
        )
    }
    val searchItem = remember {
        FloatingNavItem(1, "Search", Icons.Outlined.Search, Icons.Filled.Search)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AuriObsidianBackground)
    ) {
        val maxSheetHeight = maxHeight
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val navBarTotalHeight = 72.dp + navBarPadding
        val navAlpha = (1f - playerSheetState.progress * 1.5f).coerceIn(0f, 1f)

        // Screen Content (under floating navigation)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (playbackState.currentSong != null) 70.dp else 10.dp)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    quickPicks = viewModel.quickPicks,
                    trending = viewModel.trending,
                    keepListening = keepListening,
                    forgottenFavorites = forgottenFavorites,
                    categories = viewModel.categories,
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) }
                )
                1 -> SearchScreen(
                    onSearch = { query -> viewModel.searchOnline(query) },
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) }
                )
                2 -> LibraryScreen(
                    likedSongs = likedSongs,
                    cachedSongs = cachedSongs,
                    downloadedSongs = downloadedSongs,
                    playbackState = playbackState,
                    sortMode = librarySortMode,
                    viewMode = libraryViewMode,
                    onSortModeChange = { viewModel.setLibrarySortMode(it) },
                    onViewModeChange = { viewModel.setLibraryViewMode(it) },
                    onSongClick = { song, queue -> viewModel.playSong(song, queue) }
                )
            }
        }

        // Metrolist Floating Capsule Navigation
        if (navAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .alpha(navAlpha),
                contentAlignment = Alignment.Center
            ) {
                MetrolistFloatingNavigation(
                    items = navItems,
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    separateSearchItem = searchItem
                )
            }
        }

        // Persistent Morphing BottomSheet (MiniPlayer <-> NowPlayingSheet)
        MorphingPlayerSheet(
            visible = playbackState.currentSong != null,
            state = playerSheetState,
            collapsedHeight = ExpressiveMiniPlayerHeight + 8.dp,
            bottomNavHeight = navBarTotalHeight,
            maxHeight = maxSheetHeight,
            collapsedContent = {
                MiniPlayer(
                    playbackState = playbackState,
                    onExpandClick = { playerSheetState.expand() },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onNextClick = { viewModel.skipNext() },
                    onPreviousClick = { viewModel.skipPrevious() },
                    isLiked = playbackState.currentSong?.isLiked == true,
                    onLikeClick = { viewModel.toggleLike() }
                )
            },
            expandedContent = {
                NowPlayingSheet(
                    playbackState = playbackState,
                    lyrics = currentLyrics,
                    isLyricsLoading = isLyricsLoading,
                    lyricsOffsetMs = lyricsOffsetMs,
                    onAdjustLyricsOffset = { viewModel.adjustLyricsOffset(it) },
                    onResetLyricsOffset = { viewModel.resetLyricsOffset() },
                    onCollapse = { playerSheetState.collapse() },
                    onPlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.skipNext() },
                    onPrevious = { viewModel.skipPrevious() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onSeekBy = { viewModel.seekBy(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onToggleLike = { viewModel.toggleLike() },
                    onToggleNormalizer = { viewModel.toggleNormalizer() },
                    onSelectEqPreset = { viewModel.selectEqPreset(it) },
                    onVolumeChange = { viewModel.setVolume(it) },
                    onSelectAudioDevice = { viewModel.setPreferredAudioDevice(it) },
                    onSelectQueueTrack = { viewModel.playSong(it, playbackState.queue) },
                    onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                    onRemoveQueueItem = { viewModel.removeQueueItem(it) }
                )
            }
        )
    }
}
