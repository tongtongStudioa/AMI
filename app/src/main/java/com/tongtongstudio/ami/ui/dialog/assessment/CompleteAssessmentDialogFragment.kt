package com.tongtongstudio.ami.ui.dialog.assessment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.DialogCompleteAssessmentBinding
import com.tongtongstudio.ami.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CompleteAssessmentDialogFragment : DialogFragment() {

    private lateinit var binding: DialogCompleteAssessmentBinding
    private val viewModel: CompleteAssessmentViewModel by viewModels()
    private val sharedViewModel: MainViewModel by activityViewModels()
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
                    onDialogNegativeButton(this)
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

    private fun onDialogNegativeButton(dialogFragment: CompleteAssessmentDialogFragment) {
        viewModel.delayAssessment()
        //Log.i("SHOW SNACK BAR COMPLETED", "launch event !")
        sharedViewModel.showIsAssessmentCompleted(false)
    }

    private fun onDialogPositiveClick(dialog: CompleteAssessmentDialogFragment) {
        viewModel.saveCompletedAssessment()
        sharedViewModel.showIsAssessmentCompleted(true)
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
                viewModel.removeOne()
            }
            btnPlus.setOnClickListener {
                viewModel.addOne()
            }
        }
        viewModel.assessment = args.assessment
        viewModel.assessment?.let {
            fillContent(it)
        }

        binding.inputLayoutResult.editText?.doOnTextChanged { commentChar, _, _, _ ->
            if (commentChar?.isNotBlank() == true)
                viewModel.updateComment(
                    commentChar.toString()
                )
        }
        viewModel.result.observe(viewLifecycleOwner) {
            binding.inputLayoutResult.editText?.setText(it.toString())
        }
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

            // evaluation target targetScore
            tvTargetGoal.text =
                getString(R.string.target_goal, assessment.targetGoal, assessment.unit)
        }
    }
}