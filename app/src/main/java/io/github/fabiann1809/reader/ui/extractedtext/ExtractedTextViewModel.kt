package io.github.fabiann1809.reader.ui.extractedtext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ai.MAX_TEXT_LENGTH
import io.github.fabiann1809.reader.ocr.NoTextFoundException
import io.github.fabiann1809.reader.ocr.TextRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ExtractedTextUiState {
    data object Recognizing : ExtractedTextUiState

    /** [uncertainLines] are the lines the OCR doubted (T12.4); the ones still in [text] get marked. */
    data class Editing(val text: String, val uncertainLines: List<String> = emptyList()) : ExtractedTextUiState {
        val isTooLong: Boolean get() = text.length > MAX_TEXT_LENGTH
        val canContinue: Boolean get() = text.isNotBlank() && !isTooLong

        /** Doubted lines the user hasn't corrected yet; the warning shows while there are any. */
        val linesToCheck: List<String> get() = uncertainLines.filter { it.isNotBlank() && it in text }
    }

    data class Failed(val reason: OcrFailure) : ExtractedTextUiState
}

enum class OcrFailure { NO_TEXT_FOUND, UNREADABLE_IMAGE }

class ExtractedTextViewModel(
    private val imageUri: String,
    private val textRecognizer: TextRecognizer,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExtractedTextUiState>(ExtractedTextUiState.Recognizing)
    val uiState: StateFlow<ExtractedTextUiState> = _uiState.asStateFlow()

    init {
        recognize()
    }

    private fun recognize() {
        viewModelScope.launch {
            _uiState.value = textRecognizer.recognize(imageUri).fold(
                onSuccess = { ExtractedTextUiState.Editing(it.text, it.uncertainLines) },
                onFailure = { error ->
                    val reason = if (error is NoTextFoundException) OcrFailure.NO_TEXT_FOUND else OcrFailure.UNREADABLE_IMAGE
                    ExtractedTextUiState.Failed(reason)
                },
            )
        }
    }

    fun onTextChange(text: String) {
        val state = _uiState.value as? ExtractedTextUiState.Editing ?: return
        _uiState.value = state.copy(text = text)
    }
}
