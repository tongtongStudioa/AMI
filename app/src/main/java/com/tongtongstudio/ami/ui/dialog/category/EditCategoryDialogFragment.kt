package com.tongtongstudio.ami.ui.dialog.category

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.AttributeListener
import com.tongtongstudio.ami.adapter.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Category
import com.tongtongstudio.ami.databinding.DialogEditCategoryBinding
import dagger.hilt.android.AndroidEntryPoint

const val CATEGORY_EDIT_TAG = "category_edit_tag"

@AndroidEntryPoint
class EditCategoryDialogFragment : DialogFragment() {

    private var isNewCategory: Boolean = true
    private val viewModel: EditCategoryViewModel by viewModels()
    private lateinit var binding: DialogEditCategoryBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogEditCategoryBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(binding.root)
                .setTitle("Edit an category")
                // Add action buttons
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, _ ->
                    dialog?.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding.apply {

            // all categories
            val categoriesAdapter =
                EditAttributesAdapter(object : AttributeListener<Category> {
                    override fun onItemClicked(attribute: Category) {
                        viewModel.onCategorySelected(attribute)
                        fillOutInputLayout(attribute)
                    }

                    override fun onRemoveCrossClick(attribute: Category) {
                        viewModel.removeCategory(attribute)
                    }
                }) { binding, category ->
                    binding.titleOverview.text = category.title
                }
            viewModel.categories.observe(viewLifecycleOwner) {
                categoriesAdapter.submitList(it)
            }
            categoriesRv.apply {
                adapter = categoriesAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(false)
            }

            // check if we update or create a category
            viewModel.category.observe(viewLifecycleOwner) {
                isNewCategory = it == null
                if (isNewCategory) {
                    binding.btnSaveEdit.text = "Save"
                    binding.apply {
                        categoryTitle.editText?.setText("")
                        categoryDescription.editText?.setText("")
                    }
                } else {
                    binding.btnSaveEdit.text = "Update"
                }
            }

            // category's title
            categoryTitle.editText?.doOnTextChanged { text, _, _, _ ->
                categoryTitle.error = null
                val title = text.toString()
                viewModel.title = if (title != "") title.replaceFirst(
                    title.first(),
                    title.first().uppercaseChar()
                ) else ""
            }
            // category's description
            categoryDescription.editText?.doOnTextChanged { text, start, before, count ->
                viewModel.description = text.toString()
            }

            btnSaveEdit.setOnClickListener {
                if (validateCategoryTitle())
                    viewModel.safeSave(isNewCategory)
            }

            btnClearInputLayout.setOnClickListener {
                viewModel.clearCategory()
            }
        }

        return binding.root
    }

    private fun fillOutInputLayout(category: Category) {
        binding.apply {
            categoryTitle.editText?.setText(category.title)
            categoryDescription.editText?.setText(category.description)
        }
    }

    private fun validateCategoryTitle(): Boolean {
        return if (viewModel.title == null || binding.categoryTitle.editText?.text?.isBlank() == true) {
            binding.categoryTitle.error = getString(R.string.error_no_title)
            false
        } else true
    }
}