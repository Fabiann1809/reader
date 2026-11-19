package io.github.fabiann1809.reader.data.sync.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** A file in the app's hidden Drive folder. [version] grows with every change to its content. */
data class DriveFile(val id: String, val name: String, val version: String)

/**
 * The few Google Drive v3 calls the sync needs, all in `appDataFolder`: a space only this app can
 * see, that doesn't show in the user's Drive. Plain REST over OkHttp, no Google client library.
 * The token is sent only as a header and never reaches an error message.
 */
class DriveApi(
    private val httpClient: OkHttpClient,
    private val tokens: AccessTokenProvider,
    baseUrl: String = DEFAULT_BASE_URL,
) {
    private val base: HttpUrl = baseUrl.trimEnd('/').toHttpUrl()

    /** Every file in the folder, following the pages. */
    suspend fun list(): List<DriveFile> {
        val files = mutableListOf<DriveFile>()
        var pageToken: String? = null
        do {
            val url = base.newBuilder().addPathSegments("drive/v3/files")
                .addQueryParameter("spaces", SPACE)
                .addQueryParameter("pageSize", "1000")
                .addQueryParameter("fields", "nextPageToken,files(id,name,version)")
                .apply { pageToken?.let { addQueryParameter("pageToken", it) } }
                .build()
            val body = execute(Request.Builder().url(url)).json()
            files += body.getValue("files").jsonArray.map { it.jsonObject.toDriveFile() }
            pageToken = body["nextPageToken"]?.jsonPrimitive?.contentOrNull
        } while (pageToken != null)
        return files
    }

    suspend fun find(name: String): DriveFile? = list().firstOrNull { it.name == name }

    /** Creates the file [name], or replaces the content of [existing]; returns it with its new version. */
    suspend fun upload(name: String, content: RequestBody, existing: DriveFile?): DriveFile {
        val metadata = buildJsonObject {
            if (existing == null) {
                put("name", name)
                put("parents", JsonArray(listOf(JsonPrimitive(SPACE))))
            }
        }
        val path = if (existing == null) "upload/drive/v3/files" else "upload/drive/v3/files/${existing.id}"
        val initUrl = base.newBuilder().addPathSegments(path)
            .addQueryParameter("uploadType", "resumable")
            .addQueryParameter("fields", FILE_FIELDS)
            .build()
        val start = Request.Builder().url(initUrl)
            .method(if (existing == null) "POST" else "PATCH", metadata.toString().toRequestBody(JSON))
        val sessionUrl = execute(start).use { response ->
            response.header("Location") ?: throw SyncError.Unknown("Drive gave no upload address")
        }
        return execute(Request.Builder().url(sessionUrl).put(content)).json().toDriveFile()
    }

    suspend fun uploadFile(name: String, file: File, existing: DriveFile?): DriveFile =
        upload(name, file.asRequestBody(OCTET_STREAM), existing)

    suspend fun uploadText(name: String, text: String, existing: DriveFile?): DriveFile =
        upload(name, text.toRequestBody(JSON), existing)

    suspend fun readText(id: String): String? =
        executeOrNull(Request.Builder().url(contentUrl(id)))?.use { it.body.string() }

    /** Streams the content into [into] through a temporary file, so a failure leaves nothing half written. */
    suspend fun download(id: String, into: File) {
        val response = executeOrNull(Request.Builder().url(contentUrl(id)))
            ?: throw SyncError.Unknown("The file is not in the cloud")
        val partial = File(into.parentFile, into.name + ".part")
        try {
            response.use { r ->
                withContext(Dispatchers.IO) { partial.outputStream().use { r.body.byteStream().copyTo(it) } }
            }
            Files.move(partial.toPath(), into.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            partial.delete()
        }
    }

    suspend fun delete(id: String) {
        val url = base.newBuilder().addPathSegments("drive/v3/files/$id").build()
        executeOrNull(Request.Builder().url(url).delete())?.close()
    }

    private fun contentUrl(id: String) =
        base.newBuilder().addPathSegments("drive/v3/files/$id").addQueryParameter("alt", "media").build()

    /** Sends the request and turns what Drive answers wrong into a [SyncError]. A 404 is an error here. */
    private suspend fun execute(builder: Request.Builder): Response =
        executeOrNull(builder) ?: throw SyncError.Unknown("Drive could not find it")

    /** Like [execute], but a 404 gives null. */
    private suspend fun executeOrNull(builder: Request.Builder): Response? {
        val request = builder.header("Authorization", "Bearer ${tokens.accessToken()}").build()
        val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
        if (response.isSuccessful) return response
        response.use {
            if (it.code == 404) return null
            throw errorFor(it.code, it.body.string())
        }
    }

    private fun errorFor(code: Int, body: String): SyncError {
        val reason = runCatching {
            Json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("errors")?.jsonArray
                ?.firstOrNull()?.jsonObject?.get("reason")?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        return when {
            code == 401 -> SyncError.NotAuthorized()
            reason == "storageQuotaExceeded" -> SyncError.QuotaExceeded()
            code == 403 && reason in RATE_LIMIT_REASONS -> SyncError.Unknown("Drive asked to slow down (HTTP $code)")
            code == 403 -> SyncError.NotAuthorized("Drive denied access")
            else -> SyncError.Unknown("Drive answered HTTP $code")
        }
    }

    private fun Response.json(): JsonObject = use { Json.parseToJsonElement(it.body.string()).jsonObject }

    private fun JsonObject.toDriveFile() = DriveFile(
        id = getValue("id").jsonPrimitive.content,
        name = this["name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        version = this["version"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )

    companion object {
        const val DEFAULT_BASE_URL = "https://www.googleapis.com"
        private const val SPACE = "appDataFolder"
        private const val FILE_FIELDS = "id,name,version"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val OCTET_STREAM = "application/octet-stream".toMediaType()
        private val RATE_LIMIT_REASONS = setOf("userRateLimitExceeded", "rateLimitExceeded", "backendError")
    }
}
