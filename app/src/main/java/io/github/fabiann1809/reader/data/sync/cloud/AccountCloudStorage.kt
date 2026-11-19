package io.github.fabiann1809.reader.data.sync.cloud

import io.github.fabiann1809.reader.data.auth.AuthRepository
import java.io.File

/**
 * The cloud space of whoever is signed in (T18.9c): each call goes to the storage with that account's
 * [CloudNamespace], so the same Drive holds a separate library per Reader account. With nobody signed in
 * every call fails and touches nothing. [create] builds the storage for a prefix.
 */
class AccountCloudStorage(
    private val auth: AuthRepository,
    private val create: (prefix: String) -> CloudStorage,
) : CloudStorage {

    private val byPrefix = mutableMapOf<String, CloudStorage>()

    private suspend fun <T> onStorage(block: suspend (CloudStorage) -> Result<T>): Result<T> {
        val uid = auth.currentUser.value?.uid ?: return Result.failure(SyncError.NotAuthorized("Nobody is signed in"))
        val prefix = CloudNamespace.prefixFor(uid)
        return block(synchronized(byPrefix) { byPrefix.getOrPut(prefix) { create(prefix) } })
    }

    override suspend fun readManifest() = onStorage { it.readManifest() }

    override suspend fun writeManifest(content: String, expectedVersion: String?) =
        onStorage { it.writeManifest(content, expectedVersion) }

    override suspend fun listFiles() = onStorage { it.listFiles() }

    override suspend fun uploadFile(name: String, file: File) = onStorage { it.uploadFile(name, file) }

    override suspend fun downloadFile(name: String, into: File) = onStorage { it.downloadFile(name, into) }

    override suspend fun deleteFile(name: String) = onStorage { it.deleteFile(name) }

    override suspend fun adoptLegacyFiles() = onStorage { it.adoptLegacyFiles() }
}
