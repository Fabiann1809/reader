package io.github.fabiann1809.reader.data.book.importing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainTextTest {

    @Test
    fun textInUtf8OrLatin1IsText() {
        assertTrue(looksLikePlainText("Érase una vez…".toByteArray(Charsets.UTF_8)))
        assertTrue(looksLikePlainText("Érase una vez".toByteArray(Charsets.ISO_8859_1)))
    }

    @Test
    fun utf16WithByteOrderMarkIsText() {
        // UTF-16 is full of NUL bytes, so only its byte order mark tells it apart from binary data.
        assertTrue(looksLikePlainText("Hola".toByteArray(Charsets.UTF_16)))
    }

    @Test
    fun binaryDataOrAnEmptyFileIsNotText() {
        assertFalse(looksLikePlainText(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00)))
        assertFalse(looksLikePlainText(ByteArray(0)))
    }

    @Test
    fun extensionIsCheckedIgnoringCase() {
        assertTrue(hasExtension("Cuento.TXT", "txt"))
        assertFalse(hasExtension("cuento.txt.zip", "txt"))
        assertFalse(hasExtension("txt", "txt"))
        assertFalse(hasExtension(null, "txt"))
    }
}
