package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.book.CoverStore

/** Pretends to store covers: "content://x" becomes "covers/x.jpg", unless told to fail. */
class FakeCoverStore : CoverStore {
    var fails = false
    val stored = mutableSetOf<String>()

    override suspend fun save(uri: String): String? {
        if (fails) return null
        return "covers/${uri.substringAfterLast('/')}.jpg".also { stored += it }
    }

    override fun delete(coverPath: String?) {
        stored.remove(coverPath)
    }
}
