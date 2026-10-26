package io.github.fabiann1809.reader.ui.library

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book

/**
 * The library's floating button (design 03 §FAB, 7.3). With a book to continue it is "Continuar":
 * a tap opens that book where it was left, a long press unfolds the mini actions (for now only
 * "Añadir libro"; Captura and Nota de voz come in later phases). Otherwise it is "Añadir libro".
 */
@Composable
fun LibraryFab(bookToContinue: Book?, onContinue: (Long) -> Unit, onAddBook: () -> Unit) {
    if (bookToContinue == null) {
        PrimaryFab(R.drawable.ic_plus_circle, stringResource(R.string.library_add_book), onClick = onAddBook)
        return
    }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isExpanded) { isExpanded = false }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isExpanded) {
            MiniAction(R.drawable.ic_plus_circle, stringResource(R.string.library_add_book)) {
                isExpanded = false
                onAddBook()
            }
        }
        PrimaryFab(
            icon = R.drawable.ic_book_open_text,
            label = stringResource(R.string.library_continue),
            description = stringResource(R.string.library_continue_description, bookToContinue.title),
            onLongClickLabel = stringResource(R.string.library_more_actions),
            onLongClick = { isExpanded = !isExpanded },
            onClick = {
                isExpanded = false
                onContinue(bookToContinue.id)
            },
        )
    }
}

/** Extended FAB (56 dp, primary). A Surface rather than ExtendedFloatingActionButton, which has no long press. */
@Composable
private fun PrimaryFab(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    description: String = label,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    if (onLongClick == null) {
        // Content overload on purpose: the text/icon overload hides the label from screen readers.
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { FabContent(icon, label) }
        return
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 6.dp,
    ) {
        // The gesture goes on the content, inside the Surface's clip, so the ripple keeps the button's shape.
        Row(
            modifier = Modifier
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = onLongClickLabel,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .semantics { contentDescription = description }
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { FabContent(icon, label) }
    }
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
