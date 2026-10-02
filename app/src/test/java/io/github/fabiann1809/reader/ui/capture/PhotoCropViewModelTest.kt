package io.github.fabiann1809.reader.ui.capture

import io.github.fabiann1809.reader.ocr.Corner
import io.github.fabiann1809.reader.ocr.ImageArea
import io.github.fabiann1809.reader.testing.FakeTextRecognizer
import io.github.fabiann1809.reader.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class PhotoCropViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val firstParagraph = ImageArea(0.1f, 0.1f, 0.9f, 0.3f)
    private val secondParagraph = ImageArea(0.1f, 0.5f, 0.9f, 0.7f)
    private val recognizer = FakeTextRecognizer().apply { paragraphsResult = Result.success(listOf(firstParagraph, secondParagraph)) }

    private fun viewModel() = PhotoCropViewModel("file:///cache/captures/page.jpg", recognizer)

    @Test
    fun startsWithTheWholePhoto() {
        assertTrue(viewModel().uiState.value.area.isWholeImage)
    }

    @Test
    fun draggingACornerCropsThePhoto() {
        val viewModel = viewModel()

        viewModel.moveCorner(Corner.TOP_LEFT, dx = 0.2f, dy = 0.1f)

        assertEquals(ImageArea(0.2f, 0.1f, 1f, 1f), viewModel.uiState.value.area)
    }

    @Test
    fun tappingAParagraphCropsToIt() {
        val viewModel = viewModel()
        viewModel.startPickingParagraph()
        assertTrue(viewModel.uiState.value.pickingParagraph)

        viewModel.tapWhilePicking(0.5f, 0.6f)

        val state = viewModel.uiState.value
        assertFalse(state.pickingParagraph)
        assertTrue(state.area.contains(0.1f, 0.5f) && state.area.contains(0.9f, 0.7f))
        assertFalse(state.area.contains(0.5f, 0.2f))
    }

    @Test
    fun aPieceOfLineInsideAParagraphPicksTheWholeParagraph() {
        val pieceOfLine = ImageArea(0.4f, 0.5f, 0.9f, 0.53f)
        recognizer.paragraphsResult = Result.success(listOf(firstParagraph, secondParagraph, pieceOfLine))
        val viewModel = viewModel()
        viewModel.startPickingParagraph()

        viewModel.tapWhilePicking(0.6f, 0.51f)

        assertTrue(viewModel.uiState.value.area.contains(0.1f, 0.7f))
    }

    @Test
    fun aTapOutsideEveryParagraphKeepsPicking() {
        val viewModel = viewModel()
        viewModel.startPickingParagraph()

        viewModel.tapWhilePicking(0.5f, 0.95f)

        assertTrue(viewModel.uiState.value.pickingParagraph)
        assertTrue(viewModel.uiState.value.area.isWholeImage)
    }

    @Test
    fun aPhotoWithoutTextSaysSoAndDoesNotPick() {
        recognizer.paragraphsResult = Result.failure(IOException("unreadable"))
        val viewModel = viewModel()

        viewModel.startPickingParagraph()

        assertTrue(viewModel.uiState.value.noParagraphs)
        assertFalse(viewModel.uiState.value.pickingParagraph)
    }

    @Test
    fun wholePhotoUndoesTheCrop() {
        val viewModel = viewModel()
        viewModel.moveCorner(Corner.BOTTOM_RIGHT, dx = -0.5f, dy = -0.5f)

        viewModel.reset()

        assertTrue(viewModel.uiState.value.area.isWholeImage)
    }
}
