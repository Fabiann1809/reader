package io.github.fabiann1809.reader.data.collection

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A collection created by the user (e.g. "Para el trabajo"). Built-in collections are [SmartCollection]s. */
@Entity(tableName = "collections")
data class Collection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // Epoch milliseconds.
    val createdAt: Long = System.currentTimeMillis(),
)
