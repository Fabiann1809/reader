package io.github.fabiann1809.reader.data.book

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Fills in [Book.fileHash] for books imported before it existed (database version 8), so importing
 * one of them again is also noticed. Cheap once done: books that have their hash are skipped.
 */
class FileHashBackfill(private val bookRepository: BookRepository, private val bookFiles: BookFiles) {

    suspend fun run() = withContext(Dispatchers.IO) {
        val missing = bookRepository.observeBooks().first().filter { it.fileHash == null && it.filePath != null }
        for (book in missing) {
            val file = bookFiles.resolve(book.filePath ?: continue)
            if (!file.exists()) continue
            val hash = file.sha256()
            // Read again right before saving, so a change made meanwhile (e.g. a page turn) is kept.
            val current = bookRepository.getBook(book.id) ?: continue
            bookRepository.updateBook(current.copy(fileHash = hash))
        }
    }
}
