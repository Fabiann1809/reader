package io.github.fabiann1809.reader.data.auth

/**
 * Reasons a sign-in call can fail. [AuthRepository] translates the service's own errors into these,
 * so the UI can show a specific message without knowing which service is behind it.
 *
 * Messages are for developers only; they must never contain a password or a token.
 */
sealed class AuthError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class InvalidEmail : AuthError("The email address is not valid")

    class WeakPassword : AuthError("The password is too weak")

    class EmailAlreadyInUse : AuthError("An account with this email already exists")

    /** Wrong email or password. The service does not say which, so neither do we. */
    class WrongCredentials : AuthError("Wrong email or password")

    class AccountDisabled : AuthError("The account is disabled")

    class TooManyAttempts : AuthError("Too many attempts; try again later")

    class NoInternet(cause: Throwable) : AuthError("No internet connection", cause)

    /** The person closed the Google account picker. Not really a failure. */
    class Cancelled : AuthError("Sign-in was cancelled")

    class Unknown(cause: Throwable? = null) : AuthError("Unexpected sign-in error", cause)
}
