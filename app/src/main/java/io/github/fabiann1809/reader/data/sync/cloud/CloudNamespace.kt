package io.github.fabiann1809.reader.data.sync.cloud

import java.security.MessageDigest

/**
 * Keeps each Reader account's files apart inside one Drive (T18.9c): the manifest and every file of an
 * account carry this prefix, so two accounts on the same phone, even with the same Google account
 * behind Drive, never see each other's library. It is derived from the account's uid, which doesn't
 * change between devices, so the account finds its files on all of them.
 */
object CloudNamespace {

    private const val HASH_LENGTH = 20

    /** Starts with a letter no old (unprefixed) file name starts with, so the two are told apart. */
    fun prefixFor(userId: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(userId.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "u${hash.take(HASH_LENGTH)}-"
    }
}
