package io.github.fabiann1809.reader.testing

import android.content.Intent
import io.github.fabiann1809.reader.data.sync.auth.DriveAuthorizer
import io.github.fabiann1809.reader.data.sync.auth.DriveGrant
import io.github.fabiann1809.reader.data.sync.cloud.SyncError

/** A [DriveAuthorizer] that answers what the test says: [onAuthorize] when asked, [afterConsent] after the screen. */
class FakeDriveAuthorizer(
    var onAuthorize: DriveGrant = DriveGrant.Granted("token"),
    var afterConsent: DriveGrant = DriveGrant.Granted("token"),
) : DriveAuthorizer {

    var authorizeCalls = 0
        private set

    override suspend fun authorize(): DriveGrant {
        authorizeCalls++
        return onAuthorize
    }

    override fun grantFrom(data: Intent?): DriveGrant = afterConsent

    override suspend fun accessToken(): String = when (val grant = onAuthorize) {
        is DriveGrant.Granted -> grant.accessToken
        else -> throw SyncError.NotAuthorized()
    }
}
