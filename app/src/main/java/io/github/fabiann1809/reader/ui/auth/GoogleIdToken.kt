package io.github.fabiann1809.reader.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.fabiann1809.reader.data.auth.AuthError

/**
 * Opens the Google account picker of Android and returns the ID token Firebase needs.
 * [context] must be an Activity, because the picker is drawn on top of it.
 */
suspend fun requestGoogleIdToken(context: Context, webClientId: String): String {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
        .build()
    try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw AuthError.Unknown()
    } catch (e: GetCredentialCancellationException) {
        throw AuthError.Cancelled()
    } catch (e: NoCredentialException) {
        throw AuthError.NoGoogleAccount()
    } catch (e: GetCredentialException) {
        throw AuthError.Unknown(e)
    } catch (e: GoogleIdTokenParsingException) {
        throw AuthError.Unknown(e)
    }
}
