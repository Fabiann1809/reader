package io.github.fabiann1809.reader.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The stored cover image of a book, or null while it loads or if there is none.
 * [coverPath] is relative to the app's files folder (see BookFiles).
 */
@Composable
fun rememberCoverImage(coverPath: String?): ImageBitmap? {
    val filesDir = LocalContext.current.filesDir
    // A cached cover shows on the first frame; otherwise it is decoded off the main thread.
    val image by produceState(initialValue = coverPath?.let(CoverCache::get), coverPath) {
        value = coverPath?.let { path ->
            CoverCache.get(path) ?: withContext(Dispatchers.IO) { CoverCache.load(File(filesDir, path), path) }
        }
    }
    return image
}

/** Decoded covers, so scrolling the shelves does not read the same files again. */
private object CoverCache {
    // An eighth of the app's memory, counted in KiB.
    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * BYTES_PER_PIXEL / 1024
    }

    fun get(path: String): ImageBitmap? = cache.get(path)

    fun load(file: File, path: String): ImageBitmap? {
        // RGB_565 halves the memory of each cover; covers are opaque JPEGs, so nothing is lost.
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 }
        val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return null
        return bitmap.asImageBitmap().also { cache.put(path, it) }
    }

    private const val BYTES_PER_PIXEL = 2
}
