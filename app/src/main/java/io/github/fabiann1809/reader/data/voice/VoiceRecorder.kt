package io.github.fabiann1809.reader.data.voice

/** Records voice notes into the app's private storage (T13.1). One recording at a time. */
interface VoiceRecorder {
    /** Starts a new recording; fails if the microphone can't be used (e.g. another app has it). */
    fun start(): Result<Unit>

    /**
     * Ends the recording and returns its file's path. Fails, deleting the file, when nothing usable
     * was recorded (a recording of a fraction of a second has no audio).
     */
    fun stop(): Result<String>

    /** Ends the recording, if any, and throws it away. */
    fun cancel()

    /** Deletes a finished recording that won't be kept. */
    fun delete(path: String)
}
