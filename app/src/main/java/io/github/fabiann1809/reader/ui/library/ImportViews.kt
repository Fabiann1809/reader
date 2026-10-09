package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.importing.ImportStatus

/**
 * Import feedback over the top of the books: a progress card while importing, or the error card.
 * New books appear on the shelves by themselves, so success shows nothing.
 */
@Composable
fun BoxScope.ImportFeedback(
    status: ImportStatus,
    contentPadding: PaddingValues,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cardModifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = contentPadding.calculateTopPadding())
        .padding(horizontal = 20.dp, vertical = 14.dp)
    when (status) {
        ImportStatus.Idle -> Unit
        is ImportStatus.Importing -> ImportProgressCard(status, cardModifier)
        is ImportStatus.AlreadyInLibrary -> AlreadyInLibraryCard(status, onDismiss, cardModifier)
        is ImportStatus.Failed -> ImportErrorCard(status, onRetry, onDismiss, cardModifier)
    }
}

/** "Importando 7 libros… 3 de 7": a spinner and a bar that fills as files finish. */
@Composable
fun ImportProgressCard(status: ImportStatus.Importing, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            // Screen readers hear the count change without moving focus.
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = pluralStringResource(R.plurals.import_progress, status.total, status.total, minOf(status.done + 1, status.total)),
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(
                progress = { status.done.toFloat() / status.total },
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                drawStopIndicator = {},
                gapSize = 0.dp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 4.dp, max = 4.dp)
                    .clip(CircleShape),
            )
        }
    }
}

/**
 * "Ya tenías este libro": the picked file was already on the shelves, so it was not added twice.
 * Same card as the import errors, with an info icon and only "Entendido".
 */
@Composable
fun AlreadyInLibraryCard(status: ImportStatus.AlreadyInLibrary, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val count = status.titles.size
    ImportCard(
        container = MaterialTheme.colorScheme.primaryContainer,
        onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
        accent = MaterialTheme.colorScheme.primary,
        onAccent = MaterialTheme.colorScheme.onPrimary,
        icon = R.drawable.ic_info,
        title = pluralStringResource(R.plurals.import_duplicate_title, count, count),
        message = pluralStringResource(R.plurals.import_duplicate_message, count),
        names = status.titles.joinToString(" · ") { "«$it»" },
        dismissLabel = null,
        onDismiss = {},
        actionLabel = stringResource(R.string.import_duplicate_ok),
        onAction = onDismiss,
        modifier = modifier,
    )
}

/** "No pudimos importar 2 archivos", with the file names, Descartar and Reintentar. */
@Composable
fun ImportErrorCard(status: ImportStatus.Failed, onRetry: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val count = status.failures.size
    val reason = stringResource(
        if (status.failures.all { it.isUnsupported }) R.string.import_error_unsupported else R.string.import_error_copy,
    )
    val message = if (status.importedCount > 0) "$reason ${stringResource(R.string.import_error_rest_imported)}" else reason
    val names = status.failures.map { it.fileName ?: stringResource(R.string.import_error_unknown_file) }
    ImportCard(
        container = MaterialTheme.colorScheme.errorContainer,
        onContainer = MaterialTheme.colorScheme.onErrorContainer,
        accent = MaterialTheme.colorScheme.error,
        onAccent = MaterialTheme.colorScheme.onError,
        icon = R.drawable.ic_warning_circle,
        title = pluralStringResource(R.plurals.import_error_title, count, count),
        message = message,
        names = names.joinToString(" · "),
        dismissLabel = stringResource(R.string.import_error_dismiss),
        onDismiss = onDismiss,
        actionLabel = stringResource(R.string.import_error_retry),
        onAction = onRetry,
        modifier = modifier,
    )
}

/** Tinted card of a notice about the files picked: icon, title, message, the names and its buttons. */
@Composable
private fun ImportCard(
    container: Color,
    onContainer: Color,
    accent: Color,
    onAccent: Color,
    icon: Int,
    title: String,
    message: String,
    names: String,
    dismissLabel: String?,
    onDismiss: () -> Unit,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(22.dp))
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = onContainer)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = names,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dismissLabel?.let { label ->
                CardButton(label, color = Color.Transparent, textColor = onContainer, onClick = onDismiss)
            }
            CardButton(actionLabel, color = accent, textColor = onAccent, onClick = onAction)
        }
    }
}

@Composable
private fun CardButton(label: String, color: Color, textColor: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = textColor)
    }
}
