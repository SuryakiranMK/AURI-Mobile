package com.auri.music.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/**
 * Monitors Bluetooth A2DP / wired audio route changes so playback can pause or recover cleanly.
 */
class BluetoothA2dpMonitor(
    context: Context,
    private val onRouteDisconnected: () -> Unit
) {
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var registered = false

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val hadBtOrWired = removedDevices.any {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
            if (hadBtOrWired) {
                onRouteDisconnected()
            }
        }
    }

    fun start() {
        if (registered) return
        runCatching {
            audioManager?.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
            registered = true
        }
    }

    fun stop() {
        if (!registered) return
        runCatching {
            audioManager?.unregisterAudioDeviceCallback(callback)
        }
        registered = false
    }
}
