package io.github.fabiann1809.reader.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthErrorsTest {

    @Test
    fun aBadEmailAndAWrongPasswordAreTold() {
        assertTrue(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "x").toAuthError() is AuthError.InvalidEmail)
        assertTrue(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "x").toAuthError() is AuthError.WrongCredentials)
    }

    @Test
    fun anUnknownAccountLooksLikeWrongCredentials() {
        assertTrue(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "x").toAuthError() is AuthError.WrongCredentials)
        assertTrue(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "x").toAuthError() is AuthError.AccountDisabled)
    }

    @Test
    fun signUpProblemsAreTold() {
        assertTrue(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "x").toAuthError() is AuthError.EmailAlreadyInUse)
        assertTrue(FirebaseAuthWeakPasswordException("x", "x", "too short").toAuthError() is AuthError.WeakPassword)
    }

    @Test
    fun networkAndRateLimitsAreTold() {
        assertTrue(FirebaseNetworkException("x").toAuthError() is AuthError.NoInternet)
        assertTrue(FirebaseTooManyRequestsException("x").toAuthError() is AuthError.TooManyAttempts)
    }

    @Test
    fun anythingElseIsUnknownAndACancellationPassesThrough() {
        assertTrue(IllegalStateException("x").toAuthError() is AuthError.Unknown)
        val cancellation = CancellationException("x")
        assertSame(cancellation, cancellation.toAuthError())
    }
}
