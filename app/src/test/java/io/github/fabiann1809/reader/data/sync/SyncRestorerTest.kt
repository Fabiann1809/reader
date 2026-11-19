package io.github.fabiann1809.reader.data.sync

import io.github.fabiann1809.reader.data.sync.auth.Consent
import io.github.fabiann1809.reader.data.sync.auth.DriveGrant
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import io.github.fabiann1809.reader.testing.FakeCloudStorage
import io.github.fabiann1809.reader.testing.FakeDriveAuthorizer
import io.github.fabiann1809.reader.testing.FakeLocalChangeSource
import io.github.fabiann1809.reader.testing.FakeSyncControl
import io.github.fabiann1809.reader.testing.FakeSyncSettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRestorerTest {

    private val settings = FakeSyncSettingsStore(SyncSettings(restoreFor = setOf("ana")))
    private val authorizer = FakeDriveAuthorizer()
    private val cloud = FakeCloudStorage()
    private val sync = FakeSyncControl().apply { statusAfterEnable = SyncStatus.Synced(5_000) }
    private val changes = FakeLocalChangeSource()

    private fun restorer() = SyncRestorer(settings, authorizer, cloud, sync, changes)

    @Test
    fun theAccountThatSignedOutWithAnEmptyPhoneHasALibraryWaiting() = runTest {
        assertTrue(restorer().isPending("ana"))
    }

    @Test
    fun anotherAccountRestoresNothing() = runTest {
        assertFalse(restorer().isPending("beto"))
        // Ana's library keeps waiting for her.
        assertEquals(setOf("ana"), settings.settings.value.restoreFor)
    }

    @Test
    fun severalAccountsCanWaitAtOnceAndOneRestoringDoesNotForgetTheOthers() = runTest {
        settings.settings.value = SyncSettings(restoreFor = setOf("ana", "beto"))

        assertTrue(restorer().isPending("beto"))
        restorer().restore("ana").getOrThrow()

        assertEquals(setOf("beto"), settings.settings.value.restoreFor)
        assertTrue(restorer().isPending("beto"))
    }

    @Test
    fun nobodyIsWaitingWhenNoAccountSignedOutWithTheSyncOn() = runTest {
        settings.settings.value = SyncSettings(restoreFor = emptySet())

        assertFalse(restorer().isPending("ana"))
    }

    @Test
    fun aPhoneThatAlreadyHoldsDataIsNeverMixedWithTheCloud() = runTest {
        changes.lastChange = 1_234

        assertFalse(restorer().isPending("ana"))
        // And it stops waiting, so it doesn't ask again.
        assertTrue(settings.settings.value.restoreFor.isEmpty())
    }

    @Test
    fun restoringTurnsTheSyncOnWaitsForItsFirstRunAndStopsWaiting() = runTest {
        val result = restorer().restore("ana")

        assertTrue(result.isSuccess)
        assertEquals(listOf("enabled:true"), sync.calls)
        assertTrue(settings.settings.value.restoreFor.isEmpty())
    }

    @Test
    fun aFailedFirstSyncFailsTheRestoreAndKeepsWaiting() = runTest {
        sync.statusAfterEnable = SyncStatus.Failed(SyncError.NoInternet(java.io.IOException()))

        val result = restorer().restore("ana")

        assertTrue(result.exceptionOrNull() is SyncError.NoInternet)
        assertEquals(setOf("ana"), settings.settings.value.restoreFor)
    }

    @Test
    fun withoutThePermissionItAsksNothingAndFails() = runTest {
        authorizer.onAuthorize = DriveGrant.NeedsConsent(Consent { error("Not opened") })

        val result = restorer().restore("ana")

        assertTrue(result.exceptionOrNull() is SyncError.NotAuthorized)
        assertTrue(sync.calls.isEmpty())
    }

    @Test
    fun aDriveThatRejectsTheTokenFailsBeforeTurningTheSyncOn() = runTest {
        cloud.failWith = SyncError.NotAuthorized()

        val result = restorer().restore("ana")

        assertTrue(result.exceptionOrNull() is SyncError.NotAuthorized)
        assertTrue(sync.calls.isEmpty())
    }

    @Test
    fun googleFailingFailsTheRestore() = runTest {
        authorizer.onAuthorize = DriveGrant.Failed()

        assertTrue(restorer().restore("ana").isFailure)
    }
}
