package com.auri.music.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.model.Lyrics
import com.auri.music.model.LyricsLine
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletElectric
import java.text.Bidi
import kotlin.math.abs

/**
 * SimpMusic + InnerTune + Metrolist fused LyricsView:
 * - Frame-clock interpolated playhead (`rememberSmoothPlayhead`) for smooth word-sync
 * - 4-second manual scroll pause (`NestedScrollConnection`)
 * - AMLL depth-of-field blur (API 31+) with alpha fallback (API 24-30)
 * - Bidi RTL/LTR per-line text direction
 * - Read-side `LyricsOffsetBar` (-100ms / +100ms / Reset)
 * - `keepScreenOn = true` while LyricsView is visible
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsView(
    lyrics: Lyrics,
    currentPositionMs: Long,
    isPlaying: Boolean,
    offsetMs: Long,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val listState = rememberLazyListState()
    val plainLines = remember(lyrics.plainText) {
        lyrics.plainText.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    val smoothPosMs = rememberSmoothPlayhead(currentPositionMs, isPlaying) - offsetMs
    val activeIndex = lyrics.syncedLines.indexOfLast { it.timestampMs <= smoothPosMs }

    // 4-second pause when user manually scrolls lyrics
    var lastUserScrollTime by remember { mutableLongStateOf(0L) }
    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    lastUserScrollTime = System.currentTimeMillis()
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                lastUserScrollTime = System.currentTimeMillis()
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && System.currentTimeMillis() - lastUserScrollTime > 4000L) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, AuriGlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = AuriGlassSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Lyrics",
                        tint = AuriVioletElectric,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Synchronized Lyrics",
                            color = AuriTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lyrics.source,
                            color = AuriTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AuriTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sync Offset Bar
            LyricsOffsetBar(
                offsetMs = offsetMs,
                onAdjustOffset = onAdjustOffset,
                onResetOffset = onResetOffset
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = AuriVioletBright,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Fetching synced lyrics...",
                                color = AuriTextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                lyrics.syncedLines.isNotEmpty() -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .nestedScroll(scrollConnection),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(lyrics.syncedLines) { index, line ->
                            val isActive = index == activeIndex
                            val dist = if (activeIndex >= 0) abs(index - activeIndex) else 0
                            SyncedLyricLineRow(
                                line = line,
                                isActive = isActive,
                                distanceFromActive = dist,
                                smoothPosMs = smoothPosMs,
                                onClick = { onSeekTo(line.timestampMs + offsetMs) }
                            )
                        }
                    }
                }

                plainLines.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(plainLines) { line ->
                            Text(
                                text = line,
                                color = AuriTextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No lyrics available for this track yet.",
                            color = AuriTextMuted,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyncedLyricLineRow(
    line: LyricsLine,
    isActive: Boolean,
    distanceFromActive: Int,
    smoothPosMs: Long,
    onClick: () -> Unit
) {
    val isRtl = remember(line.text) {
        !Bidi(line.text, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT).isLeftToRight
    }
    val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val blurModifier = if (!isActive && distanceFromActive >= 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(radius = (distanceFromActive.coerceAtMost(3) * 0.8f).dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isActive) Color(0x338B5CF6) else Color.Transparent)
                .clickable(onClick = onClick)
                .then(blurModifier)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (isActive && line.words.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (word in line.words) {
                        val sung = smoothPosMs >= word.startMs
                        val activeWord = smoothPosMs in word.startMs..word.endMs
                        Text(
                            text = word.text,
                            color = when {
                                activeWord -> Color.White
                                sung -> AuriVioletBright
                                else -> AuriTextSecondary.copy(alpha = 0.55f)
                            },
                            fontSize = 20.sp,
                            fontWeight = if (activeWord || sung) FontWeight.Bold else FontWeight.SemiBold,
                            lineHeight = 28.sp
                        )
                    }
                }
            } else {
                Text(
                    text = line.text,
                    color = if (isActive) AuriVioletBright else AuriTextSecondary.copy(alpha = 0.55f),
                    fontSize = if (isActive) 20.sp else 16.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

@Composable
private fun LyricsOffsetBar(
    offsetMs: Long,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuriGlassSurfaceElevated)
            .border(1.dp, AuriGlassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sync Offset: ${if (offsetMs >= 0) "+" else ""}${offsetMs}ms",
            color = AuriTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAdjustOffset(-100L) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "-100ms", tint = AuriTextPrimary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { onAdjustOffset(100L) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Add, contentDescription = "+100ms", tint = AuriTextPrimary, modifier = Modifier.size(16.dp))
            }
            if (offsetMs != 0L) {
                IconButton(onClick = onResetOffset, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Offset", tint = AuriVioletBright, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun rememberSmoothPlayhead(rawPositionMs: Long, isPlaying: Boolean): Long {
    var smoothMs by remember { mutableLongStateOf(rawPositionMs) }
    LaunchedEffect(rawPositionMs, isPlaying) {
        if (!isPlaying) {
            smoothMs = rawPositionMs
            return@LaunchedEffect
        }
        val startWallNanos = System.nanoTime()
        val basePos = rawPositionMs
        while (true) {
            withFrameNanos { nowNanos ->
                val elapsedMs = ((nowNanos - startWallNanos) / 1_000_000L).coerceIn(0L, 400L)
                smoothMs = basePos + elapsedMs
            }
        }
    }
    return smoothMs
}
