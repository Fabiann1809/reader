package io.github.fabiann1809.reader.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException

/** Turns what Firebase throws into an [AuthError]. */
internal fun Throwable.toAuthError(): Throwable = when (this) {
    is CancellationException -> this
    is AuthError -> this
    is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword()
    is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse()
    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == INVALID_EMAIL) AuthError.InvalidEmail() else AuthError.WrongCredentials()
    is FirebaseAuthInvalidUserException ->
        if (errorCode == USER_DISABLED) AuthError.AccountDisabled() else AuthError.WrongCredentials()
    is FirebaseTooManyRequestsException -> AuthError.TooManyAttempts()
    is FirebaseNetworkException -> AuthError.NoInternet(this)
    else -> AuthError.Unknown(this)
}

private const val INVALID_EMAIL = "ERROR_INVALID_EMAIL"
private const val USER_DISABLED = "ERROR_USER_DISABLED"
