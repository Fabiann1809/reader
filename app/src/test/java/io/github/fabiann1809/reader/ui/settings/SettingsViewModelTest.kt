package io.github.fabiann1809.reader.ui.settings

import io.github.fabiann1809.reader.testing.FakeApiKeyStore
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val keyStore = FakeApiKeyStore()

    // Lazy so it is built after MainDispatcherRule swaps Dispatchers.Main (viewModelScope needs it).
    private val viewModel by lazy { SettingsViewModel(keyStore) }

    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    @Test
    fun cannotSaveBlankKey() = runTest {
        collectUiState()

        viewModel.onKeyInputChange("   ")

        assertFalse(viewModel.uiState.value.canSaveKey)
    }

    @Test
    fun saveStoresKeyClearsInputAndShowsMessage() = runTest {
        collectUiState()
        viewModel.onKeyInputChange("AIza-test-key")

        viewModel.saveKey()

        val state = viewModel.uiState.value
        assertEquals("AIza-test-key", keyStore.currentKey)
        assertTrue(state.hasApiKey)
        assertEquals("", state.keyInput)
        assertEquals(SettingsMessage.KEY_SAVED, state.message)
    }

    @Test
    fun clearRemovesKey() = runTest {
        keyStore.saveApiKey("AIza-existing")
        collectUiState()
        assertTrue(viewModel.uiState.value.hasApiKey)

        viewModel.clearKey()

        assertNull(keyStore.currentKey)
        assertFalse(viewModel.uiState.value.hasApiKey)
        assertEquals(SettingsMessage.KEY_CLEARED, viewModel.uiState.value.message)
    }

    @Test
    fun messageIsConsumedOnce() = runTest {
        collectUiState()
        viewModel.onKeyInputChange("AIza-test-key")
        viewModel.saveKey()

        viewModel.onMessageShown()

        assertNull(viewModel.uiState.value.message)
    }
}
