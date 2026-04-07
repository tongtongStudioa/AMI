package com.tongtongstudio.ami.ui.completed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.ThingToDo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CompletedThingToDoViewModel @Inject constructor(
    repository: Repository
) : ViewModel() {
    val thingsToDoCompleted = repository.getCompletedTasks().asLiveData()

    private val _filteredTasks = MutableLiveData<List<ThingToDo>>()
    val filteredTasks: LiveData<List<ThingToDo>> = _filteredTasks

    fun filterTasks(query: String) {
        val currentList: List<ThingToDo> = thingsToDoCompleted.value ?: emptyList()

        _filteredTasks.value = if (query.isBlank()) {
            currentList
        } else {
            currentList.filter {
                it.taskRelations.mainTask.title.contains(query, ignoreCase = true) ||
                        it.taskRelations.mainTask.description?.contains(query, ignoreCase = true) == true
            }
        }
    }
}