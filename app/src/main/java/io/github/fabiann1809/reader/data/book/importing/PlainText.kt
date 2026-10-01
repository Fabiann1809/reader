package io.github.fabiann1809.reader.data.book.importing

import java.io.File

// Enough of the start of a file to tell text from binary data.
private const val SAMPLE_BYTES = 64 * 1024

/**
 * True when [head] (the first bytes of a file) looks like plain text: not empty and, unless it
 * starts with a UTF-16 byte order mark, free of NUL bytes, which binary files are full of.
 * Older Spanish texts are often Latin-1 rather than UTF-8, so the encoding itself is not checked.
 */
fun looksLikePlainText(head: ByteArray): Boolean {
    if (head.isEmpty()) return false
    val utf16 = head.size >= 2 &&
        ((head[0] == 0xFF.toByte() && head[1] == 0xFE.toByte()) || (head[0] == 0xFE.toByte() && head[1] == 0xFF.toByte()))
    return utf16 || head.none { it == 0.toByte() }
}

/** [looksLikePlainText] on the start of [file]. */
fun isPlainTextFile(file: File): Boolean {
    val head = file.inputStream().use { input ->
        val buffer = ByteArray(SAMPLE_BYTES)
        val read = input.read(buffer).coerceAtLeast(0)
        buffer.copyOf(read)
    }
    return looksLikePlainText(head)
}

/** True when [fileName] ends with ".[extension]", ignoring case. */
fun hasExtension(fileName: String?, extension: String): Boolean =
    fileName?.substringAfterLast('.', missingDelimiterValue = "")?.equals(extension, ignoreCase = true) == true
