package io.github.fabiann1809.reader.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.backup.BackupError
import io.github.fabiann1809.reader.data.backup.BackupRepository
import io.github.fabiann1809.reader.data.backup.BackupSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

sealed interface ExportState {
    data object Idle : ExportState

    data object Exporting : ExportState

    data class Done(val summary: BackupSummary) : ExportState

    data object Failed : ExportState
}

sealed interface ImportState {
    data object Idle : ImportState

    /** A backup was picked: the user confirms it replaces everything. */
    data class Confirming(val uri: String) : ImportState

    data object Importing : ImportState

    data class Done(val summary: BackupSummary) : ImportState

    data class Failed(val reason: ImportFailure) : ImportState
}

enum class ImportFailure { NOT_A_BACKUP, NEWER_VERSION, UNREADABLE }

/** "Respaldo": exports everything to a ZIP the user saves where they like (T17.2), and restores one (T17.3). */
class BackupViewModel(
    private val backupRepository: BackupRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _export = MutableStateFlow<ExportState>(ExportState.Idle)
    val export: StateFlow<ExportState> = _export.asStateFlow()

    private val _import = MutableStateFlow<ImportState>(ImportState.Idle)
    val import: StateFlow<ImportState> = _import.asStateFlow()

    /** A backup picked in the open dialog: asks before replacing everything. */
    fun pickedBackup(uri: String) {
        if (_import.value != ImportState.Importing) _import.value = ImportState.Confirming(uri)
    }

    fun cancelImport() {
        if (_import.value is ImportState.Confirming) _import.value = ImportState.Idle
    }

    /** "Reemplazar": the library becomes the backup's. */
    fun confirmImport() {
        val uri = (_import.value as? ImportState.Confirming)?.uri ?: return
        _import.value = ImportState.Importing
        viewModelScope.launch {
            _import.value = backupRepository.import(uri).fold(
                onSuccess = { ImportState.Done(it) },
                onFailure = { error ->
                    ImportState.Failed(
                        when (error) {
                            is BackupError.NewerVersion -> ImportFailure.NEWER_VERSION
                            is BackupError.InvalidFile -> ImportFailure.NOT_A_BACKUP
                            else -> ImportFailure.UNREADABLE
                        },
                    )
                },
            )
        }
    }

    /** The file name the save dialog suggests, with today's date: "reader-respaldo-2026-10-03.zip". */
    fun suggestedFileName(): String =
        "reader-respaldo-${Instant.ofEpochMilli(now()).atZone(ZoneId.systemDefault()).toLocalDate()}.zip"

    /** The document the user picked in the save dialog. */
    fun exportTo(uri: String) {
        if (_export.value == ExportState.Exporting) return
        _export.value = ExportState.Exporting
        viewModelScope.launch {
            _export.value = backupRepository.export(uri).fold({ ExportState.Done(it) }, { ExportState.Failed })
        }
    }
}
