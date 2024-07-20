package com.tongtongstudio.ami.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.task.MissedTaskAdapter
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.DialogInformationMissedRecurringTasksBinding
import com.tongtongstudio.ami.ui.MainViewModel

class MissedRecurringTasksDialogFragment : DialogFragment() {

    private lateinit var binding: DialogInformationMissedRecurringTasksBinding
    private lateinit var sharedViewModel: MainViewModel
    private var missedTasks: Array<Ttd>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            sharedViewModel = ViewModelProvider(it)[MainViewModel::class.java]
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogInformationMissedRecurringTasksBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(binding.root)
                .setTitle(getString(R.string.missed_tasks_dialog_title))
                // Add action buttons
                .setPositiveButton(R.string.ok) { _, _ ->
                    onDialogPositiveClick(this)
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        missedTasks = arguments?.let {
            MissedRecurringTasksDialogFragmentArgs.fromBundle(it).missedRecurringTasks
        }
        val dialogAdapter = MissedTaskAdapter()
        binding.rvMissedTasks.apply {
            adapter = dialogAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
        if (missedTasks != null) {
            binding.information.text =
                getString(R.string.text_nb_missed_recurring_tasks, missedTasks!!.size)
            dialogAdapter.swapData(missedTasks!!.toList())
        }
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        sharedViewModel.updateRecurringTasksMissed(missedTasks!!.toList())
    }

    private fun onDialogPositiveClick(dialog: MissedRecurringTasksDialogFragment) {
        dialog.dismiss()
    }
}