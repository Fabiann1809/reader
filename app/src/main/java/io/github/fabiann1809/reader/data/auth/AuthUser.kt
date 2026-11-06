package io.github.fabiann1809.reader.data.auth

/** The person signed in. Only what the screens show; the password never leaves the auth service. */
data class AuthUser(val uid: String, val email: String?)
