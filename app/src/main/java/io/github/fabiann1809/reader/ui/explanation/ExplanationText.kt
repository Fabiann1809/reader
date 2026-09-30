package io.github.fabiann1809.reader.ui.explanation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.Explanation

/** Titles of the explanation blocks. They come from strings.xml because the user reads them. */
data class ExplanationLabels(
    val mainIdea: String,
    val simpleExplanation: String,
    val analogy: String,
    val keyTerms: String,
    val caveat: String,
) {
    companion object {
        fun from(context: Context) = ExplanationLabels(
            mainIdea = context.getString(R.string.explanation_block_main_idea),
            simpleExplanation = context.getString(R.string.explanation_block_simple),
            analogy = context.getString(R.string.explanation_block_analogy),
            keyTerms = context.getString(R.string.explanation_block_key_terms),
            caveat = context.getString(R.string.explanation_block_caveat),
        )
    }
}

@Composable
fun explanationLabels(): ExplanationLabels = ExplanationLabels.from(LocalContext.current)

/**
 * Plain-text version of an explanation, with one titled paragraph per block.
 * Used as the content of the saved note, so notes stay readable anywhere text is shown.
 */
fun Explanation.toPlainText(labels: ExplanationLabels): String = buildList {
    add("${labels.mainIdea}: $mainIdea")
    add("${labels.simpleExplanation}: $simpleExplanation")
    add("${labels.analogy}: $analogy")
    if (keyTerms.isNotEmpty()) {
        add(labels.keyTerms + ":\n" + keyTerms.joinToString("\n") { "• ${it.term}: ${it.definition}" })
    }
    caveat?.let { add("${labels.caveat}: $it") }
}.joinToString("\n\n")
