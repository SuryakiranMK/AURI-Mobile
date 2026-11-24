package com.auri.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletElectric
import com.auri.music.ui.theme.AuriVioletGlow

@Composable
fun AlbumArt(
    thumbnailUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 4.dp
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = AuriVioletGlow, spotColor = AuriVioletGlow)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1435),
                        Color(0xFF110E1C),
                        Color(0xFF0D0B14)
                    )
                )
            )
            .border(1.dp, AuriGlassBorder, shape),
        contentAlignment = Alignment.Center
    ) {
        // Gradient acoustic disc pattern
        Box(
            modifier = Modifier
                .fillMaxSize(0.7f)
                .border(
                    width = 1.dp,
                    color = AuriVioletBright.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(cornerRadius * 0.8f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = contentDescription,
                tint = AuriVioletElectric,
                modifier = Modifier.size(cornerRadius * 2)
            )
        }
    }
}
