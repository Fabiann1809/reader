package io.github.fabiann1809.reader.ui.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

enum class CameraPermissionStatus {
    GRANTED,

    /** Never asked yet: show the explanation and ask. */
    NOT_REQUESTED,

    /** Denied once: explain again and allow asking again. */
    DENIED,

    /** Denied with "don't ask again": the only way back is the system settings. */
    PERMANENTLY_DENIED,
}

/**
 * Pure decision logic (unit-tested). Android only reports "permanently denied" indirectly:
 * after a request, a denied permission that no longer needs a rationale can't be asked again.
 */
fun cameraPermissionStatus(
    isGranted: Boolean,
    hasRequested: Boolean,
    shouldShowRationale: Boolean,
): CameraPermissionStatus = when {
    isGranted -> CameraPermissionStatus.GRANTED
    !hasRequested -> CameraPermissionStatus.NOT_REQUESTED
    shouldShowRationale -> CameraPermissionStatus.DENIED
    else -> CameraPermissionStatus.PERMANENTLY_DENIED
}

class CameraPermissionState(
    val status: CameraPermissionStatus,
    val request: () -> Unit,
)

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var isGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    // Kept in state (not read during composition) so a second denial, which changes only
    // this value, still triggers a recomposition.
    var shouldShowRationale by remember { mutableStateOf(activity?.shouldShowCameraRationale() ?: false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasRequested = true
        isGranted = granted
        shouldShowRationale = activity?.shouldShowCameraRationale() ?: false
    }

    // The user may grant the permission from system settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isGranted = context.hasCameraPermission()
        shouldShowRationale = activity?.shouldShowCameraRationale() ?: false
    }

    val status = cameraPermissionStatus(
        isGranted = isGranted,
        hasRequested = hasRequested,
        shouldShowRationale = shouldShowRationale,
    )
    return CameraPermissionState(status = status, request = { launcher.launch(Manifest.permission.CAMERA) })
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Activity.shouldShowCameraRationale(): Boolean =
    shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
