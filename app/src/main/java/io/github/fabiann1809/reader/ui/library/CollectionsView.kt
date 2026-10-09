package io.github.fabiann1809.reader.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.collection.Collection
import io.github.fabiann1809.reader.data.collection.CollectionGroup
import io.github.fabiann1809.reader.data.collection.SmartCollection
import io.github.fabiann1809.reader.ui.components.BookCover
import io.github.fabiann1809.reader.ui.theme.PastelDots
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the "Colecciones" view can do: open a collection or a book, create one, add books, rename, delete. */
class CollectionsActions(
    val onOpen: (CollectionGroup) -> Unit = {},
    val onBookClick: (Long) -> Unit = {},
    val onNewCollection: () -> Unit = {},
    val onAddBooks: (Collection) -> Unit = {},
    val onRename: (Collection) -> Unit = {},
    val onDelete: (Collection) -> Unit = {},
)

/** "Colecciones": each collection as a fan of covers behind a pastel band, with its name and count below. */
@Composable
fun CollectionsView(
    groups: List<CollectionGroup>,
    contentPadding: PaddingValues,
    bottomSpace: Dp,
    actions: CollectionsActions,
    header: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + bottomSpace,
        ),
        verticalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        header?.let { item { it() } }
        item { NewCollectionLink(actions.onNewCollection) }
        items(groups, key = { it.filter.encode() }) { group -> CollectionCard(group, actions) }
    }
}

@Composable
private fun NewCollectionLink(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .padding(start = 20.dp)
            .height(44.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.collection_new),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private val CoverWidth = 86.dp
private val CoverHeight = 128.dp
private val BandTop = 66.dp
private val BandHeight = 118.dp
private const val FAN_SIZE = 5
private const val FAN_SPREAD_DP = 50
private const val FAN_TILT_DEGREES = 4f
private const val FAN_RISE_DP = 7

@Composable
private fun CollectionCard(group: CollectionGroup, actions: CollectionsActions) {
    val index = pastelIndex(group)
    Column(Modifier.padding(horizontal = 20.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(BandTop + BandHeight + 10.dp)) {
            CoverFan(group, actions.onBookClick, maxWidth)
            Band(group, index, actions)
        }
        NameAndCount(group, index, onClick = { actions.onOpen(group) })
    }
}

private fun pastelIndex(group: CollectionGroup): Int = when {
    group.smart != null -> SmartCollection.entries.indexOf(group.smart)
    else -> group.collection?.colorIndex ?: 0
}.mod(PastelDots.size)

@Composable
private fun CoverFan(group: CollectionGroup, onBookClick: (Long) -> Unit, width: Dp) {
    val covers = group.books.take(FAN_SIZE)
    val center = (covers.size - 1) / 2f
    covers.forEachIndexed { i, book ->
        val d = i - center
        BookCover(
            book = book,
            titleSize = 15.sp,
            aspect = CoverWidth / CoverHeight,
            onClick = { onBookClick(book.id) },
            modifier = Modifier
                .offset(x = width / 2 - CoverWidth / 2 + (d * FAN_SPREAD_DP).dp, y = (abs(d) * FAN_RISE_DP).dp)
                .zIndex(FAN_SIZE * 2f - (abs(d) * 2).roundToInt())
                .rotate(d * FAN_TILT_DEGREES)
                .width(CoverWidth),
        )
    }
}

/** Frosted pastel band over the lower half of the covers; the buttons only exist for the user's collections. */
@Composable
private fun Band(group: CollectionGroup, index: Int, actions: CollectionsActions) {
    val colors = ReaderTheme.colors
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .offset(y = BandTop)
            .zIndex(BAND_Z)
            .fillMaxWidth()
            .height(BandHeight)
            .clip(shape)
            .background(Brush.horizontalGradient(colors.pastels[index])),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.44f)
                .background(Color.White.copy(alpha = 0.2f)),
        )
        group.collection?.let { collection ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 11.dp, end = 14.dp),
            ) {
                BandButton(R.drawable.ic_plus, stringResource(R.string.collection_add_books)) { actions.onAddBooks(collection) }
                CollectionMenuButton(collection, actions)
            }
        }
    }
}

private const val BAND_Z = 20f

@Composable
private fun BandButton(icon: Int, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = BandIcon, modifier = Modifier.size(20.dp))
    }
}

private val BandIcon = Color(0xFF241B15)

@Composable
private fun CollectionMenuButton(collection: Collection, actions: CollectionsActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        BandButton(R.drawable.ic_dots_three, stringResource(R.string.more_options)) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.collection_rename)) },
                onClick = {
                    expanded = false
                    actions.onRename(collection)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.collection_delete), color = MaterialTheme.colorScheme.error) },
                onClick = {
                    expanded = false
                    actions.onDelete(collection)
                },
            )
        }
    }
}

@Composable
private fun NameAndCount(group: CollectionGroup, index: Int, onClick: () -> Unit) {
    val name = group.collection?.name ?: stringResource(group.smart?.nameRes() ?: R.string.collection_all)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            Modifier
                .width(150.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Box(Modifier.size(7.dp).background(PastelDots[index], CircleShape))
            Text(
                text = pluralStringResource(R.plurals.library_book_count, group.books.size, group.books.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
