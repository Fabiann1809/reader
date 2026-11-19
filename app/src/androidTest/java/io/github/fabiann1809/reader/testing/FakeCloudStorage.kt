package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.sync.cloud.CloudManifest
import io.github.fabiann1809.reader.data.sync.cloud.CloudStorage
import io.github.fabiann1809.reader.data.sync.cloud.SyncError
import java.io.File

/**
 * An in-memory [CloudStorage]. Shared by every device a test simulates, like the real cloud.
 * Set [failWith] to make every call fail, and use [otherDeviceWrites] to change the manifest behind
 * a device's back.
 */
class FakeCloudStorage : CloudStorage {

    var manifest: CloudManifest? = null
        private set

    /** File contents by name. */
    val files = mutableMapOf<String, ByteArray>()

    var failWith: SyncError? = null

    /** Every write in order ("manifest", "upload:name", "delete:name"), to check what went first. */
    val writes = mutableListOf<String>()

    /** Runs once, just before the next manifest write: what another device does while this one works. */
    var beforeNextManifestWrite: (() -> Unit)? = null

    private var versions = 0

    /** What another device does: writes the manifest without the caller knowing. */
    fun otherDeviceWrites(content: String) {
        manifest = CloudManifest(content, nextVersion())
    }

    override suspend fun readManifest(): Result<CloudManifest?> = fail() ?: Result.success(manifest)

    override suspend fun writeManifest(content: String, expectedVersion: String?): Result<String> {
        fail<String>()?.let { return it }
        beforeNextManifestWrite?.let { hook ->
            beforeNextManifestWrite = null
            hook()
        }
        if (manifest?.version != expectedVersion) return Result.failure(SyncError.Conflict())
        val written = CloudManifest(content, nextVersion())
        manifest = written
        writes += "manifest"
        return Result.success(written.version)
    }

    override suspend fun listFiles(): Result<Set<String>> = fail() ?: Result.success(files.keys.toSet())

    override suspend fun uploadFile(name: String, file: File): Result<Unit> {
        fail<Unit>()?.let { return it }
        files[name] = file.readBytes()
        writes += "upload:$name"
        return Result.success(Unit)
    }

    override suspend fun downloadFile(name: String, into: File): Result<Unit> {
        fail<Unit>()?.let { return it }
        val bytes = files[name] ?: return Result.failure(SyncError.Unknown("The file is not in the cloud"))
        into.writeBytes(bytes)
        return Result.success(Unit)
    }

    override suspend fun deleteFile(name: String): Result<Unit> {
        fail<Unit>()?.let { return it }
        files.remove(name)
        writes += "delete:$name"
        return Result.success(Unit)
    }

    private fun nextVersion() = (++versions).toString()

    private fun <T> fail(): Result<T>? = failWith?.let { Result.failure(it) }
}
