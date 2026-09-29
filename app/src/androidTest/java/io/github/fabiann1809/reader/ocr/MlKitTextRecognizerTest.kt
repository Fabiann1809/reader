package io.github.fabiann1809.reader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
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
        val text = recognizer.recognize(assetUri("test_page.jpg")).getOrThrow()

        assertTrue(text, text.contains("entropía"))
        assertTrue(text, text.contains("segundo principio de la termodinámica"))
        // Lines of the same paragraph are joined, so the sentence isn't split by line breaks.
        assertTrue(text, text.contains("un vaso que se rompe no vuelve a unirse"))
    }

    @Test
    fun blankImageFailsWithNoTextFound() = runTest {
        val file = File(appContext.cacheDir, "blank.png")
        Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(android.graphics.Color.WHITE) }
            .compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())

        val error = recognizer.recognize(Uri.fromFile(file)).exceptionOrNull()

        assertTrue("was $error", error is NoTextFoundException)
    }
}
