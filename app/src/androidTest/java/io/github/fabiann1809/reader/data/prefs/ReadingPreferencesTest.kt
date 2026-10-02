package io.github.fabiann1809.reader.data.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The real DataStore, on a file of its own so the app's settings are left alone. */
@RunWith(AndroidJUnit4::class)
class ReadingPreferencesTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // One file per test: DataStore refuses two live stores on the same file in one process.
    private val testFile = "reading_preferences_test_${System.nanoTime()}"

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(testFile).delete()
    }

    @Test
    fun aNewStoreStartsWithTheDefaults() = runTest {
        assertEquals(ReadingSettings(), DataStoreReadingPreferences(context, testFile).settings.first())
    }

    @Test
    fun savedSettingsComeBackAndOutOfRangeValuesAreClamped() = runTest {
        val preferences = DataStoreReadingPreferences(context, testFile)

        preferences.setSettings(
            ReadingSettings(
                theme = ReadingTheme.AMOLED,
                font = ReadingFont.OPEN_DYSLEXIC,
                fontSize = 9.0,
                lineHeight = 2.2,
                margins = 0.1,
                alignment = ReadingAlignment.JUSTIFY,
                showClock = false,
                showBattery = false,
                showPage = true,
                keepScreenOn = true,
                customBackground = 0xFFE6F5EC,
                customText = 0xFF22533D,
            ),
        )
        val read = preferences.settings.first()

        assertEquals(ReadingTheme.AMOLED, read.theme)
        assertEquals(ReadingFont.OPEN_DYSLEXIC, read.font)
        assertEquals(ReadingSettings.FontSizeRange.endInclusive, read.fontSize, 0.0001)
        assertEquals(2.2, read.lineHeight, 0.0001)
        assertEquals(ReadingSettings.MarginsRange.start, read.margins, 0.0001)
        assertEquals(ReadingAlignment.JUSTIFY, read.alignment)
        assertEquals(listOf(false, false, true, true), listOf(read.showClock, read.showBattery, read.showPage, read.keepScreenOn))
        assertEquals(0xFFE6F5EC, read.customBackground)
        assertEquals(0xFF22533D, read.customText)
    }
}
