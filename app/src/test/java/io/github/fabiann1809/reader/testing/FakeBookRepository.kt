package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory BookRepository for ViewModel tests. */
class FakeBookRepository(initialBooks: List<Book> = emptyList()) : BookRepository {

    private val books = MutableStateFlow(initialBooks)
    private var nextId = (initialBooks.maxOfOrNull { it.id } ?: 0) + 1

    val currentBooks: List<Book> get() = books.value

    override fun observeBooks(): Flow<List<Book>> = books.map { list -> list.sortedByDescending { it.createdAt } }

    override fun observeBook(id: Long): Flow<Book?> = books.map { list -> list.find { it.id == id } }

    override suspend fun getBook(id: Long): Book? = books.value.find { it.id == id }

    override suspend fun addBook(book: Book): Long {
        val id = nextId++
        books.update { it + book.copy(id = id) }
        return id
    }

    override suspend fun updateBook(book: Book) {
        books.update { list -> list.map { if (it.id == book.id) book else it } }
    }

    override suspend fun deleteBook(book: Book) {
        books.update { list -> list.filterNot { it.id == book.id } }
    }
}
