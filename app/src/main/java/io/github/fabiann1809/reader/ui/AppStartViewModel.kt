package io.github.fabiann1809.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.auth.AuthRepository
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import io.github.fabiann1809.reader.ui.navigation.LibraryRoute
import io.github.fabiann1809.reader.ui.navigation.OnboardingRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Decides the first screen: the sign-in when nobody is signed in (T22.4), otherwise onboarding on
 * the very first launch and the library afterwards.
 * Read once per launch so finishing the onboarding doesn't rebuild the navigation graph.
 */
class AppStartViewModel(preferences: AppPreferences, auth: AuthRepository) : ViewModel() {

    /** False shows the sign-in screen. The saved session is known right away, so there is no flicker. */
    val isSignedIn: StateFlow<Boolean> = auth.currentUser.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = auth.currentUser.value != null)

    /** Null while the stored flag is being read. */
    val startDestination: StateFlow<Any?> = flow {
        emit(if (preferences.hasSeenOnboarding.first()) LibraryRoute else OnboardingRoute)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)
}
