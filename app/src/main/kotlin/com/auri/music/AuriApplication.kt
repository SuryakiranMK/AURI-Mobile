package com.auri.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.auri.music.data.AuriPreferences
import com.auri.music.data.DefaultAuriRepository
import com.auri.music.data.cache.AuriCacheManager
import com.auri.music.data.lyrics.LyricsWaterfallRepository
import com.auri.music.data.sponsorblock.SponsorBlockRepository
import com.auri.music.db.AuriDatabase
import com.auri.music.db.PrefsToRoomMigrator
import com.auri.music.network.NetworkFirstInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuriApplication : Application(), ImageLoaderFactory {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AuriDatabase by lazy { AuriDatabase.getInstance(this) }
    val preferences: AuriPreferences by lazy { AuriPreferences(this) }
    val cacheManager: AuriCacheManager by lazy {
        AuriCacheManager.getInstance(this, preferences.settings.value.maxSongCacheSizeMb)
    }
    val sponsorBlock: SponsorBlockRepository by lazy { SponsorBlockRepository() }
    val lyricsWaterfall: LyricsWaterfallRepository by lazy {
        LyricsWaterfallRepository(database.auriDao())
    }
    val repository: DefaultAuriRepository by lazy { DefaultAuriRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch {
            PrefsToRoomMigrator.migrateIfNeeded(this@AuriApplication, database.auriDao())
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(cacheManager.okHttpClient)
            .components {
                add(NetworkFirstInterceptor())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_art_cache"))
                    .maxSizeBytes(128L * 1024L * 1024L)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    companion object {
        @Volatile
        lateinit var instance: AuriApplication
            private set
    }
}
