package io.github.fabiann1809.reader.ui.explanation

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.ai.Explanation
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.ui.components.ReaderFilterChip
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.KeyTerm
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

// Explainer blocks. Each one has its own style so the eye finds the main idea first.

/** The blocks of an explanation, selectable so parts of it can be copied, then the key terms. */
@Composable
fun ExplanationBlocks(explanation: Explanation) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            explanation.caveat?.let { CaveatBlock(it) }
            MainIdeaBlock(explanation.mainIdea)
            SimpleExplanationBlock(explanation.simpleExplanation)
            AnalogyBlock(explanation.analogy)
        }
    }
    KeyTermsBlock(explanation.keyTerms)
}

/** "Idea central": the dominant block, on a pastel gradient with its label in small capitals. */
@Composable
fun MainIdeaBlock(text: String) {
    val colors = ReaderTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(colors.pastels[0]))
            .padding(18.dp),
    ) {
        BlockLabel(stringResource(R.string.explanation_block_main_idea).uppercase(), color = colors.onPastel)
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold),
            color = colors.onPastel,
        )
    }
}

/** "Explicación sencilla": plain body text, no decoration. */
@Composable
fun SimpleExplanationBlock(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BlockTitle(stringResource(R.string.explanation_block_simple))
        Text(text = text, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 23.sp), color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

/** "Analogía cotidiana": a warm card with a light bulb. */
@Composable
fun AnalogyBlock(text: String) {
    val colors = ReaderTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.warningContainer, RoundedCornerShape(22.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lightbulb),
            contentDescription = null,
            tint = colors.warning,
            modifier = Modifier.size(24.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BlockTitle(stringResource(R.string.explanation_block_analogy), color = colors.onWarningContainer)
            Text(text = text, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp))
        }
    }
}

/** "Términos clave": chips that reveal their definition when tapped. */
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
private fun BlockTitle(text: String, color: Color = Color.Unspecified) {
    // Headings let screen readers jump from block to block in order.
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.semantics { heading() })
}

@Composable
private fun BlockLabel(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.14.em),
        color = color,
        modifier = Modifier.semantics { heading() },
    )
}
