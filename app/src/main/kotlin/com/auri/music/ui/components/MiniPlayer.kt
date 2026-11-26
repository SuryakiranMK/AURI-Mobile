package com.auri.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.model.PlaybackState
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriGreenHighRes
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletElectric

@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onExpandClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentSong = playbackState.currentSong ?: return
    val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val shape = RoundedCornerShape(16.dp)
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(shape)
            .background(AuriGlassSurfaceElevated.copy(alpha = 0.96f))
            .border(1.dp, AuriGlassBorder, shape)
            .pointerInput(currentSong.id) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount -> swipeOffsetX += dragAmount },
                    onDragEnd = {
                        if (swipeOffsetX < -90f) {
                            onNextClick()
                        } else if (swipeOffsetX > 90f) {
                            onPreviousClick()
                        }
                        swipeOffsetX = 0f
                    },
                    onDragCancel = { swipeOffsetX = 0f }
                )
            }
            .clickable(onClick = onExpandClick)
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = AuriVioletElectric,
                trackColor = Color(0x338B5CF6),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(
                    thumbnailUrl = currentSong.thumbnailUrl,
                    contentDescription = currentSong.title,
                    modifier = Modifier.size(46.dp),
                    cornerRadius = 10.dp,
                    elevation = 2.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        color = AuriTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentSong.artist,
                            color = AuriTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentSong.isOpus) "OPUS" else "AAC",
                            color = AuriGreenHighRes,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0x2210B981), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier
                        .size(38.dp)
                        .background(AuriVioletElectric, CircleShape)
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Next",
                        tint = AuriTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
