package com.tongtongstudio.ami.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.databinding.DialogCustomTimePickerBinding

const val ESTIMATED_TIME_LISTENER_REQUEST_KEY = "estimated_time_user_selection"
const val ESTIMATED_TIME_RESULT_KEY = "estimated_time_user_selection_result"
const val ESTIMATED_TIME_DIALOG_TAG = "estimated_time_selection_tag"

class CustomTimePickerDialogFragment(val title: String) : DialogFragment() {

    private lateinit var binding: DialogCustomTimePickerBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val dialog = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogCustomTimePickerBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            dialog.setView(binding.root)
                .setTitle(title)
                // Add action buttons
                .setPositiveButton(R.string.ok) { _, _ ->
                    onDialogPositiveClick(this)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ ->
                    getDialog()?.cancel()
                }
            dialog.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val values = (0..59).step(5).toList()
        binding.apply {
            hoursPicker.minValue = 0
            hoursPicker.maxValue = 25
            minutesPicker.minValue = 0
            minutesPicker.maxValue = 11
            minutesPicker.displayedValues = values.map { it.toString() }.toTypedArray()
        }
        return binding.root
    }

    private fun onDialogPositiveClick(dialog: CustomTimePickerDialogFragment) {
        val hours: Int = binding.hoursPicker.value
        val minutes: Int =
            binding.minutesPicker.value * 5 // minutesPicker.value return value between 0 and 11
        val result = IntArray(2)
        result[0] = hours
        result[1] = minutes

        dialog.setFragmentResult(
            ESTIMATED_TIME_LISTENER_REQUEST_KEY,
            bundleOf(ESTIMATED_TIME_RESULT_KEY to result)
        )
        dialog.dismiss()
    }
}