package io.github.fabiann1809.reader.data.voice

/** Plays a recorded voice note (T13.1). One at a time. */
interface VoicePlayer {
    /** Plays the file at [path] from the start, or from where it was paused; [onDone] runs at the end. */
    fun play(path: String, onDone: () -> Unit): Result<Unit>

    fun pause()

    /** Stops and frees the player. */
    fun stop()
}
