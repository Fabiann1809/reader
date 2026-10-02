package io.github.fabiann1809.reader.data.voice

import android.Manifest
import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MediaVoiceRecorderTest {

    private val appContext = ApplicationProvider.getApplicationContext<Context>()
    private val recorder = MediaVoiceRecorder(appContext)

    @Before
    fun grantMicrophone() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(appContext.packageName, Manifest.permission.RECORD_AUDIO)
    }

    @Test
    fun recordsAPlayableFileInPrivateStorage() {
        recorder.start().getOrThrow()
        Thread.sleep(1_500)
        val path = recorder.stop().getOrThrow()

        val file = File(path)
        assertTrue(path, file.path.startsWith(appContext.filesDir.path))
        val duration = MediaMetadataRetriever().run {
            setDataSource(path)
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong().also { release() }
        }
        assertTrue("duration $duration", duration in 1_000..3_000)

        recorder.delete(path)
        assertFalse(file.exists())
    }

    @Test
    fun cancellingLeavesNoFile() {
        val folder = File(appContext.filesDir, "voice")
        val before = folder.listFiles().orEmpty().toSet()
        recorder.start().getOrThrow()
        Thread.sleep(300)

        recorder.cancel()

        assertTrue(folder.listFiles().orEmpty().toSet() == before)
    }
}
