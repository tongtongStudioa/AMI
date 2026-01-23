package com.tongtongstudio.ami.ui.dialog.blocking_task

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.simple.AttributeListener
import com.tongtongstudio.ami.adapter.simple.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.databinding.DialogEditLinkedTaskBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates

const val BLOCKING_TASK_REQUEST_KEY = "BLOCKING_TASK_REQUEST_KEY"
const val BLOCKING_TASK_RESULT_KEY = "BLOCKING_TASK_RESULT_KEY"


@AndroidEntryPoint
class EditBlockingTaskDialogFragment : DialogFragment() {
    private var taskId: Long?= null
    private lateinit var binding: DialogEditLinkedTaskBinding
    val args: EditBlockingTaskDialogFragmentArgs by navArgs()
    private val viewModel: EditBlockingTaskViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.updateBlockingTask(args.blockingTask)
        taskId = args.taskId
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val dialog = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogEditLinkedTaskBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            dialog.setView(binding.root)
                .setTitle(getString(R.string.edit_blocking_task))
                // Add action buttons
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
        val projectLinkAdapter = EditAttributesAdapter(object : AttributeListener<Task> {
            override fun onItemClicked(attribute: Task) {
                viewModel.updateBlockingTask(attribute)
                onBlockingTaskSelected(this@EditBlockingTaskDialogFragment)
            }

            override fun onRemoveCrossClick(attribute: Task) {
                viewModel.removeBlockingTask()
            }
        }) { binding, composedTask ->
            binding.titleOverview.text = composedTask.title
        }

        binding.rvProjects.apply {
            adapter = projectLinkAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        viewModel.blockingTask.observe(viewLifecycleOwner) {
            projectLinkAdapter.actionBindView = { binding, task ->
                binding.titleOverview.text = task.title
            }
        }


        viewModel.potentialBlockingTasks(taskId).observe(viewLifecycleOwner) { listPotentialProjects ->
            projectLinkAdapter.submitList(listPotentialProjects)
        }

        return binding.root
    }

    private fun onBlockingTaskSelected(dialog: EditBlockingTaskDialogFragment) {
        val result = viewModel.blockingTask.value
        dialog.setFragmentResult(
            BLOCKING_TASK_REQUEST_KEY,
            bundleOf(BLOCKING_TASK_RESULT_KEY to result)
        )
        dialog.dismiss()
    }
}