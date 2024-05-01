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

enum class SortOrder { BY_NAME, BY_EISENHOWER_MATRIX, BY_2MINUTES_RULES, BY_EAT_THE_FROG, BY_CREATOR_SORT, BY_DEADLINE }
enum class LaterFilter { TOMORROW, NEXT_WEEK, LATER }
enum class LayoutMode { EXTENT, SIMPLIFIED }

data class FilterPreferences(
    val sortOrder: SortOrder,
    val hideCompleted: Boolean,
    val filter: LaterFilter
)

data class LayoutPreferences(
    val layoutMode: LayoutMode
)

private const val USER_PREFERENCES_NAME = "user_preferences"

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val Context.dataStore by preferencesDataStore(
        name = USER_PREFERENCES_NAME
    )
    private val dataStore = context.dataStore

    // where preferences are stocked
    val filterPreferencesFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_CREATOR_SORT.name
            )
            val hideCompleted = preferences[PreferencesKeys.HIDE_COMPLETED] ?: false

            val filter = LaterFilter.valueOf(
                preferences[PreferencesKeys.FILTER] ?: LaterFilter.LATER.name
            )

            FilterPreferences(sortOrder, hideCompleted, filter)
        }

    val globalPreferencesFlow = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val layoutMode = LayoutMode.valueOf(
                preferences[PreferencesKeys.LAYOUT_MODE] ?: LayoutMode.SIMPLIFIED.name
            )
            LayoutPreferences(layoutMode)
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

    suspend fun updateLayoutMode(layoutMode: LayoutMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAYOUT_MODE] = layoutMode.name
        }
    }

    private object PreferencesKeys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
        val FILTER = stringPreferencesKey("later_filter")
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
    }
}