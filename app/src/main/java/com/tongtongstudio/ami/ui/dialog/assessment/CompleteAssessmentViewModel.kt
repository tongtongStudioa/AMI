package com.tongtongstudio.ami.ui.dialog.assessment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Assessment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompleteAssessmentViewModel @Inject constructor(
    val repository: Repository,
    stateHandle: SavedStateHandle
) : ViewModel() {

    var assessment: Assessment? = null //stateHandle.get<Assessment>("assessment")

    private val _result = MutableLiveData(0F)
    val result: LiveData<Float>
        get() = _result

    fun saveCompletedAssessment() = viewModelScope.launch {
        assessment?.let {
            repository.updateAssessment(
                it.copy(
                    score = result.value
                )
            )
        }
    }

    fun updateResult(result: Float) {
        _result.value = result
    }

    fun remove1() {
        if (_result.value!! > 0)
            _result.value = _result.value?.minus(1)
    }

    fun add1() {
        _result.value = _result.value?.plus(1)
    }
}