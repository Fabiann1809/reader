package io.github.fabiann1809.reader.data.sync.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
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

    override suspend fun authorize(): DriveGrant = try {
        grantOf(client.authorize(request).await())
    } catch (e: ApiException) {
        if (e.statusCode == CommonStatusCodes.CANCELED) DriveGrant.Declined else DriveGrant.Failed
    }

    override fun grantFrom(data: Intent?): DriveGrant = try {
        grantOf(client.getAuthorizationResultFromIntent(data))
    } catch (e: ApiException) {
        if (e.statusCode == CommonStatusCodes.CANCELED) DriveGrant.Declined else DriveGrant.Failed
    }

    /** The background sync has no screen to ask on: without the permission it can only ask to be reconnected. */
    override suspend fun accessToken(): String = when (val grant = authorize()) {
        is DriveGrant.Granted -> grant.accessToken
        is DriveGrant.NeedsConsent, DriveGrant.Declined -> throw SyncError.NotAuthorized()
        DriveGrant.Failed -> throw SyncError.Unknown("Google could not give the Drive permission")
    }

    private fun grantOf(result: AuthorizationResult): DriveGrant {
        val pendingIntent = result.pendingIntent
        val token = result.accessToken
        return when {
            result.hasResolution() && pendingIntent != null ->
                DriveGrant.NeedsConsent { IntentSenderRequest.Builder(pendingIntent).build() }
            token != null -> DriveGrant.Granted(token)
            else -> DriveGrant.Failed
        }
    }

    private companion object {
        const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}
