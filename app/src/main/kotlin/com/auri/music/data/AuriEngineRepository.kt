package com.auri.music.data

import android.content.Context
import com.auri.music.data.cache.AuriCacheManager
import com.auri.music.data.lyrics.LyricsWaterfallRepository
import com.auri.music.db.AuriDatabase
import com.auri.music.db.EventEntity
import com.auri.music.db.FormatEntity
import com.auri.music.db.RelatedSongMap
import com.auri.music.db.SongEntity
import com.auri.music.model.Lyrics
import com.auri.music.model.LyricsLine
import com.auri.music.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface AuriRepository {
    fun getQuickPicks(): List<Song>
    fun getTrending(): List<Song>
    fun getMoodCategories(): List<String>
    fun getCategoryTracks(category: String): List<Song>
    fun search(query: String): List<Song>
    suspend fun searchOnline(query: String): List<Song>
    suspend fun resolveStream(song: Song): Song
    suspend fun getLyrics(song: Song): Lyrics
    fun getLyrics(songId: String): Lyrics
    fun registerSong(song: Song)
    fun isSongLiked(songId: String): Boolean
    fun toggleLike(songId: String): Boolean
    fun getLikedSongs(): Flow<List<Song>>
    fun getCachedSongs(): Flow<List<Song>> = MutableStateFlow(emptyList())
    fun getDownloadedSongs(): Flow<List<Song>> = MutableStateFlow(emptyList())
    suspend fun getLocalQuickPicks(limit: Int = 10): List<Song> = getQuickPicks()
    suspend fun getKeepListening(limit: Int = 10): List<Song> = emptyList()
    suspend fun getForgottenFavorites(limit: Int = 10): List<Song> = emptyList()
    suspend fun recordPlayEvent(song: Song, playTimeMs: Long) {}
    suspend fun recordRelated(seedSongId: String, relatedSong: Song) {}
    fun calculateLoudnessGain(loudnessDb: Double?): Double
    fun getEngineVersion(): String
}

class DefaultAuriRepository(
    context: Context? = null
) : AuriRepository {

    private val appContext = context?.applicationContext
    private val dao = appContext?.let { AuriDatabase.getInstance(it).auriDao() }
    private val preferences = appContext?.let { AuriPreferences(it) }
    private val cacheManager = appContext?.let { AuriCacheManager.getInstance(it) }
    private val lyricsWaterfall = LyricsWaterfallRepository(dao)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val allSongs: List<Song> = listOf(
        Song(
            id = "auri_synth_01",
            title = "Midnight City Echoes",
            artist = "Neon Dreamer",
            album = "Obsidian Horizon",
            duration = "3:48",
            durationMs = 228000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            isOpus = true,
            audioQuality = "OPUS 160 kbps • 48 kHz",
            loudnessDb = 6.8,
            isLiked = true
        ),
        Song(
            id = "auri_synth_02",
            title = "Electric Resonance",
            artist = "Violet Frequency",
            album = "Quantum Pulse",
            duration = "4:12",
            durationMs = 252000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
            isOpus = true,
            audioQuality = "OPUS 160 kbps • 48 kHz",
            loudnessDb = 7.4,
            isLiked = false
        ),
        Song(
            id = "auri_chill_01",
            title = "Subtle Gravity",
            artist = "Aether Void",
            album = "Starlight Transit",
            duration = "3:15",
            durationMs = 195000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-__nbsp_.mp3",
            isOpus = true,
            audioQuality = "OPUS 160 kbps • 48 kHz",
            loudnessDb = 5.2,
            isLiked = false
        ),
        Song(
            id = "auri_chill_02",
            title = "Cybernetic Rainfall",
            artist = "Kira & The Glitch",
            album = "Neo Tokyo Dusk",
            duration = "2:58",
            durationMs = 178000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-assets/sounddogs/soundtrack.ogg",
            isOpus = true,
            audioQuality = "OPUS 160 kbps • 48 kHz",
            loudnessDb = 7.1,
            isLiked = true
        ),
        Song(
            id = "auri_audiophile_01",
            title = "Hyperdrive Odyssey",
            artist = "AURI Sound Lab",
            album = "Audiophile Showcase 2026",
            duration = "5:04",
            durationMs = 304000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/riceracer_assets/music/menu.ogg",
            isOpus = true,
            audioQuality = "OPUS 160 kbps • 48 kHz",
            loudnessDb = 8.0,
            isLiked = false
        ),
        Song(
            id = "auri_audiophile_02",
            title = "Prism Light Beam",
            artist = "Luminary Array",
            album = "Reflections in Glass",
            duration = "3:35",
            durationMs = 215000L,
            thumbnailUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            streamUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Kangaroo_MusiQue_-_The_Neverending_Story.mp3",
            isOpus = false,
            audioQuality = "AAC 256 kbps • HIGH RES",
            loudnessDb = 6.2,
            isLiked = false
        )
    )

    private val knownSongs = ConcurrentHashMap<String, Song>().apply {
        allSongs.forEach { put(it.id, it) }
    }
    private val lyricsCache = ConcurrentHashMap<String, Lyrics>()
    private val likedSongIds = MutableStateFlow(setOf("auri_synth_01", "auri_chill_02"))
    private val likedSongsMemoryFlow = MutableStateFlow<List<Song>>(emptyList())

    init {
        refreshMemoryLikedSongs()
        if (dao != null) {
            repoScope.launch {
                allSongs.forEach { song ->
                    dao.insertSongIfAbsent(SongEntity.fromModel(song))
                }
                dao.getLikedSongs().collect { entities ->
                    val ids = entities.map { it.id }.toSet()
                    likedSongIds.value = ids
                    entities.forEach { knownSongs[it.id] = it.toModel() }
                }
            }
        }
    }

    override fun getEngineVersion(): String = AuriNativeEngine.getVersion()

    override fun getQuickPicks(): List<Song> {
        val liked = likedSongIds.value
        return allSongs.take(4).map { it.copy(isLiked = liked.contains(it.id)) }
    }

    override fun getTrending(): List<Song> {
        val liked = likedSongIds.value
        return allSongs.map { it.copy(isLiked = liked.contains(it.id)) }
    }

    override fun getMoodCategories(): List<String> = listOf(
        "All", "Synthwave", "Cyber Chill", "Audiophile Master", "Deep Focus", "Late Night"
    )

    override fun getCategoryTracks(category: String): List<Song> {
        val liked = likedSongIds.value
        val list = when (category) {
            "Synthwave" -> allSongs.filter { it.title.contains("Echoes") || it.title.contains("Electric") }
            "Cyber Chill" -> allSongs.filter { it.title.contains("Gravity") || it.title.contains("Rainfall") }
            "Audiophile Master" -> allSongs.filter { it.title.contains("Odyssey") || it.title.contains("Prism") }
            else -> allSongs
        }
        return list.map { it.copy(isLiked = liked.contains(it.id)) }
    }

    override fun search(query: String): List<Song> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val liked = likedSongIds.value
        return allSongs.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }.map { it.copy(isLiked = liked.contains(it.id)) }
    }

    override suspend fun searchOnline(query: String): List<Song> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()
        val liked = likedSongIds.value

        val nativeResults = AuriNativeEngine.search(q)
        if (nativeResults.isNotEmpty()) {
            return@withContext nativeResults.map { song ->
                val updated = song.copy(isLiked = liked.contains(song.id))
                registerSong(updated)
                updated
            }
        }

        search(q)
    }

    override suspend fun resolveStream(song: Song): Song = withContext(Dispatchers.IO) {
        registerSong(song)
        if (song.streamUrl.isNotEmpty()) return@withContext song.copy(isLiked = isSongLiked(song.id))

        val preferredLang = preferences?.settings?.value?.preferredAudioLanguage ?: ""
        val resolved = AuriNativeEngine.resolveStream(song.id, preferredLang)
        if (resolved != null && resolved.url.isNotEmpty()) {
            val updated = song.copy(
                streamUrl = resolved.url,
                durationMs = if (resolved.durationMs > 0) resolved.durationMs else song.durationMs,
                isOpus = resolved.isOpus,
                audioQuality = resolved.audioQuality,
                loudnessDb = resolved.loudnessDb ?: song.loudnessDb,
                isLiked = isSongLiked(song.id)
            )
            knownSongs[updated.id] = updated

            cacheManager?.cacheResolvedUrl(
                songId = song.id,
                url = resolved.url,
                itag = resolved.itag,
                contentLength = resolved.contentLength,
                expiresInSeconds = resolved.expiresInSeconds
            )
            dao?.upsertFormat(
                FormatEntity(
                    id = song.id,
                    itag = resolved.itag,
                    mimeType = resolved.mimeType,
                    codecs = resolved.codecs,
                    bitrate = resolved.bitrate,
                    sampleRate = resolved.sampleRate,
                    contentLength = resolved.contentLength,
                    loudnessDb = resolved.loudnessDb,
                    streamUrl = resolved.url,
                    expiresAtMs = System.currentTimeMillis() + (resolved.expiresInSeconds.coerceAtLeast(60L) * 1000L)
                )
            )
            return@withContext updated
        }

        song.copy(streamUrl = allSongs.first().streamUrl, isLiked = isSongLiked(song.id))
    }

    override suspend fun getLyrics(song: Song): Lyrics = withContext(Dispatchers.IO) {
        lyricsCache[song.id]?.let { return@withContext it }

        if (song.id.startsWith("auri_")) {
            val demo = getLyrics(song.id)
            lyricsCache[song.id] = demo
            return@withContext demo
        }

        val resolved = lyricsWaterfall.getLyrics(song)
        if (resolved.syncedLines.isNotEmpty() || resolved.plainText.isNotEmpty()) {
            lyricsCache[song.id] = resolved
        }
        resolved
    }

    override fun getLyrics(songId: String): Lyrics {
        return lyricsCache[songId] ?: Lyrics(
            syncedLines = listOf(
                LyricsLine(0L, "♪ (Deep synthesizer intro playing) ♪"),
                LyricsLine(12000L, "Walking through neon streets at midnight"),
                LyricsLine(24000L, "Obsidian shadows reflecting violet light"),
                LyricsLine(36000L, "Hear the pulse of the engine inside"),
                LyricsLine(48000L, "Pure sound flowing without disguise"),
                LyricsLine(62000L, "No latency, no boundary, no delay"),
                LyricsLine(78000L, "Into the electric night we fade away"),
                LyricsLine(95000L, "♪ (Rich melodic synthesizer solo) ♪"),
                LyricsLine(120000L, "In the frequency of the stars we stand"),
                LyricsLine(140000L, "AURI guides the music in our hands")
            ),
            plainText = """
                Walking through neon streets at midnight
                Obsidian shadows reflecting violet light
                Hear the pulse of the engine inside
                Pure sound flowing without disguise
                No latency, no boundary, no delay
                Into the electric night we fade away
                In the frequency of the stars we stand
                AURI guides the music in our hands
            """.trimIndent(),
            source = "AURI Rust Engine / Demo Synced Lyrics"
        )
    }

    override fun registerSong(song: Song) {
        val existing = knownSongs[song.id]
        if (existing == null || (existing.streamUrl.isEmpty() && song.streamUrl.isNotEmpty())) {
            knownSongs[song.id] = song.copy(isLiked = likedSongIds.value.contains(song.id))
        }
        if (dao != null) {
            repoScope.launch {
                dao.insertSongIfAbsent(SongEntity.fromModel(song.copy(isLiked = likedSongIds.value.contains(song.id))))
            }
        }
    }

    override fun isSongLiked(songId: String): Boolean = likedSongIds.value.contains(songId)

    override fun toggleLike(songId: String): Boolean {
        var isNowLiked = false
        likedSongIds.update { set ->
            if (set.contains(songId)) {
                isNowLiked = false
                set - songId
            } else {
                isNowLiked = true
                set + songId
            }
        }
        knownSongs[songId]?.let { knownSongs[songId] = it.copy(isLiked = isNowLiked) }
        refreshMemoryLikedSongs()
        if (dao != null) {
            repoScope.launch {
                val song = knownSongs[songId]
                if (song != null) {
                    dao.insertSongIfAbsent(SongEntity.fromModel(song))
                }
                dao.setLiked(songId, isNowLiked, if (isNowLiked) System.currentTimeMillis() else null)
            }
        }
        return isNowLiked
    }

    override fun getLikedSongs(): Flow<List<Song>> {
        return dao?.getLikedSongs()?.map { list -> list.map { it.toModel() } }
            ?: likedSongsMemoryFlow.asStateFlow()
    }

    override fun getCachedSongs(): Flow<List<Song>> {
        return dao?.getCachedSongs()?.map { list -> list.map { it.toModel() } }
            ?: MutableStateFlow(emptyList())
    }

    override fun getDownloadedSongs(): Flow<List<Song>> {
        return dao?.getDownloadedSongs()?.map { list -> list.map { it.toModel() } }
            ?: MutableStateFlow(emptyList())
    }

    override suspend fun getLocalQuickPicks(limit: Int): List<Song> = withContext(Dispatchers.IO) {
        val local = dao?.getQuickPicks(limit)?.map { it.toModel() }.orEmpty()
        if (local.size >= 3) local else getQuickPicks()
    }

    override suspend fun getKeepListening(limit: Int): List<Song> = withContext(Dispatchers.IO) {
        dao?.getKeepListening(limit)?.map { it.toModel() }.orEmpty()
    }

    override suspend fun getForgottenFavorites(limit: Int): List<Song> = withContext(Dispatchers.IO) {
        val fourteenDaysAgo = System.currentTimeMillis() - (14L * 24L * 60L * 60L * 1000L)
        dao?.getForgottenFavorites(fourteenDaysAgo, limit)?.map { it.toModel() }.orEmpty()
    }

    override suspend fun recordPlayEvent(song: Song, playTimeMs: Long) = withContext(Dispatchers.IO) {
        if (playTimeMs < 30_000L) return@withContext
        if (preferences?.settings?.value?.pauseListenHistory == true) return@withContext
        val d = dao ?: return@withContext
        d.insertSongIfAbsent(SongEntity.fromModel(song))
        d.insertEvent(EventEntity(songId = song.id, timestampMs = System.currentTimeMillis(), playTimeMs = playTimeMs))
        d.addPlayTime(song.id, playTimeMs)
    }

    override suspend fun recordRelated(seedSongId: String, relatedSong: Song) = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext
        d.insertSongIfAbsent(SongEntity.fromModel(relatedSong))
        d.insertRelated(RelatedSongMap(songId = seedSongId, relatedSongId = relatedSong.id))
    }

    override fun calculateLoudnessGain(loudnessDb: Double?): Double {
        return AuriNativeEngine.calculateLoudnessGain(loudnessDb)
    }

    private fun refreshMemoryLikedSongs() {
        val likedIds = likedSongIds.value
        likedSongsMemoryFlow.value = likedIds.mapNotNull { id ->
            knownSongs[id]?.copy(isLiked = true)
        }
    }
}
