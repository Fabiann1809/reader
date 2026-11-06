package io.github.fabiann1809.reader.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {

    private val user = MutableStateFlow(auth.currentUser?.toAuthUser())
    override val currentUser: StateFlow<AuthUser?> = user

    init {
        // Firebase also tells us when the session ends on its own (e.g. the account was deleted).
        auth.addAuthStateListener { user.value = it.currentUser?.toAuthUser() }
    }

    override suspend fun signUp(email: String, password: String) = call {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signIn(email: String, password: String) = call {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun signInWithGoogle(idToken: String) = call {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    override suspend fun sendPasswordReset(email: String) = call {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    override fun signOut() = auth.signOut()

    private suspend fun call(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            throw e.toAuthError()
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email)
}
