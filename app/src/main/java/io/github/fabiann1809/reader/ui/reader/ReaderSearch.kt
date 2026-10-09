package io.github.fabiann1809.reader.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.reader.SearchHit
import io.github.fabiann1809.reader.ui.components.StatusMessage

/**
 * "Buscar en el libro": a field with the accent ring over the results, each one with its chapter and
 * the text around the match, which leads to that place.
 */
@Composable
fun ReaderSearch(
    bookTitle: String,
    search: ReaderSearchState,
    onQueryChange: (String) -> Unit,
    onResult: (SearchHit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            ) {
                BackButton(onBack)
                SearchField(bookTitle, search.query, onQueryChange, Modifier.weight(1f))
            }
            when {
                search.unsupported -> Message(R.drawable.ic_magnifying_glass, R.string.reader_search_unsupported, null)
                search.query.isBlank() -> Message(R.drawable.ic_magnifying_glass, R.string.reader_search_prompt, null)
                search.isSearching -> Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                search.results.isEmpty() -> Message(
                    icon = R.drawable.ic_text_aa,
                    title = R.string.reader_search_none,
                    message = R.string.reader_search_none_hint,
                )
                else -> Results(search, onResult)
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = stringResource(R.string.navigate_up))
    }
}

@Composable
private fun SearchField(bookTitle: String, query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .padding(start = 16.dp, end = 6.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_magnifying_glass),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.reader_search_placeholder, bookTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                // Results already follow the typing; the key only hides the keyboard.
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onQueryChange("") },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_x),
                    contentDescription = stringResource(R.string.library_search_clear),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun Message(icon: Int, title: Int, message: Int?) {
    StatusMessage(
        icon = icon,
        title = stringResource(title),
        message = message?.let { stringResource(it) },
        modifier = Modifier.padding(top = 48.dp),
    )
}

@Composable
private fun Results(search: ReaderSearchState, onResult: (SearchHit) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)) {
        item(key = "count") {
            Text(
                text = pluralStringResource(R.plurals.reader_search_results, search.results.size, search.results.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        itemsIndexed(search.results, key = { index, hit -> "$index-${hit.location}" }) { _, hit ->
            ResultRow(hit, onClick = { onResult(hit) })
        }
    }
}

@Composable
private fun ResultRow(hit: SearchHit, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val accentSoft = MaterialTheme.colorScheme.primaryContainer
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        hit.chapter?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            text = buildAnnotatedString {
                append(hit.before)
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, background = accentSoft, color = accent)) { append(hit.match) }
                append(hit.after)
            },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
