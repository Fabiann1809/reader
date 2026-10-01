package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.book.importing.FailedImport
import io.github.fabiann1809.reader.data.book.importing.FailedImportStore
import io.github.fabiann1809.reader.data.book.importing.FileAccess

/** Records which files the code keeps access to. */
class FakeFileAccess : FileAccess {
    val kept = mutableSetOf<String>()

    override fun keep(uri: String) {
        kept += uri
    }

    override fun release(uri: String) {
        kept -= uri
    }
}

/** In-memory [FailedImportStore], optionally holding a list saved by a "previous run". */
class FakeFailedImportStore(var saved: List<FailedImport> = emptyList()) : FailedImportStore {
    override suspend fun load(): List<FailedImport> = saved

    override suspend fun save(failures: List<FailedImport>) {
        saved = failures
    }
}
