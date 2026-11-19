package io.github.fabiann1809.reader.data.sync.cloud

import java.io.IOException

/** Why talking to the cloud failed. Never carries the access token. */
sealed class SyncError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** No usable permission: the user must sign in or grant access again. */
    class NotAuthorized(message: String = "Not authorized") : SyncError(message)

    /** The user's cloud storage is full. */
    class QuotaExceeded : SyncError("Cloud storage is full")

    class NoInternet(cause: Throwable) : SyncError("No internet connection", cause)

    /** Someone else changed the manifest since it was read; read it and merge again. */
    class Conflict : SyncError("The cloud data changed meanwhile")

    /** The cloud holds data from a newer version of the app. */
    class NewerFormat : SyncError("The cloud data is from a newer version of the app")

    class Unknown(message: String, cause: Throwable? = null) : SyncError(message, cause)
}

/** Runs a cloud call and reports whatever goes wrong as a [SyncError]. */
internal inline fun <T> runCloudCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: SyncError) {
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(SyncError.NoInternet(e))
    } catch (e: IllegalArgumentException) {
        // Includes a body that is not valid JSON.
        Result.failure(SyncError.Unknown("Unexpected response", e))
    }
