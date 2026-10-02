package io.github.fabiann1809.reader.ui.reader

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.highlight.HighlightColor
import io.github.fabiann1809.reader.ui.theme.Ai80
import io.github.fabiann1809.reader.ui.theme.DarkSurfaceContainerHigh
import io.github.fabiann1809.reader.ui.theme.Ink900
import kotlin.math.roundToInt

// Space between the selected text and the capsule.
private val Gap = 8.dp

/** What each button of the capsule does; the ones without a feature yet are connected in their task. */
class SelectionActions(
    val onExplain: () -> Unit = {},
    val onHighlight: (HighlightColor) -> Unit = {},
    val onNote: () -> Unit = {},
    val onVoiceNote: () -> Unit = {},
    val onCard: () -> Unit = {},
    val onCopy: () -> Unit,
    val onSearch: () -> Unit,
    val onShare: () -> Unit,
)

/**
 * The capsule for a selection in the reader: "Explicar" opens the explainer ([onExplain]),
 * "Resaltar" saves it in a color ([onHighlight]), "Nota" writes a note on it ([onNote]) and "Más"
 * copies, searches or shares the text, "Nota de voz" records one on that page ([onVoiceNote],
 * T13.3) and "Crear ficha" makes a card of it ([onCard], T14.2); each one then ends the selection
 * with [onEnd].
 */
@Composable
fun ReaderSelection(
    selection: TextSelection,
    darkPage: Boolean,
    onExplain: () -> Unit,
    onHighlight: (HighlightColor) -> Unit,
    onNote: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
    onCard: () -> Unit = {},
    onVoiceNote: () -> Unit = {},
) {
    val context = LocalContext.current
    val actions = SelectionActions(
        onExplain = {
            onExplain()
            onEnd()
        },
        onHighlight = { color ->
            onHighlight(color)
            onEnd()
        },
        onNote = {
            onNote()
            onEnd()
        },
        onCard = {
            onCard()
            onEnd()
        },
        onVoiceNote = {
            onVoiceNote()
            onEnd()
        },
        onCopy = {
            copyText(context, selection.text)
            onEnd()
        },
        onSearch = {
            searchText(context, selection.text)
            onEnd()
        },
        onShare = {
            shareText(context, selection.text)
            onEnd()
        },
    )
    SelectionToolbar(selection, darkPage, actions, modifier)
}

/**
 * The floating capsule over a selection (design 7.7): "Explicar" with its label in the AI color,
 * then highlight, note, voice note and card as icons, and "Más" (copy, search, share). It sits
 * above the selected text, or below it when there is no room, always inside the page.
 */
@Composable
fun SelectionToolbar(selection: TextSelection, darkPage: Boolean, actions: SelectionActions, modifier: Modifier = Modifier) {
    Layout(
        content = { Capsule(darkPage, actions) },
        modifier = modifier,
    ) { measurables, constraints ->
        val capsule = measurables.single().measure(Constraints(maxWidth = constraints.maxWidth))
        layout(constraints.maxWidth, constraints.maxHeight) {
            val bounds = selection.bounds
            val gap = Gap.roundToPx()
            val (x, y) = if (bounds == null) {
                // Unknown place: centered near the top of the page.
                (constraints.maxWidth - capsule.width) / 2 to gap * 6
            } else {
                val centerX = ((bounds.left + bounds.right) / 2).roundToInt() - capsule.width / 2
                val above = bounds.top.roundToInt() - capsule.height - gap
                val below = bounds.bottom.roundToInt() + gap
                centerX to if (above >= 0) above else below
            }
            capsule.place(
                x.coerceIn(0, (constraints.maxWidth - capsule.width).coerceAtLeast(0)),
                y.coerceIn(0, (constraints.maxHeight - capsule.height).coerceAtLeast(0)),
            )
        }
    }
}

@Composable
private fun Capsule(darkPage: Boolean, actions: SelectionActions) {
    // ink-900 on light pages, surface-container-high on dark ones, where ink-900 would vanish.
    val background = if (darkPage) DarkSurfaceContainerHigh else Ink900
    // "Resaltar" swaps the actions for the four colors (design 01 §4.4: "Resaltar (4 colores)").
    var pickingColor by remember { mutableStateOf(false) }
    Surface(color = background, contentColor = Color.White, shape = CircleShape, shadowElevation = 8.dp) {
        if (pickingColor) {
            ColorChoices(actions.onHighlight)
        } else {
            Actions(actions, onHighlight = { pickingColor = true })
        }
    }
}

@Composable
private fun ColorChoices(onPick: (HighlightColor) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        HighlightColor.entries.forEach { color ->
            val name = stringResource(color.label)
            IconButton(onClick = { onPick(color) }, modifier = Modifier.semantics { contentDescription = name }) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(Color(color.argb()), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun Actions(actions: SelectionActions, onHighlight: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        TextButton(onClick = actions.onExplain) {
            Icon(painterResource(R.drawable.ic_sparkle), contentDescription = null, tint = Ai80, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.selection_explain), color = Ai80)
        }
        Divider()
        CapsuleIcon(R.drawable.ic_highlighter, R.string.selection_highlight, onHighlight)
        CapsuleIcon(R.drawable.ic_note_pencil, R.string.selection_note, actions.onNote)
        CapsuleIcon(R.drawable.ic_microphone, R.string.selection_voice_note, actions.onVoiceNote)
        CapsuleIcon(R.drawable.ic_cards_three, R.string.selection_card, actions.onCard)
        MoreMenu(actions)
    }
}

// A thin line between "Explicar" and the icons.
@Composable
private fun Divider() {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(width = 1.dp, height = 20.dp)
            .background(Color.White.copy(alpha = 0.2f)),
    )
}

@Composable
private fun CapsuleIcon(@DrawableRes icon: Int, @StringRes description: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), contentDescription = stringResource(description), modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun MoreMenu(actions: SelectionActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        CapsuleIcon(R.drawable.ic_dots_three, R.string.selection_more) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MoreItem(R.string.selection_copy, actions.onCopy) { open = false }
            MoreItem(R.string.selection_search, actions.onSearch) { open = false }
            MoreItem(R.string.selection_share, actions.onShare) { open = false }
        }
    }
}

@Composable
private fun MoreItem(@StringRes label: Int, action: () -> Unit, close: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = {
            close()
            action()
        },
    )
}

private val HighlightColor.label: Int
    get() = when (this) {
        HighlightColor.YELLOW -> R.string.highlight_yellow
        HighlightColor.GREEN -> R.string.highlight_green
        HighlightColor.BLUE -> R.string.highlight_blue
        HighlightColor.PINK -> R.string.highlight_pink
    }
