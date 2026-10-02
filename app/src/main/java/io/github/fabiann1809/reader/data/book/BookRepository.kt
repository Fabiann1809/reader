package io.github.fabiann1809.reader.data.book

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Single entry point to book data. ViewModels depend on this interface so tests can use fakes. */
interface BookRepository {
    fun observeBooks(): Flow<List<Book>>

    fun observeBook(id: Long): Flow<Book?>

    suspend fun getBook(id: Long): Book?

    /** The book whose file has this SHA-256, or null: the file is already in the library. */
    suspend fun findByFileHash(hash: String): Book?

    /** Returns the id of the new book. */
    suspend fun addBook(book: Book): Long

    suspend fun updateBook(book: Book)

    /** Also deletes the book's notes (cascade). */
    suspend fun deleteBook(book: Book)
}

class DefaultBookRepository(private val bookDao: BookDao, private val bookFiles: BookFiles) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = bookDao.observeAll()

    override fun observeBook(id: Long): Flow<Book?> = bookDao.observeById(id)

    override suspend fun getBook(id: Long): Book? = bookDao.getById(id)

    override suspend fun findByFileHash(hash: String): Book? = bookDao.findByFileHash(hash)

    override suspend fun addBook(book: Book): Long = bookDao.insert(book)

    override suspend fun updateBook(book: Book) = bookDao.update(book)

    /** Also removes the imported file and the cover, which nothing else points to. */
    override suspend fun deleteBook(book: Book) {
        val voiceNoteAudio = bookDao.voiceNoteAudio(book.id)
        bookDao.delete(book)
        withContext(Dispatchers.IO) {
            bookFiles.delete(book.filePath)
            bookFiles.delete(book.coverPath)
            // Also relative to the app's files, so BookFiles can delete them too.
            voiceNoteAudio.forEach(bookFiles::delete)
        }
    }
}
