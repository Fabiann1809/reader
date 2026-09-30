package io.github.fabiann1809.reader.ui.extractedtext

import io.github.fabiann1809.reader.ai.MAX_TEXT_LENGTH
import io.github.fabiann1809.reader.ocr.NoTextFoundException
import io.github.fabiann1809.reader.testing.FakeTextRecognizer
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ExtractedTextViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val imageUri = "file:///cache/captures/page.jpg"

    private fun viewModel(result: Result<String>) = ExtractedTextViewModel(imageUri, FakeTextRecognizer(result))

    @Test
    fun showsRecognizedTextForEditing() {
        val state = viewModel(Result.success("Texto de la página")).uiState.value

        assertEquals(ExtractedTextUiState.Editing("Texto de la página"), state)
    }

    @Test
    fun userCanCorrectTheText() {
        val viewModel = viewModel(Result.success("Txto con error"))

        viewModel.onTextChange("Texto corregido")

        assertEquals(ExtractedTextUiState.Editing("Texto corregido"), viewModel.uiState.value)
    }

    @Test
    fun textOverTheLimitCannotContinue() {
        val viewModel = viewModel(Result.success("ok"))

        viewModel.onTextChange("a".repeat(MAX_TEXT_LENGTH + 1))

        val state = viewModel.uiState.value as ExtractedTextUiState.Editing
        assertTrue(state.isTooLong)
        assertFalse(state.canContinue)
    }

    @Test
    fun textAtTheLimitCanContinue() {
        val viewModel = viewModel(Result.success("ok"))

        viewModel.onTextChange("a".repeat(MAX_TEXT_LENGTH))

        assertTrue((viewModel.uiState.value as ExtractedTextUiState.Editing).canContinue)
    }

    @Test
    fun blankTextCannotContinue() {
        val viewModel = viewModel(Result.success("ok"))

        viewModel.onTextChange("   ")

        assertFalse((viewModel.uiState.value as ExtractedTextUiState.Editing).canContinue)
    }

    @Test
    fun noTextFoundIsReported() {
        val state = viewModel(Result.failure(NoTextFoundException())).uiState.value

        assertEquals(ExtractedTextUiState.Failed(OcrFailure.NO_TEXT_FOUND), state)
    }

    @Test
    fun otherErrorsAreReportedAsUnreadableImage() {
        val state = viewModel(Result.failure(IOException("gone"))).uiState.value

        assertEquals(ExtractedTextUiState.Failed(OcrFailure.UNREADABLE_IMAGE), state)
    }
}
