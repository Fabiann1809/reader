package io.github.fabiann1809.reader

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingBooksTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val epub = Uri.parse("content://com.example.files/libros/principito.epub")
    private val pdf = Uri.parse("content://com.example.files/libros/cosmos.pdf")

    @Test
    fun openWithGivesItsFile() {
        assertEquals(listOf(epub), incomingBookUris(Intent(Intent.ACTION_VIEW, epub)))
    }

    @Test
    fun shareGivesOneOrSeveralFiles() {
        val one = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, epub)
        val several = Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(epub, pdf))

        assertEquals(listOf(epub), incomingBookUris(one))
        assertEquals(listOf(epub, pdf), incomingBookUris(several))
    }

    @Test
    fun anythingElseGivesNothing() {
        assertTrue(incomingBookUris(Intent(Intent.ACTION_MAIN)).isEmpty())
        // Shared text (a message or a link) has no file.
        assertTrue(incomingBookUris(Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "Hola")).isEmpty())
        // file:// could point at the app's own private files.
        assertTrue(incomingBookUris(Intent(Intent.ACTION_VIEW, Uri.parse("file:///data/data/x/libro.epub"))).isEmpty())
    }

    private fun readerHandles(intent: Intent): Boolean =
        context.packageManager.queryIntentActivities(intent.setPackage(context.packageName), PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()

    @Test
    fun readerIsOfferedForBookFilesOnly() {
        assertTrue(readerHandles(Intent(Intent.ACTION_SEND).setType("application/epub+zip")))
        assertTrue(readerHandles(Intent(Intent.ACTION_SEND_MULTIPLE).setType("application/pdf")))
        assertTrue(readerHandles(Intent(Intent.ACTION_VIEW).setDataAndType(epub, "application/epub+zip")))
        assertTrue(readerHandles(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://x/cuento.txt"), "text/plain")))

        // Sharing a message or a photo must not list Reader.
        assertFalse(readerHandles(Intent(Intent.ACTION_SEND).setType("text/plain")))
        assertFalse(readerHandles(Intent(Intent.ACTION_SEND).setType("image/jpeg")))
    }
}
