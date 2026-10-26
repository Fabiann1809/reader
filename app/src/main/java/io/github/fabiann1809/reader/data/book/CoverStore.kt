package io.github.fabiann1809.reader.data.book

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Keeps the pictures the user chooses as book covers. */
interface CoverStore {
    /** Stores the picture at [uri] as a cover and returns its path, or null if it can't be read. */
    suspend fun save(uri: String): String?

    fun delete(coverPath: String?)
}

class ContentCoverStore(private val contentResolver: ContentResolver, private val bookFiles: BookFiles) : CoverStore {

    override suspend fun save(uri: String): String? = withContext(Dispatchers.IO) {
        try {
            decode(uri)?.let(bookFiles::saveCover)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    override fun delete(coverPath: String?) = bookFiles.delete(coverPath)

    // Photos are far bigger than a cover needs: they are shrunk while they are read.
    private fun decode(uri: String): Bitmap? {
        val source = uri.toUri()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(maxOf(bounds.outWidth, bounds.outHeight)) }
        val bitmap = contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        return bitmap.rotatedUpright(orientationOf(uri))
    }

    private fun orientationOf(uri: String): Int =
        contentResolver.openInputStream(uri.toUri())?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

    private fun Bitmap.rotatedUpright(orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return this
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)
    }

    private fun sampleSize(longestSide: Int): Int {
        var size = 1
        while (longestSide / (size * 2) >= MAX_SIDE) size *= 2
        return size
    }

    private companion object {
        const val MAX_SIDE = 1200
    }
}
