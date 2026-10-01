package io.github.fabiann1809.reader.ui.library

import io.github.fabiann1809.reader.data.book.Book
import java.text.Normalizer

/** True when the title or the author contains [query], ignoring case and accents ("garcia" finds "García"). */
fun Book.matchesSearch(query: String): Boolean {
    val words = query.normalizeForSearch().split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val text = "$title $author".normalizeForSearch()
    return words.all { it in text }
}

private val DIACRITICS = Regex("""\p{Mn}+""")

private fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()
