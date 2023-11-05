package com.tongtongstudio.ami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.ItemMissedTaskBinding

class MissedTaskAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val data: MutableList<ThingToDo>

    init {
        data = ArrayList()
    }

    fun swapData(newData: List<ThingToDo>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_missed_task, parent, false)
        val binding = ItemMissedTaskBinding.bind(view)
        return TaskMissedViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val task = data[position]
        (holder as TaskMissedViewHolder).bind(task as Task)
    }

    class TaskMissedViewHolder(val binding: ItemMissedTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(thingToDo: Task) {
            binding.apply {
                tvTaskName.text = thingToDo.taskName
                tvNature.text = Nature.TASK.name
                if (thingToDo.taskDeadline != null) {
                    tvDeadline.text = thingToDo.getDeadlineFormatted()
                } else tvDeadline.isVisible = false
                // todo: show missed times count
                tvMissedCount.isVisible = false
                //tvMissedCount.text = countMissedTimes(task)

            }
        }

        // TODO: count number task where missed
        /*private fun countMissedTimes(task: Task): String {
            val todayDate = Calendar.getInstance().run {
                set(Calendar.HOUR_OF_DAY,23)
                set(Calendar.MINUTE,59)
                timeInMillis
            }
            if (task.taskStartDate != null) {
                Calendar.getInstance().run {
                    while (task.taskStartDate< todayDate) {
                        add(Calendar.DAY_OF_MONTH,1)
                        if (task.taskStartDate)
                    }
                }
            }

        }*/
    }
}
