package io.github.fabiann1809.reader.ui.onboarding

import io.github.fabiann1809.reader.testing.FakeAppPreferences
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun finishingStoresTheFlagAndReportsIt() = runTest {
        val preferences = FakeAppPreferences()
        val viewModel = OnboardingViewModel(preferences)
        assertFalse(viewModel.isFinished.value)

        viewModel.finish()

        assertTrue(viewModel.isFinished.value)
        assertTrue(preferences.hasSeenOnboarding.first())
    }
}
