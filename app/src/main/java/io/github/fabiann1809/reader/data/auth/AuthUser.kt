package io.github.fabiann1809.reader.data.auth

/**
 * The person signed in. Only what the screens show; the password never leaves the auth service.
 * [emailVerified] is false for an email account whose link wasn't opened yet; Google accounts come verified.
 */
data class AuthUser(val uid: String, val email: String?, val emailVerified: Boolean = true)
