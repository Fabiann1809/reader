package io.github.fabiann1809.reader.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** "Respaldo" (T17.2): exports everything to a ZIP the user saves where they like. */
class BackupViewModel(
    private val backupRepository: BackupRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _export = MutableStateFlow<ExportState>(ExportState.Idle)
    val export: StateFlow<ExportState> = _export.asStateFlow()

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
