package io.github.fabiann1809.reader.data.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import kotlin.math.log10

/** [VoiceRecorder] with Android's MediaRecorder: AAC in an .m4a file under `files/voice`. */
class MediaVoiceRecorder(
    private val appContext: Context,
    private val voiceFiles: VoiceFiles,
) : VoiceRecorder {

    private var recorder: MediaRecorder? = null
    private var file: File? = null

    override fun start(): Result<Unit> {
        cancel()
        val output = voiceFiles.newRecording()
        val mediaRecorder = newMediaRecorder()
        return try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(SAMPLING_RATE)
                setAudioEncodingBitRate(BIT_RATE)
                setOutputFile(output.path)
                prepare()
                start()
            }
            recorder = mediaRecorder
            file = output
            Result.success(Unit)
        } catch (e: Exception) {
            // prepare() and start() throw IOException or IllegalStateException when the mic is unavailable.
            mediaRecorder.release()
            output.delete()
            Result.failure(e)
        }
    }

    override fun stop(): Result<String> {
        val mediaRecorder = recorder ?: return Result.failure(IllegalStateException("Not recording"))
        val output = file!!
        recorder = null
        file = null
        return try {
            // Throws RuntimeException when stopped before any audio was captured.
            mediaRecorder.stop()
            Result.success(voiceFiles.storedPath(output))
        } catch (e: RuntimeException) {
            output.delete()
            Result.failure(e)
        } finally {
            mediaRecorder.release()
        }
    }

    override fun cancel() {
        val mediaRecorder = recorder ?: return
        runCatching { mediaRecorder.stop() }
        mediaRecorder.release()
        file?.delete()
        recorder = null
        file = null
    }

    override fun delete(path: String) = voiceFiles.delete(path)

    override fun level(): Float {
        // getMaxAmplitude() is the peak since the last call, up to 32767; it throws once stopped.
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        if (amplitude <= 0) return 0f
        // In decibels, like hearing: normal speech (about -25 dB) fills half the wave instead of a sliver.
        val decibels = 20 * log10(amplitude / MAX_AMPLITUDE)
        return ((decibels + WAVE_RANGE_DB) / WAVE_RANGE_DB).coerceIn(0f, 1f)
    }

    @Suppress("DEPRECATION")
    private fun newMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(appContext) else MediaRecorder()

    private companion object {
        // Speech needs little: mono AAC at 64 kbps is clear and takes about 0.5 MB per minute.
        const val SAMPLING_RATE = 44_100
        const val BIT_RATE = 64_000
        const val MAX_AMPLITUDE = 32_767f

        // Sounds quieter than this many decibels below the maximum draw as silence.
        const val WAVE_RANGE_DB = 50f
    }
}
