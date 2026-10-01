package io.github.fabiann1809.reader.data.book.folder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.fabiann1809.reader.ReaderApplication

/** Background job that checks the watched folder. The work itself is in [WatchedFolderSync]. */
class WatchedFolderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sync = (applicationContext as ReaderApplication).container.watchedFolderSync
        // An unavailable folder is not retried: the next periodic check will try again anyway.
        sync.sync()
        return Result.success()
    }
}
