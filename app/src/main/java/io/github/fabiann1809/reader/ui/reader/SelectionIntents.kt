package io.github.fabiann1809.reader.ui.reader

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

// The "Más" actions of the selection capsule (design 01 §4.4): copy, search and share.

fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    // Android 13+ confirms the copy itself, so no message of our own.
    clipboard.setPrimaryClip(ClipData.newPlainText(text.take(CLIP_LABEL_LENGTH), text))
}

/** Searches the text in the phone's search app; nothing happens if there is none. */
fun searchText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, text)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No search app installed: there is nothing to open.
    }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, null))
}

private const val CLIP_LABEL_LENGTH = 40
