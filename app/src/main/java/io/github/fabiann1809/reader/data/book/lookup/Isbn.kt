package io.github.fabiann1809.reader.data.book.lookup

/** An ISBN-10 or ISBN-13, as the digits (and a final X) the check digit is computed over. */
object Isbn {

    /** The ISBN in [raw] (hyphens and spaces allowed), or null if it is not a valid one. */
    fun normalize(raw: String): String? {
        val code = raw.filterNot { it == '-' || it.isWhitespace() }.uppercase()
        val valid = when (code.length) {
            ISBN_10 -> isValidIsbn10(code)
            ISBN_13 -> isValidIsbn13(code)
            else -> false
        }
        return code.takeIf { valid }
    }

    // Digits weighted 10 down to 1 (the last may be X = 10) add up to a multiple of 11.
    private fun isValidIsbn10(code: String): Boolean {
        val sum = code.mapIndexed { index, char ->
            val value = if (char == 'X' && index == ISBN_10 - 1) 10 else char.digitToIntOrNull() ?: return false
            value * (ISBN_10 - index)
        }.sum()
        return sum % 11 == 0
    }

    // Digits weighted 1, 3, 1, 3… add up to a multiple of 10.
    private fun isValidIsbn13(code: String): Boolean {
        val sum = code.mapIndexed { index, char ->
            (char.digitToIntOrNull() ?: return false) * if (index % 2 == 0) 1 else 3
        }.sum()
        return sum % 10 == 0
    }

    private const val ISBN_10 = 10
    private const val ISBN_13 = 13
}
