package io.github.fabiann1809.reader.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * The only entry point to sign-in. Calls throw [AuthError]; the rest of the app never sees Firebase.
 */
interface AuthRepository {
    /** Who is signed in, or null. Starts with the session saved on the device. */
    val currentUser: StateFlow<AuthUser?>

    suspend fun signUp(email: String, password: String)

    suspend fun signIn(email: String, password: String)

    /** [idToken] comes from the Google account picker. */
    suspend fun signInWithGoogle(idToken: String)

    suspend fun sendPasswordReset(email: String)

    fun signOut()
}
