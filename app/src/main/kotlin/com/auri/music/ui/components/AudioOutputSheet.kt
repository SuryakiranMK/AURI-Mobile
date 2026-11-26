package com.auri.music.ui.components

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auri.music.ui.theme.AuriGlassBorder
import com.auri.music.ui.theme.AuriGlassSurface
import com.auri.music.ui.theme.AuriGlassSurfaceElevated
import com.auri.music.ui.theme.AuriTextMuted
import com.auri.music.ui.theme.AuriTextPrimary
import com.auri.music.ui.theme.AuriTextSecondary
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletElectric

@Composable
fun AudioOutputSheet(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onSelectDevice: (AudioDeviceInfo?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val devices = remember {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        am?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            ?.filter {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
            }
            ?.distinctBy { "${it.type}_${it.productName}" }
            ?: emptyList()
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "Audio Output",
                        tint = AuriVioletElectric,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Audio Output Routing",
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

            Spacer(modifier = Modifier.height(14.dp))

            // Volume Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Volume",
                    tint = AuriTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = AuriVioletBright,
                        activeTrackColor = AuriVioletElectric,
                        inactiveTrackColor = Color(0x338B5CF6)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Available Output Devices",
                color = AuriTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // System Default
            OutputDeviceRow(
                icon = Icons.Default.Speaker,
                label = "System Default Route",
                subtitle = "Automatic OS routing",
                onClick = {
                    onSelectDevice(null)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            for (dev in devices) {
                val icon = when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> Icons.Default.BluetoothAudio
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> Icons.Default.PhoneAndroid
                    else -> Icons.Default.Headphones
                }
                val label = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Audio Device"
                val typeLabel = when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                    AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE Audio"
                    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio / DAC"
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
                    else -> "External Output"
                }
                OutputDeviceRow(
                    icon = icon,
                    label = label,
                    subtitle = typeLabel,
                    onClick = {
                        onSelectDevice(dev)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun OutputDeviceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuriGlassSurfaceElevated)
            .border(1.dp, AuriGlassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AuriVioletBright,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = AuriTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = AuriTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
