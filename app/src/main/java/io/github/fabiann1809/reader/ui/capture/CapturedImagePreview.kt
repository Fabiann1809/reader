package io.github.fabiann1809.reader.ui.capture

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.util.decodeScaledBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_MAX_DIMENSION = 1600

/** Shows the photo that was just taken or picked, with actions below it. */
@Composable
fun CapturedImagePreview(
    imageUri: Uri,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUri) {
        value = withContext(Dispatchers.IO) { decodeScaledBitmap(context, imageUri, PREVIEW_MAX_DIMENSION) }
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val image = bitmap
            if (image == null) {
                CircularProgressIndicator()
            } else {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = stringResource(R.string.capture_photo_description),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.capture_retake))
        }
    }
}
