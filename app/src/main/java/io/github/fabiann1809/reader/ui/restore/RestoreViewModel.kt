package io.github.fabiann1809.reader.ui.restore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.auth.AuthRepository
import io.github.fabiann1809.reader.data.sync.SyncRestorer
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface RestoreState {

    /** Finding out whether this person's library is waiting in Drive; only a few milliseconds. */
    data object Checking : RestoreState

    /** Bringing the library back from Drive. */
    data object Restoring : RestoreState

    /** It couldn't be brought back; the person can try again or go on without it. */
    data class Failed(val error: SyncError) : RestoreState

    /** Nothing to restore, or it's done, or the person went on: the app opens. */
    data object Done : RestoreState
}

/**
 * Brings the library back after signing in again (T18.9b), while the app root shows a loading screen
 * instead of an empty library. See [SyncRestorer] for when that happens.
 */
class RestoreViewModel(auth: AuthRepository, private val restorer: SyncRestorer) : ViewModel() {

    private val _state = MutableStateFlow<RestoreState>(RestoreState.Checking)
    val state: StateFlow<RestoreState> = _state.asStateFlow()

    private var job: Job? = null
    private var userId: String? = null

    init {
        viewModelScope.launch {
            auth.currentUser.collectLatest { user ->
                job?.cancel()
                userId = user?.uid
                // Signing out resets it, so the next sign-in is checked again.
                if (user == null) _state.value = RestoreState.Checking else check(user.uid)
            }
        }
    }

    fun retry() {
        val uid = userId ?: return
        start { restore(uid) }
    }

    /** "Continuar sin esperar": the download goes on in the background, and the books appear as they arrive. */
    fun continueWithout() {
        job?.cancel()
        val uid = userId
        viewModelScope.launch {
            if (uid != null) restorer.clear(uid)
            _state.value = RestoreState.Done
        }
    }

    private fun check(userId: String) = start {
        if (restorer.isPending(userId)) restore(userId) else _state.value = RestoreState.Done
    }

    private fun start(block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch { block() }
    }

    private suspend fun restore(userId: String) {
        _state.value = RestoreState.Restoring
        restorer.restore(userId).fold(
            onSuccess = { _state.value = RestoreState.Done },
            onFailure = { _state.value = RestoreState.Failed(it as? SyncError ?: SyncError.Unknown("The restore failed", it)) },
        )
    }
}
