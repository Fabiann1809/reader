package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import io.github.fabiann1809.reader.data.collection.Collection

/**
 * Checklist of the collections a book can be added to: "Mis favoritos" (the favorite flag) and the
 * user's collections. The status-based defaults are left out because they follow the reading status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToCollectionSheet(
    isFavorite: Boolean,
    collections: List<Collection>,
    collectionIds: Set<Long>,
    onFavoriteChange: (Boolean) -> Unit,
    onCollectionChange: (collectionId: Long, isIncluded: Boolean) -> Unit,
    onNewCollection: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.collection_add_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            CheckRow(stringResource(R.string.collection_favorites), isChecked = isFavorite, onChange = onFavoriteChange)
            if (collections.isNotEmpty()) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                collections.forEach { collection ->
                    CheckRow(collection.name, isChecked = collection.id in collectionIds) { isIncluded ->
                        onCollectionChange(collection.id, isIncluded)
                    }
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.collection_new), color = MaterialTheme.colorScheme.primary) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus_circle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.clickable(role = Role.Button, onClick = onNewCollection),
            )
        }
    }
}

@Composable
private fun CheckRow(name: String, isChecked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        // The whole row toggles, so the checkbox itself is not separately clickable.
        trailingContent = { Checkbox(checked = isChecked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.toggleable(value = isChecked, role = Role.Checkbox, onValueChange = onChange),
    )
}
