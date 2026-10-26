package io.github.fabiann1809.reader.data.reminder

import io.github.fabiann1809.reader.data.flashcard.FlashcardRepository
import io.github.fabiann1809.reader.data.prefs.AppSettingsStore
import io.github.fabiann1809.reader.data.prefs.current
import kotlinx.coroutines.flow.first

/** Tells the user that cards are waiting. */
fun interface ReviewNotifier {
    fun notifyDue(count: Int)
}

/**
 * What happens at the reminder's hour (T14.6): if cards are due, a notification says how many; either
 * way, tomorrow's reminder is set. Nothing happens once the reminder is turned off.
 */
class ReviewReminder(
    private val settings: AppSettingsStore,
    private val flashcards: FlashcardRepository,
    private val notifier: ReviewNotifier,
    private val scheduler: ReviewReminderScheduler,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun deliver() {
        val current = settings.current()
        if (!current.reviewReminder) return
        val due = flashcards.observeDue(now()).first().size
        if (due > 0) notifier.notifyDue(due)
        scheduler.schedule(current.reminderMinutes)
    }
}
