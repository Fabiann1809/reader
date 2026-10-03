package io.github.fabiann1809.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import android.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import io.github.fabiann1809.reader.data.prefs.AppTheme
import io.github.fabiann1809.reader.ui.navigation.ReaderApp
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// FragmentActivity (not just ComponentActivity): Readium's reader is a Fragment hosted inside Compose.
class MainActivity : FragmentActivity() {

    // Asks the navigation to show the library, where the import progress is.
    private val showLibraryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val container: AppContainer
        get() = (application as ReaderApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh start: after a rotation the same intent would import the files again.
        if (savedInstanceState == null) {
            receiveBooks(intent)
            lifecycleScope.launch { container.watchedFolderManager.checkOnAppStart() }
            lifecycleScope.launch { container.fileHashBackfill.run() }
        }
        setContent {
            // The theme chosen in Ajustes (T17.1); the phone's until it's read.
            val theme by container.appSettings.settings.map { it.theme }.collectAsStateWithLifecycle(AppTheme.SYSTEM)
            val dark = when (theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }
            // The system bars' icons follow the app's theme, not the phone's.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_NAVIGATION_SCRIM, DARK_NAVIGATION_SCRIM) { dark },
                )
                onDispose {}
            }
            ReaderTheme(darkTheme = dark) {
                ReaderApp(showLibraryRequests = showLibraryRequests)
            }
        }
    }

    // The activity is singleTask, so files shared while Reader is open arrive here instead of a new copy.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveBooks(intent)
    }

    private fun receiveBooks(intent: Intent) {
        val uris = incomingBookUris(intent)
        if (uris.isEmpty()) return
        container.importQueue.add(uris.map { it.toString() })
        // On a fresh start the library is already the first screen, so a lost request does not matter.
        showLibraryRequests.tryEmit(Unit)
    }
}

// enableEdgeToEdge's own navigation bar scrims, for 3-button navigation.
private val LIGHT_NAVIGATION_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_NAVIGATION_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
