package com.tongtongstudio.ami.adapter.thingToDo

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.ItemMissedTaskBinding

class MissedTaskAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val data: MutableList<ThingToDo> = ArrayList()

    fun swapData(newData: List<ThingToDo>) {
        data.clear()
        data.addAll(newData)
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

    inner class TaskMissedViewHolder(val binding: ItemMissedTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(thingToDo: ThingToDo) {
            binding.apply {
                tvTaskName.text = thingToDo.taskRelations.mainTask.title
                tvCategory.text = thingToDo.taskRelations.category?.title ?: ""
                tvCategory.isVisible = thingToDo.taskRelations.category != null
                tvDeadline.text = context.getString(
                    R.string.las_due_date,
                    Task.getDateFormatted(thingToDo.taskRelations.mainTask.dueDate)
                )
                // TODO: add missed times since last completion with unique query
                tvMissedCount.text =
                    context.getString(R.string.no_information)
            }
        }
    }
}
