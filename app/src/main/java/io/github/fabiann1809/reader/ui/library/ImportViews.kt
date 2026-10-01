package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.importing.ImportStatus
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.theme.ShelfTopA

/** Thin bar under the library bar, filled with the shelf wood as files finish (design 1f). */
@Composable
fun ImportProgressBar(status: ImportStatus.Importing, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { status.done.toFloat() / status.total },
        color = ShelfTopA,
        trackColor = Color.White.copy(alpha = 0.2f),
        drawStopIndicator = {},
        gapSize = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp),
    )
}

/** Dark pill at the bottom of the shelves: "Importando 7 libros… 3 de 7". */
@Composable
fun ImportProgressPill(status: ImportStatus.Importing, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.inverseSurface, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp)
            // Screen readers hear the count change without moving focus.
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_down),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = pluralStringResource(R.plurals.import_progress, status.total, status.total, minOf(status.done + 1, status.total)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.inverseOnSurface,
        )
    }
}

/** "No pudimos importar 2 archivos", with the file names, Descartar and Reintentar (design 1f). */
@Composable
fun ImportErrorCard(status: ImportStatus.Failed, onRetry: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val count = status.failures.size
    val reason = stringResource(
        if (status.failures.all { it.isUnsupported }) R.string.import_error_unsupported else R.string.import_error_copy,
    )
    val message = if (status.importedCount > 0) "$reason ${stringResource(R.string.import_error_rest_imported)}" else reason
    val names = status.failures.map { it.fileName ?: stringResource(R.string.import_error_unknown_file) }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = pluralStringResource(R.plurals.import_error_title, count, count),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = names.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.import_error_dismiss)) }
                PrimaryButton(text = stringResource(R.string.import_error_retry), onClick = onRetry)
            }
        }
    }
}
