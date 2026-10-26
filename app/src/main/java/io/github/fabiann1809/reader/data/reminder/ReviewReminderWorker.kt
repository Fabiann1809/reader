package io.github.fabiann1809.reader.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.fabiann1809.reader.ReaderApplication

/** Background job that fires at the reminder's hour. The work itself is in [ReviewReminder]. */
class ReviewReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        (applicationContext as ReaderApplication).container.reviewReminder.deliver()
        return Result.success()
    }
}
