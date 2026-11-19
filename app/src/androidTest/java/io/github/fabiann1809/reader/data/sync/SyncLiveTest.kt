package io.github.fabiann1809.reader.data.sync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.ReaderApplication
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.data.sync.merge.SyncSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The whole background sync for real (T18.6): the app's own database, WorkManager and the user's Drive.
 * A change is made and has to reach the cloud by itself, a deletion too. Opt-in, needs the Drive
 * permission given in Ajustes, and skips itself when the cloud holds real data. It cleans up after itself:
 * the test book is deleted and the cloud is emptied. Run with:
 * adb shell am instrument -w -e liveDrive true -e class io.github.fabiann1809.reader.data.sync.SyncLiveTest ...
 */
@RunWith(AndroidJUnit4::class)
class SyncLiveTest {

    private val container = ApplicationProvider.getApplicationContext<ReaderApplication>().container
    private val coordinator = container.syncCoordinator
    private val cloud = container.cloudStorage

    @Before
    fun onlyWhenRequestedAndWhenThereIsNoRealData() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveDrive") == "true")
        assumeTrue("Nobody is signed in: the cloud space is the account's", ApplicationProvider.getApplicationContext<ReaderApplication>().container.authRepository.currentUser.value != null)
        assumeTrue("The cloud holds real data", runBlocking { cloud.readManifest().getOrThrow() } == null)
    }

    @Test
    fun aChangeReachesTheCloudByItselfAndSoDoesItsDeletion() = runBlocking {
        val book = Book(title = "Live sync test", author = "Nobody")
        val id = container.bookRepository.addBook(book)
        try {
            coordinator.onAppOpened()
            coordinator.setEnabled(true)

            waitUntil("the book reaches the cloud") { cloudHasTheBook() }
            waitUntil("the status says synced") { coordinator.status.first() is SyncStatus.Synced }

            container.bookRepository.deleteBook(container.bookRepository.getBook(id)!!)
            waitUntil("the deletion reaches the cloud") { !cloudHasTheBook() }
            assertTrue(snapshot().deletions.isNotEmpty())
        } finally {
            coordinator.setEnabled(false)
            container.bookRepository.getBook(id)?.let { container.bookRepository.deleteBook(it) }
            cloud.deleteFile("manifest.json")
        }
    }

    private suspend fun snapshot() = cloud.readManifest().getOrThrow()?.let { SyncManifest.decode(it.content) } ?: SyncSnapshot()

    private suspend fun cloudHasTheBook() = snapshot().records.any { it.data.contains("Live sync test") }

    private suspend fun waitUntil(what: String, check: suspend () -> Boolean) {
        withTimeout(TIMEOUT_MILLIS) {
            while (!check()) delay(POLL_MILLIS)
        }.also { println("ok: $what") }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 120_000L
        const val POLL_MILLIS = 2_000L
    }
}
