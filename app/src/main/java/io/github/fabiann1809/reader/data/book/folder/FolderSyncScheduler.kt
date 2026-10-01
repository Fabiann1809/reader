package io.github.fabiann1809.reader.data.book.folder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** When the watched folder gets checked. */
interface FolderSyncScheduler {
    /** Checks now and then periodically, also after the app is closed or the phone restarts. */
    fun start()

    /** One extra check now (e.g. when the app opens), without changing the periodic ones. */
    fun checkNow()

    fun stop()
}

/** [FolderSyncScheduler] with WorkManager, which survives app restarts and reboots. */
class WorkManagerFolderSyncScheduler(context: Context) : FolderSyncScheduler {

    private val workManager = WorkManager.getInstance(context)

    override fun start() {
        // A cheap folder listing, so once an hour does not cost noticeable battery.
        val periodic = PeriodicWorkRequestBuilder<WatchedFolderWorker>(CHECK_INTERVAL_HOURS, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        checkNow()
    }

    override fun checkNow() {
        val once = OneTimeWorkRequestBuilder<WatchedFolderWorker>().build()
        // KEEP: if a check is already waiting or running, a second one would find nothing new.
        workManager.enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.KEEP, once)
    }

    override fun stop() {
        workManager.cancelUniqueWork(PERIODIC_WORK)
        workManager.cancelUniqueWork(ONE_TIME_WORK)
    }

    private companion object {
        const val PERIODIC_WORK = "watched-folder-periodic"
        const val ONE_TIME_WORK = "watched-folder-now"
        const val CHECK_INTERVAL_HOURS = 1L
    }
}
