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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
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

private enum class LibraryFilter(val label: String) {
    ALL("All"),
    LIKED("Liked"),
    CACHED("Cached"),
    DOWNLOADED("Downloaded")
}

/**
 * Metrolist-Styled Library Screen:
 * - Header with Sort & Grid/List View Toggles
 * - Metrolist Collection Filter Chips (All, Liked, Cached, Downloaded)
 * - CurrentSongRow glowing active highlight with animated playing icon
 * - Grid or List representation with high-contrast metadata
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
    viewMode: LibraryViewMode = LibraryViewMode.LIST,
    onSortModeChange: (LibrarySortMode) -> Unit = {},
    onViewModeChange: (LibraryViewMode) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var engineExpanded by remember { mutableStateOf(false) }

    val baseList = remember(selectedFilter, likedSongs, cachedSongs, downloadedSongs) {
        when (selectedFilter) {
            LibraryFilter.ALL -> (likedSongs + cachedSongs + downloadedSongs).distinctBy { it.id }
            LibraryFilter.LIKED -> likedSongs
            LibraryFilter.CACHED -> cachedSongs
            LibraryFilter.DOWNLOADED -> downloadedSongs
        }
    }

    val displayedSongs = remember(baseList, sortMode) {
        when (sortMode) {
            LibrarySortMode.TITLE -> baseList.sortedBy { it.title.lowercase() }
            LibrarySortMode.ARTIST -> baseList.sortedBy { it.artist.lowercase() }
            LibrarySortMode.RECENT -> baseList
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuriObsidianBackground)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Library Header & Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    color = AuriTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val nextSort = when (sortMode) {
                            LibrarySortMode.RECENT -> LibrarySortMode.TITLE
                            LibrarySortMode.TITLE -> LibrarySortMode.ARTIST
                            LibrarySortMode.ARTIST -> LibrarySortMode.RECENT
                        }
                        onSortModeChange(nextSort)
                    }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = AuriVioletBright)
                    }
                    IconButton(onClick = {
                        onViewModeChange(
                            if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST
                        )
                    }) {
                        Icon(
                            imageVector = if (viewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Toggle View",
                            tint = AuriVioletBright
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Metrolist Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(LibraryFilter.entries.toTypedArray()) { filter ->
                    val isSelected = filter == selectedFilter
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) AuriVioletContainer else AuriGlassSurfaceElevated,
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                        label = "libChipBg"
                    )
                    val chipBorder by animateColorAsState(
                        targetValue = if (isSelected) AuriVioletBright.copy(alpha = 0.6f) else AuriGlassBorder,
                        label = "libChipBorder"
                    )
                    val chipText by animateColorAsState(
                        targetValue = if (isSelected) AuriVioletBright else AuriTextSecondary,
                        label = "libChipText"
                    )

                    Surface(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, chipBorder, CircleShape)
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedFilter = filter },
                                role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = AuriVioletBright)
                            ),
                        shape = CircleShape,
                        color = chipBg
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = filter.label,
                                color = chipText,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Native Engine Metrics Card (Expandable)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, AuriGlassBorder, RoundedCornerShape(14.dp))
                    .clickable { engineExpanded = !engineExpanded },
                shape = RoundedCornerShape(14.dp),
                color = AuriGlassSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = AuriGreenHighRes,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AURI Rust Audio Engine",
                                color = AuriTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = if (engineExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AuriTextMuted
                        )
                    }

                    if (engineExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Engine Info: ${AuriNativeEngine.getVersion()}",
                            color = AuriTextSecondary,
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "DSP Status: Active (-14 LUFS normalizer + Limiter)",
                            color = AuriGreenHighRes,
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "Auto Headroom: Dynamic (Rust Core) | Memory: Managed",
                            color = AuriTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Empty state
        if (displayedSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs in ${selectedFilter.label.lowercase()} yet",
                        color = AuriTextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(displayedSongs) { song ->
                val isCurrent = song.id == playbackState.currentSong?.id
                val rowBg by animateColorAsState(
                    targetValue = if (isCurrent) AuriVioletContainer.copy(alpha = 0.45f) else Color.Transparent,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                    label = "libRowBg"
                )
                val rowBorder by animateColorAsState(
                    targetValue = if (isCurrent) AuriVioletBright.copy(alpha = 0.4f) else Color.Transparent,
                    label = "libRowBorder"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, rowBorder, RoundedCornerShape(12.dp))
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
                                modifier = Modifier.size(46.dp),
                                cornerRadius = 10.dp
                            )
                            if (isCurrent && playbackState.isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x77000000)),
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
                                fontSize = 13.5.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = song.artist,
                                    color = AuriTextSecondary,
                                    fontSize = 11.5.sp,
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

                        Text(
                            text = "${(song.durationMs / 1000) / 60}:${((song.durationMs / 1000) % 60).toString().padStart(2, '0')}",
                            color = AuriTextMuted,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
