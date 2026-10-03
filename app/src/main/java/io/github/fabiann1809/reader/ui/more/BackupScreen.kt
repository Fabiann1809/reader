package io.github.fabiann1809.reader.ui.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.settings.SettingsDivider
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

@Composable
fun BackupScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val export by viewModel.export.collectAsStateWithLifecycle()
    val import by viewModel.import.collectAsStateWithLifecycle()
    val openDialog = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.pickedBackup(uri.toString())
    }
    // The system's save dialog: the user picks where the ZIP goes (Drive, Descargas...), no permission needed.
    val saveDialog = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ZIP_MIME_TYPE)) { uri ->
        if (uri != null) viewModel.exportTo(uri.toString())
    }
    BackupContent(
        export = export,
        onExport = { saveDialog.launch(viewModel.suggestedFileName()) },
        onNavigateUp = onNavigateUp,
        modifier = modifier,
        import = import,
        onImport = { openDialog.launch(arrayOf(ZIP_MIME_TYPE)) },
    )
    if (import is ImportState.Confirming) {
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) {
                    Text(stringResource(R.string.backup_import_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** "Respaldo" (T17.2): what a backup holds, and exporting it. */
@Composable
fun BackupContent(
    export: ExportState,
    onExport: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    import: ImportState = ImportState.Idle,
    onImport: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = { ReaderTopAppBar(title = stringResource(R.string.backup_title), onNavigateUp = onNavigateUp) },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.backup_export_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.backup_export_message), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_shield_check),
                    contentDescription = null,
                    tint = ReaderTheme.colors.success,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    stringResource(R.string.backup_no_key),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrimaryButton(
                text = stringResource(R.string.backup_export),
                onClick = onExport,
                enabled = export != ExportState.Exporting,
                icon = R.drawable.ic_archive,
                modifier = Modifier.fillMaxWidth(),
            )
            ExportResult(export)
            SettingsDivider()
            Text(stringResource(R.string.backup_import_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.backup_import_message), style = MaterialTheme.typography.bodyMedium)
            OutlineButton(
                text = stringResource(R.string.backup_import),
                onClick = onImport,
                enabled = import != ImportState.Importing,
                modifier = Modifier.fillMaxWidth(),
            )
            ImportResult(import)
        }
    }
}

@Composable
private fun ExportResult(export: ExportState) {
    when (export) {
        ExportState.Idle -> Unit
        ExportState.Exporting -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.backup_exporting), style = MaterialTheme.typography.bodyMedium)
        }
        is ExportState.Done -> Text(
            stringResource(
                R.string.backup_exported,
                export.summary.books,
                export.summary.notes,
                export.summary.flashcards,
                export.summary.files,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = ReaderTheme.colors.success,
        )
        ExportState.Failed -> Text(
            stringResource(R.string.backup_export_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ImportResult(import: ImportState) {
    when (import) {
        ImportState.Idle, is ImportState.Confirming -> Unit
        ImportState.Importing -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.backup_importing), style = MaterialTheme.typography.bodyMedium)
        }
        is ImportState.Done -> Text(
            stringResource(R.string.backup_imported, import.summary.books, import.summary.notes, import.summary.flashcards, import.summary.files),
            style = MaterialTheme.typography.bodyMedium,
            color = ReaderTheme.colors.success,
        )
        is ImportState.Failed -> Text(
            stringResource(
                when (import.reason) {
                    ImportFailure.NOT_A_BACKUP -> R.string.backup_import_not_a_backup
                    ImportFailure.NEWER_VERSION -> R.string.backup_import_newer
                    ImportFailure.UNREADABLE -> R.string.backup_import_unreadable
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private const val ZIP_MIME_TYPE = "application/zip"
