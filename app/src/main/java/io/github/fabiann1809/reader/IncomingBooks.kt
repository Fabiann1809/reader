package io.github.fabiann1809.reader

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/**
 * Book files another app handed to Reader with "Compartir" (SEND, SEND_MULTIPLE) or
 * "Abrir con…" (VIEW). Empty for any other intent, such as a normal launch.
 *
 * Only content:// URIs are accepted: a file:// one could point at Reader's own private files,
 * and Android already forbids passing them between apps.
 */
fun incomingBookUris(intent: Intent): List<Uri> {
    val uris = when (intent.action) {
        Intent.ACTION_VIEW -> listOfNotNull(intent.data)
        Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        else -> emptyList()
    }
    return uris.filter { it.scheme == ContentResolver.SCHEME_CONTENT }
}
