package io.github.fabiann1809.reader.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

// Fixed colors of the key badge: it sits on a pastel gradient in both themes.
private val KeyIconColor = Color(0xFF3F2F8F)
private val KeyBadgeColor = Color.White.copy(alpha = 0.75f)

/** Whether an AI key is saved, with the reminder that only text fragments leave the phone. */
@Composable
fun KeyStatusCard(hasApiKey: Boolean, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.horizontalGradient(colors.pastels[1]))
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(KeyBadgeColor, CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_key_fill), contentDescription = null, tint = KeyIconColor)
        }
        Column {
            Text(
                text = stringResource(if (hasApiKey) R.string.more_key_active else R.string.more_key_missing),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onPastel,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(if (hasApiKey) colors.success else colors.warning, CircleShape),
                )
                Text(
                    text = stringResource(if (hasApiKey) R.string.more_key_active_note else R.string.more_key_missing_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPastel,
                )
            }
        }
    }
}

/** The destinations of the tab in one card, a circular icon and a one-line hint on each row. */
@Composable
fun MoreEntries(uiState: MoreUiState, onOpen: (MoreEntry) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            MoreEntry.entries.forEach { entry ->
                val hint = entry.hint?.let { stringResource(it) } ?: notesSummary(uiState)
                MoreRow(entry, hint, onClick = { onOpen(entry) })
            }
        }
    }
}

@Composable
private fun notesSummary(uiState: MoreUiState): String =
    if (uiState.noteCount == 0) {
        stringResource(R.string.more_notes_empty)
    } else {
        stringResource(
            R.string.more_notes_summary,
            pluralStringResource(R.plurals.more_notes_count, uiState.noteCount, uiState.noteCount),
            pluralStringResource(R.plurals.more_books_count, uiState.bookCount, uiState.bookCount),
        )
    }

@Composable
private fun MoreRow(entry: MoreEntry, hint: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        ) {
            Icon(
                painter = painterResource(entry.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(entry.title), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            Text(hint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            painter = painterResource(R.drawable.ic_caret_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
