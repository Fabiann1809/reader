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

    override suspend fun recognize(imageUri: String): Result<String> {
        val image = try {
            // Reads the EXIF orientation, so rotated photos are recognized correctly.
            InputImage.fromFilePath(appContext, Uri.parse(imageUri))
        } catch (e: IOException) {
            return Result.failure(e)
        }

        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
                        val box = line.boundingBox ?: return@mapNotNull null
                        OcrLine(text = line.text, left = box.left, top = box.top, right = box.right, bottom = box.bottom)
                    }
                    val text = formatRecognizedText(lines)
                    continuation.resume(
                        if (text.isBlank()) Result.failure(NoTextFoundException()) else Result.success(text),
                    )
                }
                .addOnFailureListener { error -> continuation.resume(Result.failure(error)) }
        }
    }
}
