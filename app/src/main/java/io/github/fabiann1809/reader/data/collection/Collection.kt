package io.github.fabiann1809.reader.data.collection

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** A collection created by the user (e.g. "Para el trabajo"). Built-in collections are [SmartCollection]s. */
@Entity(tableName = "collections")
@Serializable
data class Collection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    // Which of the four pastel colors its band and dot use (0 until [COLOR_COUNT]).
    @ColumnInfo(defaultValue = "0")
    val colorIndex: Int = 0,
    // Epoch milliseconds.
    val createdAt: Long = System.currentTimeMillis(),
)

const val COLOR_COUNT = 4
