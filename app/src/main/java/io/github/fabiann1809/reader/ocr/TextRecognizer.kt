package io.github.fabiann1809.reader.ocr

/** Extracts text from an image. Implementations run fully on the device. */
interface TextRecognizer {

    /**
     * [imageUri] is the image's URI as a string (content:// or file://).
     * Returns the recognized text with paragraphs separated by blank lines,
     * or a failure ([NoTextFoundException] when the image contains no readable text).
     */
    suspend fun recognize(imageUri: String): Result<String>
}

class NoTextFoundException : Exception("No text found in the image")
