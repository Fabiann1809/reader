package io.github.fabiann1809.reader.ai

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.ReaderApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Asks the real provider for a quiz through [GenerateQuiz]. Opt-in like [ExplainerPromptLiveTest]: `-e liveAi true`. */
@RunWith(AndroidJUnit4::class)
class QuizLiveTest {

    private val generateQuiz = ApplicationProvider.getApplicationContext<ReaderApplication>().container.generateQuiz

    @Before
    fun onlyWhenRequested() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveAi") == "true")
    }

    @Test
    fun returnsAValidQuizAboutTheText() = runBlocking {
        val chapter = "La entropía es una medida del desorden de un sistema. Según el segundo principio de la " +
            "termodinámica, en un sistema aislado la entropía nunca disminuye: con el tiempo, la energía tiende a " +
            "dispersarse y los procesos espontáneos avanzan en una sola dirección. Por eso un vaso que se rompe no " +
            "vuelve a unirse por sí mismo, y el calor fluye siempre del cuerpo más caliente al más frío. " +
            "Los seres vivos parecen contradecir esta ley, porque crean orden; pero lo hacen consumiendo energía " +
            "y aumentando el desorden de su entorno, de modo que la entropía total del universo sigue creciendo."

        val quiz = generateQuiz(chapter, questionCount = 3).getOrThrow()
        // Only the model's answer is logged, never the key.
        quiz.questions.forEach { Log.i("QuizLiveTest", "$it") }

        assertEquals(3, quiz.questions.size)
        quiz.questions.forEach { question ->
            assertEquals(QUIZ_OPTIONS, question.options.size)
            assertTrue(question.correctIndex in 0..3)
            assertTrue(question.toString(), question.options.distinct().size == QUIZ_OPTIONS)
        }
        assertEquals(3, quiz.questions.map { it.question }.distinct().size)
    }
}
