package io.github.fabiann1809.reader.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookAppearanceTest {

    @Test
    fun booksStartHiddenAndEndInPlace() {
        assertEquals(0f, bookAppearanceProgress(elapsedMillis = 0f, index = 0, reduceMotion = false), 0f)
        assertEquals(1f, bookAppearanceProgress(elapsedMillis = 300f, index = 0, reduceMotion = false), 0.001f)
    }

    @Test
    fun eachBookStartsThirtyMillisAfterThePreviousOne() {
        assertEquals(0f, bookAppearanceProgress(elapsedMillis = 30f, index = 1, reduceMotion = false), 0f)
        assertTrue(bookAppearanceProgress(elapsedMillis = 31f, index = 1, reduceMotion = false) > 0f)
        assertEquals(1f, bookAppearanceProgress(elapsedMillis = 330f, index = 1, reduceMotion = false), 0.001f)
    }

    @Test
    fun booksFarDownShareTheLastDelay() {
        assertEquals(
            bookAppearanceProgress(elapsedMillis = 500f, index = 12, reduceMotion = false),
            bookAppearanceProgress(elapsedMillis = 500f, index = 200, reduceMotion = false),
        )
        assertEquals(1f, bookAppearanceProgress(elapsedMillis = 660f, index = 200, reduceMotion = false), 0.001f)
    }

    @Test
    fun reduceMotionFadesEveryBookTogetherInAHundredMillis() {
        assertEquals(0.5f, bookAppearanceProgress(elapsedMillis = 50f, index = 0, reduceMotion = true), 0.001f)
        assertEquals(0.5f, bookAppearanceProgress(elapsedMillis = 50f, index = 9, reduceMotion = true), 0.001f)
        assertEquals(1f, bookAppearanceProgress(elapsedMillis = 100f, index = 9, reduceMotion = true), 0f)
    }
}
