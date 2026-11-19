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

    /** Sends the link that proves the signed-in email address is the person's. */
    suspend fun sendEmailVerification()

    /** Asks the service again whether the link was opened, and updates [currentUser] with the answer. */
    suspend fun refreshUser()

    fun signOut()
}
