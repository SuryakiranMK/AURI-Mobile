package com.auri.music.ui.components

import android.media.AudioDeviceInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.model.Lyrics
import com.auri.music.model.PlaybackState
import com.auri.music.model.RepeatMode
import com.auri.music.model.Song
import com.auri.music.ui.components.shader.ArtworkBackgroundShader
import com.auri.music.ui.components.sliders.SquigglyProgress
import com.auri.music.ui.theme.AuriCyanAccent
import com.auri.music.ui.theme.AuriGlassBorder
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
import kotlinx.coroutines.delay

/**
 * 1-to-1 Metrolist Full-Screen Player:
 * - Top Bar: Down arrow, Audio Output pill in center, 3-dot More menu
 * - Hero Artwork with rounded corners (24dp) & double tap +/-10s seek
 * - Metadata line with Share & Like action buttons
 * - High-Res audio quality tags
 * - Progress slider with timestamp counters
 * - Metrolist Pill Playback Controls: Circular Previous, Large Pill Play/Pause, Circular Next
 * - Bottom Tool Tray: Queue, Sleep Timer, Shuffle, Lyrics, Repeat, EQ
 */
@Composable
fun NowPlayingSheet(
    playbackState: PlaybackState,
    lyrics: Lyrics,
    isLyricsLoading: Boolean = false,
    lyricsOffsetMs: Long = 0L,
    onAdjustLyricsOffset: (Long) -> Unit = {},
    onResetLyricsOffset: () -> Unit = {},
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekBy: (Long) -> Unit = {},
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleNormalizer: () -> Unit,
    onSelectEqPreset: (com.auri.music.model.EqPreset) -> Unit,
    onVolumeChange: (Float) -> Unit = {},
    onSelectAudioDevice: (AudioDeviceInfo?) -> Unit = {},
    onSelectQueueTrack: (Song) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveQueueItem: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return
    var showEqualizer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showOutputSheet by remember { mutableStateOf(false) }
    var doubleTapHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(doubleTapHint) {
        if (doubleTapHint != null) {
            delay(700L)
            doubleTapHint = null
        }
    }

    BackHandler(enabled = showEqualizer || showLyrics || showQueue || showOutputSheet) {
        when {
            showLyrics -> showLyrics = false
            showEqualizer -> showEqualizer = false
            showQueue -> showQueue = false
            showOutputSheet -> showOutputSheet = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        // Metrolist-inspired 128px Artwork Ambient AGSL Shader
        ArtworkBackgroundShader(
            thumbnailUrl = song.thumbnailUrl,
            isPlaying = playbackState.isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Down arrow, Audio Output pill in center, More menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
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

                // Audio Output Pill in top-center
                Surface(
                    onClick = { showOutputSheet = true },
                    shape = CircleShape,
                    color = AuriGlassSurfaceElevated.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuriGlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = AuriVioletBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Phone speaker",
                            color = AuriTextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

            Spacer(modifier = Modifier.height(10.dp))

            // Large Hero Album Art with Double-Tap +/-10s Seek
            Box(
                modifier = Modifier
                    .size(290.dp)
                    .pointerInput(song.id) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                if (offset.x < size.width / 2f) {
                                    onSeekBy(-10_000L)
                                    doubleTapHint = "-10s"
                                } else {
                                    onSeekBy(10_000L)
                                    doubleTapHint = "+10s"
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AlbumArt(
                    thumbnailUrl = song.thumbnailUrl,
                    contentDescription = song.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(24.dp, RoundedCornerShape(24.dp), ambientColor = AuriVioletGlow, spotColor = AuriVioletGlow),
                    cornerRadius = 24.dp,
                    elevation = 16.dp
                )

                this@Column.AnimatedVisibility(
                    visible = doubleTapHint != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xAA08080B))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = doubleTapHint.orEmpty(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track Meta, Share & Like buttons
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
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = song.artist,
                        color = AuriTextSecondary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = AuriTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like Song",
                            tint = if (song.isLiked) AuriRedHeart else AuriTextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // High Fidelity Badges Row
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
                        text = if (song.isOpus) "OPUS 160 kbps • 48 kHz" else "AAC 256 kbps • 44.1 kHz",
                        color = AuriGreenHighRes,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x228B5CF6))
                        .border(1.dp, AuriVioletBright.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "-14.0 LUFS (-6.8 dB)",
                        color = AuriVioletBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Squiggly Waveform Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                val progressFrac = if (playbackState.durationMs > 0) {
                    (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                SquigglyProgress(
                    progress = progressFrac,
                    isPlaying = playbackState.isPlaying,
                    onSeekFraction = { frac ->
                        onSeekTo((frac * playbackState.durationMs).toLong())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    activeColor = AuriVioletBright,
                    inactiveColor = Color(0x33A78BFA)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatMs(playbackState.currentPositionMs), color = AuriTextMuted, fontSize = 12.sp)
                    Text(text = formatMs(playbackState.durationMs), color = AuriTextMuted, fontSize = 12.sp)
                }
            }

            // Metrolist Pill Playback Controls: Circular Previous, Large Pill Play/Pause, Circular Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Previous button
                val scheme = MaterialTheme.colorScheme
                Surface(
                    onClick = onPrevious,
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Large Pill Play/Pause button (Metrolist style)
                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = scheme.primary,
                    modifier = Modifier
                        .width(130.dp)
                        .height(60.dp)
                        .shadow(12.dp, CircleShape)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedContent(
                            targetState = playbackState.isPlaying,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "playerPlayPause"
                        ) { isPlaying ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = scheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPlaying) "Pause" else "Play",
                                    color = scheme.onPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Circular Next button
                Surface(
                    onClick = onNext,
                    shape = CircleShape,
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = scheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Metrolist Bottom Tool Tray: Queue, Sleep Timer, Shuffle, Lyrics, Repeat, More
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showQueue = !showQueue }) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (showQueue) AuriVioletBright else AuriTextSecondary
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Sleep timer",
                        tint = AuriTextSecondary
                    )
                }

                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffle) AuriVioletBright else AuriTextSecondary
                    )
                }

                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) AuriCyanAccent else AuriTextSecondary
                    )
                }

                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) AuriVioletBright else AuriTextSecondary
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = AuriTextSecondary
                    )
                }
            }
        }

        // Animated Equalizer Sheet
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

        // Animated Audio Output Sheet
        AnimatedVisibility(
            visible = showOutputSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AudioOutputSheet(
                volume = playbackState.volume,
                onVolumeChange = onVolumeChange,
                onSelectDevice = onSelectAudioDevice,
                onDismiss = { showOutputSheet = false }
            )
        }

        // Animated Lyrics View
        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LyricsView(
                lyrics = lyrics,
                currentPositionMs = playbackState.currentPositionMs,
                isPlaying = playbackState.isPlaying,
                offsetMs = lyricsOffsetMs,
                onAdjustOffset = onAdjustLyricsOffset,
                onResetOffset = onResetLyricsOffset,
                onSeekTo = onSeekTo,
                onDismiss = { showLyrics = false },
                isLoading = isLyricsLoading
            )
        }

        // Animated Queue Sheet
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            QueueSheet(
                queue = playbackState.queue,
                currentIndex = playbackState.queueIndex,
                onSelectTrack = onSelectQueueTrack,
                onMoveItem = onMoveQueueItem,
                onRemoveItem = onRemoveQueueItem,
                onDismiss = { showQueue = false }
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
