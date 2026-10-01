package io.github.fabiann1809.reader.ui.reader

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic touches on a 1080 × 2400 layout whose child records what reaches the page. */
@RunWith(AndroidJUnit4::class)
class ReaderGestureLayoutTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var layout: ReaderGestureLayout
    private val pageActions = mutableListOf<Int>()
    private val downTime = SystemClock.uptimeMillis()
    private var time = downTime

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            layout = ReaderGestureLayout(context)
            layout.addView(
                View(context).apply {
                    setOnTouchListener { _, event ->
                        pageActions += event.actionMasked
                        true
                    }
                },
            )
            layout.measure(MeasureSpec.makeMeasureSpec(1080, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(2400, MeasureSpec.EXACTLY))
            layout.layout(0, 0, 1080, 2400)
        }
    }

    @Test
    fun aTapStillReachesThePage() {
        var pinched: Float? = null
        layout.onPinch = { pinched = it }

        send(MotionEvent.ACTION_DOWN, 540f to 1200f)
        send(MotionEvent.ACTION_UP, 540f to 1200f)

        assertEquals(listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP), pageActions)
        assertNull(pinched)
    }

    @Test
    fun spreadingTwoFingersIsAPinchThatGrowsTheText() {
        var pinched: Float? = null
        layout.onPinch = { pinched = it }

        send(MotionEvent.ACTION_DOWN, 440f to 1200f)
        send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 440f to 1200f, 640f to 1200f)
        for (step in 1..10) send(MotionEvent.ACTION_MOVE, (440f - step * 25) to 1200f, (640f + step * 25) to 1200f)
        send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 190f to 1200f, 890f to 1200f)
        send(MotionEvent.ACTION_UP, 190f to 1200f)

        // Android measures a pinch once the fingers are a minimum distance apart, so only "it grew" is certain.
        assertTrue("scale was $pinched", (pinched ?: 0f) > 1.2f)
        // The page saw the first finger, then a cancel: it must not turn a page or select text.
        assertEquals(MotionEvent.ACTION_CANCEL, pageActions.last())
    }

    @Test
    fun withoutAPinchHandlerTheTwoFingersGoToThePage() {
        // The PDF view zooms with two fingers by itself.
        send(MotionEvent.ACTION_DOWN, 440f to 1200f)
        send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), 440f to 1200f, 640f to 1200f)

        assertEquals(MotionEvent.ACTION_POINTER_DOWN, pageActions.last())
    }

    @Test
    fun aVerticalDragOnTheLeftEdgeChangesTheBrightness() {
        val drags = mutableListOf<Float>()
        layout.onEdgeDrag = { drags += it }

        send(MotionEvent.ACTION_DOWN, 40f to 1600f)
        for (step in 1..8) send(MotionEvent.ACTION_MOVE, 40f to (1600f - step * 60))
        send(MotionEvent.ACTION_UP, 40f to 1120f)

        // Upwards: negative fractions of the height.
        assertTrue("drags were $drags", drags.isNotEmpty() && drags.all { it < 0 })
        assertEquals(MotionEvent.ACTION_CANCEL, pageActions.last())
    }

    @Test
    fun aHorizontalSwipeFromTheEdgeStaysWithThePage() {
        layout.onEdgeDrag = { error("not a brightness drag") }

        send(MotionEvent.ACTION_DOWN, 40f to 1200f)
        for (step in 1..8) send(MotionEvent.ACTION_MOVE, (40f + step * 80) to 1200f)
        send(MotionEvent.ACTION_UP, 680f to 1200f)

        assertEquals(MotionEvent.ACTION_UP, pageActions.last())
    }

    private fun send(action: Int, vararg points: Pair<Float, Float>) {
        time += 16
        val properties = Array(points.size) { index -> MotionEvent.PointerProperties().apply { id = index; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        val coords = Array(points.size) { index -> MotionEvent.PointerCoords().apply { x = points[index].first; y = points[index].second; pressure = 1f; size = 1f } }
        val event = MotionEvent.obtain(downTime, time, action, points.size, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { layout.dispatchTouchEvent(event) }
        event.recycle()
    }
}
