package com.tongtongstudio.ami.ui.dialog.linkproject

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.AttributeListener
import com.tongtongstudio.ami.adapter.EditAttributesAdapter
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.DialogEditProjectLinkedBinding
import dagger.hilt.android.AndroidEntryPoint

const val PROJECT_LINKED_LISTENER_REQUEST_KEY = "project_linked_listener_request_key"
const val PROJECT_LINKED_RESULT_KEY = "project_linked_result_key"
const val PROJECT_ID = "project_id"
const val CURRENT_PROJECT_ID_REQUEST_KEY = "current_project_id_request_key"

@AndroidEntryPoint
class EditProjectLinkedDialogFragment : DialogFragment() {
    private lateinit var binding: DialogEditProjectLinkedBinding
    private val viewModel: EditProjectLinkedViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val dialog = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogEditProjectLinkedBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            dialog.setView(binding.root)
                .setTitle("Edit link's project")
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

        val projectLinkAdapter = EditAttributesAdapter(object : AttributeListener<Ttd> {
            override fun onItemClicked(attribute: Ttd) {
                viewModel.changeProjectId(attribute.id)
                onMainTaskSelected(this@EditProjectLinkedDialogFragment)
            }

            override fun onRemoveCrossClick(attribute: Ttd) {
                viewModel.removeProjectId()
            }
        }) { binding, composedTask ->
            binding.titleOverview.text = composedTask.title
        }

        binding.rvProjects.apply {
            adapter = projectLinkAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        setFragmentResultListener(CURRENT_PROJECT_ID_REQUEST_KEY) { _, bundle ->
            viewModel.changeProjectId(bundle.getLong(PROJECT_ID))
            binding.rvProjects.apply {
                adapter = projectLinkAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }
        }
        /*viewModel.projectId.observe(viewLifecycleOwner) {
            projectLinkAdapter.actionBindView = { binding, composedTask ->
                binding.titleOverview.text = composedTask.title
            }
        }*/
        viewModel.projects.observe(viewLifecycleOwner) {
            projectLinkAdapter.submitList(it)
        }
        return binding.root
    }

    private fun onMainTaskSelected(dialog: EditProjectLinkedDialogFragment) {
        val result = if (viewModel.projectId.value != null) viewModel.projectId.value else 0
        dialog.setFragmentResult(
            PROJECT_LINKED_LISTENER_REQUEST_KEY,
            bundleOf(PROJECT_LINKED_RESULT_KEY to result)
        )
        dialog.dismiss()
    }
}