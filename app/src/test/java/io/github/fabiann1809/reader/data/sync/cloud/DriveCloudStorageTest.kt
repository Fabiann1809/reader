package io.github.fabiann1809.reader.data.sync.cloud

import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit

/** [DriveCloudStorage] against a small simulation of Drive's `appDataFolder` that answers like the real one. */
class DriveCloudStorageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val drive = SimulatedDrive()
    private val server = MockWebServer()
    private var token = "secret-token"
    private lateinit var storage: DriveCloudStorage
    private lateinit var api: DriveApi

    @Before
    fun setUp() {
        server.dispatcher = drive
        server.start()
        drive.baseUrl = server.url("/").toString().trimEnd('/')
        val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build()
        api = DriveApi(client, { token }, drive.baseUrl)
        storage = DriveCloudStorage(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun thereIsNoManifestUntilOneIsWritten() = runTest {
        assertNull(storage.readManifest().getOrThrow())
    }

    @Test
    fun aWrittenManifestComesBackWithItsVersion() = runTest {
        val version = storage.writeManifest("""{"a":1}""", expectedVersion = null).getOrThrow()

        val read = storage.readManifest().getOrThrow()!!

        assertEquals("""{"a":1}""", read.content)
        assertEquals(version, read.version)
    }

    @Test
    fun everyWriteChangesTheVersion() = runTest {
        val first = storage.writeManifest("one", null).getOrThrow()
        val second = storage.writeManifest("two", first).getOrThrow()

        assertNotEquals(first, second)
        assertEquals("two", storage.readManifest().getOrThrow()!!.content)
        assertEquals(1, drive.files.count { it.value.name == "manifest.json" })
    }

    @Test
    fun writingOverAVersionThatChangedIsAConflict() = runTest {
        val seen = storage.writeManifest("mine", null).getOrThrow()
        drive.changeBehindTheBack("manifest.json", "other device")

        val result = storage.writeManifest("mine again", seen)

        assertTrue(result.exceptionOrNull() is SyncError.Conflict)
        assertEquals("other device", storage.readManifest().getOrThrow()!!.content)
    }

    @Test
    fun writingAsIfThereWasNoManifestWhenThereIsOneIsAConflict() = runTest {
        storage.writeManifest("first", null).getOrThrow()

        assertTrue(storage.writeManifest("second", null).exceptionOrNull() is SyncError.Conflict)
    }

    @Test
    fun filesGoUpAndComeBackAndAreListedWithoutTheManifest() = runTest {
        storage.writeManifest("m", null).getOrThrow()
        val book = tmp.newFile("book.epub").apply { writeText("the book") }
        storage.uploadFile("book-1.epub", book).getOrThrow()

        val downloaded = tmp.newFile("copy.epub")
        storage.downloadFile("book-1.epub", downloaded).getOrThrow()

        assertEquals(setOf("book-1.epub"), storage.listFiles().getOrThrow())
        assertEquals("the book", downloaded.readText())
    }

    @Test
    fun uploadingTheSameNameReplacesTheContentInsteadOfDuplicating() = runTest {
        val file = tmp.newFile("c.jpg")
        file.writeText("old")
        storage.uploadFile("cover-1.jpg", file).getOrThrow()
        file.writeText("new")
        storage.uploadFile("cover-1.jpg", file).getOrThrow()

        val copy = tmp.newFile("copy.jpg")
        storage.downloadFile("cover-1.jpg", copy).getOrThrow()

        assertEquals(1, drive.files.size)
        assertEquals("new", copy.readText())
    }

    @Test
    fun deletingAFileRemovesItAndDeletingAMissingOneIsFine() = runTest {
        storage.uploadFile("voice-1.m4a", tmp.newFile("v").apply { writeText("x") }).getOrThrow()

        storage.deleteFile("voice-1.m4a").getOrThrow()
        storage.deleteFile("voice-1.m4a").getOrThrow()

        assertTrue(storage.listFiles().getOrThrow().isEmpty())
    }

    @Test
    fun aMissingFileFailsToDownloadAndLeavesNothingBehind() = runTest {
        val target = tmp.newFile("target")
        target.delete()

        val result = storage.downloadFile("nope.epub", target)

        assertTrue(result.exceptionOrNull() is SyncError.Unknown)
        assertFalse(target.exists())
        assertEquals(emptyList<String>(), tmp.root.list()!!.filter { it.endsWith(".part") })
    }

    @Test
    fun listingFollowsThePages() = runTest {
        repeat(5) { drive.addFile("f$it.bin", "x") }
        drive.pageSize = 2

        assertEquals(5, storage.listFiles().getOrThrow().size)
    }

    @Test
    fun anExpiredTokenIsNotAuthorized() = runTest {
        token = "expired"

        assertTrue(storage.readManifest().exceptionOrNull() is SyncError.NotAuthorized)
    }

    @Test
    fun aFullDriveIsReportedAsQuotaExceeded() = runTest {
        drive.full = true

        val result = storage.writeManifest("m", null)

        assertTrue(result.exceptionOrNull() is SyncError.QuotaExceeded)
    }

    @Test
    fun noNetworkIsNoInternet() = runTest {
        server.close()

        assertTrue(storage.readManifest().exceptionOrNull() is SyncError.NoInternet)
    }

    @Test
    fun theTokenNeverAppearsInAnError() = runTest {
        drive.failWith = 500

        val error = storage.readManifest().exceptionOrNull()!!

        assertFalse(error.message.orEmpty().contains(token))
        assertEquals("Drive answered HTTP 500", error.message)
    }

    @Test
    fun unsafeNamesAreRefusedBeforeAnyRequest() = runTest {
        val file = tmp.newFile("x")

        val result = runCatching { storage.uploadFile("a' or name contains '", file) }

        assertTrue(result.getOrNull()!!.isFailure)
        assertEquals(0, drive.requestCount)
    }

    @Test
    fun twoAccountsKeepSeparateFilesInTheSameDrive() = runTest {
        val ana = DriveCloudStorage(api, "uana-")
        val beto = DriveCloudStorage(api, "ubeto-")
        ana.writeManifest("ana", null).getOrThrow()
        ana.uploadFile("book-1.epub", tmp.newFile("a.epub").apply { writeText("ana book") }).getOrThrow()

        assertNull(beto.readManifest().getOrThrow())
        assertTrue(beto.listFiles().getOrThrow().isEmpty())
        beto.writeManifest("beto", null).getOrThrow()

        assertEquals("ana", ana.readManifest().getOrThrow()!!.content)
        assertEquals(setOf("book-1.epub"), ana.listFiles().getOrThrow())
        assertEquals(setOf("uana-manifest.json", "uana-book-1.epub", "ubeto-manifest.json"), drive.files.values.map { it.name }.toSet())
    }

    @Test
    fun adoptingRenamesTheOldFilesIntoTheAccountsSpaceAndLeavesOthersAlone() = runTest {
        drive.addFile("manifest.json", "old library")
        drive.addFile("book-1.epub", "old book")
        drive.addFile("something-else.bin", "x")
        val ana = DriveCloudStorage(api, "uana-")

        val taken = ana.adoptLegacyFiles().getOrThrow()

        assertEquals(2, taken)
        assertEquals("old library", ana.readManifest().getOrThrow()!!.content)
        assertEquals(setOf("book-1.epub"), ana.listFiles().getOrThrow())
        assertTrue(drive.files.values.any { it.name == "something-else.bin" })
        assertFalse(drive.files.values.any { it.name == "manifest.json" })
    }

    @Test
    fun theSecondAccountFindsNothingLeftToAdopt() = runTest {
        drive.addFile("manifest.json", "old library")
        DriveCloudStorage(api, "uana-").adoptLegacyFiles().getOrThrow()

        val beto = DriveCloudStorage(api, "ubeto-")

        assertEquals(0, beto.adoptLegacyFiles().getOrThrow())
        assertNull(beto.readManifest().getOrThrow())
    }

    @Test
    fun adoptingNeverOverwritesAFileTheAccountAlreadyHas() = runTest {
        val ana = DriveCloudStorage(api, "uana-")
        ana.writeManifest("new library", null).getOrThrow()
        drive.addFile("manifest.json", "old library")

        assertEquals(0, ana.adoptLegacyFiles().getOrThrow())
        assertEquals("new library", ana.readManifest().getOrThrow()!!.content)
    }

    @Test
    fun withoutAPrefixThereIsNothingToAdopt() = runTest {
        drive.addFile("manifest.json", "old library")

        assertEquals(0, storage.adoptLegacyFiles().getOrThrow())
    }

    /** Drive's behavior as far as the sync uses it: a flat folder of files with a version that grows. */
    private class SimulatedDrive : Dispatcher() {
        data class Stored(val id: String, var name: String, var version: Long, var content: String)

        lateinit var baseUrl: String
        val files = mutableMapOf<String, Stored>()
        var pageSize = 1000
        var full = false
        var failWith: Int? = null
        var requestCount = 0
        private var nextId = 0
        // Upload sessions: session id to the file it writes (null while it creates one) and its name.
        private val sessions = mutableMapOf<String, Pair<Stored?, String>>()

        fun addFile(name: String, content: String) {
            val id = "id${++nextId}"
            files[id] = Stored(id, name, 1, content)
        }

        fun changeBehindTheBack(name: String, content: String) {
            files.values.first { it.name == name }.apply {
                this.content = content
                version++
            }
        }

        override fun dispatch(request: RecordedRequest): MockResponse {
            requestCount++
            failWith?.let { return MockResponse.Builder().code(it).body("{}").build() }
            if (request.headers["Authorization"] != "Bearer secret-token") {
                return error(401, "authError")
            }
            val path = request.url.encodedPath
            return when {
                request.method == "GET" && path == "/drive/v3/files" -> list(request)
                request.method == "GET" && request.url.queryParameter("alt") == "media" ->
                    files[path.substringAfterLast('/')]?.let { ok(it.content) } ?: error(404, "notFound")
                request.method == "PATCH" && path.startsWith("/drive/v3/files/") -> rename(request, path)
                request.method == "DELETE" -> {
                    files.remove(path.substringAfterLast('/'))
                    MockResponse.Builder().code(204).build()
                }
                path.startsWith("/upload/drive/v3/files") -> startUpload(request, path)
                request.method == "PUT" && path.startsWith("/session/") -> finishUpload(request, path)
                else -> error(404, "notFound")
            }
        }

        private fun rename(request: RecordedRequest, path: String): MockResponse {
            val stored = files[path.substringAfterLast('/')] ?: return error(404, "notFound")
            stored.name = Regex(""""name":"([^"]+)"""").find(request.body!!.utf8())!!.groupValues[1]
            return ok("""{"id":"${stored.id}","name":"${stored.name}","version":"${stored.version}"}""")
        }

        private fun list(request: RecordedRequest): MockResponse {
            val all = files.values.sortedBy { it.id }
            val from = request.url.queryParameter("pageToken")?.toInt() ?: 0
            val page = all.drop(from).take(pageSize)
            val next = if (from + pageSize < all.size) ""","nextPageToken":"${from + pageSize}"""" else ""
            val items = page.joinToString(",") { """{"id":"${it.id}","name":"${it.name}","version":"${it.version}"}""" }
            return ok("""{"files":[$items]$next}""")
        }

        private fun startUpload(request: RecordedRequest, path: String): MockResponse {
            if (full) return error(403, "storageQuotaExceeded")
            val id = path.substringAfterLast('/').takeIf { it != "files" }
            val existing = id?.let { files[it] }
            val name = Regex(""""name":"([^"]+)"""").find(request.body!!.utf8())?.groupValues?.get(1)
            val session = "s${sessions.size + 1}"
            sessions[session] = existing to (name ?: existing?.name.orEmpty())
            return MockResponse.Builder().code(200).addHeader("Location", "$baseUrl/session/$session").build()
        }

        private fun finishUpload(request: RecordedRequest, path: String): MockResponse {
            val (existing, name) = sessions.getValue(path.substringAfterLast('/'))
            val stored = existing ?: Stored("id${++nextId}", name, 0, "").also { files[it.id] = it }
            stored.content = request.body!!.utf8()
            stored.version++
            return ok("""{"id":"${stored.id}","name":"${stored.name}","version":"${stored.version}"}""")
        }

        private fun ok(body: String) = MockResponse.Builder().code(200).body(body).build()

        private fun error(code: Int, reason: String) =
            MockResponse.Builder().code(code).body("""{"error":{"errors":[{"reason":"$reason"}]}}""").build()
    }
}
