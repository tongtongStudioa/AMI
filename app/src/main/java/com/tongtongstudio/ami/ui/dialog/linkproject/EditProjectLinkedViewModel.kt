package com.tongtongstudio.ami.ui.dialog.linkproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditProjectLinkedViewModel @Inject constructor(
    val repository: Repository
) : ViewModel() {
    fun changeProjectId(id: Long) {
        _projectId.value = id
    }

    fun removeProjectId() {
        _projectId.value = null
    }

    private val _projectId = MutableLiveData<Long?>()
    val projectId: LiveData<Long?>
        get() = _projectId
    private val _currentTaskId = MutableLiveData<Long?>()
    val currentTaskId: LiveData<Long?>
        get() = _currentTaskId

    fun getPotentialsProjects (currentTaskId: Long) {
        repository.getPotentialProjects().asLiveData()
    }
}