package io.github.fabiann1809.reader.data.auth

import io.github.fabiann1809.reader.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class FakeAuthRepositoryTest {

    private val auth = FakeAuthRepository()

    @Test
    fun signingUpSignsTheNewAccountIn() = runTest {
        auth.signUp("ana@example.com", "secret1")

        assertEquals("ana@example.com", auth.currentUser.value?.email)
    }

    @Test
    fun signingOutClearsTheUser() = runTest {
        auth.signUp("ana@example.com", "secret1")
        auth.signOut()

        assertNull(auth.currentUser.value)
    }

    @Test
    fun aWrongPasswordIsRejected() = runTest {
        auth.signUp("ana@example.com", "secret1")
        auth.signOut()

        assertThrows(AuthError.WrongCredentials::class.java) {
            kotlinx.coroutines.runBlocking { auth.signIn("ana@example.com", "other") }
        }
        assertNull(auth.currentUser.value)
    }

    @Test
    fun anExistingEmailCannotSignUpAgain() = runTest {
        auth.signUp("ana@example.com", "secret1")

        assertThrows(AuthError.EmailAlreadyInUse::class.java) {
            kotlinx.coroutines.runBlocking { auth.signUp("ana@example.com", "secret2") }
        }
    }
}
