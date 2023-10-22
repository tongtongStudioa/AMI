package com.tongtongstudio.ami.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.adapter.MainAdapter.TaskViewHolder
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.databinding.ItemTaskBinding

class SubAdapter(
    private val listener: ThingToDoListener,
    private val listSubTasks: List<Task>,
    private val context: Context
) : RecyclerView.Adapter<TaskViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding, listener, listSubTasks, context)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = listSubTasks[position]
        holder.bind(currentTask, RecyclerView.RecycledViewPool())
    }

    override fun getItemCount(): Int {
        return listSubTasks.size
    }
}