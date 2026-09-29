package io.github.fabiann1809.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.data.apikey.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-off feedback shown to the user (e.g. in a snackbar). */
enum class SettingsMessage { KEY_SAVED, KEY_CLEARED }

data class SettingsUiState(
    val hasApiKey: Boolean = false,
    // What the user is typing. The stored key is never loaded back into the UI.
    val keyInput: String = "",
    val message: SettingsMessage? = null,
) {
    val canSaveKey: Boolean get() = keyInput.isNotBlank()
}

class SettingsViewModel(private val apiKeyStore: ApiKeyStore) : ViewModel() {

    private val formState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> =
        combine(formState, apiKeyStore.hasApiKey) { form, hasKey -> form.copy(hasApiKey = hasKey) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = SettingsUiState(),
            )

    fun onKeyInputChange(input: String) = formState.update { it.copy(keyInput = input) }

    fun saveKey() {
        val input = formState.value.keyInput
        if (input.isBlank()) return
        viewModelScope.launch {
            apiKeyStore.saveApiKey(input)
            formState.update { it.copy(keyInput = "", message = SettingsMessage.KEY_SAVED) }
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            apiKeyStore.clearApiKey()
            formState.update { it.copy(message = SettingsMessage.KEY_CLEARED) }
        }
    }

    fun onMessageShown() = formState.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
