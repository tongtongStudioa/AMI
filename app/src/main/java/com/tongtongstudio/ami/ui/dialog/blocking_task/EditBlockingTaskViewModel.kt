package com.tongtongstudio.ami.ui.dialog.blocking_task

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditBlockingTaskViewModel @Inject constructor(
    val repository: Repository
) : ViewModel() {

    fun updateBlockingTask(blockingTask: Task?) {
        _blockingTask.value = blockingTask
    }
    fun removeBlockingTask() {
        _blockingTask.value = null
    }

    private val _blockingTask= MutableLiveData<Task?>()
    val blockingTask: LiveData<Task?>
        get() = _blockingTask

    fun potentialBlockingTasks(taskIdToRemove: Long?): LiveData<List<Task>> {
        return repository.getPotentialLinkedTasks(taskIdToRemove).asLiveData()
    }
}