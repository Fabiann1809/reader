package io.github.fabiann1809.reader.ui

import io.github.fabiann1809.reader.testing.FakeAppPreferences
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import io.github.fabiann1809.reader.ui.navigation.LibraryRoute
import io.github.fabiann1809.reader.ui.navigation.OnboardingRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppStartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun firstLaunchStartsWithTheOnboarding() {
        assertEquals(OnboardingRoute, AppStartViewModel(FakeAppPreferences(seenOnboarding = false)).startDestination.value)
    }

    @Test
    fun laterLaunchesStartInTheLibrary() {
        assertEquals(LibraryRoute, AppStartViewModel(FakeAppPreferences(seenOnboarding = true)).startDestination.value)
    }
}
