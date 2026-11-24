package com.auri.music.ui.main

import com.auri.music.data.DefaultAuriRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {

    @Test
    fun testRepositoryQuickPicksAndTrending() {
        val repo = DefaultAuriRepository()
        val quickPicks = repo.getQuickPicks()
        val trending = repo.getTrending()

        assertTrue(quickPicks.isNotEmpty())
        assertTrue(trending.isNotEmpty())
        assertEquals(4, quickPicks.size)
    }

    @Test
    fun testMoodCategories_containsExpectedGenres() {
        val repo = DefaultAuriRepository()
        val categories = repo.getMoodCategories()

        assertTrue(categories.contains("Synthwave"))
        assertTrue(categories.contains("Cyber Chill"))
        assertTrue(categories.contains("Audiophile Master"))
    }

    @Test
    fun testCategoryFiltering() {
        val repo = DefaultAuriRepository()
        val synth = repo.getCategoryTracks("Synthwave")
        assertTrue(synth.isNotEmpty())
        for (song in synth) {
            assertTrue(song.title.contains("Echoes") || song.title.contains("Electric"))
        }
    }
}
