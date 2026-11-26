package com.auri.music

import com.auri.music.data.DefaultAuriRepository
import com.auri.music.data.lyrics.LyricsWaterfallRepository
import com.auri.music.model.EqPreset
import com.auri.music.model.LyricSyncType
import com.auri.music.model.RepeatMode
import com.auri.music.model.Song
import com.auri.music.player.AudioOffloadEligibility
import com.auri.music.player.CrossfadeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuriEngineRepositoryTest {

    private lateinit var repository: DefaultAuriRepository

    @Before
    fun setUp() {
        repository = DefaultAuriRepository()
    }

    @Test
    fun testLoudnessNormalizerCalculation_matchesRustEngine() {
        // Track with loudnessDb 7.77 -> -7.77 dB gain
        val gain = repository.calculateLoudnessGain(7.77)
        assertEquals(-7.77, gain, 0.01)

        // Track with negative loudnessDb (quiet master) -> +1.5 dB boost
        val boost = repository.calculateLoudnessGain(-1.5)
        assertEquals(1.5, boost, 0.01)

        // Capped at MAX_BOOST_DB (+3.0 dB)
        val capped = repository.calculateLoudnessGain(-10.0)
        assertEquals(3.0, capped, 0.01)

        // Capped at MIN_CUT_DB (-24.0 dB)
        val minCut = repository.calculateLoudnessGain(30.0)
        assertEquals(-24.0, minCut, 0.01)

        // Null loudness metadata fallback (-4.5 dB)
        val fallback = repository.calculateLoudnessGain(null)
        assertEquals(-4.5, fallback, 0.01)
    }

    @Test
    fun testQuickPicks_containsOpusAndHighResAudio() {
        val quickPicks = repository.getQuickPicks()
        assertTrue(quickPicks.isNotEmpty())
        for (song in quickPicks) {
            assertNotNull(song.id)
            assertNotNull(song.title)
            assertNotNull(song.artist)
            assertTrue(song.durationMs > 0)
            assertTrue(song.audioQuality.contains("OPUS") || song.audioQuality.contains("AAC"))
        }
    }

    @Test
    fun testSearch_filtersCorrectly() {
        val results = repository.search("Midnight")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.title.contains("Midnight") })

        val emptyResults = repository.search("NonExistentQueryString12345")
        assertTrue(emptyResults.isEmpty())
    }

    @Test
    fun testLyrics_hasSyncedTimestampsAndSource() {
        val lyrics = repository.getLyrics("auri_synth_01")
        assertNotNull(lyrics)
        assertTrue(lyrics.syncedLines.isNotEmpty())
        assertTrue(lyrics.source.contains("AURI"))
        assertTrue(lyrics.syncedLines.first().timestampMs >= 0L)
    }

    @Test
    fun testToggleLike_persistsAndUpdatesLikedSongsFlow() = runTest {
        val onlineSong = Song(
            id = "yt_test_123",
            title = "Blinding Lights",
            artist = "The Weeknd",
            duration = "3:20",
            durationMs = 200000L,
            thumbnailUrl = "https://example.com/art.jpg"
        )
        repository.registerSong(onlineSong)
        val likedFirst = repository.toggleLike(onlineSong.id)
        assertTrue(likedFirst)
        assertTrue(repository.getLikedSongs().first().any { it.id == onlineSong.id })

        val likedSecond = repository.toggleLike(onlineSong.id)
        assertFalse(likedSecond)
        assertFalse(repository.getLikedSongs().first().any { it.id == onlineSong.id })
    }

    @Test
    fun testMoodCategories_excludesGamingModes() {
        val categories = repository.getMoodCategories()
        assertFalse(categories.any { it.contains("Gaming", ignoreCase = true) })
    }

    @Test
    fun testLyricsParsers_lrcAndTtmlWordSync() {
        val waterfall = LyricsWaterfallRepository(null)
        val lrc = waterfall.parseLrc("[00:12.50]Hello world\n[00:25.00]Second line", "LRCLIB")
        assertNotNull(lrc)
        assertEquals(2, lrc!!.syncedLines.size)
        assertEquals(12500L, lrc.syncedLines[0].timestampMs)
        assertEquals(LyricSyncType.LINE_SYNCED, lrc.syncType)

        val ttml = """
            <tt><body><div>
              <p begin="00:05.000" end="00:08.000">
                <span begin="00:05.000" end="00:06.200">Blinded</span>
                <span begin="00:06.200" end="00:08.000">by lights</span>
              </p>
            </div></body></tt>
        """.trimIndent()
        val rich = waterfall.parseTtmlLyrics(ttml, "BetterLyrics")
        assertNotNull(rich)
        assertEquals(LyricSyncType.RICH_SYNCED, rich!!.syncType)
        assertEquals(2, rich.syncedLines[0].words.size)
        assertEquals("Blinded", rich.syncedLines[0].words[0].text)
    }

    @Test
    fun testAudioOffloadEligibility_andCrossfadeGuard() {
        val eligible = AudioOffloadEligibility(
            eqPreset = EqPreset.FLAT,
            normalizerEnabled = false,
            crossfadeActive = false
        )
        assertTrue(eligible.isEligible)

        val ineligibleEq = AudioOffloadEligibility(
            eqPreset = EqPreset.BASS_PUNCH,
            normalizerEnabled = false
        )
        assertFalse(ineligibleEq.isEligible)

        val s1 = Song("1", "Track 1", "Artist", "Same Album", "3:00", 180000L, "")
        val s2 = Song("2", "Track 2", "Artist", "Same Album", "3:00", 180000L, "")
        val s3 = Song("3", "Track 3", "Artist", "Other Album", "3:00", 180000L, "")
        assertFalse(CrossfadeManager.shouldCrossfade(true, s1, s2, RepeatMode.OFF))
        assertTrue(CrossfadeManager.shouldCrossfade(true, s1, s3, RepeatMode.OFF))
        assertEquals(0f, CrossfadeManager.quinticCurve(0f), 0.001f)
        assertEquals(1f, CrossfadeManager.quinticCurve(1f), 0.001f)
    }

    @Test
    fun testEqPresets_haveValid10Bands() {
        for (preset in EqPreset.entries) {
            assertEquals(10, preset.gains.size)
            for (gain in preset.gains) {
                assertTrue(gain in -12.0f..12.0f)
            }
        }
    }

    @Test
    fun testRepeatMode_enumCycles() {
        val modes = RepeatMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(RepeatMode.OFF))
        assertTrue(modes.contains(RepeatMode.ALL))
        assertTrue(modes.contains(RepeatMode.ONE))
    }
}
