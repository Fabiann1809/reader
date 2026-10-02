package io.github.fabiann1809.reader.ai

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.ReaderApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Sends a real recording (Spanish speech in the app's format, AAC in .m4a) through [TranscribeAudio]
 * with the API key saved in the app. Opt-in like [ExplainerPromptLiveTest]: `-e liveAi true`.
 */
@RunWith(AndroidJUnit4::class)
class TranscriptionLiveTest {

    private val application = ApplicationProvider.getApplicationContext<ReaderApplication>()

    @Before
    fun onlyWhenRequested() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveAi") == "true")
    }

    @Test
    fun transcribesASpanishVoiceNote() = runBlocking {
        val file = File(application.cacheDir, "test_voice.m4a")
        InstrumentationRegistry.getInstrumentation().context.assets.open("test_voice.m4a").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }

        val transcript = application.container.transcribeAudio(file.path).getOrThrow()
        // Only the model's answer is logged, never the key.
        Log.i("TranscriptionLiveTest", transcript)

        val lower = transcript.lowercase()
        assertTrue(transcript, "entropía" in lower && "sistema aislado" in lower && "capítulo" in lower)
    }
}
