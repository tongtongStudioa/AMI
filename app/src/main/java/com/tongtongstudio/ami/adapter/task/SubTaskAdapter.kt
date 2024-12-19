package com.tongtongstudio.ami.adapter.task

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ViewHolder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.databinding.ItemTaskBinding

class SubTaskAdapter(private val listener: InteractionListener, val subTasks: List<Task>) :
    RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubTaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SubTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubTaskViewHolder, position: Int) {
        val currentTask = subTasks[position]
        holder.bind(currentTask)
    }

    override fun getItemCount(): Int {
        return subTasks.size
    }

    inner class SubTaskViewHolder(
        private val binding: ItemTaskBinding
    ) : ViewHolder<Task>(binding.root) {

        init {
            binding.apply {
                checkBoxCompleted.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = subTasks[position]
                        listener.onTaskChecked(task, checkBoxCompleted.isChecked, position)
                    }
                }
                root.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = subTasks[position]
                        listener.onTaskClick(task, itemView)
                    }
                }
            }
        }

        override fun bind(thingToDo: Task) {
            binding.apply {
                tvTaskName.text = thingToDo.title
                checkBoxCompleted.isChecked = thingToDo.isCompleted
                tvTaskName.paint.isStrikeThruText = thingToDo.isCompleted
                tvNumberPriority.text =
                    this@SubTaskViewHolder.itemView.context.getString(
                        R.string.importance_thing_to_do,
                        thingToDo.priority
                    )

                if (thingToDo.isLate()) {
                    tvTaskName.setTextColor(
                        this@SubTaskViewHolder.itemView.context.resources.getColor(
                            R.color.design_default_color_error
                        )
                    )
                }
                tvDeadline.text =
                    Task.getDateFormatted(thingToDo.dueDate)
                if (thingToDo.startDate != null) {
                    tvStartDate.text = Task.getDateFormatted(thingToDo.startDate)
                } else tvStartDate.isVisible = false
            }
        }
    }

}
