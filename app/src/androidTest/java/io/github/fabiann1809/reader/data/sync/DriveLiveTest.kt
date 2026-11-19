package io.github.fabiann1809.reader.data.sync

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.ReaderApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * A real round trip with the user's Drive (T18.3): upload, list, download, replace and delete in the
 * app's hidden folder. Opt-in, and the permission must have been given before in Ajustes
 * ("Conectar con Google Drive"). It skips itself when the cloud already holds a manifest, so it never
 * touches real data. Run with:
 * adb shell am instrument -w -e liveDrive true -e class io.github.fabiann1809.reader.data.sync.DriveLiveTest ...
 */
@RunWith(AndroidJUnit4::class)
class DriveLiveTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val cloud = ApplicationProvider.getApplicationContext<ReaderApplication>().container.cloudStorage

    @Before
    fun onlyWhenRequestedAndWhenThereIsNoRealData() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveDrive") == "true")
        assumeTrue("The cloud holds real data", runBlocking { cloud.readManifest().getOrThrow() } == null)
    }

    @Test
    fun filesAndTheManifestMakeARoundTripThroughRealDrive() = runBlocking {
        val book = tmp.newFile("book.bin").apply { writeBytes(ByteArray(300_000) { (it % 251).toByte() }) }

        cloud.uploadFile(NAME, book).getOrThrow()
        assertTrue(NAME in cloud.listFiles().getOrThrow())

        val back = tmp.newFile("back.bin")
        cloud.downloadFile(NAME, back).getOrThrow()
        assertTrue(book.readBytes().contentEquals(back.readBytes()))

        // Uploading the same name replaces the content: still one file.
        book.writeText("replaced")
        cloud.uploadFile(NAME, book).getOrThrow()
        cloud.downloadFile(NAME, back).getOrThrow()
        assertEquals("replaced", back.readText())
        assertEquals(1, cloud.listFiles().getOrThrow().count { it == NAME })

        val version = cloud.writeManifest("""{"formatVersion":1}""", expectedVersion = null).getOrThrow()
        val read = cloud.readManifest().getOrThrow()
        assertNotNull(read)
        assertEquals(version, read!!.version)
        assertTrue(cloud.writeManifest("again", expectedVersion = null).isFailure)

        cloud.deleteFile(NAME).getOrThrow()
        cloud.deleteFile("manifest.json").getOrThrow()
        assertFalse(NAME in cloud.listFiles().getOrThrow())
        assertNull(cloud.readManifest().getOrThrow())
    }

    private companion object {
        const val NAME = "live-test-file.bin"
    }
}
