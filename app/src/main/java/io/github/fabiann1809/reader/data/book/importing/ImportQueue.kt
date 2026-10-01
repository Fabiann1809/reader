package io.github.fabiann1809.reader.data.book.importing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** A picked file that could not be imported, kept so it can be retried. */
@Serializable
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
 *
 * Failed files are remembered ([store]) with their read access kept ([fileAccess]), so "Reintentar"
 * still works after the app is closed and reopened.
 *
 * Call it from the main thread only; [scope] should run on it too, so its state needs no locking.
 */
class ImportQueue(
    private val importer: BookImporter,
    private val scope: CoroutineScope,
    private val fileAccess: FileAccess,
    private val store: FailedImportStore,
) {

    private val _status = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val status: StateFlow<ImportStatus> = _status.asStateFlow()

    private val pending = ArrayDeque<String>()
    private val failures = mutableListOf<FailedImport>()
    private var done = 0
    private var total = 0
    private var imported = 0
    private var worker: Job? = null

    init {
        // The error card of a batch from a previous run, unless something new already started.
        scope.launch {
            val saved = store.load()
            if (_status.value == ImportStatus.Idle) {
                if (saved.isNotEmpty()) _status.value = ImportStatus.Failed(saved, importedCount = 0)
            } else {
                // A batch started first and will replace the saved list, so those files are let go.
                saved.forEach { fileAccess.release(it.uri) }
            }
        }
    }

    fun add(uris: List<String>) {
        if (uris.isEmpty()) return
        // Access can only be kept now, while the picker's permission is fresh.
        uris.forEach(fileAccess::keep)
        if (worker?.isActive != true) {
            // A new batch replaces the error card of the previous one; files not being retried are let go.
            (_status.value as? ImportStatus.Failed)?.failures
                ?.filterNot { it.uri in uris }
                ?.forEach { fileAccess.release(it.uri) }
            resetBatch()
        }
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
        val failed = (_status.value as? ImportStatus.Failed)?.failures ?: return
        failed.forEach { fileAccess.release(it.uri) }
        _status.value = ImportStatus.Idle
        scope.launch { store.save(emptyList()) }
    }

    private suspend fun importPending() {
        while (pending.isNotEmpty()) {
            val uri = pending.removeFirst()
            when (val result = importer.import(uri)) {
                is ImportResult.Imported -> {
                    imported++
                    // The book is now a copy in private storage; the original is no longer needed.
                    fileAccess.release(uri)
                }
                is ImportResult.Unsupported -> failures += FailedImport(uri, result.fileName, isUnsupported = true)
                is ImportResult.Failed -> failures += FailedImport(uri, result.fileName, isUnsupported = false)
            }
            done++
            _status.value = ImportStatus.Importing(done, total)
        }
        store.save(failures.toList())
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
