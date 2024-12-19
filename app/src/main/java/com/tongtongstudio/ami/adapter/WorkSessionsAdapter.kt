package com.tongtongstudio.ami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.WorkSession
import com.tongtongstudio.ami.databinding.ItemWorkSessionBinding
import com.tongtongstudio.ami.timer.TrackingTimeUtility

class WorkSessionsAdapter(val listener: WorkSessionListener) :
    RecyclerView.Adapter<ViewHolder<WorkSession>>() {

    private val workSessionList = mutableListOf<WorkSession>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<WorkSession> {
        val context = parent.context
        LayoutInflater.from(context).inflate(R.layout.item_work_session, parent, false)
        val binding = ItemWorkSessionBinding.inflate(LayoutInflater.from(context), parent, false)
        return WorkSessionViewHolder(binding)
    }

    inner class WorkSessionViewHolder(val binding: ItemWorkSessionBinding) :
        ViewHolder<WorkSession>(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val workSession = workSessionList[position]
                    listener.onClick(workSession)
                }
            }
            binding.btnDeleteSession.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val workSession = workSessionList[position]
                    listener.onRemoveClick(workSession)
                }
            }
        }

        override fun bind(data: WorkSession) {
            binding.apply {
                tvSessionDate.text = Task.getDateFormatted(data.date)
                tvElapsedTime.text =
                    TrackingTimeUtility.getFormattedWorkingTime(data.duration) ?: "no_information"
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder<WorkSession>, position: Int) {
        val element = workSessionList[position]
        holder.bind(element)
    }

    override fun getItemCount(): Int {
        return workSessionList.size
    }

    fun getWorkSessionList(): List<WorkSession> {
        return workSessionList
    }

    fun submitList(newGoalsList: List<WorkSession>) {
        workSessionList.clear()
        workSessionList.addAll(newGoalsList)
        notifyDataSetChanged()
    }
}
