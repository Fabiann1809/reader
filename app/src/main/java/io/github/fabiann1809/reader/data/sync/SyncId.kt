package io.github.fabiann1809.reader.data.sync

import java.util.UUID

/** A new id that identifies a record on every device, unlike the local autoincrement `id`. */
fun newSyncId(): String = UUID.randomUUID().toString()
