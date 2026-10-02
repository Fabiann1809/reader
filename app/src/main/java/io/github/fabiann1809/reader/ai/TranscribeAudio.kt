package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.data.voice.VoiceFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// About 20 minutes of the app's recordings: the audio travels inside the request, which has a size limit.
const val MAX_AUDIO_BYTES = 10 * 1024 * 1024

/** The format of the app's recordings (AAC in an MPEG-4 file, see MediaVoiceRecorder). */
const val VOICE_NOTE_MIME_TYPE = "audio/mp4"

/**
 * Use case: turns a recorded voice note into text (T13.2). A file too big to send fails with
 * [IllegalArgumentException] before any request is made, so it never costs the user quota.
 */
class TranscribeAudio(
    private val aiProvider: AiProvider,
    private val voiceFiles: VoiceFiles,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /** Transcribes the recording at [path], a [VoiceFiles] stored path. */
    suspend operator fun invoke(path: String): Result<String> {
        val audio = withContext(ioDispatcher) { runCatching { readRecording(voiceFiles.resolve(path)) } }
        return audio.fold(
            onSuccess = { bytes -> aiProvider.transcribe(bytes, VOICE_NOTE_MIME_TYPE).map { it.trim() } },
            onFailure = { Result.failure(it) },
        )
    }

    /** The recording's bytes; a missing file throws an IOException. */
    private fun readRecording(file: File): ByteArray {
        require(file.length() <= MAX_AUDIO_BYTES) { "Recording too long" }
        return file.readBytes()
    }
}
