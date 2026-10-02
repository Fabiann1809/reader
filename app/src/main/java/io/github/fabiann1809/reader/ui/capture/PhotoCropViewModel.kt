package io.github.fabiann1809.reader.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fabiann1809.reader.ocr.Corner
import io.github.fabiann1809.reader.ocr.ImageArea
import io.github.fabiann1809.reader.ocr.TextRecognizer
import io.github.fabiann1809.reader.ocr.movedBy
import io.github.fabiann1809.reader.ocr.withCornerMoved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The photo's crop (T12.5): [area] is what goes on to the text recognition. "Seleccionar párrafo"
 * looks for the [paragraphs] once and, while [pickingParagraph], one of them can be tapped.
 */
data class PhotoCropUiState(
    val area: ImageArea = ImageArea.WholeImage,
    val paragraphs: List<ImageArea>? = null,
    val pickingParagraph: Boolean = false,
    val findingParagraphs: Boolean = false,
) {
    /** The search ended without a paragraph: a picture with no text, or one too blurry. */
    val noParagraphs: Boolean get() = paragraphs?.isEmpty() == true
}

class PhotoCropViewModel(
    private val imageUri: String,
    private val textRecognizer: TextRecognizer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoCropUiState())
    val uiState: StateFlow<PhotoCropUiState> = _uiState.asStateFlow()

    fun moveCorner(corner: Corner, dx: Float, dy: Float) =
        _uiState.update { it.copy(area = it.area.withCornerMoved(corner, dx, dy, MIN_SIZE)) }

    fun moveArea(dx: Float, dy: Float) = _uiState.update { it.copy(area = it.area.movedBy(dx, dy)) }

    /** "Seleccionar párrafo": shows the paragraphs to tap, finding them the first time. */
    fun startPickingParagraph() {
        val state = _uiState.value
        if (state.paragraphs != null) {
            _uiState.update { it.copy(pickingParagraph = it.paragraphs?.isNotEmpty() == true) }
            return
        }
        if (state.findingParagraphs) return
        _uiState.update { it.copy(findingParagraphs = true) }
        viewModelScope.launch {
            val found = textRecognizer.paragraphs(imageUri).getOrDefault(emptyList())
            _uiState.update { it.copy(paragraphs = found, findingParagraphs = false, pickingParagraph = found.isNotEmpty()) }
        }
    }

    fun stopPickingParagraph() = _uiState.update { it.copy(pickingParagraph = false) }

    /**
     * A tap at ([x], [y]) (fractions of the photo) while picking: the crop becomes that paragraph.
     * ML Kit sometimes splits a piece of a line into a block inside the paragraph; the largest
     * block under the finger is the whole paragraph.
     */
    fun tapWhilePicking(x: Float, y: Float) {
        val paragraph = _uiState.value.paragraphs
            ?.filter { it.contains(x, y) }
            ?.maxByOrNull { it.width * it.height } ?: return
        _uiState.update { it.copy(area = paragraph.grownBy(PARAGRAPH_MARGIN), pickingParagraph = false) }
    }

    /** Back to the whole photo. */
    fun reset() = _uiState.update { it.copy(area = ImageArea.WholeImage, pickingParagraph = false) }

    private companion object {
        // A crop smaller than this (of the photo's side) can't hold a line of text.
        const val MIN_SIZE = 0.05f

        // Room around a picked paragraph, so its first and last letters aren't cut.
        const val PARAGRAPH_MARGIN = 0.01f
    }
}
