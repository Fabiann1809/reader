package io.github.fabiann1809.reader.data.sync.auth

import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import io.github.fabiann1809.reader.data.sync.cloud.AccessTokenProvider

/** What the user has to see to give the permission: Google's consent screen. */
fun interface Consent {
    /** The request the screen opens with; the result comes back through [DriveAuthorizer.grantFrom]. */
    fun toIntentSenderRequest(): IntentSenderRequest
}

/** The answer to asking for the permission to use the app's hidden Drive folder. */
sealed interface DriveGrant {

    /** The permission is given, and [accessToken] opens Drive for a while. */
    data class Granted(val accessToken: String) : DriveGrant

    /** The user has to accept first, on Google's screen. */
    data class NeedsConsent(val consent: Consent) : DriveGrant

    /** The user said no or closed the screen. */
    data object Declined : DriveGrant

    /** Google couldn't answer (no network, Play Services missing or outdated...). */
    data object Failed : DriveGrant
}

/**
 * The permission to use the app's hidden Drive folder (T18.4), which is separate from signing in.
 * Everything about Google's authorization stays behind this interface.
 */
interface DriveAuthorizer : AccessTokenProvider {

    /** Asks for the permission. If it was given before, answers with the token without showing anything. */
    suspend fun authorize(): DriveGrant

    /** Reads the answer of the consent screen: [data] is what the screen returned. */
    fun grantFrom(data: Intent?): DriveGrant
}
