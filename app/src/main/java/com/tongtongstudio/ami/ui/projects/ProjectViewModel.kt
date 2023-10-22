package com.tongtongstudio.ami.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.PreferencesManager
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val repository: Repository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val preferencesFlow = preferencesManager.preferencesFlow

    private val projectsFlow = preferencesFlow
        .flatMapLatest { filterPreferences ->
            repository.getAllProjects(
                filterPreferences.sortOrder,
                filterPreferences.hideCompleted,
            )
        }

    val projects = projectsFlow.asLiveData()
}