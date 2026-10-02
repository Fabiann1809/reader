package io.github.fabiann1809.reader.data.voice

import android.media.MediaPlayer

/** [VoicePlayer] with Android's MediaPlayer. */
class MediaVoicePlayer : VoicePlayer {

    private var player: MediaPlayer? = null
    private var playingPath: String? = null

    override fun play(path: String, onDone: () -> Unit): Result<Unit> {
        val paused = player?.takeIf { playingPath == path }
        if (paused != null) {
            paused.start()
            return Result.success(Unit)
        }
        stop()
        val mediaPlayer = MediaPlayer()
        return try {
            mediaPlayer.setDataSource(path)
            mediaPlayer.setOnCompletionListener {
                stop()
                onDone()
            }
            mediaPlayer.prepare()
            mediaPlayer.start()
            player = mediaPlayer
            playingPath = path
            Result.success(Unit)
        } catch (e: Exception) {
            // A missing or damaged file: IOException or IllegalStateException.
            mediaPlayer.release()
            Result.failure(e)
        }
    }

    override fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    override fun stop() {
        player?.release()
        player = null
        playingPath = null
    }
}
