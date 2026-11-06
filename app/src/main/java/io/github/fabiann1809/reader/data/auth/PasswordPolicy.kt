package io.github.fabiann1809.reader.data.auth

/** One requirement of a new password. The screen lists them and ticks each one as it is met. */
enum class PasswordRule { MIN_LENGTH, HAS_UPPERCASE, HAS_LOWERCASE, HAS_DIGIT, HAS_SYMBOL }

/**
 * What a new password must satisfy (T22.6): at least 8 characters with an uppercase letter, a
 * lowercase letter, a digit and a symbol (type 4 of the apphive guide). Only used when creating an
 * account: signing in accepts whatever the account already has. The service enforces its own
 * minimum too, so this is the friendly first line, not the only one.
 */
object PasswordPolicy {
    const val MIN_LENGTH = 8

    fun passes(rule: PasswordRule, password: String): Boolean = when (rule) {
        PasswordRule.MIN_LENGTH -> password.length >= MIN_LENGTH
        PasswordRule.HAS_UPPERCASE -> password.any { it.isUpperCase() }
        PasswordRule.HAS_LOWERCASE -> password.any { it.isLowerCase() }
        PasswordRule.HAS_DIGIT -> password.any { it.isDigit() }
        // Any character that is not a letter, a digit or a space: the guide's regex only lists @$!%*?&.
        PasswordRule.HAS_SYMBOL -> password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    }

    fun failedRules(password: String): List<PasswordRule> = PasswordRule.entries.filterNot { passes(it, password) }

    fun isValid(password: String): Boolean = failedRules(password).isEmpty()
}
