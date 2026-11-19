package io.github.fabiann1809.reader.data.sync.cloud

import io.github.fabiann1809.reader.data.auth.AuthUser
import io.github.fabiann1809.reader.testing.FakeAuthRepository
import io.github.fabiann1809.reader.testing.FakeCloudStorage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountCloudStorageTest {

    private val auth = FakeAuthRepository()

    // One fake space per prefix, like the real Drive keeps one set of files per prefix.
    private val spaces = mutableMapOf<String, FakeCloudStorage>()
    private val storage = AccountCloudStorage(auth) { prefix -> spaces.getOrPut(prefix) { FakeCloudStorage() } }

    @Test
    fun theNamespaceOfAnAccountIsStableAndDifferentFromAnothers() {
        assertEquals(CloudNamespace.prefixFor("ana"), CloudNamespace.prefixFor("ana"))
        assertNotEquals(CloudNamespace.prefixFor("ana"), CloudNamespace.prefixFor("beto"))
    }

    @Test
    fun aNamespaceIsAValidDriveNameStartThatNoOldFileNameHas() {
        val prefix = CloudNamespace.prefixFor("GNkk5d4Eo7NQ409ji0kjNeEjOxF2")

        assertTrue(prefix.matches(Regex("u[0-9a-f]{20}-")))
        listOf("manifest.json", "book-", "cover-", "voice-").forEach { assertTrue(!it.startsWith(prefix.take(1))) }
    }

    @Test
    fun twoAccountsOnTheSamePhoneEachSeeOnlyTheirOwnManifest() = runTest {
        auth.signInAs(AuthUser("ana", "ana@example.com"))
        storage.writeManifest("ana's library", null).getOrThrow()
        auth.signOut()

        auth.signInAs(AuthUser("beto", "beto@example.com"))
        assertNull(storage.readManifest().getOrThrow())
        storage.writeManifest("beto's library", null).getOrThrow()
        auth.signOut()

        auth.signInAs(AuthUser("ana", "ana@example.com"))
        assertEquals("ana's library", storage.readManifest().getOrThrow()!!.content)
    }

    @Test
    fun nobodySignedInFailsWithoutTouchingAnything() = runTest {
        val result = storage.writeManifest("x", null)

        assertTrue(result.exceptionOrNull() is SyncError.NotAuthorized)
        assertTrue(spaces.isEmpty())
        assertTrue(storage.listFiles().isFailure)
        assertTrue(storage.adoptLegacyFiles().isFailure)
    }

    @Test
    fun adoptingGoesToTheSignedInAccountOnly() = runTest {
        auth.signInAs(AuthUser("ana", "ana@example.com"))
        storage.adoptLegacyFiles().getOrThrow()
        auth.signOut()
        auth.signInAs(AuthUser("beto", "beto@example.com"))
        storage.adoptLegacyFiles().getOrThrow()

        assertEquals(1, spaces.getValue(CloudNamespace.prefixFor("ana")).adoptCalls)
        assertEquals(1, spaces.getValue(CloudNamespace.prefixFor("beto")).adoptCalls)
    }
}
