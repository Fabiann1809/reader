package io.github.fabiann1809.reader.data.voice

import java.io.File

/**
 * Voice note recordings in the app's private storage. Like [io.github.fabiann1809.reader.data.book.BookFiles],
 * paths are relative to [filesDir] (e.g. "voice/voice-1790….m4a") so they stay valid after a backup restore.
 */
class VoiceFiles(private val filesDir: File) {

    fun newRecording(): File =
        File(File(filesDir, VOICE_DIR).apply { mkdirs() }, "voice-${System.currentTimeMillis()}.m4a")

    fun storedPath(file: File): String = file.relativeTo(filesDir).invariantSeparatorsPath

    fun resolve(storedPath: String): File = File(filesDir, storedPath)

    fun delete(storedPath: String?) {
        if (storedPath != null) resolve(storedPath).delete()
    }

    private companion object {
        const val VOICE_DIR = "voice"
    }
}
