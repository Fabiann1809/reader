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

    override suspend fun paragraphs(imageUri: String): Result<List<ImageArea>> {
        val image = try {
            InputImage.fromFilePath(appContext, Uri.parse(imageUri))
        } catch (e: IOException) {
            return Result.failure(e)
        }
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // ML Kit's boxes are in pixels of the upright image; as fractions they fit any preview size.
                    val width = image.width.toFloat()
                    val height = image.height.toFloat()
                    val areas = visionText.textBlocks.mapNotNull { block ->
                        val box = block.boundingBox ?: return@mapNotNull null
                        ImageArea(box.left / width, box.top / height, box.right / width, box.bottom / height)
                    }.sortedBy { it.top }
                    continuation.resume(Result.success(areas))
                }
                .addOnFailureListener { error -> continuation.resume(Result.failure(error)) }
        }
    }

    override suspend fun recognize(imageUri: String): Result<RecognizedText> {
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
                        OcrLine(
                            text = line.text,
                            left = box.left,
                            top = box.top,
                            right = box.right,
                            bottom = box.bottom,
                            confidence = line.confidence,
                        )
                    }
                    val text = formatRecognizedText(lines)
                    continuation.resume(
                        if (text.isBlank()) {
                            Result.failure(NoTextFoundException())
                        } else {
                            Result.success(RecognizedText(text, uncertainLines(lines)))
                        },
                    )
                }
                .addOnFailureListener { error -> continuation.resume(Result.failure(error)) }
        }
    }
}
