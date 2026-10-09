package io.github.fabiann1809.reader.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/** "Para hoy": the big count of cards due on a pastel card, the books they come from and the start button. */
@Composable
fun TodayCard(count: Int, bookTitles: List<String>, onStartSession: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(colors.pastels[0]))
            .padding(22.dp),
    ) {
        Text(
            stringResource(R.string.review_for_today),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.onPastel,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 56.sp, fontWeight = FontWeight.ExtraBold),
                color = colors.onPastel,
            )
            Text(
                text = pluralStringResource(R.plurals.review_cards_unit, count),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPastel,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (bookTitles.isNotEmpty()) {
            Text(
                text = bookTitles.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPastel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        PrimaryButton(
            text = stringResource(R.string.review_start_session),
            onClick = onStartSession,
            icon = R.drawable.ic_play_fill,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        )
    }
}
