package io.github.fabiann1809.reader.ui.quiz

import io.github.fabiann1809.reader.ai.QUIZ_SIZES

// A quiz about one paragraph (after its explanation or after writing about it) is short.
const val PARAGRAPH_QUIZ_SIZE = 3

// Text length needed for 5 and 10 questions; shorter texts only give enough for fewer.
private const val CHARS_FOR_FIVE = 800
private const val CHARS_FOR_TEN = 3000

/** The most questions worth asking about [source]: a short text can't back ten different ones. */
fun maxQuizSizeFor(source: String): Int = when {
    source.length >= CHARS_FOR_TEN -> QUIZ_SIZES.last()
    source.length >= CHARS_FOR_FIVE -> QUIZ_SIZES[1]
    else -> QUIZ_SIZES.first()
}
