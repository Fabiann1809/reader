package io.github.fabiann1809.reader

import android.content.Context
import io.github.fabiann1809.reader.ai.AiProvider
import io.github.fabiann1809.reader.ai.ExplainText
import io.github.fabiann1809.reader.ai.ExplainerPrompt
import io.github.fabiann1809.reader.ai.gemini.GeminiProvider
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.apikey.ApiKeyStore
import io.github.fabiann1809.reader.data.apikey.KeystoreApiKeyStore
import io.github.fabiann1809.reader.data.book.BookFiles
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.DefaultBookRepository
import io.github.fabiann1809.reader.data.book.importing.BookImporter
import io.github.fabiann1809.reader.data.book.importing.DefaultBookImporter
import io.github.fabiann1809.reader.data.book.importing.ReadiumBookFileReader
import io.github.fabiann1809.reader.data.collection.CollectionRepository
import io.github.fabiann1809.reader.data.collection.DefaultCollectionRepository
import io.github.fabiann1809.reader.data.note.DefaultNoteRepository
import io.github.fabiann1809.reader.data.note.NoteRepository
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import io.github.fabiann1809.reader.data.prefs.DataStoreAppPreferences
import io.github.fabiann1809.reader.ocr.MlKitTextRecognizer
import io.github.fabiann1809.reader.ocr.TextRecognizer
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency injection: builds the app-wide singletons once.
 * Everything is lazy so nothing is created (e.g. the database) until first use.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    private val bookFiles: BookFiles by lazy { BookFiles(appContext.filesDir) }

    val bookRepository: BookRepository by lazy { DefaultBookRepository(database.bookDao(), bookFiles) }

    val bookImporter: BookImporter by lazy {
        DefaultBookImporter(
            contentResolver = appContext.contentResolver,
            bookFiles = bookFiles,
            fileReader = ReadiumBookFileReader(appContext),
            bookRepository = bookRepository,
            untitled = appContext.getString(R.string.book_untitled),
        )
    }

    val noteRepository: NoteRepository by lazy { DefaultNoteRepository(database.noteDao()) }

    val collectionRepository: CollectionRepository by lazy {
        DefaultCollectionRepository(database.collectionDao(), database.bookDao())
    }

    val apiKeyStore: ApiKeyStore by lazy { KeystoreApiKeyStore(appContext) }

    val appPreferences: AppPreferences by lazy { DataStoreAppPreferences(appContext) }

    val textRecognizer: TextRecognizer by lazy { MlKitTextRecognizer(appContext) }

    // No logging interceptor on purpose: requests carry the user's API key.
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            // Model responses can take a while to generate.
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val aiProvider: AiProvider by lazy {
        GeminiProvider(
            apiKeyStore = apiKeyStore,
            systemInstruction = ExplainerPrompt.SYSTEM_INSTRUCTION,
            httpClient = httpClient,
        )
    }

    val explainText: ExplainText by lazy { ExplainText(aiProvider) }
}
