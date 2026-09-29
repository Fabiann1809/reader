package io.github.fabiann1809.reader.ocr

import android.net.Uri

/** Extracts text from an image. Implementations run fully on the device. */
interface TextRecognizer {

    /**
     * Returns the recognized text with paragraphs separated by blank lines,
     * or a failure ([NoTextFoundException] when the image contains no readable text).
     */
    suspend fun recognize(imageUri: Uri): Result<String>
}

class NoTextFoundException : Exception("No text found in the image")
