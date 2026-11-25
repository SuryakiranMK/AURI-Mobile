package com.auri.music.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.model.Lyrics
import com.auri.music.model.PlaybackState
import com.auri.music.model.RepeatMode
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
import com.auri.music.ui.theme.AuriVioletGlow

@Composable
fun NowPlayingSheet(
    playbackState: PlaybackState,
    lyrics: Lyrics,
    isLyricsLoading: Boolean = false,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleNormalizer: () -> Unit,
    onSelectEqPreset: (com.auri.music.model.EqPreset) -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return
    var showEqualizer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    BackHandler(enabled = showEqualizer || showLyrics) {
        if (showLyrics) showLyrics = false
        else if (showEqualizer) showEqualizer = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1035),
                        AuriObsidianBackground,
                        AuriObsidianBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = AuriTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM AURI",
                        color = AuriTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = song.album.ifEmpty { "High-Res Stream" },
                        color = AuriVioletBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = { showEqualizer = !showEqualizer }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "DSP Settings",
                        tint = if (showEqualizer) AuriVioletElectric else AuriTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Hero Album Art
            AlbumArt(
                thumbnailUrl = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(280.dp)
                    .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = AuriVioletGlow, spotColor = AuriVioletGlow),
                cornerRadius = 24.dp,
                elevation = 16.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Track Meta & Heart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = AuriTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        color = AuriTextSecondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onToggleLike) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like Song",
                        tint = if (song.isLiked) AuriRedHeart else AuriTextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // High Fidelity Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x2210B981))
                        .border(1.dp, AuriGreenHighRes.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = song.audioQuality,
                        color = AuriGreenHighRes,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (playbackState.isLoudnessNormalizerEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x228B5CF6))
                            .border(1.dp, AuriVioletBright.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "-14.0 LUFS (${String.format("%.1f", playbackState.loudnessGainDb)} dB)",
                            color = AuriVioletBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Scrubber Slider
            var isDraggingSlider by remember { mutableStateOf(false) }
            var sliderPosition by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

            val progress = if (isDraggingSlider) {
                sliderPosition
            } else if (playbackState.durationMs > 0) {
                (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = { frac ->
                        isDraggingSlider = true
                        sliderPosition = frac
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        val target = (sliderPosition * playbackState.durationMs).toLong()
                        onSeekTo(target)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = AuriVioletBright,
                        activeTrackColor = AuriVioletElectric,
                        inactiveTrackColor = Color(0x338B5CF6)
                    )
                )

                val displayCurrentMs = if (isDraggingSlider) {
                    (sliderPosition * playbackState.durationMs).toLong()
                } else {
                    playbackState.currentPositionMs
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(displayCurrentMs),
                        color = AuriTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatMs(playbackState.durationMs),
                        color = AuriTextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Transport Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffle) AuriVioletBright else AuriTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = AuriTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause Hero Button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(12.dp, CircleShape, ambientColor = AuriVioletGlow, spotColor = AuriVioletGlow)
                        .background(AuriVioletElectric, CircleShape)
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = AuriTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) AuriVioletBright else AuriTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Bottom Utilities Bar: Normalizer, EQ, Lyrics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleNormalizer) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Toggle Normalizer",
                        tint = if (playbackState.isLoudnessNormalizerEnabled) AuriGreenHighRes else AuriTextMuted
                    )
                }

                IconButton(onClick = { showEqualizer = !showEqualizer }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Equalizer",
                        tint = if (showEqualizer) AuriVioletElectric else AuriTextMuted
                    )
                }

                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) AuriCyanAccent else AuriTextMuted
                    )
                }
            }
        }

        // Overlay Equalizer Sheet
        AnimatedVisibility(
            visible = showEqualizer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EqualizerSheet(
                activePreset = playbackState.eqPreset,
                isLoudnessNormalizerEnabled = playbackState.isLoudnessNormalizerEnabled,
                loudnessGainDb = playbackState.loudnessGainDb,
                onPresetSelect = { onSelectEqPreset(it) },
                onLoudnessToggle = onToggleNormalizer,
                onDismiss = { showEqualizer = false }
            )
        }

        // Overlay Lyrics View
        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LyricsView(
                lyrics = lyrics,
                currentPositionMs = playbackState.currentPositionMs,
                isLoading = isLyricsLoading,
                onSeekTo = onSeekTo,
                onDismiss = { showLyrics = false }
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
