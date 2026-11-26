package com.auri.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.model.Song
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriRedHeart
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletElectric
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun QueueSheet(
    queue: List<Song>,
    currentIndex: Int,
    onSelectTrack: (Song) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onMoveItem(from.index, to.index)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = AuriVioletElectric,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Up Next Queue",
                            color = AuriTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${queue.size} tracks • Drag handle to reorder",
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

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(queue, key = { idx, s -> "${s.id}_$idx" }) { index, song ->
                    ReorderableItem(reorderableState, key = "${song.id}_$index") { isDragging ->
                        val isCurrent = index == currentIndex
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart && queue.size > 1) {
                                    onRemoveItem(index)
                                    true
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AuriRedHeart.copy(alpha = 0.25f))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = AuriRedHeart
                                    )
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isDragging -> AuriGlassSurfaceElevated
                                            isCurrent -> Color(0x338B5CF6)
                                            else -> AuriGlassSurfaceElevated.copy(alpha = 0.65f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isCurrent) AuriVioletBright else AuriGlassBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onSelectTrack(song) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArt(
                                    thumbnailUrl = song.thumbnailUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier.size(42.dp),
                                    cornerRadius = 8.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = if (isCurrent) AuriVioletBright else AuriTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • ${song.duration}",
                                        color = AuriTextSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = AuriTextMuted,
                                    modifier = Modifier
                                        .draggableHandle()
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
