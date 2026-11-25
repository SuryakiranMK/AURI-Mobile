package com.auri.music.data

import android.content.Context
import com.auri.music.model.Lyrics
import com.auri.music.model.LyricsLine
import com.auri.music.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    fun calculateLoudnessGain(loudnessDb: Double?): Double
    fun getEngineVersion(): String
}

class DefaultAuriRepository(
    context: Context? = null
) : AuriRepository {

    private val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
    private val likedSongIds = MutableStateFlow(loadInitialLikedIds())
    private val likedSongsFlow = MutableStateFlow<List<Song>>(emptyList())

    init {
        loadSavedSongsMetadata()
        refreshLikedSongsFlow()
    }

    override fun getEngineVersion(): String {
        return AuriNativeEngine.getVersion()
    }

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
                knownSongs[song.id] = updated
                updated
            }
        }

        search(q)
    }

    override suspend fun resolveStream(song: Song): Song = withContext(Dispatchers.IO) {
        registerSong(song)
        if (song.streamUrl.isNotEmpty()) return@withContext song.copy(isLiked = isSongLiked(song.id))

        val resolved = AuriNativeEngine.resolveStream(song.id)
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

        val nativeLyrics = AuriNativeEngine.getLyrics(
            videoId = song.id,
            title = song.title,
            artist = song.artist,
            durationSec = song.durationMs / 1000
        )
        if (nativeLyrics != null && (nativeLyrics.syncedLines.isNotEmpty() || nativeLyrics.plainText.isNotEmpty())) {
            lyricsCache[song.id] = nativeLyrics
            return@withContext nativeLyrics
        }

        Lyrics(
            syncedLines = emptyList(),
            plainText = "",
            source = "No lyrics found for \"${song.title}\""
        )
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
    }

    override fun isSongLiked(songId: String): Boolean {
        return likedSongIds.value.contains(songId)
    }

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
        refreshLikedSongsFlow()
        persistLikedSongs()
        return isNowLiked
    }

    override fun getLikedSongs(): Flow<List<Song>> = likedSongsFlow.asStateFlow()

    override fun calculateLoudnessGain(loudnessDb: Double?): Double {
        return AuriNativeEngine.calculateLoudnessGain(loudnessDb)
    }

    private fun refreshLikedSongsFlow() {
        val likedIds = likedSongIds.value
        val result = likedIds.mapNotNull { id ->
            knownSongs[id]?.copy(isLiked = true)
        }
        likedSongsFlow.value = result
    }

    private fun loadInitialLikedIds(): Set<String> {
        val stored = prefs?.getStringSet(KEY_LIKED_IDS, null)
        return stored?.toSet() ?: setOf("auri_synth_01", "auri_chill_02")
    }

    private fun loadSavedSongsMetadata() {
        val raw = prefs?.getString(KEY_LIKED_METADATA, null) ?: return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", "")
                if (id.isNotEmpty() && !knownSongs.containsKey(id)) {
                    knownSongs[id] = Song(
                        id = id,
                        title = obj.optString("title", "Unknown Track"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", ""),
                        duration = obj.optString("duration", "3:30"),
                        durationMs = obj.optLong("durationMs", 210000L),
                        thumbnailUrl = obj.optString("thumbnailUrl", ""),
                        streamUrl = "",
                        isOpus = obj.optBoolean("isOpus", true),
                        audioQuality = obj.optString("audioQuality", "OPUS 160 kbps • 48 kHz"),
                        loudnessDb = if (obj.has("loudnessDb") && !obj.isNull("loudnessDb")) obj.getDouble("loudnessDb") else null,
                        isLiked = true
                    )
                }
            }
        } catch (_: Throwable) {}
    }

    private fun persistLikedSongs() {
        val p = prefs ?: return
        try {
            val likedIds = likedSongIds.value
            val arr = JSONArray()
            for (id in likedIds) {
                val s = knownSongs[id] ?: continue
                arr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("artist", s.artist)
                        put("album", s.album)
                        put("duration", s.duration)
                        put("durationMs", s.durationMs)
                        put("thumbnailUrl", s.thumbnailUrl)
                        put("isOpus", s.isOpus)
                        put("audioQuality", s.audioQuality)
                        if (s.loudnessDb != null) put("loudnessDb", s.loudnessDb)
                    }
                )
            }
            p.edit()
                .putStringSet(KEY_LIKED_IDS, likedIds)
                .putString(KEY_LIKED_METADATA, arr.toString())
                .apply()
        } catch (_: Throwable) {}
    }

    companion object {
        private const val PREFS_NAME = "auri_library_prefs"
        private const val KEY_LIKED_IDS = "liked_song_ids"
        private const val KEY_LIKED_METADATA = "liked_songs_metadata"
    }
}
