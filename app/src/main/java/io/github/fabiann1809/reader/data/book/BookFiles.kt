package io.github.fabiann1809.reader.data.book

import java.io.File
import java.util.UUID

/**
 * Book files copied into the app's private storage. [Book.filePath] holds a path relative to
 * [filesDir] (e.g. "books/1f3c….epub") so it stays valid if the app data moves (e.g. a backup restore).
 */
class BookFiles(private val filesDir: File) {

    private val booksDir: File
        get() = File(filesDir, BOOKS_DIR).apply { mkdirs() }

    /** A new, unique file in the books folder; random names avoid clashes between books with the same title. */
    fun newFile(extension: String): File = File(booksDir, "${UUID.randomUUID()}.$extension")

    fun relativePath(file: File): String = file.relativeTo(filesDir).invariantSeparatorsPath

    fun resolve(relativePath: String): File = File(filesDir, relativePath)

    fun delete(relativePath: String?) {
        if (relativePath != null) resolve(relativePath).delete()
    }

    private companion object {
        const val BOOKS_DIR = "books"
    }
}
