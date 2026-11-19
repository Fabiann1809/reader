package io.github.fabiann1809.reader.testing

import io.github.fabiann1809.reader.data.sync.LocalChangeSource
import kotlinx.coroutines.flow.MutableSharedFlow

/** A [LocalChangeSource] where the test says when data changes and how recent the newest change is. */
class FakeLocalChangeSource(var lastChange: Long = 0L) : LocalChangeSource {
    val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    override val changes = flow
    override suspend fun lastChangeAt() = lastChange
}
