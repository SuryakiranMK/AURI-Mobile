package com.auri.music.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.auri.music.ui.components.MiniPlayer
import com.auri.music.ui.components.NowPlayingSheet
import com.auri.music.ui.screens.HomeScreen
import com.auri.music.ui.screens.LibraryScreen
import com.auri.music.ui.screens.SearchScreen
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriObsidianBackground
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletElectric

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val likedSongs by viewModel.likedSongs.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuriObsidianBackground)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AuriObsidianBackground,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Docked MiniPlayer
                    AnimatedVisibility(
                        visible = playbackState.currentSong != null && !isNowPlayingExpanded,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onExpandClick = { isNowPlayingExpanded = true },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.skipNext() }
                        )
                    }

                    // Bottom Navigation Bar
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
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Home"
                                )
                            },
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
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search"
                                )
                            },
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
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = "Library"
                                )
                            },
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
                        categories = viewModel.categories,
                        onSongClick = { song, queue -> viewModel.playSong(song, queue) }
                    )
                    1 -> SearchScreen(
                        onSearch = { query -> viewModel.search(query) },
                        onSongClick = { song, queue -> viewModel.playSong(song, queue) }
                    )
                    2 -> LibraryScreen(
                        likedSongs = likedSongs,
                        playbackState = playbackState,
                        onSongClick = { song, queue -> viewModel.playSong(song, queue) }
                    )
                }
            }
        }

        // Full Screen Now Playing HUD Modal
        AnimatedVisibility(
            visible = isNowPlayingExpanded && playbackState.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val song = playbackState.currentSong ?: return@AnimatedVisibility
            NowPlayingSheet(
                playbackState = playbackState,
                lyrics = viewModel.getLyrics(song.id),
                onCollapse = { isNowPlayingExpanded = false },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.skipNext() },
                onPrevious = { viewModel.skipPrevious() },
                onSeekTo = { pos -> viewModel.seekTo(pos) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleLike = { viewModel.toggleLike() },
                onToggleNormalizer = { viewModel.toggleNormalizer() },
                onSelectEqPreset = { preset -> viewModel.selectEqPreset(preset) }
            )
        }
    }
}
