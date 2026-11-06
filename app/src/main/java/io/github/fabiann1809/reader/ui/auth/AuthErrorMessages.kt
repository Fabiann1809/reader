package io.github.fabiann1809.reader.ui.auth

import androidx.annotation.StringRes
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.auth.AuthError

/** User-facing message for a sign-in failure. */
@StringRes
fun authErrorMessageRes(error: AuthError): Int = when (error) {
    is AuthError.InvalidEmail -> R.string.auth_error_email_invalid
    is AuthError.WeakPassword -> R.string.auth_error_password_weak
    is AuthError.EmailAlreadyInUse -> R.string.auth_error_email_in_use
    is AuthError.WrongCredentials -> R.string.auth_error_wrong_credentials
    is AuthError.AccountDisabled -> R.string.auth_error_disabled
    is AuthError.TooManyAttempts -> R.string.auth_error_too_many
    is AuthError.NoInternet -> R.string.auth_error_no_internet
    is AuthError.NoGoogleAccount -> R.string.auth_error_no_google_account
    is AuthError.Cancelled, is AuthError.Unknown -> R.string.auth_error_unknown
}

/** User-facing message for a problem under a field. */
@StringRes
fun fieldErrorMessageRes(error: FieldError): Int = when (error) {
    FieldError.EMAIL_REQUIRED -> R.string.auth_error_email_required
    FieldError.EMAIL_INVALID -> R.string.auth_error_email_invalid
    FieldError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    FieldError.PASSWORD_REQUIRED -> R.string.auth_error_password_required
    FieldError.PASSWORD_WEAK -> R.string.auth_error_password_weak
    FieldError.CONFIRM_REQUIRED -> R.string.auth_error_confirm_required
    FieldError.CONFIRM_MISMATCH -> R.string.auth_error_confirm_mismatch
}
