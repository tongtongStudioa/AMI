package com.tongtongstudio.ami.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.DialogEditCategoryBinding

class EditCategoryDialogFragment : DialogFragment() {
    private lateinit var binding: DialogEditCategoryBinding
    private lateinit var title: String
    private var description: String? = null
    private var parentId: Long? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogEditCategoryBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(binding.root)
                .setTitle("Edit an evaluation")
                // Add action buttons
                .setPositiveButton(R.string.ok) { dialog, id ->
                    onDialogPositiveClick(this)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { dialog, id ->
                    getDialog()?.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onDialogPositiveClick(dialog: EditCategoryDialogFragment) {
        //TODO("Not yet implemented")
        dialog.dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.apply {

            // category's title
            if (title != null)
                categoryTitle.editText?.setText(title)
            categoryTitle.editText?.doOnTextChanged { text, _, _, _ ->
                title = text.toString()
            }
            // category's description
            if (description != null)
                categoryDescription.editText?.setText(description)
            categoryDescription.editText?.doOnTextChanged { text, start, before, count ->
                description = text.toString()
            }
        }

        return binding.root
    }
}