package com.tongtongstudio.ami.adapter.thingToDo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ViewHolder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.databinding.ItemTaskBinding

class SubTaskAdapter(private val listener: InteractionListener) :
    RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder>() {

    val subTasks: MutableList<Task> = mutableListOf()

    fun swapData(newSubTasks: List<Task>) {
        subTasks.clear()
        subTasks.addAll(newSubTasks)
        notifyDataSetChanged()
        //notifyItemRangeInserted(0,newSubTasks.size)
    }
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
                        //todo : listener.onTaskChecked(task, checkBoxCompleted.isChecked, position)
                        //notifyItemChanged(position)
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

        override fun bind(data: Task) {
            binding.apply {
                ViewCompat.setTransitionName(binding.root, "shared_element_${data.id}")
                tvTaskName.text = data.title
                // TODO: retrieve completion infos from db
                checkBoxCompleted.isChecked = false
                tvTaskName.paint.isStrikeThruText = true
                tvNumberPriority.text =
                    this@SubTaskViewHolder.itemView.context.getString(
                        R.string.importance_thing_to_do,
                        data.priority
                    )
                tvNumberPriority.isVisible = data.priority != null

                /*if (data.isLate()) {
                    tvTaskName.setTextColor(
                        this@SubTaskViewHolder.itemView.context.resources.getColor(
                            R.color.design_default_color_error
                        )
                    )
                }*/
                tvDeadline.text = Task.getDateFormatted(data.dueDate)
                tvDeadline.isVisible = data.dueDate != null
                tvStartDate.text = Task.getDateFormatted(data.startDate)
                tvStartDate.isVisible = data.startDate != null
            }
        }
    }

}
