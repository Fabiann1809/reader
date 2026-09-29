package io.github.fabiann1809.reader

import android.content.Context
import io.github.fabiann1809.reader.ai.AiProvider
import io.github.fabiann1809.reader.ai.gemini.GeminiProvider
import io.github.fabiann1809.reader.data.AppDatabase
import io.github.fabiann1809.reader.data.apikey.ApiKeyStore
import io.github.fabiann1809.reader.data.apikey.KeystoreApiKeyStore
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.DefaultBookRepository
import io.github.fabiann1809.reader.data.note.DefaultNoteRepository
import io.github.fabiann1809.reader.data.note.NoteRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency injection: builds the app-wide singletons once.
 * Everything is lazy so nothing is created (e.g. the database) until first use.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    val bookRepository: BookRepository by lazy { DefaultBookRepository(database.bookDao()) }

    val noteRepository: NoteRepository by lazy { DefaultNoteRepository(database.noteDao()) }

    val apiKeyStore: ApiKeyStore by lazy { KeystoreApiKeyStore(appContext) }

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
            // Temporary instruction; replaced by the Feynman explainer prompt in T6.1.
            systemInstruction = "Explica el siguiente texto de forma breve y sencilla, en el mismo idioma.",
            httpClient = httpClient,
        )
    }
}
