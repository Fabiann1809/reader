package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.auth.AuthError
import io.github.fabiann1809.reader.data.auth.AuthRepository
import io.github.fabiann1809.reader.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory accounts. Set [failWith] to make the next calls fail with that error. */
class FakeAuthRepository(signedIn: AuthUser? = null) : AuthRepository {
    private val user = MutableStateFlow(signedIn)
    override val currentUser: StateFlow<AuthUser?> = user

    private val accounts = mutableMapOf<String, String>()
    val resetEmails = mutableListOf<String>()
    var failWith: AuthError? = null

    override suspend fun signUp(email: String, password: String) {
        failWith?.let { throw it }
        if (email in accounts) throw AuthError.EmailAlreadyInUse()
        accounts[email] = password
        user.value = AuthUser(uid = "uid-$email", email = email)
    }

    override suspend fun signIn(email: String, password: String) {
        failWith?.let { throw it }
        if (accounts[email] != password) throw AuthError.WrongCredentials()
        user.value = AuthUser(uid = "uid-$email", email = email)
    }

    override suspend fun signInWithGoogle(idToken: String) {
        failWith?.let { throw it }
        user.value = AuthUser(uid = "google-$idToken", email = "google@example.com")
    }

    override suspend fun sendPasswordReset(email: String) {
        failWith?.let { throw it }
        resetEmails += email
    }

    override fun signOut() {
        user.value = null
    }
}
