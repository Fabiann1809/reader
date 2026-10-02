package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.voice.VoicePlayer
import io.github.fabiann1809.reader.data.voice.VoiceRecorder

class FakeVoiceRecorder : VoiceRecorder {
    var startResult: Result<Unit> = Result.success(Unit)
    var stopResult: Result<String> = Result.success("/files/voice/voice-1.m4a")
    var recording = false
    val deleted = mutableListOf<String>()

    override fun start(): Result<Unit> = startResult.onSuccess { recording = true }

    override fun stop(): Result<String> {
        recording = false
        return stopResult
    }

    override fun cancel() {
        recording = false
    }

    override fun delete(path: String) {
        deleted += path
    }
}

class FakeVoicePlayer : VoicePlayer {
    var playResult: Result<Unit> = Result.success(Unit)
    var playing: String? = null
    private var onDone: (() -> Unit)? = null

    override fun play(path: String, onDone: () -> Unit): Result<Unit> = playResult.onSuccess {
        playing = path
        this.onDone = onDone
    }

    override fun pause() {
        playing = null
    }

    override fun stop() {
        playing = null
    }

    /** The recording reached its end. */
    fun finish() {
        playing = null
        onDone?.invoke()
    }
}
