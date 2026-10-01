package io.github.fabiann1809.reader.ui.library

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R

/**
 * "Añadir libro" (03-navegacion 3.1 and 3.2): a file, or a paper book. The system picker already
 * lists Google Drive and Dropbox when they are installed, so they need no option of their own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookSheet(onFromFile: () -> Unit, onPhysicalBook: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.library_add_book),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Option(R.drawable.ic_book, R.string.add_book_from_file, R.string.add_book_from_file_hint, onFromFile)
            Option(R.drawable.ic_hand, R.string.add_book_physical, R.string.add_book_physical_hint, onPhysicalBook)
        }
    }
}

@Composable
private fun Option(@DrawableRes icon: Int, @StringRes title: Int, @StringRes hint: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(hint)) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick),
    )
}
