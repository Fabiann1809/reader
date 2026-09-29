package io.github.fabiann1809.reader.ui.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPermissionStatusTest {

    @Test
    fun grantedWinsOverEverythingElse() {
        assertEquals(
            CameraPermissionStatus.GRANTED,
            cameraPermissionStatus(isGranted = true, hasRequested = true, shouldShowRationale = false),
        )
    }

    @Test
    fun notRequestedBeforeFirstAsk() {
        assertEquals(
            CameraPermissionStatus.NOT_REQUESTED,
            cameraPermissionStatus(isGranted = false, hasRequested = false, shouldShowRationale = false),
        )
    }

    @Test
    fun deniedWhenRationaleCanStillBeShown() {
        assertEquals(
            CameraPermissionStatus.DENIED,
            cameraPermissionStatus(isGranted = false, hasRequested = true, shouldShowRationale = true),
        )
    }

    @Test
    fun permanentlyDeniedWhenAskedAndNoRationale() {
        assertEquals(
            CameraPermissionStatus.PERMANENTLY_DENIED,
            cameraPermissionStatus(isGranted = false, hasRequested = true, shouldShowRationale = false),
        )
    }
}
