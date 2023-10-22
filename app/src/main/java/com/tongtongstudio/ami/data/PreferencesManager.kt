package com.tongtongstudio.ami.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class SortOrder { BY_NAME, BY_IMPORTANCE_PRIORITY, BY_DEADLINE }
enum class LaterFilter {TOMORROW, NEXT_WEEK, LATER}

data class FilterPreferences(
    val sortOrder: SortOrder,
    val hideCompleted: Boolean,
    val filter: LaterFilter
)

private const val USER_PREFERENCES_NAME = "user_preferences"

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val Context.dataStore by preferencesDataStore(
        name = USER_PREFERENCES_NAME
    )
    private val dataStore = context.dataStore
    // where preferences are stocked
    val preferencesFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_IMPORTANCE_PRIORITY.name
            )
            val hideCompleted = preferences[PreferencesKeys.HIDE_COMPLETED] ?: false

            val filter = LaterFilter.valueOf(preferences[PreferencesKeys.FILTER] ?: LaterFilter.LATER.name
            )

            FilterPreferences(sortOrder, hideCompleted, filter)
        }

    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun updateHideCompleted(hideCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_COMPLETED] = hideCompleted
        }
    }

    suspend fun updateLaterFilter(filter: LaterFilter) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FILTER] = filter.name
        }
    }

    private object PreferencesKeys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
        val FILTER = stringPreferencesKey("later_filter")
    }
}