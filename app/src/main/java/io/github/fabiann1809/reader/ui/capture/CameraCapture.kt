package io.github.fabiann1809.reader.ui.capture

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme
import io.github.fabiann1809.reader.util.newCaptureFile

/**
 * Full-screen camera (design 10.6): close and flash on top, a page frame as a guide,
 * and the gallery and shutter buttons in the thumb zone. Requires the camera permission.
 */
@Composable
fun CameraCapture(
    onImageCaptured: (Uri) -> Unit,
    onError: () -> Unit,
    onPickFromGallery: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isCapturing by remember { mutableStateOf(false) }
    var flashAuto by rememberSaveable { mutableStateOf(true) }

    // The controller binds the camera to the screen's lifecycle: it opens on start and closes on stop.
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        }
    }
    DisposableEffect(lifecycleOwner) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose { controller.unbind() }
    }
    controller.imageCaptureFlashMode = if (flashAuto) ImageCapture.FLASH_MODE_AUTO else ImageCapture.FLASH_MODE_OFF

    val takePicture = {
        if (!isCapturing) {
            isCapturing = true
            val file = newCaptureFile(context)
            controller.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        onImageCaptured(output.savedUri ?: Uri.fromFile(file))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        onError()
                    }
                },
            )
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    this.controller = controller
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundOverlayButton(R.drawable.ic_x, stringResource(R.string.capture_close), onClose)
            RoundOverlayButton(
                icon = if (flashAuto) R.drawable.ic_lightning else R.drawable.ic_lightning_slash,
                description = stringResource(if (flashAuto) R.string.capture_flash_auto else R.string.capture_flash_off),
                onClick = { flashAuto = !flashAuto },
                role = Role.Switch,
            )
        }
        PageFrame(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 96.dp)
                .fillMaxWidth(FRAME_WIDTH_FRACTION)
                .aspectRatio(PAGE_ASPECT_RATIO),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.capture_frame_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GalleryButton(onClick = onPickFromGallery)
                ShutterButton(isCapturing = isCapturing, onClick = takePicture)
                // Keeps the shutter centered.
                Spacer(Modifier.width(SIDE_BUTTON_SIZE))
            }
        }
    }
}

/** Round dark button over the preview; the flash one tells its state to screen readers. */
@Composable
private fun RoundOverlayButton(icon: Int, description: String, onClick: () -> Unit, role: Role = Role.Button) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(role = role, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

/** Page-shaped guide: the preview is dimmed around it, and its amber corners pulse while a line scans it. */
@Composable
private fun PageFrame(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "frame")
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "cornerPulse",
    )
    val scan by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(tween(SCAN_MILLIS), RepeatMode.Reverse),
        label = "scan",
    )
    val corner = Color(0xFFF5B963).copy(alpha = if (reduceMotion) 1f else pulse)
    Box(
        modifier = modifier.drawBehind {
            val frame = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(18.dp.toPx()))) }
            clipPath(frame, ClipOp.Difference) {
                drawRect(Color.Black.copy(alpha = 0.35f), topLeft = Offset(-SCRIM_REACH, -SCRIM_REACH), size = Size(size.width + 2 * SCRIM_REACH, size.height + 2 * SCRIM_REACH))
            }
            if (!reduceMotion) {
                val y = size.height * scan
                drawLine(Color(0xFFF5B963), Offset(8.dp.toPx(), y), Offset(size.width - 8.dp.toPx(), y), 2.dp.toPx())
            }
            val stroke = 4.dp.toPx()
            val arm = 30.dp.toPx()
            val w = size.width
            val h = size.height
            // Each corner is two short lines meeting at the frame's corner.
            listOf(
                Triple(Offset(0f, 0f), 1f, 1f),
                Triple(Offset(w, 0f), -1f, 1f),
                Triple(Offset(0f, h), 1f, -1f),
                Triple(Offset(w, h), -1f, -1f),
            ).forEach { (point, dx, dy) ->
                drawLine(corner, point, point + Offset(arm * dx, 0f), stroke, StrokeCap.Round)
                drawLine(corner, point, point + Offset(0f, arm * dy), stroke, StrokeCap.Round)
            }
        },
    )
}

@Composable
private fun GalleryButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(SIDE_BUTTON_SIZE)
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_images),
            contentDescription = stringResource(R.string.capture_pick_gallery),
            tint = Color.White,
        )
    }
}

/** White shutter with a translucent outer ring; shrinks a little when pressed. */
@Composable
private fun ShutterButton(isCapturing: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val description = stringResource(R.string.capture_take_photo)
    Box(
        modifier = Modifier
            .size(82.dp)
            .scale(if (pressed) 0.88f else 1f)
            .border(4.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            .padding(7.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isCapturing) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color.Black)
        }
    }
}

private val SIDE_BUTTON_SIZE = 52.dp
private const val FRAME_WIDTH_FRACTION = 0.82f
private const val PULSE_MILLIS = 1_000
private const val SCAN_MILLIS = 2_200
private const val SCRIM_REACH = 4_000f

// Typical book page proportions (width / height).
private const val PAGE_ASPECT_RATIO = 0.7f
