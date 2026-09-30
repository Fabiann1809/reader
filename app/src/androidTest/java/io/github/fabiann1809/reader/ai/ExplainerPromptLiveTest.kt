package io.github.fabiann1809.reader.ai

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.ReaderApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sends real texts to the configured AI provider using the API key saved in the app.
 * Opt-in (uses quota and needs internet), run with:
 * ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.liveAi=true
 *   -Pandroid.testInstrumentationRunnerArguments.class=io.github.fabiann1809.reader.ai.ExplainerPromptLiveTest
 */
@RunWith(AndroidJUnit4::class)
class ExplainerPromptLiveTest {

    private val provider = ApplicationProvider.getApplicationContext<ReaderApplication>().container.aiProvider

    @Before
    fun onlyWhenRequested() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveAi") == "true")
    }

    private fun explainAndCheck(name: String, text: String) = runBlocking {
        val explanation = provider.explain(text).getOrThrow()
        // Logged so a person can judge the quality; only the model's answer is logged, never the key.
        Log.i(TAG, "=== $name ===\n$explanation")

        assertTrue(explanation, explanation.contains(ExplainerPrompt.SECTION_MAIN_IDEA, ignoreCase = true))
        assertTrue(explanation, explanation.contains("Analogía", ignoreCase = true))
        assertTrue(explanation, explanation.contains(ExplainerPrompt.SECTION_KEY_TERMS, ignoreCase = true))
        assertFalse("Markdown found:\n$explanation", explanation.contains("**"))
    }

    @Test
    fun explainsScienceText() = explainAndCheck(
        "science",
        "La entropía es una medida del desorden de un sistema. Según el segundo principio de la " +
            "termodinámica, en un sistema aislado la entropía nunca disminuye: con el tiempo, la energía " +
            "tiende a dispersarse y los procesos espontáneos avanzan en una sola dirección.",
    )

    @Test
    fun explainsEconomicsText() = explainAndCheck(
        "economics",
        "El coste de oportunidad de una decisión es el valor de la mejor alternativa a la que se renuncia. " +
            "Como los recursos son escasos, elegir una opción implica siempre dejar de obtener los beneficios " +
            "de otra, aunque ese coste no aparezca en ninguna factura.",
    )

    @Test
    fun explainsPhilosophyText() = explainAndCheck(
        "philosophy",
        "Para el empirismo, todo conocimiento procede de la experiencia sensible: la mente, al nacer, sería " +
            "como una hoja en blanco. Las ideas complejas se formarían combinando impresiones simples, por lo " +
            "que no existirían ideas innatas anteriores a toda percepción.",
    )

    private companion object {
        const val TAG = "ExplainerLive"
    }
}
