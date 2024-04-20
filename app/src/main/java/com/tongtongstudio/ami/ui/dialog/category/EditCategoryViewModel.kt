package com.tongtongstudio.ami.ui.dialog.category

import androidx.lifecycle.*
import com.tongtongstudio.ami.data.Repository
import com.tongtongstudio.ami.data.datatables.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditCategoryViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _category = MutableLiveData<Category?>()
    val category: LiveData<Category?>
        get() = _category

    var title: String? = null
    var description: String? = null

    val categories: LiveData<List<Category>> = repository.getCategories().asLiveData()

    fun removeCategory(category: Category) = viewModelScope.launch {
        repository.deleteCategory(category)
    }

    private fun createNewCategory(category: Category) = viewModelScope.launch {
        repository.insertCategory(category)
    }

    private fun updateCategory(category: Category) = viewModelScope.launch {
        repository.updateCategory(category)
    }

    fun onCategorySelected(category: Category) {
        _category.value = category
    }

    fun clearCategory() {
        _category.value = null
    }

    fun safeSave(isNewCategory: Boolean) {
        if (isNewCategory)
            createNewCategory(
                Category(
                    title = title!!,
                    description = description
                )
            )
        else {
            val updatedCategory = category.value?.copy(
                title = title!!,
                description = description
            )
            updateCategory(updatedCategory!!)
        }
    }

}
