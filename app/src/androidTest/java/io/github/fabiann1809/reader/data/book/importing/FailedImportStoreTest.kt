package io.github.fabiann1809.reader.data.book.importing

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FailedImportStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    // A file of its own, never the app's real one.
    private val store = DataStoreFailedImportStore(context, FILE_NAME)

    @After
    fun tearDown() {
        context.preferencesDataStoreFile(FILE_NAME).delete()
    }

    @Test
    fun savedFailuresAreLoadedBackAndAnEmptyListClearsThem() = runTest {
        val failures = listOf(
            FailedImport("content://a/1", "apuntes-cap3.rar", isUnsupported = true),
            FailedImport("content://a/2", null, isUnsupported = false),
        )

        store.save(failures)
        assertEquals(failures, store.load())

        store.save(emptyList())
        assertEquals(emptyList<FailedImport>(), store.load())
    }

    private companion object {
        const val FILE_NAME = "failed_imports_test"
    }
}
