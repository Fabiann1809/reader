package io.github.fabiann1809.reader.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.fabiann1809.reader.ReaderApplication
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import java.util.concurrent.TimeUnit

/** Arranges the background sync (T18.6). */
interface SyncScheduler {
    /**
     * Asks for a sync in [delayMillis]. With [replace] it takes the place of one that is waiting;
     * without it, a waiting one stays (so many changes in a row make one sync).
     */
    fun schedule(wifiOnly: Boolean, delayMillis: Long, replace: Boolean)

    fun cancel()
}

/** [SyncScheduler] with WorkManager: it waits for the network, retries on its own, and survives restarts. */
class WorkManagerSyncScheduler(context: Context) : SyncScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun schedule(wifiOnly: Boolean, delayMillis: Long, replace: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "sync"
        const val BACKOFF_SECONDS = 30L
    }
}

/** Background job that syncs. The work itself is in [SyncCoordinator]. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val coordinator = (applicationContext as ReaderApplication).container.syncCoordinator
        return resultFor(coordinator.runSync().exceptionOrNull(), runAttemptCount)
    }
}

private const val MAX_ATTEMPTS = 5

/** Whether the job is done, worth trying again, or lost: [failure] is why it failed, null when it worked. */
internal fun resultFor(failure: Throwable?, attempt: Int): ListenableWorker.Result = when {
    failure == null -> ListenableWorker.Result.success()
    // Only the user can fix these: trying again changes nothing.
    failure is SyncError.NotAuthorized || failure is SyncError.QuotaExceeded || failure is SyncError.NewerFormat ->
        ListenableWorker.Result.failure()
    attempt >= MAX_ATTEMPTS -> ListenableWorker.Result.failure()
    else -> ListenableWorker.Result.retry()
}
