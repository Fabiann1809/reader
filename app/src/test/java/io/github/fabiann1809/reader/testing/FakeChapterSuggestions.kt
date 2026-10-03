package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.prefs.ChapterSuggestions

class FakeChapterSuggestions : ChapterSuggestions {
    val suggested = mutableSetOf<Pair<Long, String>>()

    override suspend fun wasSuggested(bookId: Long, href: String): Boolean = (bookId to href) in suggested

    override suspend fun markSuggested(bookId: Long, href: String) {
        suggested += bookId to href
    }
}
