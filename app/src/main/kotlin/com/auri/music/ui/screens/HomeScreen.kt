package com.auri.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.R
import com.auri.music.model.Song
import com.auri.music.ui.components.AlbumArt
import com.auri.music.ui.theme.AuriCyanAccent
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriGreenHighRes
import com.auri.music.ui.theme.AuriObsidianBackground
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletContainer
import com.auri.music.ui.theme.AuriVioletElectric

/**
 * Metrolist-Styled Home Screen:
 * - Clean Brand Header with Rust Status pill
 * - Metrolist Filter Chip Shelf (capsule pills with spring animations)
 * - 2x2 Speed Dial Quick Access Cards
 * - Metrolist Horizontal Multi-Browse Carousels (Quick Picks, Keep Listening, Forgotten Favorites, Trending)
 */
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
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // App Branding Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x2210B981))
                        .border(1.dp, AuriGreenHighRes.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Rust Engine",
                            tint = AuriGreenHighRes,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "RUST DSP",
                            color = AuriGreenHighRes,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Metrolist Chip Shelf
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) AuriVioletContainer else AuriGlassSurfaceElevated,
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                        label = "homeChipBg"
                    )
                    val chipBorder by animateColorAsState(
                        targetValue = if (isSelected) AuriVioletBright.copy(alpha = 0.6f) else AuriGlassBorder,
                        label = "homeChipBorder"
                    )
                    val chipText by animateColorAsState(
                        targetValue = if (isSelected) AuriVioletBright else AuriTextSecondary,
                        label = "homeChipText"
                    )

                    Surface(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, chipBorder, CircleShape)
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
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
                                text = category,
                                color = chipText,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Metrolist Speed Dial (2x2 Grid)
        if (quickPicks.isNotEmpty()) {
            item {
                Text(
                    text = "Speed Dial",
                    color = AuriTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                val speedPicks = quickPicks.take(4)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    speedPicks.chunked(2).forEach { rowSongs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSongs.forEach { song ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, AuriGlassBorder, RoundedCornerShape(12.dp))
                                        .clickable { onSongClick(song, quickPicks) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = AuriGlassSurfaceElevated
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AlbumArt(
                                            thumbnailUrl = song.thumbnailUrl,
                                            contentDescription = song.title,
                                            modifier = Modifier.size(60.dp),
                                            cornerRadius = 0.dp
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 10.dp)
                                        ) {
                                            Text(
                                                text = song.title,
                                                color = AuriTextPrimary,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artist,
                                                color = AuriTextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowSongs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Quick Picks Shelf
        item {
            Text(
                text = "Quick Picks",
                color = AuriTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(quickPicks) { song ->
                    MetrolistCard(
                        song = song,
                        onClick = { onSongClick(song, quickPicks) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Keep Listening Shelf
        if (keepListening.isNotEmpty()) {
            item {
                Text(
                    text = "Keep Listening",
                    color = AuriTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(keepListening) { song ->
                        MetrolistCard(
                            song = song,
                            onClick = { onSongClick(song, keepListening) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Forgotten Favorites Shelf
        if (forgottenFavorites.isNotEmpty()) {
            item {
                Text(
                    text = "Forgotten Favorites",
                    color = AuriTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(forgottenFavorites) { song ->
                        MetrolistCard(
                            song = song,
                            onClick = { onSongClick(song, forgottenFavorites) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // Trending Now Section
        item {
            Text(
                text = "Trending Now",
                color = AuriTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        items(displayedSongs) { song ->
            MetrolistSongRow(
                song = song,
                onClick = { onSongClick(song, displayedSongs) }
            )
        }
    }
}

@Composable
private fun MetrolistCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, AuriGlassBorder, RoundedCornerShape(16.dp))
        ) {
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 16.dp
            )
            // Play overlay circle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AuriVioletContainer)
                    .border(1.dp, AuriVioletBright.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = AuriVioletBright,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = AuriTextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
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
    }
}

@Composable
private fun MetrolistSongRow(
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.size(48.dp),
                cornerRadius = 10.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = AuriTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            Text(
                text = "${(song.durationMs / 1000) / 60}:${((song.durationMs / 1000) % 60).toString().padStart(2, '0')}",
                color = AuriTextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
