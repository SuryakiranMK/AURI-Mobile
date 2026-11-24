package com.auri.music.data

import com.auri.music.model.EqPreset
import com.auri.music.model.Lyrics
import com.auri.music.model.LyricsLine
import com.auri.music.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface AuriRepository {
    fun getQuickPicks(): List<Song>
    fun getTrending(): List<Song>
    fun getMoodCategories(): List<String>
    fun getCategoryTracks(category: String): List<Song>
    fun search(query: String): List<Song>
    fun getLyrics(songId: String): Lyrics
    fun toggleLike(songId: String): Boolean
    fun getLikedSongs(): Flow<List<Song>>
    fun calculateLoudnessGain(loudnessDb: Double?): Double
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
     * Port of AURI's -14.0 LUFS broadcast normalizer formula:
     * gain_db = (-raw).coerceIn(-24.0, 3.0)
     */
    override fun calculateLoudnessGain(loudnessDb: Double?): Double {
        return when {
            loudnessDb != null && loudnessDb.isFinite() -> (-loudnessDb).coerceIn(-24.0, 3.0)
            else -> -4.5
        }
    }
}
