package io.github.fabiann1809.reader.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(private val preferences: AppPreferences) : ViewModel() {

    private val _isFinished = MutableStateFlow(false)

    /** Becomes true once the "seen" flag is stored, so the screen can move on to the library. */
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    /** Called both when the user finishes and when they skip. */
    fun finish() {
        viewModelScope.launch {
            preferences.markOnboardingSeen()
            _isFinished.value = true
        }
    }
}
