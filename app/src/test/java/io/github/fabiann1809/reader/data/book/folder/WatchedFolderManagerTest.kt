package io.github.fabiann1809.reader.data.book.folder

import io.github.fabiann1809.reader.testing.FakeFileAccess
import io.github.fabiann1809.reader.testing.FakeFolderLister
import io.github.fabiann1809.reader.testing.FakeFolderSyncScheduler
import io.github.fabiann1809.reader.testing.FakeWatchedFolderStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchedFolderManagerTest {

    private val store = FakeWatchedFolderStore()
    private val access = FakeFileAccess()
    private val scheduler = FakeFolderSyncScheduler()
    private val manager = WatchedFolderManager(store, FakeFolderLister(), access, scheduler)

    @Test
    fun watchingKeepsAccessRemembersTheFolderAndStartsChecking() = runTest {
        manager.watch("content://tree/Libros", fallbackName = "Carpeta")

        assertEquals(WatchedFolder("content://tree/Libros", "Libros"), manager.folder.first())
        assertEquals(setOf("content://tree/Libros"), access.kept)
        assertTrue(scheduler.isRunning)
    }

    @Test
    fun anUnreadableNameFallsBack() = runTest {
        manager.watch("content://tree/", fallbackName = "Carpeta")

        assertEquals("Carpeta", manager.folder.first()?.name)
    }

    @Test
    fun choosingAnotherFolderLetsGoOfThePreviousOne() = runTest {
        manager.watch("content://tree/Libros", "Carpeta")
        store.markHandled(listOf("a.epub"))

        manager.watch("content://tree/Comics", "Carpeta")

        assertEquals(setOf("content://tree/Comics"), access.kept)
        // Files of the old folder no longer count as handled.
        assertTrue(store.handled.isEmpty())
    }

    @Test
    fun stoppingForgetsTheFolderAndItsChecks() = runTest {
        manager.watch("content://tree/Libros", "Carpeta")

        manager.stopWatching()

        assertNull(manager.folder.first())
        assertTrue(access.kept.isEmpty())
        assertFalse(scheduler.isRunning)
    }

    @Test
    fun openingTheAppChecksOnlyWhenWatching() = runTest {
        manager.checkOnAppStart()
        assertEquals(0, scheduler.extraChecks)

        manager.watch("content://tree/Libros", "Carpeta")
        manager.checkOnAppStart()
        assertEquals(1, scheduler.extraChecks)
    }
}
