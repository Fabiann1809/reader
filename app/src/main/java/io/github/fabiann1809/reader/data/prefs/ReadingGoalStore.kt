package io.github.fabiann1809.reader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the daily goal counts (T16.3). */
enum class GoalUnit { MINUTES, PAGES }

/** The daily reading goal: [amount] minutes or pages a day. */
data class ReadingGoal(val unit: GoalUnit = GoalUnit.MINUTES, val amount: Int = DEFAULT_GOAL_MINUTES) {
    companion object {
        // The design's example: 30 minutes a day.
        const val DEFAULT_GOAL_MINUTES = 30
        val AMOUNTS = 1..999
    }
}

interface ReadingGoalStore {
    val goal: Flow<ReadingGoal>

    suspend fun setGoal(goal: ReadingGoal)

    /** The last day (epoch day) the flame celebrated a met goal (T16.5): it does so once a day. */
    val lastCelebratedDay: Flow<Long?>

    suspend fun markCelebrated(epochDay: Long)
}

// Tests pass their own [fileName] so they never touch the app's data.
class DataStoreReadingGoalStore(context: Context, fileName: String = FILE_NAME) : ReadingGoalStore {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    override val goal: Flow<ReadingGoal> = dataStore.data.map { prefs ->
        ReadingGoal(
            unit = GoalUnit.entries.find { it.name == prefs[UNIT] } ?: GoalUnit.MINUTES,
            amount = prefs[AMOUNT] ?: ReadingGoal.DEFAULT_GOAL_MINUTES,
        )
    }

    override suspend fun setGoal(goal: ReadingGoal) {
        dataStore.edit { prefs ->
            prefs[UNIT] = goal.unit.name
            prefs[AMOUNT] = goal.amount.coerceIn(ReadingGoal.AMOUNTS)
        }
    }

    override val lastCelebratedDay: Flow<Long?> = dataStore.data.map { it[CELEBRATED_DAY] }

    override suspend fun markCelebrated(epochDay: Long) {
        dataStore.edit { it[CELEBRATED_DAY] = epochDay }
    }

    private companion object {
        const val FILE_NAME = "reading_goal"
        val UNIT = stringPreferencesKey("unit")
        val AMOUNT = intPreferencesKey("amount")
        val CELEBRATED_DAY = longPreferencesKey("celebrated_day")
    }
}
