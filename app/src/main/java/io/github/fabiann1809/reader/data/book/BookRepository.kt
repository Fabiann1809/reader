package io.github.fabiann1809.reader.data.book

import kotlinx.coroutines.flow.Flow

/** Single entry point to book data. ViewModels depend on this interface so tests can use fakes. */
interface BookRepository {
    fun observeBooks(): Flow<List<Book>>

    fun observeBook(id: Long): Flow<Book?>

    suspend fun getBook(id: Long): Book?

    /** Returns the id of the new book. */
    suspend fun addBook(book: Book): Long

    suspend fun updateBook(book: Book)

    /** Also deletes the book's notes (cascade). */
    suspend fun deleteBook(book: Book)
}

class DefaultBookRepository(private val bookDao: BookDao) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = bookDao.observeAll()

    override fun observeBook(id: Long): Flow<Book?> = bookDao.observeById(id)

    override suspend fun getBook(id: Long): Book? = bookDao.getById(id)

    override suspend fun addBook(book: Book): Long = bookDao.insert(book)

    override suspend fun updateBook(book: Book) = bookDao.update(book)

    override suspend fun deleteBook(book: Book) = bookDao.delete(book)
}
