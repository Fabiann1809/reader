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
import io.github.fabiann1809.reader.data.book.BookOrganizer
import io.github.fabiann1809.reader.data.book.BookRepository
import io.github.fabiann1809.reader.data.book.DefaultBookRepository
import io.github.fabiann1809.reader.data.book.ReadiumToolkit
import io.github.fabiann1809.reader.data.book.folder.DataStoreWatchedFolderStore
import io.github.fabiann1809.reader.data.book.folder.DocumentFolderLister
import io.github.fabiann1809.reader.data.book.folder.FolderLister
import io.github.fabiann1809.reader.data.book.folder.WatchedFolderManager
import io.github.fabiann1809.reader.data.book.folder.WatchedFolderStore
import io.github.fabiann1809.reader.data.book.folder.WatchedFolderSync
import io.github.fabiann1809.reader.data.book.folder.WorkManagerFolderSyncScheduler
import io.github.fabiann1809.reader.data.book.importing.BookImporter
import io.github.fabiann1809.reader.data.book.importing.DataStoreFailedImportStore
import io.github.fabiann1809.reader.data.book.importing.DefaultBookImporter
import io.github.fabiann1809.reader.data.book.importing.FileAccess
import io.github.fabiann1809.reader.data.book.importing.ImportQueue
import io.github.fabiann1809.reader.data.book.importing.PersistedFileAccess
import io.github.fabiann1809.reader.data.book.importing.ReadiumBookFileReader
import io.github.fabiann1809.reader.data.collection.CollectionRepository
import io.github.fabiann1809.reader.data.collection.DefaultCollectionRepository
import io.github.fabiann1809.reader.data.bookmark.BookmarkRepository
import io.github.fabiann1809.reader.data.bookmark.DefaultBookmarkRepository
import io.github.fabiann1809.reader.data.note.DefaultNoteRepository
import io.github.fabiann1809.reader.data.note.NoteRepository
import io.github.fabiann1809.reader.data.prefs.AppPreferences
import io.github.fabiann1809.reader.data.prefs.DataStoreAppPreferences
import io.github.fabiann1809.reader.data.prefs.DataStoreReadingPreferences
import io.github.fabiann1809.reader.data.prefs.ReadingPreferences
import io.github.fabiann1809.reader.data.reader.ReaderSession
import io.github.fabiann1809.reader.data.reader.ReadiumReaderSession
import io.github.fabiann1809.reader.ocr.MlKitTextRecognizer
import io.github.fabiann1809.reader.ocr.TextRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val readium: ReadiumToolkit by lazy { ReadiumToolkit(appContext) }

    val readerSession: ReaderSession by lazy { ReadiumReaderSession(readium, bookFiles) }

    val bookRepository: BookRepository by lazy { DefaultBookRepository(database.bookDao(), bookFiles) }

    // App-wide work that must outlive any screen (e.g. an import started from another app's share).
    // Main dispatcher: the import queue keeps its state on the main thread; the slow parts switch to IO.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val bookImporter: BookImporter by lazy {
        DefaultBookImporter(
            contentResolver = appContext.contentResolver,
            bookFiles = bookFiles,
            fileReader = ReadiumBookFileReader(readium),
            bookRepository = bookRepository,
            untitled = appContext.getString(R.string.book_untitled),
        )
    }

    private val fileAccess: FileAccess by lazy { PersistedFileAccess(appContext.contentResolver) }

    val importQueue: ImportQueue by lazy {
        ImportQueue(
            importer = bookImporter,
            scope = applicationScope,
            fileAccess = fileAccess,
            store = DataStoreFailedImportStore(appContext),
        )
    }

    // The watched folder (T10.6): its store and lister are shared by the background check and the screen.
    private val watchedFolderStore: WatchedFolderStore by lazy { DataStoreWatchedFolderStore(appContext) }
    private val folderLister: FolderLister by lazy { DocumentFolderLister(appContext) }

    val watchedFolderSync: WatchedFolderSync by lazy { WatchedFolderSync(watchedFolderStore, folderLister, bookImporter) }

    val watchedFolderManager: WatchedFolderManager by lazy {
        WatchedFolderManager(
            store = watchedFolderStore,
            lister = folderLister,
            folderAccess = fileAccess,
            scheduler = WorkManagerFolderSyncScheduler(appContext),
        )
    }

    val noteRepository: NoteRepository by lazy { DefaultNoteRepository(database.noteDao()) }

    val bookmarkRepository: BookmarkRepository by lazy { DefaultBookmarkRepository(database.bookmarkDao()) }

    val collectionRepository: CollectionRepository by lazy {
        DefaultCollectionRepository(database.collectionDao(), database.bookDao())
    }

    val bookOrganizer: BookOrganizer by lazy { BookOrganizer(bookRepository, collectionRepository) }

    val apiKeyStore: ApiKeyStore by lazy { KeystoreApiKeyStore(appContext) }

    val appPreferences: AppPreferences by lazy { DataStoreAppPreferences(appContext) }

    val readingPreferences: ReadingPreferences by lazy { DataStoreReadingPreferences(appContext) }

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
