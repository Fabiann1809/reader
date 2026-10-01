package io.github.fabiann1809.reader.data.book.importing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A picked file that could not be imported, kept so it can be retried. */
data class FailedImport(val uri: String, val fileName: String?, val isUnsupported: Boolean)

/** What the library shows about imports (design 1f). */
sealed interface ImportStatus {
    data object Idle : ImportStatus

    /** "Importando 7 libros… 3 de 7": [done] files are finished out of [total]. */
    data class Importing(val done: Int, val total: Int) : ImportStatus

    /** Shown until the user retries or discards. [importedCount] books of the same batch did make it. */
    data class Failed(val failures: List<FailedImport>, val importedCount: Int) : ImportStatus
}

/**
 * Imports files one at a time (copying several big files at once would only compete for the disk).
 * There is one per app (see AppContainer), so files picked in the library and files shared from
 * other apps join the same batch, and the counter keeps growing instead of resetting.
 * Call it from the main thread only; [scope] should run on it too, so its state needs no locking.
 */
class ImportQueue(private val importer: BookImporter, private val scope: CoroutineScope) {

    private val _status = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val status: StateFlow<ImportStatus> = _status.asStateFlow()

    private val pending = ArrayDeque<String>()
    private val failures = mutableListOf<FailedImport>()
    private var done = 0
    private var total = 0
    private var imported = 0
    private var worker: Job? = null

    fun add(uris: List<String>) {
        if (uris.isEmpty()) return
        // A new batch replaces the error card of the previous one.
        if (worker?.isActive != true) resetBatch()
        pending.addAll(uris)
        total += uris.size
        _status.value = ImportStatus.Importing(done, total)
        if (worker?.isActive != true) worker = scope.launch { importPending() }
    }

    /** "Reintentar": imports again every file that failed. */
    fun retry() {
        val failed = (_status.value as? ImportStatus.Failed)?.failures ?: return
        add(failed.map { it.uri })
    }

    /** "Descartar": forgets the failed files. */
    fun dismiss() {
        if (_status.value is ImportStatus.Failed) _status.value = ImportStatus.Idle
    }

    private suspend fun importPending() {
        while (pending.isNotEmpty()) {
            val uri = pending.removeFirst()
            when (val result = importer.import(uri)) {
                is ImportResult.Imported -> imported++
                is ImportResult.Unsupported -> failures += FailedImport(uri, result.fileName, isUnsupported = true)
                is ImportResult.Failed -> failures += FailedImport(uri, result.fileName, isUnsupported = false)
            }
            done++
            _status.value = ImportStatus.Importing(done, total)
        }
        // When everything worked there is nothing to say: the new books are already on the shelves.
        _status.value = if (failures.isEmpty()) ImportStatus.Idle else ImportStatus.Failed(failures.toList(), imported)
    }

    private fun resetBatch() {
        failures.clear()
        done = 0
        total = 0
        imported = 0
    }
}
