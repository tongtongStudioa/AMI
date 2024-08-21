package com.tongtongstudio.ami.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.PreferencesManager
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    repository: Repository,
    preferencesManager: PreferencesManager
) : ViewModel() {

    private val preferencesFlow = preferencesManager.filterPreferencesFlow

    val habits = repository.getHabits().asLiveData()
}