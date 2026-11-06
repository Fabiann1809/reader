package io.github.fabiann1809.reader.ui.settings

import io.github.fabiann1809.reader.data.auth.AuthUser
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun showsTheEmailOfThePersonSignedIn() {
        val viewModel = AccountViewModel(FakeAuthRepository(AuthUser("uid", "ana@example.com")))

        assertEquals("ana@example.com", viewModel.email.value)
    }

    @Test
    fun signingOutEndsTheSession() = runTest(mainDispatcherRule.testDispatcher) {
        val auth = FakeAuthRepository(AuthUser("uid", "ana@example.com"))
        val viewModel = AccountViewModel(auth)
        viewModel.email.launchIn(backgroundScope)

        viewModel.signOut()

        assertNull(auth.currentUser.value)
        assertNull(viewModel.email.value)
    }
}
