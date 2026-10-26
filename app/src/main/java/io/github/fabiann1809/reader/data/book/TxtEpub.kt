package io.github.fabiann1809.reader.data.book

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Roughly a few dozen screens per file: small enough for the WebView, big enough to keep the file count low.
private const val CHUNK_CHARS = 30_000

/**
 * Readium has no plain text parser, so a TXT book is shown as a small EPUB made from it. That way
 * it gets the whole reader (selection, highlights, search, "Aa" settings) for free. The EPUB lives
 * in the cache and is rebuilt when missing or older than the TXT.
 */
object TxtEpub {

    /** Writes the EPUB of [txt] into [target], replacing it. */
    fun write(txt: File, title: String, language: String?, target: File) {
        val chunks = toChunks(decode(txt.readBytes()))
        target.parentFile?.mkdirs()
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            // The EPUB format asks for "mimetype" to be the first entry and stored as it is.
            zip.putStored("mimetype", "application/epub+zip")
            zip.putText("META-INF/container.xml", containerXml())
            zip.putText("OEBPS/content.opf", packageXml(title, language ?: DEFAULT_LANGUAGE, chunks.size))
            zip.putText("OEBPS/nav.xhtml", navXhtml(title))
            chunks.forEachIndexed { index, paragraphs -> zip.putText("OEBPS/text-$index.xhtml", chapterXhtml(title, paragraphs)) }
        }
    }

    /**
     * The text of [bytes]: UTF-16 when it starts with a byte order mark, UTF-8 when it decodes
     * cleanly, and otherwise Windows-1252, which is what old Spanish texts usually are.
     */
    fun decode(bytes: ByteArray): String {
        if (bytes.startsWith(0xFF, 0xFE)) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        if (bytes.startsWith(0xFE, 0xFF)) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        val utf8 = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            null
        }
        return (utf8 ?: String(bytes, Charset.forName("windows-1252"))).removePrefix("﻿")
    }

    /**
     * The paragraphs of [text], grouped so that each group is one file. Paragraphs are separated by
     * blank lines; a text without any has one paragraph per line instead.
     */
    fun toChunks(text: String): List<List<String>> {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val paragraphs = if (BLANK_LINE.containsMatchIn(normalized)) {
            // Hard-wrapped lines inside a paragraph are joined back into one.
            normalized.split(BLANK_LINE).map { it.lines().joinToString(" ") { line -> line.trim() } }
        } else {
            normalized.lines()
        }.map { it.trim() }.filter { it.isNotEmpty() }
        if (paragraphs.isEmpty()) return listOf(emptyList())

        val chunks = mutableListOf<MutableList<String>>()
        var size = 0
        for (paragraph in paragraphs) {
            if (chunks.isEmpty() || size >= CHUNK_CHARS) {
                chunks += mutableListOf<String>()
                size = 0
            }
            chunks.last() += paragraph
            size += paragraph.length
        }
        return chunks
    }

    private fun ByteArray.startsWith(first: Int, second: Int) =
        size >= 2 && this[0] == first.toByte() && this[1] == second.toByte()

    private val BLANK_LINE =Regex("\n[ \t]*\n")
    private const val DEFAULT_LANGUAGE = "es"

    private fun containerXml() = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>"""

    private fun packageXml(title: String, language: String, files: Int): String {
        val manifest = (0 until files).joinToString("\n") {
            """    <item id="t$it" href="text-$it.xhtml" media-type="application/xhtml+xml"/>"""
        }
        val spine = (0 until files).joinToString("\n") { """    <itemref idref="t$it"/>""" }
        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="id">txt-${title.hashCode()}</dc:identifier>
    <dc:title>${title.xmlEscaped()}</dc:title>
    <dc:language>${language.xmlEscaped()}</dc:language>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
$manifest
  </manifest>
  <spine>
$spine
  </spine>
</package>"""
    }

    // The single table of contents entry is the book itself.
    private fun navXhtml(title: String) = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
  <head><title>${title.xmlEscaped()}</title></head>
  <body><nav epub:type="toc"><ol><li><a href="text-0.xhtml">${title.xmlEscaped()}</a></li></ol></nav></body>
</html>"""

    private fun chapterXhtml(title: String, paragraphs: List<String>) = """<?xml version="1.0" encoding="UTF-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${title.xmlEscaped()}</title></head>
  <body>
${paragraphs.joinToString("\n") { "    <p>${it.xmlEscaped()}</p>" }}
  </body>
</html>"""

    // Control characters (other than tab and newline) are not allowed in XML at all.
    private fun String.xmlEscaped(): String = buildString {
        for (char in this@xmlEscaped) {
            when {
                char == '&' -> append("&amp;")
                char == '<' -> append("&lt;")
                char == '>' -> append("&gt;")
                char < ' ' && char != '\t' && char != '\n' -> Unit
                else -> append(char)
            }
        }
    }

    private fun ZipOutputStream.putText(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun ZipOutputStream.putStored(name: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val entry = ZipEntry(name).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            crc = CRC32().apply { update(bytes) }.value
        }
        putNextEntry(entry)
        write(bytes)
        closeEntry()
    }
}
