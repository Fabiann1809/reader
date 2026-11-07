package io.github.fabiann1809.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.auth.AuthRepository
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * What the app root needs to pick the first screen. Without a session the order is the onboarding
 * (only the very first time), the welcome and the sign-in (T22.4, T22.8); with one, the library.
 */
class AppStartViewModel(preferences: AppPreferences, auth: AuthRepository) : ViewModel() {

    /** False shows the way in. The saved session is known right away, so there is no flicker. */
    val isSignedIn: StateFlow<Boolean> = auth.currentUser.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = auth.currentUser.value != null)

    /** Null while the stored flag is being read; becomes true as soon as the onboarding is finished or skipped. */
    val onboardingSeen: StateFlow<Boolean?> = preferences.hasSeenOnboarding.map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)
}
