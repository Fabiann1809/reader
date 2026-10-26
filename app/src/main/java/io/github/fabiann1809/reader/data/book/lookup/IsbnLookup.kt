package io.github.fabiann1809.reader.data.book.lookup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** What a library catalog knows about a book, found by its ISBN. */
data class BookInfo(val title: String, val author: String, val totalPages: Int?)

sealed interface IsbnResult {
    data class Found(val info: BookInfo) : IsbnResult

    data object NotFound : IsbnResult

    data object NoConnection : IsbnResult
}

fun interface IsbnLookup {
    /** [isbn] is already normalized (see [Isbn.normalize]). */
    suspend fun find(isbn: String): IsbnResult
}

/** Open Library (free, no key). Only the ISBN leaves the phone. */
class OpenLibraryIsbnLookup(private val httpClient: OkHttpClient) : IsbnLookup {

    override suspend fun find(isbn: String): IsbnResult = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$API?bibkeys=ISBN:$isbn&format=json&jscmd=data").build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext IsbnResult.NoConnection
                parseOpenLibrary(response.body.string(), isbn)
            }
        } catch (e: IOException) {
            IsbnResult.NoConnection
        }
    }

    private companion object {
        const val API = "https://openlibrary.org/api/books"
    }
}

/** The answer of Open Library's books API: `{}` when it does not know the ISBN. */
internal fun parseOpenLibrary(body: String, isbn: String): IsbnResult {
    val book = try {
        Json.parseToJsonElement(body).jsonObject["ISBN:$isbn"]?.jsonObject
    } catch (e: IllegalArgumentException) {
        null
    } ?: return IsbnResult.NotFound
    val title = book.text("title") ?: return IsbnResult.NotFound
    val authors = book["authors"]?.jsonArray?.mapNotNull { (it as? JsonObject)?.text("name") }.orEmpty()
    val pages = book["number_of_pages"]?.let { runCatching { it.jsonPrimitive.int }.getOrNull() }?.takeIf { it > 0 }
    return IsbnResult.Found(BookInfo(title = title, author = authors.joinToString(", "), totalPages = pages))
}

private fun JsonObject.text(key: String): String? =
    this[key]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotEmpty() }
