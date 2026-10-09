package io.github.fabiann1809.reader.ui.bookdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.ui.reader.argb

private const val MAX_LINES = 4

/**
 * The book's highlights (T11.12), in reading order: each one's text next to a bar of its color.
 * Hidden while there are none; the detail's tabs come later (T16.4).
 */
fun LazyListScope.highlightsSection(highlights: List<Highlight>) {
    if (highlights.isEmpty()) return
    item(key = "highlights_header") {
        Text(
            text = stringResource(R.string.highlights_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    items(highlights, key = { "highlight_${it.id}" }) { highlight -> HighlightItem(highlight) }
}

/** One highlight; with [onClick] it also goes to its place in the book. */
@Composable
fun HighlightItem(highlight: Highlight, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Color(highlight.color.argb()), RoundedCornerShape(2.dp)),
        )
        Text(
            text = highlight.text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
