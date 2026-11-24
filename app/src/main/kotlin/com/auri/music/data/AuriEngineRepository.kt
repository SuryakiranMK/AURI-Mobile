package com.auri.music.data

import com.auri.music.model.Lyrics
import com.auri.music.model.LyricsLine
import com.auri.music.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

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
    fun toggleLike(songId: String): Boolean
    fun getLikedSongs(): Flow<List<Song>>
    fun calculateLoudnessGain(loudnessDb: Double?): Double
    fun getEngineVersion(): String
}

class DefaultAuriRepository : AuriRepository {

    private val likedSongIds = MutableStateFlow<Set<String>>(setOf("auri_synth_01", "auri_chill_02"))
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
        "All", "Synthwave", "Cyber Chill", "Audiophile Master", "Gaming Flow", "Deep Focus"
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

        // Try native Rust Innertube search engine
        val nativeResults = AuriNativeEngine.search(q)
        if (nativeResults.isNotEmpty()) {
            return@withContext nativeResults.map { it.copy(isLiked = liked.contains(it.id)) }
        }

        // Fallback to local catalog
        search(q)
    }

    override suspend fun resolveStream(song: Song): Song = withContext(Dispatchers.IO) {
        if (song.streamUrl.isNotEmpty()) return@withContext song

        // Attempt stream resolution from YouTube Music via Rust Innertube client
        val resolved = AuriNativeEngine.resolveStream(song.id)
        if (resolved != null && resolved.url.isNotEmpty()) {
            return@withContext song.copy(
                streamUrl = resolved.url,
                durationMs = if (resolved.durationMs > 0) resolved.durationMs else song.durationMs,
                isOpus = resolved.isOpus,
                audioQuality = resolved.audioQuality,
                loudnessDb = resolved.loudnessDb ?: song.loudnessDb
            )
        }

        // Fallback to demo audio asset if offline or stream unavailable
        song.copy(streamUrl = allSongs.first().streamUrl)
    }

    override suspend fun getLyrics(song: Song): Lyrics = withContext(Dispatchers.IO) {
        val nativeLyrics = AuriNativeEngine.getLyrics(
            videoId = song.id,
            title = song.title,
            artist = song.artist,
            durationSec = song.durationMs / 1000
        )
        if (nativeLyrics != null && (nativeLyrics.syncedLines.isNotEmpty() || nativeLyrics.plainText.isNotEmpty())) {
            return@withContext nativeLyrics
        }
        getLyrics(song.id)
    }

    override fun getLyrics(songId: String): Lyrics {
        return Lyrics(
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
            source = "AURI Rust Engine / Innertube Synced Lyrics"
        )
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
        return isNowLiked
    }

    override fun getLikedSongs(): Flow<List<Song>> {
        val flow = MutableStateFlow<List<Song>>(emptyList())
        val liked = likedSongIds.value
        flow.value = allSongs.filter { liked.contains(it.id) }.map { it.copy(isLiked = true) }
        return flow.asStateFlow()
    }

    /**
     * AURI's -14.0 LUFS broadcast normalizer:
     * Delegates to native Rust DSP calculation.
     */
    override fun calculateLoudnessGain(loudnessDb: Double?): Double {
        return AuriNativeEngine.calculateLoudnessGain(loudnessDb)
    }
}
