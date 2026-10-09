package io.github.fabiann1809.reader.ui.bookdetail

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R

/** The detail's tabs: Resumen · Notas y resaltados · Fichas · Sesiones. */
enum class DetailTab(@StringRes val label: Int) {
    SUMMARY(R.string.detail_tab_summary),
    NOTES(R.string.detail_tab_notes),
    CARDS(R.string.detail_tab_cards),
    SESSIONS(R.string.detail_tab_sessions),
}

/** Plain tab labels over a line, the chosen one underlined with the accent. */
@Composable
fun DetailTabRow(selected: DetailTab, onSelect: (DetailTab) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
            DetailTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .height(44.dp)
                        .width(IntrinsicSize.Max)
                        .semantics { this.selected = isSelected }
                        .clickable(role = Role.Tab) { onSelect(tab) },
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(tab.label),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(3.dp),
                            ),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** "+ Nueva nota" / "+ Nueva ficha": the way to add one from its tab. */
@Composable
fun AddLink(@StringRes text: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .height(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(stringResource(text), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}
