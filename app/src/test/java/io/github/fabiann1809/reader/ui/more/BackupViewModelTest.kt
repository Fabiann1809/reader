package io.github.fabiann1809.reader.ui.more

import io.github.fabiann1809.reader.data.backup.BackupRepository
import io.github.fabiann1809.reader.data.backup.BackupSummary
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId

class BackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBackupRepository(var result: Result<BackupSummary>) : BackupRepository {
        val exportedTo = mutableListOf<String>()

        override suspend fun export(uri: String): Result<BackupSummary> {
            exportedTo += uri
            return result
        }
    }

    private val summary = BackupSummary(books = 3, notes = 5, flashcards = 2, files = 4)
    private val now = LocalDateTime.parse("2026-10-03T10:00:00").atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun theBackupGoesToThePickedDocumentAndSaysWhatItHolds() {
        val repository = FakeBackupRepository(Result.success(summary))
        val viewModel = BackupViewModel(repository, now = { now })
        assertEquals("reader-respaldo-2026-10-03.zip", viewModel.suggestedFileName())

        viewModel.exportTo("content://drive/backup.zip")

        assertEquals(listOf("content://drive/backup.zip"), repository.exportedTo)
        assertEquals(ExportState.Done(summary), viewModel.export.value)
    }

    @Test
    fun aFailureIsShown() {
        val viewModel = BackupViewModel(FakeBackupRepository(Result.failure(IOException("full"))), now = { now })

        viewModel.exportTo("content://x")

        assertTrue(viewModel.export.value is ExportState.Failed)
    }
}
