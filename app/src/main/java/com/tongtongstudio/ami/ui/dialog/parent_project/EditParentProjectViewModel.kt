package com.tongtongstudio.ami.ui.dialog.parent_project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditParentProjectViewModel @Inject constructor(
    val repository: Repository
) : ViewModel() {

    fun updateParentProject(projectId: Task?) {
        _parentProject.value = projectId
    }
    fun removeParentProject() {
        _parentProject.value = null
    }

    private val _parentProject= MutableLiveData<Task?>()
    val parentProject: LiveData<Task?>
        get() = _parentProject
    private val _currentTaskId = MutableLiveData<Long?>(null)
    val currentTaskId: LiveData<Long?>
        get() = _currentTaskId

    fun updateCurrentTaskId(taskId: Long?) {
        _currentTaskId.value = if (taskId == -1L) null else taskId
    }

    fun potentialProjects(taskIdToRemove: Long?): LiveData<List<Task>> {
        return repository.getPotentialLinkedTasks(taskIdToRemove).asLiveData()
    }
}