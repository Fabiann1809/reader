package io.github.fabiann1809.reader.data.sync

import androidx.room.Dao
import androidx.room.Query

@Dao
interface SyncTombstoneDao {

    /** Every deletion still remembered. */
    @Query("SELECT * FROM sync_tombstones")
    suspend fun getAll(): List<SyncTombstone>
}
