package com.metrolist.android.utils

import android.app.Application
import com.metrolist.shared.playback.PlaybackClientStats
import com.metrolist.shared.settings.Settings

object SentryManager {
    fun initialize(application: Application) = Unit

    suspend fun reportPlaybackClientStats() {
        PlaybackClientStats.takeReportOnLaunch()
    }

    fun setEnabled(enabled: Boolean) {
        Settings.shared.sentryEnabled = enabled
    }
}
