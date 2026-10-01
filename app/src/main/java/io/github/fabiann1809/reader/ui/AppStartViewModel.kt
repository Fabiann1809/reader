package io.github.fabiann1809.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import io.github.fabiann1809.reader.ui.navigation.LibraryRoute
import io.github.fabiann1809.reader.ui.navigation.OnboardingRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Decides the first screen: onboarding on the very first launch, the library afterwards.
 * Read once per launch so finishing the onboarding doesn't rebuild the navigation graph.
 */
class AppStartViewModel(preferences: AppPreferences) : ViewModel() {

    /** Null while the stored flag is being read. */
    val startDestination: StateFlow<Any?> = flow {
        emit(if (preferences.hasSeenOnboarding.first()) LibraryRoute else OnboardingRoute)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)
}
