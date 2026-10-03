package io.github.fabiann1809.reader.ui.bookdetail

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.fabiann1809.reader.R

/** The detail's tabs (design 01 §4.2): Resumen · Notas y resaltados · Fichas · Sesiones. */
enum class DetailTab(@StringRes val label: Int) {
    SUMMARY(R.string.detail_tab_summary),
    NOTES(R.string.detail_tab_notes),
    CARDS(R.string.detail_tab_cards),
    SESSIONS(R.string.detail_tab_sessions),
}

@Composable
fun DetailTabRow(selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected.ordinal, containerColor = MaterialTheme.colorScheme.surface) {
        DetailTab.entries.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = { Text(stringResource(tab.label), style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}
