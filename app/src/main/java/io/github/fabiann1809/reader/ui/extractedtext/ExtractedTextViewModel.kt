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

    data class Editing(val text: String) : ExtractedTextUiState {
        val isTooLong: Boolean get() = text.length > MAX_TEXT_LENGTH
        val canContinue: Boolean get() = text.isNotBlank() && !isTooLong
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
                onSuccess = { ExtractedTextUiState.Editing(it) },
                onFailure = { error ->
                    val reason = if (error is NoTextFoundException) OcrFailure.NO_TEXT_FOUND else OcrFailure.UNREADABLE_IMAGE
                    ExtractedTextUiState.Failed(reason)
                },
            )
        }
    }

    fun onTextChange(text: String) {
        if (_uiState.value is ExtractedTextUiState.Editing) {
            _uiState.value = ExtractedTextUiState.Editing(text)
        }
    }
}
