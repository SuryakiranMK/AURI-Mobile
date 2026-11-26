package com.auri.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.data.AuriNativeEngine
import com.auri.music.data.LibrarySortMode
import com.auri.music.data.LibraryViewMode
import com.auri.music.model.PlaybackState
import com.auri.music.model.Song
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
import com.auri.music.ui.theme.AuriVioletElectric

private enum class LibraryFilter(val label: String) {
    ALL("All"),
    LIKED("Liked"),
    CACHED("Cached"),
    DOWNLOADED("Downloaded")
}

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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Library Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Library",
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
                        val nextView = if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST
                        onViewModeChange(nextView)
                    }) {
                        Icon(
                            imageVector = if (viewMode == LibraryViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Toggle View",
                            tint = AuriTextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Filter Chips (All, Liked, Cached, Downloaded)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(LibraryFilter.entries) { filter ->
                    val selected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) AuriVioletElectric else AuriGlassSurfaceElevated)
                            .border(
                                1.dp,
                                if (selected) AuriVioletBright else AuriGlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = filter.label,
                            color = if (selected) Color.White else AuriTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Liked Songs Hero Card
        item {
            val shape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(AuriGlassSurfaceElevated)
                    .border(1.dp, AuriGlassBorder, shape)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(AuriRedHeart.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, AuriRedHeart.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Liked", tint = AuriRedHeart, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Liked Songs", color = AuriTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("${likedSongs.size} tracks saved • Sort: ${sortMode.name.lowercase()}", color = AuriTextSecondary, fontSize = 12.sp)
                        }
                    }

                    if (likedSongs.isNotEmpty()) {
                        Button(
                            onClick = { onSongClick(likedSongs.first(), likedSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = AuriVioletElectric),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play All", tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Collapsible Audio Engine Specifications Card
        item {
            val engineCardShape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(engineCardShape)
                    .background(AuriGlassSurface)
                    .border(1.dp, AuriGlassBorder, engineCardShape)
                    .clickable { engineExpanded = !engineExpanded }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Engine Status",
                        color = AuriTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (engineExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Engine Status",
                        tint = AuriTextSecondary
                    )
                }

                if (engineExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        EngineStatusRow(
                            icon = Icons.Default.Speed,
                            iconTint = AuriGreenHighRes,
                            title = "-14.0 LUFS Normalizer",
                            subtitle = if (playbackState.isLoudnessNormalizerEnabled) {
                                "ACTIVE • Dynamic Gain: %.1f dB".format(playbackState.loudnessGainDb)
                            } else "BYPASSED"
                        )
                        EngineStatusRow(
                            icon = Icons.Default.GraphicEq,
                            iconTint = AuriVioletBright,
                            title = "10-Band Rust DSP EQ",
                            subtitle = "${playbackState.eqPreset.label} • Offload: ${if (playbackState.isOffloadActive) "ON" else "PCM"}"
                        )
                        EngineStatusRow(
                            icon = Icons.Default.Storage,
                            iconTint = AuriCyanAccent,
                            title = "Rust Audio Engine",
                            subtitle = AuriNativeEngine.getVersion()
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Tracks Section
        item {
            Text(
                text = "${selectedFilter.label} Tracks (${displayedSongs.size})",
                color = AuriTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (displayedSongs.isEmpty()) {
            item {
                Text(
                    text = "No ${selectedFilter.label.lowercase()} tracks yet.",
                    color = AuriTextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else if (viewMode == LibraryViewMode.LIST) {
            items(displayedSongs) { song ->
                SongRowItem(
                    song = song,
                    onClick = { onSongClick(song, displayedSongs) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            val rows = displayedSongs.chunked(2)
            items(rows) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (song in pair) {
                        QuickPickCard(
                            song = song,
                            onClick = { onSongClick(song, displayedSongs) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EngineStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = AuriTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AuriTextMuted, fontSize = 12.sp)
        }
    }
}
