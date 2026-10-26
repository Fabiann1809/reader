package io.github.fabiann1809.reader.ui.settings

import io.github.fabiann1809.reader.ai.AiError
import io.github.fabiann1809.reader.ai.AiProviderKind
import io.github.fabiann1809.reader.testing.FakeAiConfigStore
import io.github.fabiann1809.reader.testing.FakeAiProvider
import io.github.fabiann1809.reader.testing.FakeApiKeyStore
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import io.github.fabiann1809.reader.testing.testExplanation
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
    private val aiProvider = FakeAiProvider()
    private val configStore = FakeAiConfigStore()

    // Lazy so it is built after MainDispatcherRule swaps Dispatchers.Main (viewModelScope needs it).
    private val viewModel by lazy { SettingsViewModel(keyStore, aiProvider, configStore) }

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

    @Test
    fun cannotTestWithoutStoredKey() = runTest {
        collectUiState()

        assertFalse(viewModel.uiState.value.canTestKey)
    }

    @Test
    fun successfulTestShowsSampleResponse() = runTest {
        keyStore.saveApiKey("AIza-existing")
        aiProvider.result = Result.success(testExplanation("Las plantas fabrican su alimento con luz."))
        collectUiState()

        viewModel.testKey()

        assertEquals(
            KeyTestState.Success("Las plantas fabrican su alimento con luz."),
            viewModel.uiState.value.keyTest,
        )
        assertEquals(1, aiProvider.requests.size)
    }

    @Test
    fun failedTestShowsFailure() = runTest {
        keyStore.saveApiKey("AIza-bad")
        val error = AiError.InvalidApiKey()
        aiProvider.result = Result.failure(error)
        collectUiState()

        viewModel.testKey()

        assertEquals(KeyTestState.Failure(error), viewModel.uiState.value.keyTest)
    }

    @Test
    fun choosingAProviderSavesItAndDropsWhatWasTypedForTheOther() = runTest {
        collectUiState()
        viewModel.onKeyInputChange("clave-de-gemini")

        viewModel.selectProvider(AiProviderKind.OPENAI)

        assertEquals(AiProviderKind.OPENAI, viewModel.uiState.value.config.provider)
        assertEquals("", viewModel.uiState.value.keyInput)
    }

    @Test
    fun theModelIsSavedForTheProviderInUseOnly() = runTest {
        collectUiState()
        viewModel.selectProvider(AiProviderKind.ANTHROPIC)

        viewModel.setModel("  claude-sonnet-5-5 ")

        val config = viewModel.uiState.value.config
        assertEquals("claude-sonnet-5-5", config.model)
        assertEquals("gemini-flash-latest", config.modelOf(AiProviderKind.GEMINI))
    }

    @Test
    fun theCompatibleAddressIsSavedWithoutSpaces() = runTest {
        collectUiState()
        viewModel.selectProvider(AiProviderKind.COMPATIBLE)

        viewModel.setCompatibleUrl(" https://api.groq.com/openai/v1 ")

        assertEquals("https://api.groq.com/openai/v1", viewModel.uiState.value.config.compatibleUrl)
    }

    @Test
    fun eachProviderShowsItsOwnKeyStatus() = runTest {
        // Same wiring as the app: the key in use follows the chosen provider.
        val keys = io.github.fabiann1809.reader.testing.FakeProviderKeyStore()
        val active = io.github.fabiann1809.reader.data.apikey.ActiveApiKeyStore(keys, configStore)
        val model = SettingsViewModel(active, aiProvider, configStore)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.collect() }

        model.onKeyInputChange("clave-de-gemini")
        model.saveKey()
        assertTrue(model.uiState.value.hasApiKey)

        model.selectProvider(AiProviderKind.OPENAI)
        assertFalse(model.uiState.value.hasApiKey)

        model.selectProvider(AiProviderKind.GEMINI)
        assertTrue(model.uiState.value.hasApiKey)
    }
}
