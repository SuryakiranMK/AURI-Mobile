package com.auri.music.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.R
import com.auri.music.model.Song
import com.auri.music.ui.components.AlbumArt
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriGreenHighRes
import com.auri.music.ui.theme.AuriObsidianBackground
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletElectric

@Composable
fun HomeScreen(
    quickPicks: List<Song>,
    trending: List<Song>,
    categories: List<String>,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
    keepListening: List<Song> = emptyList(),
    forgottenFavorites: List<Song> = emptyList()
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "All") }
    val displayedSongs = remember(selectedCategory, trending) {
        when (selectedCategory) {
            "Synthwave" -> trending.filter { it.title.contains("Echoes") || it.title.contains("Electric") }
            "Cyber Chill" -> trending.filter { it.title.contains("Gravity") || it.title.contains("Rainfall") }
            "Audiophile Master" -> trending.filter { it.title.contains("Odyssey") || it.title.contains("Prism") }
            else -> trending
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuriObsidianBackground),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // App Branding Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_auri_brand),
                        contentDescription = "AURI Logo",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AURI",
                        color = AuriTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MOBILE",
                        color = AuriVioletBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x2210B981))
                        .border(1.dp, AuriGreenHighRes.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Rust Engine",
                            tint = AuriGreenHighRes,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "RUST ENGINE ACTIVE",
                            color = AuriGreenHighRes,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Mood Category Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AuriVioletElectric else AuriGlassSurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AuriVioletBright else AuriGlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.White else AuriTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Metrolist Speed Dial Compact 2-Column Quick Access Grid
        if (quickPicks.isNotEmpty()) {
            item {
                Text(
                    text = "Speed dial",
                    color = AuriTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val pairs = quickPicks.take(4).chunked(2)
                    for (pair in pairs) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (song in pair) {
                                SpeedDialTile(
                                    song = song,
                                    onClick = { onSongClick(song, quickPicks) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Quick Picks Carousel
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    text = "Quick picks",
                    color = AuriTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "High-fidelity tracks tailored to your listening",
                    color = AuriTextMuted,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(quickPicks) { song ->
                    QuickPickCard(
                        song = song,
                        onClick = { onSongClick(song, quickPicks) }
                    )
                }
            }
        }

        // Keep Listening (Room Play History)
        if (keepListening.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        text = "Keep listening",
                        color = AuriTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Most played on this device",
                        color = AuriTextMuted,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(keepListening) { song ->
                        QuickPickCard(
                            song = song,
                            onClick = { onSongClick(song, keepListening) }
                        )
                    }
                }
            }
        }

        // Forgotten Favorites
        if (forgottenFavorites.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        text = "Forgotten favorites",
                        color = AuriTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(forgottenFavorites) { song ->
                        QuickPickCard(
                            song = song,
                            onClick = { onSongClick(song, forgottenFavorites) }
                        )
                    }
                }
            }
        }

        // Trending Tracks Section
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    text = "Trending now",
                    color = AuriTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "160 kbps Opus & 256 kbps AAC lossless masters",
                    color = AuriTextMuted,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(displayedSongs) { song ->
            SongRowItem(
                song = song,
                onClick = { onSongClick(song, displayedSongs) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SpeedDialTile(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(AuriGlassSurface)
            .border(1.dp, AuriGlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(
            thumbnailUrl = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier.size(44.dp),
            cornerRadius = 8.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = AuriTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = AuriTextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun QuickPickCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .width(160.dp)
            .clip(shape)
            .background(AuriGlassSurface)
            .border(1.dp, AuriGlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.size(140.dp),
                cornerRadius = 12.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = song.title,
                color = AuriTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = AuriTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = song.audioQuality.split("•").firstOrNull()?.trim() ?: "HIGH RES",
                color = AuriGreenHighRes,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AuriGlassSurface)
            .border(1.dp, AuriGlassBorder, shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.size(52.dp),
                cornerRadius = 10.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = AuriTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        color = AuriTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = song.duration,
                        color = AuriTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0x228B5CF6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = AuriVioletBright,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
