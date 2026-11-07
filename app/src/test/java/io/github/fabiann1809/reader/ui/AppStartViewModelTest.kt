package io.github.fabiann1809.reader.ui

import io.github.fabiann1809.reader.data.auth.AuthUser
import io.github.fabiann1809.reader.testing.FakeAppPreferences
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AppStartViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun firstLaunchHasNotSeenTheOnboarding() {
        assertEquals(false, AppStartViewModel(FakeAppPreferences(seenOnboarding = false), FakeAuthRepository()).onboardingSeen.value)
    }

    @Test
    fun laterLaunchesHaveSeenIt() {
        assertEquals(true, AppStartViewModel(FakeAppPreferences(seenOnboarding = true), FakeAuthRepository()).onboardingSeen.value)
    }

    @Test
    fun finishingTheOnboardingIsReflectedRightAway() = runTest {
        val preferences = FakeAppPreferences(seenOnboarding = false)
        val viewModel = AppStartViewModel(preferences, FakeAuthRepository())

        preferences.markOnboardingSeen()

        assertEquals(true, viewModel.onboardingSeen.value)
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
