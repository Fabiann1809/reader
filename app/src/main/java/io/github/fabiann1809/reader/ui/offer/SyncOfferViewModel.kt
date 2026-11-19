package io.github.fabiann1809.reader.ui.offer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.auth.AuthRepository
import io.github.fabiann1809.reader.data.sync.SyncSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface OfferState {

    /** Finding out whether to offer the sync; only a few milliseconds. */
    data object Checking : OfferState

    /** Ask the person whether to save the library to Drive. */
    data object Offer : OfferState

    /** Nothing to ask, or it was answered: the app opens. */
    data object Done : OfferState
}

/**
 * After signing in, offers to sync with Drive, once per account (T18.9d). It doesn't offer when the sync
 * is already on, nor to an account whose library is waiting to come back (T18.9b: that already turns it on).
 */
class SyncOfferViewModel(auth: AuthRepository, private val settings: SyncSettingsStore) : ViewModel() {

    private val _state = MutableStateFlow<OfferState>(OfferState.Checking)
    val state: StateFlow<OfferState> = _state.asStateFlow()

    private var userId: String? = null

    init {
        viewModelScope.launch {
            auth.currentUser.collectLatest { user ->
                userId = user?.uid
                // Signing out resets it, so the next account is checked on its own.
                _state.value = if (user == null) OfferState.Checking else decide(user.uid)
            }
        }
    }

    /** The person answered, yes or no: it is not asked again for this account. */
    fun answered() {
        val uid = userId ?: return
        viewModelScope.launch {
            settings.update { it.copy(offeredTo = it.offeredTo + uid) }
            _state.value = OfferState.Done
        }
    }

    private suspend fun decide(userId: String): OfferState {
        val saved = settings.settings.first()
        val askable = !saved.enabled && userId !in saved.offeredTo && userId !in saved.restoreFor
        return if (askable) OfferState.Offer else OfferState.Done
    }
}
