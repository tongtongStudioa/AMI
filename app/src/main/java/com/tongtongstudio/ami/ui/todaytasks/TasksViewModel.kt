package com.tongtongstudio.ami.ui.todaytasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.PreferencesManager
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: Repository,
    preferencesManager: PreferencesManager,
) : ViewModel() {
    val preferencesFlow = preferencesManager.filterPreferencesFlow

    val startOfToday = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private val endOfToday = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        timeInMillis
    }

    private val todayThingsToDoFlow = preferencesFlow
        .flatMapLatest { filterPreferences ->
            repository.getThingsToDoToday(
                filterPreferences.sortOrder,
                filterPreferences.hideCompleted,
                startOfToday,
                endOfToday
            )
        }

    val todayThingsToDo = todayThingsToDoFlow.asLiveData()
    val upcomingTasksCount: LiveData<Int> =
        repository.getUpcomingTasksCount(endOfToday).asLiveData()

}

