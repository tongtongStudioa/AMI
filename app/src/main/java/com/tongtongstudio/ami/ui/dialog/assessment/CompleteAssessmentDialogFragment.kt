package com.tongtongstudio.ami.ui.dialog.assessment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.DialogCompleteAssessmentBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CompleteAssessmentDialogFragment : DialogFragment() {

    private lateinit var binding: DialogCompleteAssessmentBinding
    private val viewModel: CompleteAssessmentViewModel by viewModels()
    private lateinit var positiveButton: Button
    private val args: CompleteAssessmentDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let { fragmentActivity ->
            val dialogBuilder = MaterialAlertDialogBuilder(fragmentActivity)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogCompleteAssessmentBinding.inflate(inflater)
            dialogBuilder.setView(binding.root)
                .setTitle(getString(R.string.complete_assessment_dialog_title))
                // Add action buttons
                .setPositiveButton(R.string.ok) { _, _ ->
                    onDialogPositiveClick(this)
                }
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ ->
                    dialog?.cancel()
                }
            dialogBuilder.create()
            /*val alertDialog = dialogBuilder.create()
            alertDialog.setOnShowListener {
                positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.isEnabled = false
            }
            alertDialog*/
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onDialogPositiveClick(dialog: CompleteAssessmentDialogFragment) {
        viewModel.saveCompletedAssessment()
        // TODO: navigate back with result
        Snackbar.make(
            dialog.requireView(),
            getString(R.string.assessment_completed),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            btnMinus.setOnClickListener {
                viewModel.remove1()
            }
            btnPlus.setOnClickListener {
                viewModel.add1()
            }
        }
        viewModel.assessment = args.assessment
        viewModel.assessment?.let {
            fillContent(it)
        }

        // TODO: add commentary
        viewModel.result.observe(viewLifecycleOwner) {
            binding.inputLayoutResult.editText?.setText(it.toString())
        }

        // TODO: problem with input layout result and result observer because each elements modify the other
        // input result
        /*binding.inputLayoutResult.editText?.addTextChangedListener {
            if (it.toString() != "" && it.toString() != "null") {
                viewModel.updateResult(it.toString().toInt())
                //positiveButton.isEnabled = true
            }
        }*/
    }

    private fun fillContent(assessment: Assessment) {
        binding.apply {
            // assessment's goalTitle
            tvTitleAssessment.text = (assessment.title).replaceFirst(
                assessment.title.first(),
                assessment.title.first().uppercaseChar()
            )
            // evaluation description
            tvAssessmentDescription.text = assessment.description
            tvAssessmentDescription.isVisible = assessment.description != null

            // evaluation target goal
            tvTargetGoal.text =
                getString(R.string.target_goal, assessment.goal.toString(), assessment.unit)
        }
    }
}