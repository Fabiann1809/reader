package io.github.fabiann1809.reader.data.sync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.ReaderApplication
import io.github.fabiann1809.reader.data.book.Book
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Signing out for real (T18.8): the app's own data, the user's Drive. With a book saved, "save before
 * signing out" must leave the phone empty and the book in Drive; turning the sync on again must bring it back.
 * Opt-in, needs the Drive permission given in Ajustes, skips itself when the cloud holds real data, and cleans up. Run with:
 * adb shell am instrument -w -e liveDrive true -e class io.github.fabiann1809.reader.data.sync.SignOutLiveTest ...
 */
@RunWith(AndroidJUnit4::class)
class SignOutLiveTest {

    private val container = ApplicationProvider.getApplicationContext<ReaderApplication>().container
    private val coordinator = container.syncCoordinator
    private val books = container.bookRepository
    private val cloud = container.cloudStorage

    @Before
    fun onlyWhenRequestedAndWhenThereIsNoRealData() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveDrive") == "true")
        assumeTrue("Nobody is signed in: the cloud space is the account's", ApplicationProvider.getApplicationContext<ReaderApplication>().container.authRepository.currentUser.value != null)
        assumeTrue("The cloud holds real data", runBlocking { cloud.readManifest().getOrThrow() } == null)
        assumeTrue("The phone holds real books", runBlocking { books.observeBooks().first().isEmpty() })
    }

    @Test
    fun signingOutSavesToDriveEmptiesThePhoneAndTheBookComesBack() = runBlocking {
        books.addBook(Book(title = "Sign out live test", author = "Nobody"))
        try {
            coordinator.setEnabled(true)

            // What "Cerrar sesión" does with the sync on.
            coordinator.saveBeforeSignOut("live-test-user").getOrThrow()

            assertTrue("the phone is empty", books.observeBooks().first().isEmpty())
            assertFalse("the sync is off", coordinator.status.first() != SyncStatus.Disabled)
            val saved = SyncManifest.decode(cloud.readManifest().getOrThrow()!!.content)
            assertTrue("the book is in Drive", saved.records.any { it.data.contains("Sign out live test") })

            // Signing in again and turning the sync on.
            coordinator.setEnabled(true)
            coordinator.runSync().getOrThrow()

            assertEquals(listOf("Sign out live test"), books.observeBooks().first().map { it.title })
        } finally {
            coordinator.setEnabled(false)
            container.syncSettings.update { it.copy(restoreFor = emptySet()) }
            books.observeBooks().first().forEach { books.deleteBook(it) }
            cloud.deleteFile("manifest.json")
        }
    }
}
