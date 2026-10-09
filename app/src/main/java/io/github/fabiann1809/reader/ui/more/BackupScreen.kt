package io.github.fabiann1809.reader.ui.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
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

/** "Respaldo" (T17.2): a pastel card to export what a backup holds, and a card to import one. */
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 40.dp),
        ) {
            ExportCard(export, onExport)
            ImportCard(import, onImport)
        }
    }
}

@Composable
private fun ExportCard(export: ExportState, onExport: () -> Unit) {
    val colors = ReaderTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors.pastels[2]), RoundedCornerShape(30.dp))
            .padding(22.dp),
    ) {
        Icon(painterResource(R.drawable.ic_archive), contentDescription = null, tint = colors.onPastel, modifier = Modifier.size(36.dp))
        Text(
            stringResource(R.string.backup_export_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.onPastel,
        )
        Text(stringResource(R.string.backup_export_message), style = MaterialTheme.typography.bodyMedium, color = colors.onPastel)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_shield_check), contentDescription = null, tint = colors.onPastel, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.backup_no_key), style = MaterialTheme.typography.bodySmall, color = colors.onPastel)
        }
        PrimaryButton(
            text = stringResource(R.string.backup_export),
            onClick = onExport,
            enabled = export != ExportState.Exporting,
            icon = R.drawable.ic_archive,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        ExportResult(export)
    }
}

@Composable
private fun ImportCard(import: ImportState, onImport: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(30.dp))
            .padding(22.dp),
    ) {
        Text(
            stringResource(R.string.backup_import_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        )
        Text(
            stringResource(R.string.backup_import_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlineButton(
            text = stringResource(R.string.backup_import),
            onClick = onImport,
            enabled = import != ImportState.Importing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        ImportResult(import)
    }
}

@Composable
private fun ExportResult(export: ExportState) {
    when (export) {
        ExportState.Idle -> Unit
        ExportState.Exporting -> Busy(R.string.backup_exporting)
        is ExportState.Done -> Banner(
            stringResource(
                R.string.backup_exported,
                export.summary.books,
                export.summary.notes,
                export.summary.flashcards,
                export.summary.files,
            ),
            success = true,
        )
        ExportState.Failed -> Banner(stringResource(R.string.backup_export_failed), success = false)
    }
}

@Composable
private fun ImportResult(import: ImportState) {
    when (import) {
        ImportState.Idle, is ImportState.Confirming -> Unit
        ImportState.Importing -> Busy(R.string.backup_importing)
        is ImportState.Done -> Banner(
            stringResource(R.string.backup_imported, import.summary.books, import.summary.notes, import.summary.flashcards, import.summary.files),
            success = true,
        )
        is ImportState.Failed -> Banner(
            stringResource(
                when (import.reason) {
                    ImportFailure.NOT_A_BACKUP -> R.string.backup_import_not_a_backup
                    ImportFailure.NEWER_VERSION -> R.string.backup_import_newer
                    ImportFailure.UNREADABLE -> R.string.backup_import_unreadable
                },
            ),
            success = false,
        )
    }
}

@Composable
private fun Busy(message: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
    }
}

/** A soft green or red box with the outcome; the icon keeps it from relying on color alone. */
@Composable
private fun Banner(text: String, success: Boolean) {
    val colors = ReaderTheme.colors
    val scheme = MaterialTheme.colorScheme
    val accent: Color = if (success) colors.success else scheme.error
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(if (success) colors.successContainer else scheme.errorContainer, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Icon(
            painter = painterResource(if (success) R.drawable.ic_check_circle else R.drawable.ic_warning_circle),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
    }
}

private const val ZIP_MIME_TYPE = "application/zip"
