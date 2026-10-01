package io.github.fabiann1809.reader.ui.components

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Makes the status bar icons light while this is on screen, for screens with a dark top area
 * (the green library bar, the camera). The default for the current theme is restored on exit.
 */
@Composable
fun LightStatusBarIcons() {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    if (view.isInEditMode) return
    DisposableEffect(view, darkTheme) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        onDispose { controller?.isAppearanceLightStatusBars = !darkTheme }
    }
}

/**
 * Hides the status and navigation bars while this is on screen (full-screen reading). A swipe from
 * the edge shows them for a moment; they come back when the screen is left.
 */
@Composable
fun ImmersiveMode() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
