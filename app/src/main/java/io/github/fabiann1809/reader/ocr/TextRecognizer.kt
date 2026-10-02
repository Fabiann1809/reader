package io.github.fabiann1809.reader.ocr

/** Extracts text from an image. Implementations run fully on the device. */
interface TextRecognizer {

    /**
     * [imageUri] is the image's URI as a string (content:// or file://).
     * Returns the recognized text with paragraphs separated by blank lines and the lines read
     * with low confidence, or a failure ([NoTextFoundException] when the image contains no
     * readable text).
     */
    suspend fun recognize(imageUri: String): Result<RecognizedText>

    /**
     * Where the paragraphs (blocks of text) are on the image, top to bottom, to pick one of them
     * (T12.5). Empty when the image has no text.
     */
    suspend fun paragraphs(imageUri: String): Result<List<ImageArea>>
}

class NoTextFoundException : Exception("No text found in the image")
