package io.github.fabiann1809.reader.data.book

import java.io.File
import java.security.MessageDigest

private const val BUFFER_SIZE = 64 * 1024

/** The SHA-256 of the file's bytes, in hex: the same file always gives the same text. */
fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
