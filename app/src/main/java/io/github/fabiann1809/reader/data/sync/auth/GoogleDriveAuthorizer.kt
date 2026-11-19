package io.github.fabiann1809.reader.data.sync.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import kotlinx.coroutines.tasks.await

/**
 * [DriveAuthorizer] with Google's Authorization API. The only scope is `drive.appdata`: a folder
 * only this app can see, so the app can't read anything else in the user's Drive.
 */
class GoogleDriveAuthorizer(private val context: Context) : DriveAuthorizer {

    private val client get() = Identity.getAuthorizationClient(context)

    private val request = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
        .build()

    override suspend fun authorize(): DriveGrant {
        // Without Play services there is nothing to ask: say so instead of a vague failure.
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return DriveGrant.Failed(FailureCause.NO_PLAY_SERVICES)
        }
        return try {
            grantOf(client.authorize(request).await())
        } catch (e: ApiException) {
            grantFor(e)
        }
    }

    override fun grantFrom(data: Intent?): DriveGrant = try {
        grantOf(client.getAuthorizationResultFromIntent(data))
    } catch (e: ApiException) {
        grantFor(e)
    }

    private fun grantFor(e: ApiException): DriveGrant = when (e.statusCode) {
        CommonStatusCodes.CANCELED -> DriveGrant.Declined
        CommonStatusCodes.NETWORK_ERROR -> DriveGrant.Failed(FailureCause.NO_NETWORK)
        else -> DriveGrant.Failed()
    }

    /** The background sync has no screen to ask on: without the permission it can only ask to be reconnected. */
    override suspend fun accessToken(): String = when (val grant = authorize()) {
        is DriveGrant.Granted -> grant.accessToken
        is DriveGrant.NeedsConsent, DriveGrant.Declined -> throw SyncError.NotAuthorized()
        is DriveGrant.Failed -> throw SyncError.Unknown("Google could not give the Drive permission")
    }

    private fun grantOf(result: AuthorizationResult): DriveGrant {
        val pendingIntent = result.pendingIntent
        val token = result.accessToken
        return when {
            result.hasResolution() && pendingIntent != null ->
                DriveGrant.NeedsConsent { IntentSenderRequest.Builder(pendingIntent).build() }
            token != null -> DriveGrant.Granted(token)
            else -> DriveGrant.Failed()
        }
    }

    private companion object {
        const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
