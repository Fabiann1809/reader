package io.github.fabiann1809.reader.data.reminder

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidReviewNotifierTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Before
    fun allowNotifications() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun cleanUp() = manager.cancelAll()

    @Test
    fun postsOneNotificationSayingHowManyCardsAreDue() {
        AndroidReviewNotifier(context).notifyDue(3)

        val shown = manager.activeNotifications.single()
        assertEquals("Hora de repasar", shown.notification.extras.getString("android.title"))
        assertEquals("Tienes 3 fichas por repasar", shown.notification.extras.getString("android.text"))
    }

    @Test
    fun aSecondReminderReplacesTheFirstAndOneCardIsSingular() {
        val notifier = AndroidReviewNotifier(context)
        notifier.notifyDue(3)
        notifier.notifyDue(1)

        val shown = manager.activeNotifications
        assertEquals(1, shown.size)
        assertTrue(shown.single().notification.extras.getString("android.text") == "Tienes 1 ficha por repasar")
    }
}
