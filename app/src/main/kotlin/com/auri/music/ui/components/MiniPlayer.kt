package com.auri.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.model.PlaybackState
import com.auri.music.ui.theme.AuriCyanAccent
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriGreenHighRes
import com.auri.music.ui.theme.AuriRedHeart
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletContainer
import com.auri.music.ui.theme.AuriVioletElectric
import kotlinx.coroutines.launch

val ExpressiveMiniPlayerHeight = 64.dp
private val ExpressiveMiniPlayerCornerRadius = 32.dp

/**
 * Metrolist-styled Expressive MiniPlayer:
 * - Floating pill shape (64.dp height, 32.dp radius)
 * - Ringed rotating / circular progress nested around artwork
 * - Fluid horizontal swipe with spring physics for skip next / previous
 * - Quick action controls (Like, Play/Pause, Skip Next)
 */
@Composable
fun MiniPlayer(
    playbackState: PlaybackState,
    onExpandClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit = {},
    isLiked: Boolean = false,
    onLikeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentSong = playbackState.currentSong ?: return
    val progress = if (playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val scope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .height(ExpressiveMiniPlayerHeight)
            .graphicsLayer {
                translationX = dragOffsetX.value
            }
            .clip(CircleShape)
            .border(1.dp, AuriGlassBorder, CircleShape)
            .pointerInput(currentSong.id) {
                detectHorizontalDragGestures(
                    onDragStart = { },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            dragOffsetX.snapTo(dragOffsetX.value + dragAmount)
                        }
                    },
                    onDragEnd = {
                        val currentVal = dragOffsetX.value
                        if (currentVal < -80f) {
                            onNextClick()
                        } else if (currentVal > 80f) {
                            onPreviousClick()
                        }
                        scope.launch {
                            dragOffsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            dragOffsetX.animateTo(0f)
                        }
                    }
                )
            },
        shape = CircleShape,
        color = AuriGlassSurfaceElevated.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Metrolist Ringed Artwork with Circular Progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clickable(onClick = onExpandClick)
            ) {
                // Circular progress ring
                Canvas(modifier = Modifier.size(50.dp)) {
                    val strokeWidth = 2.5.dp.toPx()
                    // Background track
                    drawCircle(
                        color = Color(0x338B5CF6),
                        radius = (size.minDimension - strokeWidth) / 2,
                        style = Stroke(strokeWidth)
                    )
                    // Progress arc
                    drawArc(
                        color = AuriVioletBright,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Inner Artwork
                AlbumArt(
                    thumbnailUrl = currentSong.thumbnailUrl,
                    contentDescription = currentSong.title,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    cornerRadius = 20.dp,
                    elevation = 0.dp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Metadata Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onExpandClick),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentSong.title,
                    color = AuriTextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    Text(
                        text = currentSong.artist,
                        color = AuriTextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentSong.isOpus) "OPUS" else "AAC",
                        color = AuriGreenHighRes,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0x2210B981), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Metrolist Quick Action Buttons: Like, Play/Pause, Next
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) AuriRedHeart else AuriTextMuted,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Play / Pause circular action button
            Surface(
                onClick = onPlayPauseClick,
                shape = CircleShape,
                color = AuriVioletContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = playbackState.isPlaying,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "miniPlayerPlayPause"
                    ) { playing ->
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = AuriVioletBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = AuriTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
