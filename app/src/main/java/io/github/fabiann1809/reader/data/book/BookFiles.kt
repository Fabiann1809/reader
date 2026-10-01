package io.github.fabiann1809.reader.data.book

import android.graphics.Bitmap
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Book files copied into the app's private storage. [Book.filePath] holds a path relative to
 * [filesDir] (e.g. "books/1f3c….epub") so it stays valid if the app data moves (e.g. a backup restore).
 */
class BookFiles(private val filesDir: File) {

    /** A new, unique file in the books folder; random names avoid clashes between books with the same title. */
    fun newFile(extension: String): File = newFileIn(BOOKS_DIR, extension)

    /** Saves a cover as JPEG and returns its relative path, or null if it could not be written. */
    fun saveCover(cover: Bitmap): String? {
        val file = newFileIn(COVERS_DIR, "jpg")
        return try {
            file.outputStream().use { cover.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, it) }
            relativePath(file)
        } catch (e: IOException) {
            file.delete()
            null
        }
    }

    fun relativePath(file: File): String = file.relativeTo(filesDir).invariantSeparatorsPath

    fun resolve(relativePath: String): File = File(filesDir, relativePath)

    fun delete(relativePath: String?) {
        if (relativePath != null) resolve(relativePath).delete()
    }

    private fun newFileIn(dir: String, extension: String): File =
        File(File(filesDir, dir).apply { mkdirs() }, "${UUID.randomUUID()}.$extension")

    private companion object {
        const val BOOKS_DIR = "books"
        const val COVERS_DIR = "covers"
        const val COVER_QUALITY = 85
    }
}
