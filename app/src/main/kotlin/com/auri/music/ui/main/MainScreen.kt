package com.auri.music.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.auri.music.ui.components.MiniPlayer
import com.auri.music.ui.components.MorphingPlayerSheet
import com.auri.music.ui.components.NowPlayingSheet
import com.auri.music.ui.components.rememberAuriBottomSheetState
import com.auri.music.ui.screens.HomeScreen
import com.auri.music.ui.screens.LibraryScreen
import com.auri.music.ui.screens.SearchScreen
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriObsidianBackground
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriVioletBright

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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(AuriObsidianBackground)
    ) {
        val maxSheetHeight = maxHeight
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val navBarTotalHeight = 80.dp + navBarPadding
        val navAlpha = (1f - playerSheetState.progress * 1.5f).coerceIn(0f, 1f)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AuriObsidianBackground,
            bottomBar = {
                if (navAlpha > 0.01f) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .alpha(navAlpha)
                    ) {
                        val navShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(navShape)
                                .border(1.dp, AuriGlassBorder, navShape),
                            containerColor = AuriGlassSurface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AuriVioletBright,
                                    selectedTextColor = AuriVioletBright,
                                    indicatorColor = Color(0x338B5CF6),
                                    unselectedIconColor = AuriTextMuted,
                                    unselectedTextColor = AuriTextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                label = { Text("Search", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AuriVioletBright,
                                    selectedTextColor = AuriVioletBright,
                                    indicatorColor = Color(0x338B5CF6),
                                    unselectedIconColor = AuriTextMuted,
                                    unselectedTextColor = AuriTextMuted
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                                label = { Text("Library", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AuriVioletBright,
                                    selectedTextColor = AuriVioletBright,
                                    indicatorColor = Color(0x338B5CF6),
                                    unselectedIconColor = AuriTextMuted,
                                    unselectedTextColor = AuriTextMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
        }

        // Persistent 3-state Morphing BottomSheet (MiniPlayer <-> NowPlayingSheet)
        MorphingPlayerSheet(
            visible = playbackState.currentSong != null,
            state = playerSheetState,
            collapsedHeight = 74.dp,
            bottomNavHeight = navBarTotalHeight,
            maxHeight = maxSheetHeight,
            collapsedContent = {
                MiniPlayer(
                    playbackState = playbackState,
                    onExpandClick = { playerSheetState.expand() },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onNextClick = { viewModel.skipNext() },
                    onPreviousClick = { viewModel.skipPrevious() }
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
