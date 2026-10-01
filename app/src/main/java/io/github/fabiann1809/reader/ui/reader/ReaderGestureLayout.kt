package io.github.fabiann1809.reader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Wraps the navigator and sees every touch before the page does. It takes over two gestures the
 * navigator doesn't have: a pinch (only when [onPinch] is set; the PDF view zooms by itself) and a
 * vertical drag that starts on the left edge (brightness). Every other touch reaches the page as is.
 * It watches in dispatchTouchEvent because the page's views may forbid their parents to intercept.
 */
class ReaderGestureLayout(context: Context) : FrameLayout(context) {

    /** Called when a pinch ends, with how much it scaled (above 1 = fingers spread). */
    var onPinch: ((Float) -> Unit)? = null

    /** Called while dragging on the left edge, with the move as a fraction of the height (down = positive). */
    var onEdgeDrag: ((Float) -> Unit)? = null

    private enum class Gesture { NONE, PINCH, EDGE_DRAG }

    private var gesture = Gesture.NONE
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var lastY = 0f
    private var startedOnEdge = false
    private var pinchScale = 1f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                pinchScale *= detector.scaleFactor
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                onPinch?.invoke(pinchScale)
                pinchScale = 1f
            }
        },
    )

    @SuppressLint("ClickableViewAccessibility") // Gestures only; taps and accessibility actions stay with the page.
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (onPinch != null) scaleDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) start(event)
        if (gesture == Gesture.NONE) {
            gesture = detect(event)
            if (gesture != Gesture.NONE) cancelPageTouch(event)
        }
        if (gesture == Gesture.NONE) return super.dispatchTouchEvent(event)
        if (gesture == Gesture.EDGE_DRAG && event.actionMasked == MotionEvent.ACTION_MOVE && event.y != lastY) {
            onEdgeDrag?.invoke((event.y - lastY) / height)
            lastY = event.y
        }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            gesture = Gesture.NONE
        }
        return true
    }

    private fun start(event: MotionEvent) {
        gesture = Gesture.NONE
        downX = event.x
        downY = event.y
        lastY = event.y
        startedOnEdge = isBrightnessEdge(event.x, width)
    }

    private fun detect(event: MotionEvent): Gesture {
        if (onPinch != null && event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) return Gesture.PINCH
        if (onEdgeDrag == null || !startedOnEdge || event.actionMasked != MotionEvent.ACTION_MOVE) return Gesture.NONE
        val dy = abs(event.y - downY)
        // A clearly vertical move: a tap or a horizontal page swipe from the edge stays with the page.
        return if (dy > touchSlop && dy > abs(event.x - downX)) Gesture.EDGE_DRAG else Gesture.NONE
    }

    // The page already got the first touches of the gesture; it must forget them.
    private fun cancelPageTouch(event: MotionEvent) {
        val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
        lastY = event.y
    }
}
