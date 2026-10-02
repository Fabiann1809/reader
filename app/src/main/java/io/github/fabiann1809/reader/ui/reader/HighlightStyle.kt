package io.github.fabiann1809.reader.ui.reader

import io.github.fabiann1809.reader.data.highlight.Highlight
import io.github.fabiann1809.reader.data.highlight.HighlightColor
import org.json.JSONException
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.publication.Locator

/** The decoration group of the reader's highlights in Readium. */
const val HIGHLIGHTS_GROUP = "highlights"

/** Each highlight color as ARGB (design 2.1: "Resaltados del lector"). */
fun HighlightColor.argb(): Int = when (this) {
    HighlightColor.YELLOW -> 0xFFFFE27A
    HighlightColor.GREEN -> 0xFFA8E0B8
    HighlightColor.BLUE -> 0xFFA9D4F5
    HighlightColor.PINK -> 0xFFF5B5C8
}.toInt()

/** The Readium decoration that draws [this]; null if its saved location can't be read any more. */
fun Highlight.toDecoration(): Decoration? {
    val locator = try {
        Locator.fromJSON(JSONObject(location))
    } catch (e: JSONException) {
        null
    } ?: return null
    return Decoration(id = id.toString(), locator = locator, style = Decoration.Style.Highlight(tint = color.argb()))
}
