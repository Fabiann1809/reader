package io.github.fabiann1809.reader.data.book.importing

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import java.io.File
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt

/** Width and height in pixels (plain Kotlin, so the math below runs in JVM unit tests). */
data class PixelSize(val width: Int, val height: Int)

/**
 * Largest stored cover. The biggest cover on screen is the detail one (132 dp, about 400 px at 3x),
 * so this keeps covers sharp while each file stays small.
 */
val MaxCoverSize = PixelSize(480, 720)

/**
 * Scales [width] × [height] to fit inside [max], keeping the proportions. Only shrinks unless
 * [canEnlarge] (vector pages, like PDF ones, render sharp at any size).
 */
fun fitInside(width: Int, height: Int, max: PixelSize = MaxCoverSize, canEnlarge: Boolean = false): PixelSize {
    val fit = min(max.width.toFloat() / width, max.height.toFloat() / height)
    val scale = if (canEnlarge) fit else min(1f, fit)
    return PixelSize((width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
}

/**
 * First page of a PDF as its cover, rendered with Android's PdfRenderer on white
 * (PDF pages are transparent unless they paint a background). Null if the PDF cannot be rendered.
 */
fun renderPdfCover(file: File): Bitmap? {
    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount == 0) return null
                renderer.openPage(0).use { page ->
                    // Page sizes are in points (1/72 inch), usually smaller than the cover we want.
                    val size = fitInside(page.width, page.height, canEnlarge = true)
                    createBitmap(size.width, size.height).apply {
                        eraseColor(Color.WHITE)
                        page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        // Password-protected PDFs.
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}
