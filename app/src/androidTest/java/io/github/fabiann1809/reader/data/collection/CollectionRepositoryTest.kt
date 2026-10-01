package io.github.fabiann1809.reader.data.collection

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.book.BookStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: CollectionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = DefaultCollectionRepository(database.collectionDao(), database.bookDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun addBook(
        title: String,
        status: BookStatus = BookStatus.TO_READ,
        isFavorite: Boolean = false,
        createdAt: Long = 0,
    ): Long = database.bookDao().insert(
        Book(title = title, author = "A", status = status, isFavorite = isFavorite, createdAt = createdAt),
    )

    private suspend fun titles(filter: LibraryFilter) = repository.observeBooks(filter).first().map { it.title }

    @Test
    fun createdCollectionsAreListedByName() = runTest {
        repository.createCollection("  Trabajo ")
        repository.createCollection("apuntes")

        assertEquals(listOf("apuntes", "Trabajo"), repository.observeCollections().first().map { it.name })
    }

    @Test
    fun booksCanBeAddedAndRemovedFromACollection() = runTest {
        val collectionId = repository.createCollection("Trabajo")
        val dune = addBook("Dune", createdAt = 1)
        val cosmos = addBook("Cosmos", createdAt = 2)
        addBook("Otro")

        repository.addBook(dune, collectionId)
        repository.addBook(cosmos, collectionId)
        // Adding twice is ignored.
        repository.addBook(dune, collectionId)
        assertEquals(listOf("Cosmos", "Dune"), titles(LibraryFilter.Custom(collectionId)))
        assertEquals(listOf(collectionId), repository.observeCollectionIdsOf(dune).first())

        repository.removeBook(dune, collectionId)
        assertEquals(listOf("Cosmos"), titles(LibraryFilter.Custom(collectionId)))
    }

    @Test
    fun smartCollectionsAreComputedFromEachBook() = runTest {
        addBook("Leyendo", status = BookStatus.READING, createdAt = 1)
        addBook("Terminado", status = BookStatus.FINISHED, isFavorite = true, createdAt = 2)
        addBook("Pendiente", status = BookStatus.TO_READ, createdAt = 3)

        assertEquals(listOf("Pendiente", "Terminado", "Leyendo"), titles(LibraryFilter.Smart(SmartCollection.ALL)))
        assertEquals(listOf("Terminado"), titles(LibraryFilter.Smart(SmartCollection.FAVORITES)))
        assertEquals(listOf("Leyendo"), titles(LibraryFilter.Smart(SmartCollection.READING)))
        assertEquals(listOf("Terminado"), titles(LibraryFilter.Smart(SmartCollection.FINISHED)))
        assertEquals(listOf("Pendiente"), titles(LibraryFilter.Smart(SmartCollection.TO_READ)))
    }

    @Test
    fun deletingACollectionKeepsItsBooks() = runTest {
        val collectionId = repository.createCollection("Trabajo")
        val dune = addBook("Dune")
        repository.addBook(dune, collectionId)

        repository.deleteCollection(repository.observeCollections().first().single())

        assertTrue(repository.observeCollections().first().isEmpty())
        assertEquals("Dune", database.bookDao().getById(dune)?.title)
    }

    @Test
    fun deletingABookRemovesItFromItsCollections() = runTest {
        val collectionId = repository.createCollection("Trabajo")
        val dune = addBook("Dune")
        repository.addBook(dune, collectionId)

        database.bookDao().delete(database.bookDao().getById(dune)!!)

        assertTrue(titles(LibraryFilter.Custom(collectionId)).isEmpty())
    }

    @Test
    fun renamingKeepsTheBooks() = runTest {
        val collectionId = repository.createCollection("Trabajo")
        repository.addBook(addBook("Dune"), collectionId)

        repository.renameCollection(repository.observeCollections().first().single(), "Oficina")

        assertEquals("Oficina", repository.observeCollection(collectionId).first()?.name)
        assertEquals(listOf("Dune"), titles(LibraryFilter.Custom(collectionId)))
    }
}
