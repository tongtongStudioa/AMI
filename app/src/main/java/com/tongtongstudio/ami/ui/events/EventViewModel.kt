package com.tongtongstudio.ami.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.PreferencesManager
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repository: Repository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val preferencesFlow = preferencesManager.filterPreferencesFlow

    // TODO: 04/04/2023 remove filter preferences for events or add a local menu
    private val eventsFlow = preferencesFlow
        .flatMapLatest { filterPreferences ->
            repository.getAllEvents(
                filterPreferences.hideCompleted,
                filterPreferences.sortOrder
            )
        }
    val events = eventsFlow.asLiveData()
}