package com.tongtongstudio.ami.ui.dialog.work_session

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.PATTERN_FORMAT_DATE
import com.tongtongstudio.ami.data.datatables.WorkSession
import com.tongtongstudio.ami.databinding.DialogAddEditWorkSessionBinding
import com.tongtongstudio.ami.util.CalendarCustomFunction
import com.tongtongstudio.ami.util.showDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val WORK_SESSION_LISTENER_REQUEST_KEY = "WORK_SESSION_LISTENER_REQUEST_KEY"
const val WORK_SESSION_RESULT_KEY = "WORK_SESSION_RESULT_KEY"

@AndroidEntryPoint
class AddEditWorkSessionDialogFragment : DialogFragment() {

    private lateinit var binding: DialogAddEditWorkSessionBinding

    val args: AddEditWorkSessionDialogFragmentArgs by navArgs()

    lateinit var workSession: WorkSession
    var sessionDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        workSession = args.workSession ?: WorkSession(0,0L,null)
        super.onCreate(savedInstanceState)
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val dialog = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            binding = DialogAddEditWorkSessionBinding.inflate(inflater)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            dialog.setView(binding.root)
                .setTitle(R.string.add_working_time)
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

        binding.apply {
            hoursPicker.minValue = 0
            hoursPicker.maxValue = 24
            minutesPicker.minValue = 0
            minutesPicker.maxValue = 59
            secondesPicker.minValue = 0
            secondesPicker.maxValue = 59

            // TODO: little problem with time picker accuracy
            val hours = (workSession.duration / 3600 / 1000).toInt()
            hoursPicker.value = hours
            val minutes = (workSession.duration / 1000 / 60).toInt()
            minutesPicker.value = minutes
            val secondes = (workSession.duration / 1000 % 60).toInt()
            secondesPicker.value = secondes
            inputLayoutComment.editText?.setText(workSession.comment ?: "")
            inputLayoutDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(workSession.date)
            inputLayoutDate.setOnClickListener {
                showDatePicker(
                    workSession.date,
                    CalendarCustomFunction.buildConstraintsForStartDate(Calendar.getInstance().timeInMillis))
                    {
                        sessionDate = it
                        inputLayoutDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(sessionDate)

                    }
            }
        }


        return binding.root
    }

    private fun onDialogPositiveClick(dialog: AddEditWorkSessionDialogFragment) {
        val hours: Int = binding.hoursPicker.value
        val minutes: Int =
            binding.minutesPicker.value
        val secondes: Int = binding.secondesPicker.value
        val duration: Long =
            (hours.toLong() * 60 * 60 * 1000) + (minutes.toLong() * 60 * 1000) + (secondes * 1000)

        val comment = binding.inputLayoutComment.editText?.text.toString()
        //val date = binding.inputLayoutDate.editText?.text?.dateStringToLong()
        workSession = workSession.copy(duration = duration, comment = comment, date = sessionDate ?: workSession.date)
        dialog.setFragmentResult(
            WORK_SESSION_LISTENER_REQUEST_KEY,
            bundleOf(WORK_SESSION_RESULT_KEY to workSession)
        )
        dialog.dismiss()
    }
}