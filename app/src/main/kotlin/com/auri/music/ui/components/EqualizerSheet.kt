package com.auri.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.model.EqPreset
import com.auri.music.ui.theme.AuriCyanAccent
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriGreenHighRes
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletContainer
import com.auri.music.ui.theme.AuriVioletElectric

@Composable
fun EqualizerSheet(
    activePreset: EqPreset,
    isLoudnessNormalizerEnabled: Boolean,
    loudnessGainDb: Double,
    onPresetSelect: (EqPreset) -> Unit,
    onLoudnessToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, AuriGlassBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        colors = CardDefaults.cardColors(containerColor = AuriGlassSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "DSP Settings",
                        tint = AuriVioletElectric,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AURI DSP Audio Engine",
                        color = AuriTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AuriTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loudness Normalizer Card (-7.0 LUFS)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AuriGlassSurfaceElevated)
                    .border(1.dp, AuriGlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0x2210B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Loudness",
                                tint = AuriGreenHighRes,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "-14.0 LUFS Normalizer",
                                    color = AuriTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "YTM STANDARD",
                                    color = AuriGreenHighRes,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(Color(0x2210B981), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = if (isLoudnessNormalizerEnabled) {
                                    "Dynamic gain: %.1f dB • Headroom active".format(loudnessGainDb)
                                } else {
                                    "Bypassed (Original unadjusted volume)"
                                },
                                color = AuriTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isLoudnessNormalizerEnabled,
                        onCheckedChange = { onLoudnessToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AuriGreenHighRes,
                            uncheckedThumbColor = AuriTextMuted,
                            uncheckedTrackColor = Color(0x33282836)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 10-Band Graphic Equalizer Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Equalizer",
                    tint = AuriVioletBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "10-Band Graphic Equalizer Presets",
                    color = AuriTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preset Selection Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EqPreset.entries.toTypedArray()) { preset ->
                    val isSelected = preset == activePreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AuriVioletElectric else AuriGlassSurfaceElevated)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AuriVioletBright else AuriGlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onPresetSelect(preset) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.label,
                            color = if (isSelected) Color.White else AuriTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visualizer of 10 Frequency Bands for the active preset
            val frequencies = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
            val gains = activePreset.gains

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AuriGlassSurfaceElevated)
                    .border(1.dp, AuriGlassBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    frequencies.forEachIndexed { index, freq ->
                        val gain = gains[index]
                        val barHeight = ((gain + 12f) / 24f).coerceIn(0.1f, 1f) * 60f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = if (gain > 0) "+%.0f".format(gain) else "%.0f".format(gain),
                                color = if (gain != 0f) AuriCyanAccent else AuriTextMuted,
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (gain > 0f) AuriVioletElectric
                                        else if (gain < 0f) AuriCyanAccent
                                        else Color(0x448B5CF6)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = freq,
                                color = AuriTextMuted,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
