package com.tongtongstudio.ami.ui.completed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.tongtongstudio.ami.data.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CompletedThingToDoViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {
    val thingsToDoCompleted = repository.getAllThingsToDoCompleted().asLiveData()
}