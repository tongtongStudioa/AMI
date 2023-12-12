package com.tongtongstudio.ami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.ItemMissedTaskBinding

class MissedTaskAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val data: MutableList<Ttd>

    init {
        data = ArrayList()
    }

    fun swapData(newData: List<Ttd>) {
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
        (holder as TaskMissedViewHolder).bind(task)
    }

    class TaskMissedViewHolder(val binding: ItemMissedTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(thingToDo: Ttd) {
            binding.apply {
                tvTaskName.text = thingToDo.title
                tvNature.text = Nature.TASK.name
                tvDeadline.text = thingToDo.getDateFormatted(thingToDo.dueDate)
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
