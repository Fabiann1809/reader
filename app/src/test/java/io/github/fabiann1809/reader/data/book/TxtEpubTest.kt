package io.github.fabiann1809.reader.data.book

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipFile

class TxtEpubTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun blankLinesSeparateParagraphsAndWrappedLinesAreJoined() {
        val chunks = TxtEpub.toChunks("Primera línea\ncontinúa aquí.\n\nSegundo párrafo.\r\n\r\nTercero.")

        assertEquals(listOf(listOf("Primera línea continúa aquí.", "Segundo párrafo.", "Tercero.")), chunks)
    }

    @Test
    fun withoutBlankLinesEveryLineIsAParagraph() {
        assertEquals(listOf(listOf("Uno", "Dos")), TxtEpub.toChunks("Uno\nDos\n"))
    }

    @Test
    fun longTextsAreSplitIntoSeveralFiles() {
        val paragraph = "x".repeat(10_000)
        val chunks = TxtEpub.toChunks(List(7) { paragraph }.joinToString("\n\n"))

        assertEquals(listOf(3, 3, 1), chunks.map { it.size })
    }

    @Test
    fun decodesUtf8Utf16AndFallsBackToWindows1252() {
        assertEquals("Canción", TxtEpub.decode("Canción".toByteArray(Charsets.UTF_8)))
        assertEquals("Canción", TxtEpub.decode(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "Canción".toByteArray(Charsets.UTF_16LE)))
        assertEquals("Canción", TxtEpub.decode("Canción".toByteArray(charset("windows-1252"))))
    }

    @Test
    fun theEpubHasTheMimetypeFirstAndTheEscapedText() {
        val txt = folder.newFile("libro.txt").apply { writeText("Tom & <Jerry>\n\nFin.") }
        val epub = folder.root.resolve("out/libro.epub")

        TxtEpub.write(txt, "Tom & Jerry", "es", epub)

        ZipFile(epub).use { zip ->
            assertEquals("mimetype", zip.entries().nextElement().name)
            val chapter = zip.getInputStream(zip.getEntry("OEBPS/text-0.xhtml")).reader().readText()
            assertTrue(chapter.contains("<p>Tom &amp; &lt;Jerry&gt;</p>"))
            assertTrue(zip.getInputStream(zip.getEntry("OEBPS/content.opf")).reader().readText().contains("<dc:title>Tom &amp; Jerry</dc:title>"))
        }
    }
}
