package io.github.fabiann1809.reader.ui.bookdetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseCurrentPageTest {

    @Test
    fun acceptsPagesWithinRange() {
        assertEquals(0, parseCurrentPage("0", totalPages = 300))
        assertEquals(300, parseCurrentPage("300", totalPages = 300))
    }

    @Test
    fun acceptsAnyNonNegativePageWhenTotalIsUnknown() {
        assertEquals(1200, parseCurrentPage("1200", totalPages = null))
    }

    @Test
    fun rejectsPagesAboveTotal() {
        assertNull(parseCurrentPage("301", totalPages = 300))
    }

    @Test
    fun rejectsEmptyOrNonNumericInput() {
        assertNull(parseCurrentPage("", totalPages = 300))
        assertNull(parseCurrentPage("abc", totalPages = null))
        assertNull(parseCurrentPage("-1", totalPages = null))
    }
}
