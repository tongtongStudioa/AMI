package com.tongtongstudio.ami.ui.dialog.assessment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.domain.usecase.ScheduleAssessmentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

const val THREE_DAYS_MILLIS: Long = 3 * 24 * 3600 * 1000L
@HiltViewModel
class CompleteAssessmentViewModel @Inject constructor(
    val repository: Repository,
    val scheduleAssessmentUseCase: ScheduleAssessmentUseCase
) : ViewModel() {

    var assessment: Assessment? = null //stateHandle.get<Assessment>("assessment")

    private val _result = MutableLiveData(0F)
    val result: LiveData<Float>
        get() = _result

    private val _comment = MutableLiveData<String?>(null)
    val comment: LiveData<String?>
        get() = _comment
    fun saveCompletedAssessment() = viewModelScope.launch(Dispatchers.IO) {
        assessment?.let {
            repository.updateAssessment(
                it.copy(
                    comment = comment.value,
                    score = result.value
                )
            )
        }
    }

    fun updateComment(comment: String?) {
        _comment.value = comment
    }

    fun removeOne() {
        if (_result.value!! > 0)
            _result.value = _result.value?.minus(1)
    }

    fun addOne() {
        _result.value = _result.value?.plus(1)
    }

    fun delayAssessment() = viewModelScope.launch(Dispatchers.IO) {
        assessment?.let {
            scheduleAssessmentUseCase(listOf(it.copy(dueDate = System.currentTimeMillis() + THREE_DAYS_MILLIS)))
        }
    }
}