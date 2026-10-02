package io.github.fabiann1809.reader.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import io.github.fabiann1809.reader.ocr.ImageArea
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

// Big enough for the OCR to read small print, small enough to stay quick.
private const val CROP_MAX_DIMENSION = 3000
private const val JPEG_QUALITY = 95
private const val OLD_CROP_MILLIS = 24 * 60 * 60 * 1000L

/**
 * Saves the [area] of the photo at [uri] as a new image in the cache, upright, and returns its
 * URI; null if the photo can't be read or saved. Call it off the main thread.
 */
fun cropImage(context: Context, uri: Uri, area: ImageArea): Uri? {
    val decoded = decodeScaledBitmap(context, uri, CROP_MAX_DIMENSION) ?: return null
    // ImageDecoder may hand back a hardware bitmap, whose pixels can't be cut or compressed.
    val photo = if (decoded.config == Bitmap.Config.HARDWARE) decoded.copy(Bitmap.Config.ARGB_8888, false) else decoded
    val left = (area.left * photo.width).roundToInt().coerceIn(0, photo.width - 1)
    val top = (area.top * photo.height).roundToInt().coerceIn(0, photo.height - 1)
    val width = (area.width * photo.width).roundToInt().coerceIn(1, photo.width - left)
    val height = (area.height * photo.height).roundToInt().coerceIn(1, photo.height - top)
    val crop = Bitmap.createBitmap(photo, left, top, width, height)
    val folder = File(context.cacheDir, "crops").apply { mkdirs() }
    // One file per crop, since a screen may still be reading an earlier one; only old ones are cleared.
    val now = System.currentTimeMillis()
    folder.listFiles()?.filter { now - it.lastModified() > OLD_CROP_MILLIS }?.forEach { it.delete() }
    val file = File(folder, "crop-$now.jpg")
    return try {
        file.outputStream().use { crop.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        Uri.fromFile(file)
    } catch (e: IOException) {
        null
    }
}
