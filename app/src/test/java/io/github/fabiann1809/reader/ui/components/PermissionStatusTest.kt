package io.github.fabiann1809.reader.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionStatusTest {

    @Test
    fun grantedWinsOverEverythingElse() {
        assertEquals(
            PermissionStatus.GRANTED,
            permissionStatus(isGranted = true, hasRequested = true, shouldShowRationale = false),
        )
    }

    @Test
    fun notRequestedBeforeFirstAsk() {
        assertEquals(
            PermissionStatus.NOT_REQUESTED,
            permissionStatus(isGranted = false, hasRequested = false, shouldShowRationale = false),
        )
    }

    @Test
    fun deniedWhenRationaleCanStillBeShown() {
        assertEquals(
            PermissionStatus.DENIED,
            permissionStatus(isGranted = false, hasRequested = true, shouldShowRationale = true),
        )
    }

    @Test
    fun permanentlyDeniedWhenAskedAndNoRationale() {
        assertEquals(
            PermissionStatus.PERMANENTLY_DENIED,
            permissionStatus(isGranted = false, hasRequested = true, shouldShowRationale = false),
        )
    }
}
