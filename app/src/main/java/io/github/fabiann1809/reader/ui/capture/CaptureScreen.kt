package io.github.fabiann1809.reader.ui.capture

import android.Manifest
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.LightStatusBarIcons
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PermissionStatus
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.components.ReaderTopAppBar
import io.github.fabiann1809.reader.ui.components.rememberPermissionState
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.util.cropImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CaptureScreen(onNavigateUp: () -> Unit, onImageReady: (Uri) -> Unit) {
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val resources = LocalResources.current
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

    val uri = capturedUri
    val showCamera = uri == null && permission.status == PermissionStatus.GRANTED
    if (showCamera) LightStatusBarIcons()

    Scaffold(
        topBar = {
            if (!showCamera) {
                ReaderTopAppBar(title = stringResource(R.string.capture_title), onNavigateUp = onNavigateUp)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = if (showCamera) Color.Black else MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        if (uri != null) {
            CapturedImagePreview(
                imageUri = uri,
                onRetake = { capturedUri = null },
                onUse = { area ->
                    scope.launch {
                        val image = if (area.isWholeImage) uri else withContext(Dispatchers.IO) { cropImage(context, uri, area) }
                        if (image == null) {
                            snackbarHostState.showSnackbar(resources.getString(R.string.capture_crop_error))
                        } else {
                            // Coming back here (e.g. "take another photo") should show the live camera, not this photo.
                            capturedUri = null
                            onImageReady(image)
                        }
                    }
                },
                modifier = contentModifier,
            )
        } else if (showCamera) {
            // Edge to edge: the camera handles the system bar insets itself.
            CameraCapture(
                onImageCaptured = { capturedUri = it },
                onError = {
                    scope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.capture_error)) }
                },
                onPickFromGallery = pickFromGallery,
                onClose = onNavigateUp,
                modifier = Modifier.fillMaxSize(),
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
    status: PermissionStatus,
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
                if (status == PermissionStatus.PERMANENTLY_DENIED) {
                    R.string.camera_permission_denied_message
                } else {
                    R.string.camera_permission_message
                },
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (status == PermissionStatus.PERMANENTLY_DENIED) {
            PrimaryButton(
                text = stringResource(R.string.camera_permission_open_settings),
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.camera_permission_allow),
                onClick = onRequestPermission,
                icon = R.drawable.ic_camera,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        // The gallery works without the camera permission.
        OutlineButton(
            text = stringResource(R.string.capture_pick_gallery),
            onClick = onPickFromGallery,
            icon = R.drawable.ic_images,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraPermissionRequestPreview() {
    ReaderTheme {
        CameraPermissionRequest(
            status = PermissionStatus.NOT_REQUESTED,
            onRequestPermission = {},
            onOpenSettings = {},
            onPickFromGallery = {},
        )
    }
}
