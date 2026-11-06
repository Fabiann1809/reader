package io.github.fabiann1809.reader.ui

import io.github.fabiann1809.reader.data.auth.AuthUser
import io.github.fabiann1809.reader.testing.FakeAppPreferences
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import io.github.fabiann1809.reader.ui.navigation.LibraryRoute
import io.github.fabiann1809.reader.ui.navigation.OnboardingRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppStartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun firstLaunchStartsWithTheOnboarding() {
        assertEquals(OnboardingRoute, AppStartViewModel(FakeAppPreferences(seenOnboarding = false), FakeAuthRepository()).startDestination.value)
    }

    @Test
    fun laterLaunchesStartInTheLibrary() {
        assertEquals(LibraryRoute, AppStartViewModel(FakeAppPreferences(seenOnboarding = true), FakeAuthRepository()).startDestination.value)
    }

    @Test
    fun withoutASessionTheSignInIsShown() {
        assertFalse(AppStartViewModel(FakeAppPreferences(), FakeAuthRepository()).isSignedIn.value)
    }

    @Test
    fun aSavedSessionSkipsTheSignIn() {
        val auth = FakeAuthRepository(signedIn = AuthUser("uid", "ana@example.com"))

        assertTrue(AppStartViewModel(FakeAppPreferences(), auth).isSignedIn.value)
    }

    @Test
    fun signingOutShowsTheSignInAgain() {
        val auth = FakeAuthRepository(signedIn = AuthUser("uid", "ana@example.com"))
        val viewModel = AppStartViewModel(FakeAppPreferences(), auth)

        auth.signOut()

        assertFalse(viewModel.isSignedIn.value)
    }
}
