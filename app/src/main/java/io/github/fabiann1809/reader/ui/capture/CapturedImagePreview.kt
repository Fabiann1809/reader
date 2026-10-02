package io.github.fabiann1809.reader.ui.capture

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ocr.ImageArea
import io.github.fabiann1809.reader.ui.AppViewModelProvider
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.util.decodeScaledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_MAX_DIMENSION = 1600

/**
 * Shows the photo that was just taken or picked, with a crop rectangle over it (T12.5) and
 * actions below it. [onUse] gets the part of the photo to read.
 */
@Composable
fun CapturedImagePreview(
    imageUri: Uri,
    onRetake: () -> Unit,
    onUse: (ImageArea) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PhotoCropViewModel = viewModel(
        key = imageUri.toString(),
        factory = AppViewModelProvider.photoCrop(imageUri.toString()),
    ),
) {
    val context = LocalContext.current
    val crop by viewModel.uiState.collectAsStateWithLifecycle()
    // null while loading; a failed result if the image can't be read.
    val bitmap by produceState<Result<Bitmap>?>(initialValue = null, imageUri) {
        val decoded = withContext(Dispatchers.IO) { decodeScaledBitmap(context, imageUri, PREVIEW_MAX_DIMENSION) }
        value = decoded?.let { Result.success(it) } ?: Result.failure(IllegalStateException("Unreadable image"))
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            val result = bitmap
            val image = result?.getOrNull()
            when {
                result == null -> CircularProgressIndicator()
                image == null -> Text(
                    text = stringResource(R.string.capture_image_unreadable),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
                // The box takes the photo's exact shape, so the crop's fractions map onto it directly.
                else -> Box(modifier = Modifier.aspectRatio(image.width.toFloat() / image.height)) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = stringResource(R.string.capture_photo_description),
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize(),
                    )
                    PhotoCropper(
                        area = crop.area,
                        paragraphs = crop.paragraphs,
                        pickingParagraph = crop.pickingParagraph,
                        paragraphColor = MaterialTheme.colorScheme.primary,
                        onMoveCorner = viewModel::moveCorner,
                        onMoveArea = viewModel::moveArea,
                        onTapParagraph = viewModel::tapWhilePicking,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (bitmap?.isSuccess == true) {
            CropActions(
                state = crop,
                onPickParagraph = viewModel::startPickingParagraph,
                onCancelPick = viewModel::stopPickingParagraph,
                onWholePhoto = viewModel::reset,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton(
                text = stringResource(R.string.capture_retake),
                onClick = onRetake,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(
                text = stringResource(R.string.capture_use_photo),
                onClick = { onUse(crop.area) },
                // Only a readable image can go on to text recognition.
                enabled = bitmap?.isSuccess == true && !crop.pickingParagraph,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** "Seleccionar párrafo" and "Toda la foto", or the hint and "Cancelar" while a paragraph is picked. */
@Composable
private fun CropActions(
    state: PhotoCropUiState,
    onPickParagraph: () -> Unit,
    onCancelPick: () -> Unit,
    onWholePhoto: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        val message = when {
            state.pickingParagraph -> R.string.capture_pick_paragraph_hint
            state.noParagraphs -> R.string.capture_no_paragraphs
            else -> null
        }
        Box(modifier = Modifier.weight(1f)) {
            if (state.findingParagraphs) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (message != null) {
                Text(text = stringResource(message), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (state.pickingParagraph) {
            TextButton(onClick = onCancelPick) { Text(stringResource(R.string.capture_cancel_pick)) }
        } else {
            if (!state.area.isWholeImage) {
                TextButton(onClick = onWholePhoto) { Text(stringResource(R.string.capture_whole_photo)) }
            }
            if (!state.noParagraphs) {
                TextButton(onClick = onPickParagraph, enabled = !state.findingParagraphs) {
                    Text(stringResource(R.string.capture_pick_paragraph))
                }
            }
        }
    }
}
