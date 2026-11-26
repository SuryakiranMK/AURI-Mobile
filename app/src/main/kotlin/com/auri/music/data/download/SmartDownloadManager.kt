package com.auri.music.data.download

import android.content.Context
import android.net.ConnectivityManager
import com.auri.music.data.AuriPreferences
import com.auri.music.data.AuriRepository
import com.auri.music.db.AuriDao
import com.auri.music.db.CachedAudioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow

/**
 * Decay-scored automatic Wi-Fi caching of most-played tracks over a 30-day window
 * (14-day half-life decay), modeled after SimpMusic's Smart Downloads.
 */
class SmartDownloadManager(
    private val context: Context,
    private val dao: AuriDao,
    private val preferences: AuriPreferences,
    private val repository: AuriRepository
) {
    suspend fun runIfDue() = withContext(Dispatchers.IO) {
        val settings = preferences.settings.value
        if (!settings.smartDownloadsEnabled) return@withContext
        if (!isOnUnmeteredNetwork()) return@withContext

        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24L * 60L * 60L * 1000L
        val halfLifeDays = 14.0
        val events = dao.getEventsSince(now - thirtyDaysMs)
        if (events.isEmpty()) return@withContext

        val scores = HashMap<String, Double>()
        for (ev in events) {
            val ageDays = ((now - ev.timestampMs).coerceAtLeast(0L)) / (1000.0 * 60.0 * 60.0 * 24.0)
            val weight = 0.5.pow(ageDays / halfLifeDays)
            val durationScore = (ev.playTimeMs / 30_000.0).coerceAtLeast(0.5)
            scores[ev.songId] = (scores[ev.songId] ?: 0.0) + (weight * durationScore)
        }

        val topSongIds = scores.entries
            .sortedByDescending { it.value }
            .take(settings.smartDownloadsLimit)
            .map { it.key }

        for (songId in topSongIds) {
            val cached = dao.getCachedAudio(songId)
            if (cached?.isDownloaded == true) continue
            val song = dao.getSong(songId)?.toModel() ?: continue
            val resolved = runCatching { repository.resolveStream(song) }.getOrNull() ?: continue
            if (resolved.streamUrl.isNotEmpty()) {
                AuriDownloadHolder.enqueueDownload(context, resolved.id, resolved.streamUrl, resolved.title)
                dao.upsertCachedAudio(
                    CachedAudioEntity(
                        id = resolved.id,
                        formatItag = 251,
                        contentLength = 0L,
                        isFullyCached = cached?.isFullyCached == true,
                        isDownloaded = true,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun isOnUnmeteredNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return !cm.isActiveNetworkMetered
    }
}
