package io.github.fabiann1809.reader.ui.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book

/**
 * The library's floating button (design 03 §FAB, 7.3). With a book to continue it is "Continuar"
 * (opens that book where it was left) and a small "Añadir libro" sits above it, always visible so
 * adding books can be found. Otherwise the button is "Añadir libro".
 */
@Composable
fun LibraryFab(bookToContinue: Book?, onContinue: (Long) -> Unit, onAddBook: () -> Unit) {
    if (bookToContinue == null) {
        PrimaryFab(R.drawable.ic_plus_circle, stringResource(R.string.library_add_book), onClick = onAddBook)
        return
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        MiniAction(R.drawable.ic_plus_circle, stringResource(R.string.library_add_book), onAddBook)
        PrimaryFab(
            icon = R.drawable.ic_book_open_text,
            label = stringResource(R.string.library_continue),
            description = stringResource(R.string.library_continue_description, bookToContinue.title),
            onClick = { onContinue(bookToContinue.id) },
        )
    }
}

/** Extended FAB (56 dp, primary). */
@Composable
private fun PrimaryFab(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    description: String = label,
) {
    // Content overload on purpose: the text/icon overload hides the label from screen readers.
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.semantics { contentDescription = description },
    ) { FabContent(icon, label) }
}

@Composable
private fun FabContent(@DrawableRes icon: Int, label: String) {
    Icon(painterResource(icon), contentDescription = null)
    Spacer(Modifier.width(12.dp))
    Text(label)
}

/** Mini action of the speed-dial (48 dp, surface-container-high) with its label beside it. */
@Composable
private fun MiniAction(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // The button already carries the label for screen readers.
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.clearAndSetSemantics {},
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = label },
        ) {
            Icon(painterResource(icon), contentDescription = null)
        }
    }
}
