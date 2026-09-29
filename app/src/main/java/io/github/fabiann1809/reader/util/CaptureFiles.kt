package io.github.fabiann1809.reader.util

import android.content.Context
import java.io.File

private const val CAPTURES_DIR = "captures"

/**
 * Returns a new file for a page photo in the app's cache. Previous captures are deleted first:
 * photos are only needed until their text is extracted, so they never accumulate.
 */
fun newCaptureFile(context: Context): File {
    val dir = File(context.cacheDir, CAPTURES_DIR)
    dir.deleteRecursively()
    dir.mkdirs()
    return File(dir, "page_${System.currentTimeMillis()}.jpg")
}
