package io.github.fabiann1809.reader.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R

/** A card due today in the Repasar list: its front (the back stays hidden until the session) and its source. */
@Composable
fun DueCardItem(dueCard: DueCard, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = dueCard.card.front,
                style = MaterialTheme.typography.titleMedium,
                maxLines = FRONT_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            val page = dueCard.card.page
            Text(
                text = if (page != null) {
                    stringResource(R.string.flashcard_source_page, dueCard.bookTitle, page)
                } else {
                    dueCard.bookTitle
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val FRONT_MAX_LINES = 3
