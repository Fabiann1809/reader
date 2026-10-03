package io.github.fabiann1809.reader.ui.reader

/** A quiz of [count] questions about the open chapter, named [title], to open from the reader (T15.3). */
data class ChapterQuiz(val text: String, val count: Int, val title: String)
