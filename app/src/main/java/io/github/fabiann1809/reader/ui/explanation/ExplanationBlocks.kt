package io.github.fabiann1809.reader.ui.explanation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.KeyTerm
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

// Explainer blocks (design 7.11). Each one has its own style so the eye finds the main idea first.

/** "En una frase": the dominant block, lavender with a left accent bar. */
@Composable
fun MainIdeaBlock(text: String) {
    val colors = ReaderTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(colors.aiSoft, MaterialTheme.shapes.small),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(colors.ai),
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BlockLabel(stringResource(R.string.explanation_block_main_idea), color = colors.ai)
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** "Explicado simple": plain body text, no decoration. */
@Composable
fun SimpleExplanationBlock(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BlockTitle(stringResource(R.string.explanation_block_simple))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** "Analogía cotidiana": warm card with a light bulb, text in soft italics. */
@Composable
fun AnalogyBlock(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lightbulb),
            contentDescription = null,
            tint = ReaderTheme.colors.warning,
            modifier = Modifier.size(24.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BlockTitle(stringResource(R.string.explanation_block_analogy), style = MaterialTheme.typography.labelLarge)
            Text(text = text, style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
        }
    }
}

/** "Palabras clave": chips that reveal their definition when tapped. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyTermsBlock(terms: List<KeyTerm>) {
    if (terms.isEmpty()) return
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BlockTitle(stringResource(R.string.explanation_block_key_terms))
        Text(
            text = stringResource(R.string.explanation_key_terms_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            terms.forEach { keyTerm ->
                ReaderFilterChip(
                    selected = selected == keyTerm.term,
                    onClick = { selected = keyTerm.term.takeIf { it != selected } },
                    label = { Text(keyTerm.term) },
                )
            }
        }
        terms.firstOrNull { it.term == selected }?.let { keyTerm ->
            Text(
                text = keyTerm.definition,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                    .padding(12.dp),
            )
        }
    }
}

/** Shown when the model says the fragment is ambiguous, incomplete or badly read. */
@Composable
fun CaveatBlock(text: String) {
    val warning = ReaderTheme.colors.warning
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(warning.copy(alpha = 0.12f), MaterialTheme.shapes.medium)
            .border(1.dp, warning, MaterialTheme.shapes.medium)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning_circle),
            contentDescription = null,
            tint = warning,
            modifier = Modifier.size(22.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BlockLabel(stringResource(R.string.explanation_block_caveat), color = MaterialTheme.colorScheme.onSurface)
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun BlockTitle(text: String, style: TextStyle = MaterialTheme.typography.titleSmall) {
    // Headings let screen readers jump from block to block in order.
    Text(text = text, style = style, modifier = Modifier.semantics { heading() })
}

@Composable
private fun BlockLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier.semantics { heading() },
    )
}
