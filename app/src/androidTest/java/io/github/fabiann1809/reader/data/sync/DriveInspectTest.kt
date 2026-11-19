package io.github.fabiann1809.reader.data.sync

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.fabiann1809.reader.ReaderApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Shows what the app's hidden Drive folder holds (opt-in: `-e liveDrive true`), in logcat under "DriveInspect".
 * With `-e cleanEmptyManifest true` it also deletes a manifest that holds no records, only deletions
 * (what the live tests leave behind); a manifest with records is never touched.
 */
@RunWith(AndroidJUnit4::class)
class DriveInspectTest {

    private val cloud = ApplicationProvider.getApplicationContext<ReaderApplication>().container.cloudStorage

    @Test
    fun showsWhatDriveHolds() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("liveDrive") == "true")

        val manifest = cloud.readManifest().getOrThrow()
        val files = cloud.listFiles().getOrThrow()
        if (manifest == null) {
            Log.i(TAG, "no manifest; files: $files")
            return@runBlocking
        }
        val snapshot = SyncManifest.decode(manifest.content)
        Log.i(TAG, "manifest: ${snapshot.records.size} records, ${snapshot.deletions.size} deletions; files: $files")
        snapshot.records.forEach { Log.i(TAG, "record ${it.key.type} ${it.data.take(80)}") }

        if (args.getString("cleanEmptyManifest") == "true" && snapshot.records.isEmpty()) {
            cloud.deleteFile("manifest.json").getOrThrow()
            Log.i(TAG, "deleted the manifest: it had no records")
        }
    }

    private companion object {
        const val TAG = "DriveInspect"
    }
}
