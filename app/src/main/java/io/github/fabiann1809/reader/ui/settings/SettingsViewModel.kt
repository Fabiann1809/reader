package io.github.fabiann1809.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.AiProvider
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

/** Result of the "test key" button. */
sealed interface KeyTestState {
    data object Idle : KeyTestState

    data object Testing : KeyTestState

    data class Success(val sampleResponse: String) : KeyTestState

    data class Failure(val error: Throwable) : KeyTestState
}

data class SettingsUiState(
    val hasApiKey: Boolean = false,
    // What the user is typing. The stored key is never loaded back into the UI.
    val keyInput: String = "",
    val message: SettingsMessage? = null,
    val keyTest: KeyTestState = KeyTestState.Idle,
) {
    val canSaveKey: Boolean get() = keyInput.isNotBlank()

    val canTestKey: Boolean get() = hasApiKey && keyTest != KeyTestState.Testing
}

class SettingsViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val aiProvider: AiProvider,
) : ViewModel() {

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
            formState.update {
                it.copy(keyInput = "", message = SettingsMessage.KEY_SAVED, keyTest = KeyTestState.Idle)
            }
        }
    }

    fun clearKey() {
        viewModelScope.launch {
            apiKeyStore.clearApiKey()
            formState.update { it.copy(message = SettingsMessage.KEY_CLEARED, keyTest = KeyTestState.Idle) }
        }
    }

    /** Sends a tiny real request with the stored key to confirm it works. */
    fun testKey() {
        formState.update { it.copy(keyTest = KeyTestState.Testing) }
        viewModelScope.launch {
            val result = aiProvider.explain(TEST_TEXT)
            val keyTest = result.fold(
                onSuccess = { KeyTestState.Success(it.take(MAX_SAMPLE_LENGTH)) },
                onFailure = { KeyTestState.Failure(it) },
            )
            formState.update { it.copy(keyTest = keyTest) }
        }
    }

    fun onMessageShown() = formState.update { it.copy(message = null) }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_SAMPLE_LENGTH = 300

        // Short text so the test consumes very little of the user's quota.
        const val TEST_TEXT = "La fotosíntesis convierte la luz del sol en energía química."
    }
}
