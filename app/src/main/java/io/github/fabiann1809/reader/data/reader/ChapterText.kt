package io.github.fabiann1809.reader.data.reader

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.use

/**
 * The plain text of the chapter file [href] of an EPUB, for a quiz about it (T15.3). Null when the
 * file can't be read; PDFs have no chapter files to read this way.
 */
suspend fun Publication.chapterText(href: String): String? = withContext(Dispatchers.IO) {
    val url = Url(href) ?: return@withContext null
    val bytes = get(url)?.use { it.read().getOrNull() } ?: return@withContext null
    htmlToText(bytes.toString(Charsets.UTF_8)).takeIf { it.isNotBlank() }
}

// The head, styles and scripts hold no text to read; everything else becomes plain paragraphs.
private val NON_TEXT = Regex("<(head|style|script)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val BLANK_LINES = Regex("\n{3,}")
private const val NO_BREAK_SPACE = ' '

internal fun htmlToText(html: String): String {
    val body = html.replace(NON_TEXT, "")
    return HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
        .replace(NO_BREAK_SPACE, ' ')
        .replace(BLANK_LINES, "\n\n")
        .trim()
}
