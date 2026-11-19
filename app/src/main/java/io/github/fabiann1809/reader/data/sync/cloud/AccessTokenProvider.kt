package io.github.fabiann1809.reader.data.sync.cloud

/** Gives a valid access token for the cloud service, or fails with [SyncError.NotAuthorized]. */
fun interface AccessTokenProvider {
    suspend fun accessToken(): String
}
