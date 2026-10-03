package io.github.fabiann1809.reader.util

import java.text.Normalizer

private val DIACRITICS = Regex("""\p{Mn}+""")

/** Lowercase and without accents, so "garcia" finds "García". */
fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()

/** True when every word of [query] is somewhere in [texts], ignoring case and accents; a blank query matches. */
fun matchesSearch(query: String, vararg texts: String?): Boolean {
    val words = query.normalizeForSearch().split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val text = texts.filterNotNull().joinToString(" ").normalizeForSearch()
    return words.all { it in text }
}
