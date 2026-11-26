package com.auri.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.data.AuriNativeEngine
import com.auri.music.data.LibrarySortMode
import com.auri.music.data.LibraryViewMode
import com.auri.music.model.PlaybackState
import com.auri.music.model.Song
import com.auri.music.ui.components.AlbumArt
import com.auri.music.ui.theme.AuriCyanAccent
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriGreenHighRes
import com.auri.music.ui.theme.AuriObsidianBackground
import com.auri.music.ui.theme.AuriRedHeart
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletContainer
import com.auri.music.ui.theme.AuriVioletElectric

private enum class LibraryTab(val label: String) {
    TRACKS("Tracks"),
    PLAYLISTS("Playlists"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    PODCASTS("Podcasts")
}

/**
 * 1-to-1 Metrolist Library Screen Layout:
 * - Header: Library title + Share & Profile Avatar
 * - Filter pills: Tracks, Playlists, Artists, Albums, Podcasts
 * - Control Bar: "Date added" dropdown pill, Search icon, Grid/List toggle pill
 * - Hero Collection Tiles (in Grid mode):
 *   - Liked (Pink/Magenta gradient)
 *   - Downloaded (Orange/Yellow gradient)
 *   - Top 100 (Green trending gradient)
 *   - Cached (Purple/Blue sync gradient)
 *   - Uploaded (Cyan cloud gradient)
 * - Track list with active highlight & 3-dot menu
 * - "+ New playlist" floating action capsule pill
 */
@Composable
fun LibraryScreen(
    likedSongs: List<Song>,
    playbackState: PlaybackState,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    cachedSongs: List<Song> = emptyList(),
    downloadedSongs: List<Song> = emptyList(),
    sortMode: LibrarySortMode = LibrarySortMode.RECENT,
    viewMode: LibraryViewMode = LibraryViewMode.GRID,
    onSortModeChange: (LibrarySortMode) -> Unit = {},
    onViewModeChange: (LibraryViewMode) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    var activeCategoryFilter by remember { mutableStateOf<String?>(null) }

    val allSongs = remember(likedSongs, cachedSongs, downloadedSongs) {
        (likedSongs + cachedSongs + downloadedSongs).distinctBy { it.id }
    }

    val displayedSongs = remember(activeCategoryFilter, allSongs, likedSongs, cachedSongs, downloadedSongs, sortMode) {
        val base = when (activeCategoryFilter) {
            "Liked" -> likedSongs
            "Downloaded" -> downloadedSongs
            "Cached" -> cachedSongs
            else -> allSongs
        }
        when (sortMode) {
            LibrarySortMode.TITLE -> base.sortedBy { it.title.lowercase() }
            LibrarySortMode.ARTIST -> base.sortedBy { it.artist.lowercase() }
            LibrarySortMode.RECENT -> base
        }
    }

    Box(modifier = modifier.fillMaxSize().background(AuriObsidianBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 150.dp)
        ) {
            // Header: Library title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Library",
                        color = AuriTextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Metrolist Filter Tabs: Tracks, Playlists, Artists, Albums, Podcasts
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(LibraryTab.entries.toTypedArray()) { tab ->
                        val isSelected = tab == selectedTab
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) AuriVioletContainer else AuriGlassSurfaceElevated,
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                            label = "libTabBg"
                        )
                        val chipText by animateColorAsState(
                            targetValue = if (isSelected) AuriVioletBright else AuriTextSecondary,
                            label = "libTabTxt"
                        )

                        Surface(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    if (isSelected) AuriVioletBright.copy(alpha = 0.6f) else AuriGlassBorder,
                                    CircleShape
                                )
                                .selectable(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    role = Role.Tab,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = AuriVioletBright)
                                ),
                            shape = CircleShape,
                            color = chipBg
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = tab.label,
                                    color = chipText,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Metrolist Control Bar: "Date added" dropdown pill, Search icon, Grid/List toggle pill
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "Date added" Pill Selector
                    Surface(
                        onClick = {
                            val nextSort = when (sortMode) {
                                LibrarySortMode.RECENT -> LibrarySortMode.TITLE
                                LibrarySortMode.TITLE -> LibrarySortMode.ARTIST
                                LibrarySortMode.ARTIST -> LibrarySortMode.RECENT
                            }
                            onSortModeChange(nextSort)
                        },
                        shape = CircleShape,
                        color = AuriGlassSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AuriGlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (sortMode) {
                                    LibrarySortMode.RECENT -> "Date added"
                                    LibrarySortMode.TITLE -> "Title"
                                    LibrarySortMode.ARTIST -> "Artist"
                                },
                                color = AuriTextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AuriTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = AuriTextSecondary)
                        }
                        // Grid / List toggle pill
                        Surface(
                            onClick = {
                                onViewModeChange(
                                    if (viewMode == LibraryViewMode.GRID) LibraryViewMode.LIST else LibraryViewMode.GRID
                                )
                            },
                            shape = CircleShape,
                            color = AuriVioletContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = if (viewMode == LibraryViewMode.GRID) Icons.Default.GridView else Icons.Default.ViewList,
                                    contentDescription = "Toggle View",
                                    tint = AuriVioletBright,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Metrolist Hero Collection Tiles (Grid Mode)
            if (viewMode == LibraryViewMode.GRID && activeCategoryFilter == null) {
                item {
                    val tiles = listOf(
                        Triple("Liked", "Favorite songs (${likedSongs.size})", listOf(Color(0xFFE91E63), Color(0xFF9C27B0))),
                        Triple("Downloaded", "Offline songs (${downloadedSongs.size})", listOf(Color(0xFFFF9800), Color(0xFFFF5722))),
                        Triple("Top 100", "Your top songs", listOf(Color(0xFF4CAF50), Color(0xFF009688))),
                        Triple("Cached", "Cached songs (${cachedSongs.size})", listOf(Color(0xFF3F51B5), Color(0xFF2196F3))),
                        Triple("Uploaded", "Your uploaded songs", listOf(Color(0xFF00BCD4), Color(0xFF03A9F4))),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        tiles.chunked(3).forEach { rowTiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowTiles.forEach { tile ->
                                    MetrolistCollectionTile(
                                        title = tile.first,
                                        subtitle = tile.second,
                                        gradient = tile.third,
                                        onClick = { activeCategoryFilter = tile.first },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowTiles.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // If a category filter is active, show clear chip
            if (activeCategoryFilter != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Showing ${activeCategoryFilter}",
                            color = AuriVioletBright,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            onClick = { activeCategoryFilter = null },
                            shape = CircleShape,
                            color = AuriGlassSurfaceElevated
                        ) {
                            Text(
                                text = "Show All",
                                color = AuriTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Track Items List
            items(displayedSongs) { song ->
                val isCurrent = song.id == playbackState.currentSong?.id
                val rowBg by animateColorAsState(
                    targetValue = if (isCurrent) AuriVioletContainer.copy(alpha = 0.5f) else Color.Transparent,
                    label = "libRowBg"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSongClick(song, displayedSongs) },
                    shape = RoundedCornerShape(12.dp),
                    color = rowBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AlbumArt(
                                thumbnailUrl = song.thumbnailUrl,
                                contentDescription = song.title,
                                modifier = Modifier.size(48.dp),
                                cornerRadius = 10.dp
                            )
                            if (isCurrent && playbackState.isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x8808080B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = "Playing",
                                        tint = AuriVioletBright,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = if (isCurrent) AuriVioletBright else AuriTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = song.artist,
                                    color = AuriTextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (song.isOpus) "OPUS" else "AAC",
                                    color = AuriGreenHighRes,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0x2210B981), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = AuriTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Metrolist "+ New playlist" Floating Capsule Pill (bottom-right above miniplayer)
        Surface(
            onClick = { },
            shape = CircleShape,
            color = AuriVioletBright,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .height(48.dp),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AuriObsidianBackground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "New playlist",
                    color = AuriObsidianBackground,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Metrolist Collection Tile Card with vibrant background gradient and centered icon
 */
@Composable
private fun MetrolistCollectionTile(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (title) {
                    "Liked" -> Icons.Default.Favorite
                    "Downloaded" -> Icons.Default.ArrowDownward
                    "Top 100" -> Icons.Default.TrendingUp
                    "Cached" -> Icons.Default.Sync
                    else -> Icons.Default.CloudUpload
                }
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            color = AuriTextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = AuriTextSecondary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
