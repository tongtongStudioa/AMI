package com.tongtongstudio.ami.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.databinding.ItemGoalBinding

class GoalsAdapter(private val context: Context, private val listener: GoalsListener) :
    BaseAdapter<Assessment>() {

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
                    val goal = elementsList[position]
                    listener.onGoalClick(goal, itemView)
                }
            }
        }

        override fun bind(data: Assessment) {
            binding.apply {
                itemView.transitionName = "shared_element_${data.id}"
                tvGoalName.text = data.title
                tvTargetScore.text =
                    context.getString(R.string.target_goal, data.targetGoal, data.unit)
                tvDeadline.text = data.getFormattedDueDate()
                // TODO: adapt in function of type assessment view (checkbox, counter, timer)
            }
        }
    }

    fun getGoalsList(): List<Assessment> {
        return elementsList
    }
}