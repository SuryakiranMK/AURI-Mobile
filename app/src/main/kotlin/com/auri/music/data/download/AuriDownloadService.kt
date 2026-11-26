package com.auri.music.data.download

import android.app.Notification
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.auri.music.R
import com.auri.music.data.cache.AuriCacheManager
import java.util.concurrent.Executors

@OptIn(UnstableApi::class)
object AuriDownloadHolder {
    @Volatile
    private var downloadManager: DownloadManager? = null

    fun getDownloadManager(context: Context): DownloadManager {
        return downloadManager ?: synchronized(this) {
            downloadManager ?: buildDownloadManager(context.applicationContext).also {
                downloadManager = it
            }
        }
    }

    private fun buildDownloadManager(context: Context): DownloadManager {
        val cacheManager = AuriCacheManager.getInstance(context)
        val dbProvider = StandaloneDatabaseProvider(context)
        val upstreamFactory = cacheManager.createUpstreamFactory()

        return DownloadManager(
            context,
            dbProvider,
            cacheManager.downloadCache,
            upstreamFactory,
            Executors.newFixedThreadPool(3)
        ).apply {
            maxParallelDownloads = 3
        }
    }

    fun enqueueDownload(context: Context, songId: String, streamUrl: String, title: String) {
        val finalUrl = if (streamUrl.contains("googlevideo.com") && !streamUrl.contains("range=")) {
            "$streamUrl&range=0-100000000"
        } else {
            streamUrl
        }
        val request = DownloadRequest.Builder(songId, android.net.Uri.parse(finalUrl))
            .setCustomCacheKey(songId)
            .setData(title.toByteArray())
            .build()
        DownloadService.sendAddDownload(
            context,
            AuriDownloadService::class.java,
            request,
            false
        )
    }
}

@OptIn(UnstableApi::class)
class AuriDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.app_name,
    0
) {
    override fun getDownloadManager(): DownloadManager {
        return AuriDownloadHolder.getDownloadManager(applicationContext)
    }

    override fun getScheduler(): Scheduler {
        return PlatformScheduler(this, JOB_ID)
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return DownloadNotificationHelper(this, CHANNEL_ID).buildProgressNotification(
            this,
            R.mipmap.ic_launcher,
            null,
            null,
            downloads,
            notMetRequirements
        )
    }

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 2002
        private const val JOB_ID = 2003
        private const val CHANNEL_ID = "auri_download_channel"
    }
}
