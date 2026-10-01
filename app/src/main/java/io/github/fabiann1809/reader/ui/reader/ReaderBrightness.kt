package io.github.fabiann1809.reader.ui.reader

import android.provider.Settings
import android.view.Window
import android.view.WindowManager

/**
 * The page's brightness, set on the reader's window only: the phone's own brightness never changes.
 * The chosen value lives in the ReaderSession, so it survives a rotation and ends with the book.
 */
class ReaderBrightness(private val window: Window) {

    /** Shows [brightness] (0 to 1), or the system's when null. */
    fun show(brightness: Float?) {
        window.attributes = window.attributes.apply {
            screenBrightness = brightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    /** The brightness after a drag on the left edge, from [current] or, the first time, from the system's. */
    fun afterDrag(current: Float?, dragFraction: Float): Float = brightnessAfterDrag(current ?: systemBrightness(), dragFraction)

    // The system setting goes from 0 to 255; an unreadable one starts from the middle.
    private fun systemBrightness(): Float =
        Settings.System.getInt(window.context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
}
