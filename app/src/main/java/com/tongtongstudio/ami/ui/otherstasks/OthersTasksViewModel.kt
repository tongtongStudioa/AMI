package com.tongtongstudio.ami.ui.otherstasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.LaterFilter
import com.tongtongstudio.ami.data.PreferencesManager
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import java.util.*
import javax.inject.Inject

@HiltViewModel
class OthersTasksViewModel @Inject constructor(
    private val repository: Repository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    var laterFilter: LaterFilter? = null
    private val preferencesFlow = preferencesManager.filterPreferencesFlow

    private val endOfToday = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        timeInMillis
    }

    private val endOfTomorrow = Calendar.getInstance().run {
        timeInMillis = endOfToday
        add(Calendar.DAY_OF_MONTH, 1)
        timeInMillis
    }

    private val endOfWeek = Calendar.getInstance().run {
        timeInMillis = endOfToday
        add(Calendar.DAY_OF_MONTH, 7)
        timeInMillis
    }

    val preferencesLiveData = preferencesFlow.asLiveData()

    private val thingsToDoFlow = preferencesFlow
        .flatMapLatest { filterPreferences ->
            when (filterPreferences.filter) {
                LaterFilter.TOMORROW -> repository.getLaterThingsToDo(endOfToday, endOfTomorrow)
                LaterFilter.NEXT_WEEK -> repository.getLaterThingsToDo(endOfToday, endOfWeek)
                LaterFilter.LATER -> repository.getLaterThingsToDo(endOfToday, null)
            }

        }

    val otherThingsToDo = thingsToDoFlow.asLiveData()
}