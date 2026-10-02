package io.github.fabiann1809.reader.ui.reader

/** Where the selected text is on the page, in pixels from the page's top left corner. */
data class SelectionBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

/**
 * Text the reader selected on an EPUB page (T11.10). [location] is Readium's Locator of it as JSON
 * (to highlight it), and [bounds] is null when Readium can't tell where it is.
 */
data class TextSelection(val text: String, val bounds: SelectionBounds?, val location: String = "")
