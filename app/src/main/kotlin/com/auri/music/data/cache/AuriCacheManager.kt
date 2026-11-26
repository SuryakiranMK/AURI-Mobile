package com.auri.music.data.cache

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.auri.music.data.AuriRepository
import com.auri.music.db.AuriDao
import com.auri.music.db.CachedAudioEntity
import com.auri.music.db.FormatEntity
import com.auri.music.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class AuriCacheManager private constructor(
    context: Context,
    maxCacheMb: Int = 1024
) {
    private val appContext = context.applicationContext
    private val databaseProvider = StandaloneDatabaseProvider(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val playerCache: SimpleCache by lazy {
        val dir = File(appContext.filesDir, "exoplayer").apply { mkdirs() }
        val evictor = if (maxCacheMb <= 0) {
            NoOpCacheEvictor()
        } else {
            LeastRecentlyUsedCacheEvictor(maxCacheMb.toLong() * 1024L * 1024L)
        }
        SimpleCache(dir, evictor, databaseProvider)
    }

    val downloadCache: SimpleCache by lazy {
        val dir = File(appContext.filesDir, "download").apply { mkdirs() }
        SimpleCache(dir, NoOpCacheEvictor(), databaseProvider)
    }

    private val urlMemoryCache = ConcurrentHashMap<String, CachedStreamEntry>()

    data class CachedStreamEntry(
        val url: String,
        val itag: Int,
        val contentLength: Long,
        val expiresAtMs: Long
    )

    fun cacheResolvedUrl(songId: String, url: String, itag: Int, contentLength: Long, expiresInSeconds: Long) {
        val expiresAtMs = System.currentTimeMillis() + (expiresInSeconds.coerceAtLeast(60L) * 1000L)
        urlMemoryCache[songId] = CachedStreamEntry(url, itag, contentLength, expiresAtMs)
    }

    fun getValidCachedUrl(songId: String): CachedStreamEntry? {
        val entry = urlMemoryCache[songId] ?: return null
        return if (System.currentTimeMillis() < entry.expiresAtMs - 60_000L) entry else null
    }

    fun isFullyCached(songId: String, expectedLength: Long): Boolean {
        if (expectedLength <= 0L) return false
        return try {
            playerCache.isCached(songId, 0L, expectedLength) ||
                downloadCache.isCached(songId, 0L, expectedLength)
        } catch (_: Throwable) {
            false
        }
    }

    fun markPlaybackCacheState(dao: AuriDao, songId: String, itag: Int, contentLength: Long) {
        scope.launch {
            val cached = isFullyCached(songId, contentLength)
            val existing = dao.getCachedAudio(songId)
            dao.upsertCachedAudio(
                CachedAudioEntity(
                    id = songId,
                    formatItag = itag,
                    contentLength = contentLength,
                    isFullyCached = cached || (existing?.isFullyCached == true),
                    isDownloaded = existing?.isDownloaded == true,
                    updatedAtMs = System.currentTimeMillis()
                )
            )
        }
    }

    fun createUpstreamFactory(): DataSource.Factory {
        return OkHttpDataSource.Factory(okHttpClient)
    }

    fun createPlaybackDataSourceFactory(
        repository: AuriRepository,
        dao: AuriDao
    ): DataSource.Factory {
        val upstream = createUpstreamFactory()
        val playerCacheFactory = CacheDataSource.Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val downloadCacheFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(playerCacheFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return ResolvingDataSource.Factory(downloadCacheFactory) { dataSpec ->
            val mediaId = dataSpec.key ?: dataSpec.uri.toString()
            if (mediaId.startsWith("http://") || mediaId.startsWith("https://")) {
                return@Factory applySubrange(dataSpec, mediaId)
            }

            val cachedMem = getValidCachedUrl(mediaId)
            if (cachedMem != null) {
                return@Factory applySubrange(
                    dataSpec.buildUpon()
                        .setUri(Uri.parse(cachedMem.url))
                        .setKey(mediaId)
                        .build(),
                    cachedMem.url
                )
            }

            val resolved = runBlocking(Dispatchers.IO) {
                val dbFormat = dao.getFormat(mediaId)
                if (dbFormat != null && System.currentTimeMillis() < dbFormat.expiresAtMs - 60_000L && dbFormat.streamUrl.isNotEmpty()) {
                    cacheResolvedUrl(
                        mediaId,
                        dbFormat.streamUrl,
                        dbFormat.itag,
                        dbFormat.contentLength,
                        (dbFormat.expiresAtMs - System.currentTimeMillis()) / 1000L
                    )
                    dbFormat.streamUrl
                } else {
                    val song = dao.getSong(mediaId)?.toModel() ?: Song(
                        id = mediaId,
                        title = "",
                        artist = "",
                        duration = "0:00",
                        durationMs = 0L,
                        thumbnailUrl = ""
                    )
                    repository.resolveStream(song).streamUrl
                }
            }

            applySubrange(
                dataSpec.buildUpon()
                    .setUri(Uri.parse(resolved))
                    .setKey(mediaId)
                    .build(),
                resolved
            )
        }
    }

    private fun applySubrange(dataSpec: DataSpec, resolvedUrl: String): DataSpec {
        if (dataSpec.length != androidx.media3.common.C.LENGTH_UNSET.toLong()) return dataSpec
        if (!resolvedUrl.contains("googlevideo.com")) return dataSpec
        return dataSpec.buildUpon()
            .setLength(CHUNK_LENGTH)
            .build()
    }

    companion object {
        private const val CHUNK_LENGTH = 512L * 1024L

        @Volatile
        private var INSTANCE: AuriCacheManager? = null

        fun getInstance(context: Context, maxCacheMb: Int = 1024): AuriCacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuriCacheManager(context, maxCacheMb).also { INSTANCE = it }
            }
        }
    }
}

class NextTrackPreloadTrigger(
    private val repository: AuriRepository,
    private val cacheManager: AuriCacheManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null
    private var lastPreloadedId: String? = null

    fun preloadNext(queue: List<Song>, currentIndex: Int) {
        val nextSong = queue.getOrNull(currentIndex + 1) ?: return
        if (nextSong.id == lastPreloadedId) return
        if (nextSong.streamUrl.isNotEmpty() || cacheManager.getValidCachedUrl(nextSong.id) != null) {
            lastPreloadedId = nextSong.id
            return
        }

        lastPreloadedId = nextSong.id
        activeJob?.cancel()
        activeJob = scope.launch {
            runCatching {
                repository.resolveStream(nextSong)
            }
        }
    }

    fun cancel() {
        activeJob?.cancel()
    }
}
