package io.github.fabiann1809.reader.ui.capture

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(onNavigateUp: () -> Unit, onImageReady: (Uri) -> Unit) {
    val permission = rememberCameraPermissionState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Survives rotation so the photo isn't lost.
    var capturedUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // System photo picker: needs no storage permission and only exposes the chosen image.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) capturedUri = uri
    }
    val pickFromGallery = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        topBar = {
            ReaderTopAppBar(title = stringResource(R.string.capture_title), onNavigateUp = onNavigateUp)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        val uri = capturedUri
        if (uri != null) {
            CapturedImagePreview(
                imageUri = uri,
                onRetake = { capturedUri = null },
                onUse = {
                    // Coming back here (e.g. "take another photo") should show the live camera, not this photo.
                    capturedUri = null
                    onImageReady(uri)
                },
                modifier = contentModifier,
            )
        } else if (permission.status == CameraPermissionStatus.GRANTED) {
            CameraCapture(
                onImageCaptured = { capturedUri = it },
                onError = {
                    scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.capture_error)) }
                },
                onPickFromGallery = pickFromGallery,
                modifier = contentModifier,
            )
        } else {
            CameraPermissionRequest(
                status = permission.status,
                onRequestPermission = permission.request,
                onPickFromGallery = pickFromGallery,
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                },
                modifier = contentModifier,
            )
        }
    }
}

@Composable
fun CameraPermissionRequest(
    status: CameraPermissionStatus,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (status == CameraPermissionStatus.PERMANENTLY_DENIED) {
                    R.string.camera_permission_denied_message
                } else {
                    R.string.camera_permission_message
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (status == CameraPermissionStatus.PERMANENTLY_DENIED) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.camera_permission_open_settings))
            }
        } else {
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.camera_permission_allow))
            }
        }
        // The gallery works without the camera permission.
        OutlinedButton(onClick = onPickFromGallery, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.capture_pick_gallery))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraPermissionRequestPreview() {
    ReaderTheme {
        CameraPermissionRequest(
            status = CameraPermissionStatus.NOT_REQUESTED,
            onRequestPermission = {},
            onOpenSettings = {},
            onPickFromGallery = {},
        )
    }
}
