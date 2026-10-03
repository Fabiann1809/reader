package io.github.fabiann1809.reader.ui.library

import io.github.fabiann1809.reader.data.book.Book
import io.github.fabiann1809.reader.util.matchesSearch

/** True when the title or the author contains [query], ignoring case and accents ("garcia" finds "García"). */
fun Book.matchesSearch(query: String): Boolean = matchesSearch(query, title, author)
