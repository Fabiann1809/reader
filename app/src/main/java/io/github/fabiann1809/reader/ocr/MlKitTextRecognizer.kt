package io.github.fabiann1809.reader.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume

/**
 * [TextRecognizer] using Google ML Kit's on-device Latin-script model
 * (bundled in the APK, so it works offline and sends nothing to any server).
 */
class MlKitTextRecognizer(context: Context) : TextRecognizer {

    private val appContext = context.applicationContext
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(imageUri: Uri): Result<String> {
        val image = try {
            // Reads the EXIF orientation, so rotated photos are recognized correctly.
            InputImage.fromFilePath(appContext, imageUri)
        } catch (e: IOException) {
            return Result.failure(e)
        }

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = formatRecognizedText(
                        visionText.textBlocks.map { block -> block.lines.map { it.text } },
                    )
                    continuation.resume(
                        if (text.isBlank()) Result.failure(NoTextFoundException()) else Result.success(text),
                    )
                }
                .addOnFailureListener { error -> continuation.resume(Result.failure(error)) }
        }
    }
}
