package io.github.fabiann1809.reader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.util.cropImage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MlKitTextRecognizerTest {

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val recognizer = MlKitTextRecognizer(appContext)

    /** Copies a file from the test APK's assets to a real file the recognizer can read. */
    private fun assetUri(name: String): Uri {
        val testAssets = InstrumentationRegistry.getInstrumentation().context.assets
        val file = File(appContext.cacheDir, name)
        testAssets.open(name).use { input -> file.outputStream().use { input.copyTo(it) } }
        return Uri.fromFile(file)
    }

    @Test
    fun extractsTextFromTestPage() = runTest {
        val recognized = recognizer.recognize(assetUri("test_page.jpg").toString()).getOrThrow()
        val text = recognized.text

        assertTrue(text, text.startsWith("Capítulo 3: El orden del universo\n\n"))
        // ML Kit returns part of this printed line as a separate block; the formatter must restore reading order.
        assertTrue(text, text.contains("La entropía es una medida del desorden de un sistema."))
        assertTrue(text, text.contains("segundo principio de la termodinámica"))
        // Lines of the same paragraph are joined, so the sentence isn't split by line breaks.
        assertTrue(text, text.contains("un vaso que se rompe no vuelve a unirse"))
    }

    @Test
    fun aSharpPhotoLeavesNothingInDoubt() = runTest {
        val recognized = recognizer.recognize(assetUri("test_page.jpg").toString()).getOrThrow()

        assertTrue(recognized.uncertainLines.toString(), recognized.uncertainLines.isEmpty())
    }

    @Test
    fun aBlurryPhotoHasLinesInDoubt() = runTest {
        // The same page, blurred: ML Kit still reads it, but not all of it with confidence.
        val recognized = recognizer.recognize(assetUri("test_page_blurry.jpg").toString()).getOrThrow()

        assertTrue(recognized.text, recognized.uncertainLines.isNotEmpty())
    }

    @Test
    fun findsTheParagraphsTopToBottomAndReadsOnlyACroppedOne() = runTest {
        val page = assetUri("test_page.jpg")
        val paragraphs = recognizer.paragraphs(page.toString()).getOrThrow()
        assertTrue(paragraphs.toString(), paragraphs.size >= 2)
        assertTrue(paragraphs.zipWithNext().all { (above, below) -> above.top <= below.top })

        // The first paragraph is the chapter's title: cropping to it leaves the body out.
        val cropped = cropImage(appContext, page, paragraphs.first().grownBy(0.01f))!!
        val text = recognizer.recognize(cropped.toString()).getOrThrow().text

        assertTrue(text, text.contains("Capítulo 3"))
        assertFalse(text, text.contains("entropía es una medida"))
    }

    @Test
    fun blankImageFailsWithNoTextFound() = runTest {
        val file = File(appContext.cacheDir, "blank.png")
        Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(android.graphics.Color.WHITE) }
            .compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())

        val error = recognizer.recognize(Uri.fromFile(file).toString()).exceptionOrNull()

        assertTrue("was $error", error is NoTextFoundException)
    }
}
