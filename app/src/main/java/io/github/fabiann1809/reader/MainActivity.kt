package io.github.fabiann1809.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import io.github.fabiann1809.reader.ui.navigation.ReaderApp
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

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
        }
        setContent {
            ReaderTheme {
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
