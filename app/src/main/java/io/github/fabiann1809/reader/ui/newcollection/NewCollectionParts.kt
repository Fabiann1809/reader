package io.github.fabiann1809.reader.ui.newcollection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.library.CollectionFanArt
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private val ColorNames = listOf(
    R.string.new_collection_color_rose,
    R.string.new_collection_color_lavender,
    R.string.new_collection_color_mint,
    R.string.new_collection_color_peach,
)

/** Close, the title and the "Guardar" pill, dimmed until there is a name and a book. */
@Composable
fun NewCollectionTopBar(canSave: Boolean, onClose: () -> Unit, onSave: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClose),
        ) {
            Icon(painterResource(R.drawable.ic_x), contentDescription = stringResource(R.string.new_collection_close))
        }
        Text(
            text = stringResource(R.string.collection_new),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .heightIn(min = 44.dp)
                .alpha(if (canSave) 1f else 0.4f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(enabled = canSave, role = Role.Button, onClick = onSave)
                .padding(horizontal = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.new_collection_save),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/** The collection as it will look in the library: the first books checked, the band, the name and the count. */
@Composable
fun CollectionPreview(uiState: NewCollectionUiState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        CollectionFanArt(uiState.selectedBooks, uiState.colorIndex, emptyPlaceholder = true)
        Text(
            text = uiState.name.trim().ifEmpty { stringResource(R.string.new_collection_unnamed) },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        val count = uiState.selectedIds.size
        Text(
            text = if (count == 0) {
                stringResource(R.string.new_collection_no_books)
            } else {
                pluralStringResource(R.plurals.library_book_count, count, count)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

/** The four pastel colors, the chosen one ringed and a little larger. */
@Composable
fun ColorSwatches(selected: Int, onChange: (Int) -> Unit) {
    val pastels = ReaderTheme.colors.pastels
    val reduceMotion = rememberReduceMotion()
    Column {
        Text(
            text = stringResource(R.string.new_collection_color),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            pastels.indices.forEach { index ->
                val isSelected = index == selected
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.5f),
                    label = "swatchScale",
                )
                val ring = if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .semantics { this.selected = isSelected }
                        .clickable(role = Role.RadioButton) { onChange(index) },
                ) {
                    Box(
                        Modifier
                            .scale(scale)
                            .size(60.dp)
                            .then(ring)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(pastels[index])),
                    )
                    Text(
                        text = stringResource(ColorNames[index]),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** A cover to check, with its title below and a check badge in the corner. */
@Composable
fun BookChoice(book: Book, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.5f),
        label = "choiceScale",
    )
    Column(
        modifier
            .semantics { selected = isSelected }
            .clickable(role = Role.Checkbox, onClick = onClick),
    ) {
        Box(Modifier.scale(scale)) {
            BookCover(book = book, modifier = Modifier.fillMaxWidth())
            if (isSelected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(2.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = book.title,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
