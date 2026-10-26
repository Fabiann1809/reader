package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.reminder.ReviewReminderScheduler

/** Remembers when the reminder was set, and whether it was cancelled. */
class FakeReviewReminderScheduler : ReviewReminderScheduler {
    val scheduled = mutableListOf<Int>()
    var cancelled = false

    override fun schedule(minutesOfDay: Int) {
        scheduled += minutesOfDay
        cancelled = false
    }

    override fun cancel() {
        cancelled = true
    }
}
