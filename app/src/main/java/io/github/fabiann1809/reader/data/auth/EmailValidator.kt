package io.github.fabiann1809.reader.data.auth

/**
 * Checks that an email looks right before asking the service. It is our own check because
 * android.util.Patterns does not exist in local unit tests. The service has the last word.
 */
object EmailValidator {
    private const val MAX_LENGTH = 254

    // name@domain.tld: no spaces, no double dots, a domain with at least one dot, a TLD of 2+ letters.
    private val pattern = Regex(
        "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]*[A-Za-z0-9])?@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$",
    )

    fun isValid(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.length <= MAX_LENGTH && !trimmed.contains("..") && pattern.matches(trimmed)
    }
}
