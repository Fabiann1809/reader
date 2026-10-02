package io.github.fabiann1809.reader.ai

import io.github.fabiann1809.reader.testing.FakeAiProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class TranscribeAudioTest {

    private val aiProvider = FakeAiProvider()
    private val transcribe = TranscribeAudio(aiProvider)

    private fun recording(bytes: ByteArray) = File.createTempFile("voice", ".m4a").apply {
        writeBytes(bytes)
        deleteOnExit()
    }

    @Test
    fun sendsTheRecordingAndTrimsTheTranscript() = runTest {
        aiProvider.transcription = Result.success("  Una idea.\n")

        val transcript = transcribe(recording(byteArrayOf(7, 8)).path).getOrThrow()

        assertEquals("Una idea.", transcript)
        assertArrayEquals(byteArrayOf(7, 8), aiProvider.audios.single().first)
        assertEquals(VOICE_NOTE_MIME_TYPE, aiProvider.audios.single().second)
    }

    @Test
    fun aMissingRecordingFailsWithoutCallingTheAi() = runTest {
        val error = transcribe("/nowhere/voice.m4a").exceptionOrNull()

        assertTrue(error.toString(), error is IOException)
        assertTrue(aiProvider.audios.isEmpty())
    }

    @Test
    fun aTooLongRecordingFailsWithoutCallingTheAi() = runTest {
        val big = File.createTempFile("voice", ".m4a").apply { deleteOnExit() }
        RandomAccessFile(big, "rw").use { it.setLength(MAX_AUDIO_BYTES + 1L) }

        val error = transcribe(big.path).exceptionOrNull()

        assertTrue(error.toString(), error is IllegalArgumentException)
        assertTrue(aiProvider.audios.isEmpty())
    }
}
