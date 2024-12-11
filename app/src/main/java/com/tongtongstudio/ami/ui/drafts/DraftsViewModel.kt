package com.tongtongstudio.ami.ui.drafts

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.ThingToDo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DraftsViewModel @Inject constructor(
    repository: Repository,
) : ViewModel() {
    val draftTasks: LiveData<List<ThingToDo>> = repository.getDraftsTasks().asLiveData()

}