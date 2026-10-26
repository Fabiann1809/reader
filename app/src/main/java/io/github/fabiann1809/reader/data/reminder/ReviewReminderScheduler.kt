package io.github.fabiann1809.reader.data.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/** Arranges the day's review reminder (T14.6). */
interface ReviewReminderScheduler {
    /** Sets the next reminder at [minutesOfDay] (minutes after midnight), replacing any waiting one. */
    fun schedule(minutesOfDay: Int)

    fun cancel()
}

/** [ReviewReminderScheduler] with WorkManager, which keeps the reminder across app restarts and reboots. */
class WorkManagerReviewReminderScheduler(context: Context) : ReviewReminderScheduler {

    private val workManager = WorkManager.getInstance(context)

    // One job at a time that sets the next one when it runs, so the hour stays exact (a periodic job drifts).
    override fun schedule(minutesOfDay: Int) {
        val delay = millisUntilNext(minutesOfDay, ZonedDateTime.now())
        val request = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "review-reminder"
    }
}
