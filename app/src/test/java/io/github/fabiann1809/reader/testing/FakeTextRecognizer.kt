package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.ocr.ImageArea
import io.github.fabiann1809.reader.ocr.RecognizedText
import io.github.fabiann1809.reader.ocr.TextRecognizer

/** TextRecognizer for tests that returns a fixed [result]. */
class FakeTextRecognizer(var result: Result<RecognizedText> = Result.success(RecognizedText("Recognized text"))) : TextRecognizer {
    var paragraphsResult: Result<List<ImageArea>> = Result.success(emptyList())

    override suspend fun recognize(imageUri: String): Result<RecognizedText> = result

    override suspend fun paragraphs(imageUri: String): Result<List<ImageArea>> = paragraphsResult
}
