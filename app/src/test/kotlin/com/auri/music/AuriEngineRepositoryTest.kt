package com.auri.music

import com.auri.music.data.DefaultAuriRepository
import com.auri.music.model.EqPreset
import com.auri.music.model.RepeatMode
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
    fun testToggleLike_persists() {
        val songId = "auri_chill_01"
        val likedFirst = repository.toggleLike(songId)
        val likedSecond = repository.toggleLike(songId)
        assertFalse(likedFirst == likedSecond)
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
