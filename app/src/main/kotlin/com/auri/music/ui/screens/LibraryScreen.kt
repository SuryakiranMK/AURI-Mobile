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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun LibraryScreen(
    likedSongs: List<Song>,
    playbackState: PlaybackState,
    onSongClick: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuriObsidianBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Library Header
        item {
            Text(
                text = "Your Library",
                color = AuriTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Liked Songs Hero Banner Card
        item {
            val shape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(AuriGlassSurfaceElevated)
                    .border(1.dp, AuriGlassBorder, shape)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(AuriRedHeart.copy(alpha = 0.2f), CircleShape)
                                    .border(1.dp, AuriRedHeart.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Liked",
                                    tint = AuriRedHeart,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Liked Songs",
                                    color = AuriTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${likedSongs.size} tracks saved",
                                    color = AuriTextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (likedSongs.isNotEmpty()) {
                            Button(
                                onClick = { onSongClick(likedSongs.first(), likedSongs) },
                                colors = ButtonDefaults.buttonColors(containerColor = AuriVioletElectric),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play All",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Audio Engine Specifications Card
        item {
            Text(
                text = "Audio Engine Status",
                color = AuriTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val engineCardShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(engineCardShape)
                    .background(AuriGlassSurface)
                    .border(1.dp, AuriGlassBorder, engineCardShape)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Item 1: Normalizer Status
                    EngineStatusRow(
                        icon = Icons.Default.Speed,
                        iconTint = AuriGreenHighRes,
                        title = "-14.0 LUFS Normalizer",
                        subtitle = if (playbackState.isLoudnessNormalizerEnabled) {
                            "ACTIVE • Dynamic Gain: %.1f dB".format(playbackState.loudnessGainDb)
                        } else {
                            "BYPASSED"
                        }
                    )

                    // Item 2: Equalizer Preset
                    EngineStatusRow(
                        icon = Icons.Default.GraphicEq,
                        iconTint = AuriVioletBright,
                        title = "10-Band EQ Profile",
                        subtitle = playbackState.eqPreset.label
                    )

                    // Item 3: Stream Quality
                    EngineStatusRow(
                        icon = Icons.Default.Storage,
                        iconTint = AuriCyanAccent,
                        title = "Direct Stream Format",
                        subtitle = "Opus 160 kbps (VISIONOS Client Token)"
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Liked Tracks Section
        if (likedSongs.isNotEmpty()) {
            item {
                Text(
                    text = "Saved Tracks",
                    color = AuriTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(likedSongs) { song ->
                SongRowItem(
                    song = song,
                    onClick = { onSongClick(song, likedSongs) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = AuriTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = AuriTextMuted,
                fontSize = 12.sp
            )
        }
    }
}
