package io.github.fabiann1809.reader.ui.components

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

enum class PermissionStatus {
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
fun permissionStatus(
    isGranted: Boolean,
    hasRequested: Boolean,
    shouldShowRationale: Boolean,
): PermissionStatus = when {
    isGranted -> PermissionStatus.GRANTED
    !hasRequested -> PermissionStatus.NOT_REQUESTED
    shouldShowRationale -> PermissionStatus.DENIED
    else -> PermissionStatus.PERMANENTLY_DENIED
}

class PermissionState(
    val status: PermissionStatus,
    val request: () -> Unit,
)

/** The state of a runtime [permission] (the camera, the microphone...) and how to ask for it. */
@Composable
fun rememberPermissionState(permission: String): PermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var isGranted by remember { mutableStateOf(context.hasPermission(permission)) }
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    // Kept in state (not read during composition) so a second denial, which changes only
    // this value, still triggers a recomposition.
    var shouldShowRationale by remember { mutableStateOf(activity?.shouldShowRequestPermissionRationale(permission) ?: false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasRequested = true
        isGranted = granted
        shouldShowRationale = activity?.shouldShowRequestPermissionRationale(permission) ?: false
    }

    // The user may grant the permission from system settings and come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isGranted = context.hasPermission(permission)
        shouldShowRationale = activity?.shouldShowRequestPermissionRationale(permission) ?: false
    }

    val status = permissionStatus(
        isGranted = isGranted,
        hasRequested = hasRequested,
        shouldShowRationale = shouldShowRationale,
    )
    return PermissionState(status = status, request = { launcher.launch(permission) })
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
