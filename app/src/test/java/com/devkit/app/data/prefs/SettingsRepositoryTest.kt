package com.devkit.app.data.prefs

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Verifies theme and preference persistence via DataStore (real, not mocked). */
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private fun repo() = SettingsRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun `theme selection persists`() = runTest {
        val repository = repo()
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.settings.first().themeMode)
    }

    @Test
    fun `accent and dynamic color persist`() = runTest {
        val repository = repo()
        repository.setAccent(AccentChoice.TEAL)
        repository.setDynamicColor(true)
        val settings = repository.settings.first()
        assertEquals(AccentChoice.TEAL, settings.accent)
        assertTrue(settings.dynamicColor)
    }

    @Test
    fun `numeric settings are clamped on write and read`() = runTest {
        val repository = repo()
        repository.setJsonIndent(99)
        repository.setDefaultTimeout(999)
        repository.setFontScale(5f)
        val settings = repository.settings.first()
        assertEquals(8, settings.jsonIndent)
        assertEquals(120, settings.defaultTimeoutSeconds)
        assertEquals(1.6f, settings.fontSizeScale, 0.001f)
    }

    @Test
    fun `history retention persists`() = runTest {
        val repository = repo()
        repository.setHistoryRetention(HistoryRetention.FOREVER)
        assertEquals(HistoryRetention.FOREVER, repository.settings.first().historyRetention)
    }
}
