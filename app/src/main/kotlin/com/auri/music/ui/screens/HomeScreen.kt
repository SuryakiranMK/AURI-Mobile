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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Wifi
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
 * 1-to-1 Metrolist Home Screen Layout:
 * - Top bar: "Home" title + History, People/Listen Together, Profile Avatar icons
 * - Metrolist Filter Chip Shelf (capsule pills: Podcasts, Workout, Feel good, Energize, Romance, Relax)
 * - 3x3 Speed Dial Carousel with page indicators
 * - Quick Picks horizontal shelf with cards
 * - Keep Listening & Trending now lists
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

    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // Metrolist Top Bar: Home title on left, Action Icons on right
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Home",
                        color = scheme.onBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = scheme.onSurface)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.People, contentDescription = "Listen Together", tint = scheme.onSurface)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = scheme.onSurface)
                    }
                }
            }
        }

        // Metrolist Filter Chips Shelf
        item {
            val filterChips = remember {
                listOf("Podcasts", "Workout", "Feel good", "Energize", "Romance", "Relax")
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterChips) { chip ->
                    val isSelected = chip == selectedCategory
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) scheme.secondaryContainer else scheme.surfaceContainer,
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 700f),
                        label = "homeChipBg"
                    )
                    val chipText by animateColorAsState(
                        targetValue = if (isSelected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        label = "homeChipText"
                    )

                    Surface(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(CircleShape)
                            .border(
                                1.dp,
                                if (isSelected) scheme.primary.copy(alpha = 0.5f) else scheme.outlineVariant.copy(alpha = 0.2f),
                                CircleShape
                            )
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = if (isSelected) "All" else chip
                                },
                                role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = scheme.primary)
                            ),
                        shape = CircleShape,
                        color = chipBg
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = chip,
                                color = chipText,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Metrolist Speed Dial (3x3 Grid Carousel)
        if (quickPicks.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speed dial",
                        color = scheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        onClick = { onSongClick(quickPicks.first(), quickPicks) },
                        shape = CircleShape,
                        color = scheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Play all",
                                tint = scheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 3x3 Grid
                val dialItems = quickPicks.take(9)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    dialItems.chunked(3).forEach { rowSongs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowSongs.forEach { song ->
                                MetrolistSpeedDialCard(
                                    song = song,
                                    onClick = { onSongClick(song, quickPicks) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space if less than 3 in row
                            repeat(3 - rowSongs.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Carousel Dots Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(scheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Quick picks Shelf
        item {
            Text(
                text = "Quick picks",
                color = AuriTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(quickPicks) { song ->
                    MetrolistCarouselCard(
                        song = song,
                        onClick = { onSongClick(song, quickPicks) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Keep Listening Shelf
        if (keepListening.isNotEmpty()) {
            item {
                Text(
                    text = "Keep listening",
                    color = AuriTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(keepListening) { song ->
                        MetrolistCarouselCard(
                            song = song,
                            onClick = { onSongClick(song, keepListening) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Trending Now Section
        item {
            Text(
                text = "Trending now",
                color = AuriTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        items(displayedSongs) { song ->
            MetrolistSongRowItem(
                song = song,
                onClick = { onSongClick(song, displayedSongs) }
            )
        }
    }
}

/**
 * 3x3 Speed Dial Card matching Metrolist:
 * - Square card with rounded corners (16dp)
 * - Album artwork filling the card
 * - Dark gradient scrim at the bottom with title
 */
@Composable
private fun MetrolistSpeedDialCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, AuriGlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = AuriGlassSurfaceElevated
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 16.dp
            )

            // Bottom gradient overlay for title legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xDD08080B))
                        )
                    )
            )

            Text(
                text = song.title,
                color = AuriTextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Quick picks horizontal carousel card matching Metrolist
 */
@Composable
private fun MetrolistCarouselCard(
    song: Song,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, AuriGlassBorder, RoundedCornerShape(18.dp))
        ) {
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 18.dp
            )
            // Play overlay circle
            val scheme = MaterialTheme.colorScheme
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(scheme.primary)
                    .border(1.dp, scheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Metrolist Song Row with 3-dot options menu
 */
@Composable
private fun MetrolistSongRowItem(
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.size(50.dp),
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
