package io.github.fabiann1809.reader.ui.capture

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun CaptureScreen(onNavigateUp: () -> Unit) {
    val permission = rememberCameraPermissionState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            ReaderTopAppBar(title = stringResource(R.string.capture_title), onNavigateUp = onNavigateUp)
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        if (permission.status == CameraPermissionStatus.GRANTED) {
            // Placeholder: the CameraX preview is added in T5.2.
            Box(contentModifier, contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.capture_camera_ready))
            }
        } else {
            CameraPermissionRequest(
                status = permission.status,
                onRequestPermission = permission.request,
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_photo_camera),
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
        )
    }
}
