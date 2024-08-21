package com.tongtongstudio.ami.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.ItemGoalBinding

class GoalsAdapter(private val context: Context, private val listener: GoalsListener) :
    RecyclerView.Adapter<ViewHolder<Assessment>>() {

    private val goalsList = mutableListOf<Assessment>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<Assessment> {
        val context = parent.context
        LayoutInflater.from(context).inflate(R.layout.item_goal, parent, false)
        val binding = ItemGoalBinding.inflate(LayoutInflater.from(context), parent, false)
        return GoalViewHolder(binding)
    }

    inner class GoalViewHolder(val binding: ItemGoalBinding) :
        ViewHolder<Assessment>(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val goal = goalsList[position]
                    listener.onGoalClick(goal)
                }
            }
        }

        override fun bind(data: Assessment) {
            binding.apply {
                tvGoalName.text = data.title
                tvTargetScore.text =
                    context.getString(R.string.target_goal, data.targetGoal, data.unit)
                tvDeadline.text = data.getFormattedDueDate()
                // TODO: adapt in function of type assessment view (checkbox, counter, timer)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder<Assessment>, position: Int) {
        val element = goalsList[position]
        holder.bind(element)
    }

    override fun getItemCount(): Int {
        return goalsList.size
    }

    fun getGoalsList(): List<Assessment> {
        return goalsList
    }

    fun submitList(newGoalsList: List<Assessment>) {
        goalsList.clear()
        goalsList.addAll(newGoalsList)
        notifyDataSetChanged()
    }
}