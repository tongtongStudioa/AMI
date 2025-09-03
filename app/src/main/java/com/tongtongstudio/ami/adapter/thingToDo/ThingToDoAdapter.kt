package com.tongtongstudio.ami.adapter.thingToDo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ItemTouchHelperAdapter
import com.tongtongstudio.ami.adapter.ViewHolder
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.ItemProjectMinimizedBinding
import com.tongtongstudio.ami.databinding.ItemTaskBinding


class ThingToDoAdapter(private val listener: InteractionListener) :
    RecyclerView.Adapter<ViewHolder<*>>(), ItemTouchHelperAdapter {

    private val taskList: MutableList<ThingToDo> = mutableListOf()

    enum class ViewType {TASK, PROJECT}

    fun submitList(tasks: List<ThingToDo>) {
        taskList.clear()
        taskList.addAll(tasks)
        notifyDataSetChanged()
    }

    /*fun addTask(newTask: ThingToDo) {
        val position: Int = findInsertionPosition(newTask)
        taskList.add(position, newTask)
        notifyItemInserted(position)
    }*/

    override fun onBindViewHolder(holder: ViewHolder<*>, position: Int) {
        val element = taskList[position]
        when (holder) {
            is TaskViewHolder -> holder.bind(element)
            is TaskComposedViewHolder -> holder.bind(element)
            else -> throw IllegalArgumentException()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<*> {
        val context = parent.context
        return when (viewType) {
            ViewType.TASK.ordinal -> {
                LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
                val binding = ItemTaskBinding.inflate(LayoutInflater.from(context), parent, false)
                TaskViewHolder(binding)
            }
            ViewType.PROJECT.ordinal -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_project_minimized, parent, false)
                val binding = ItemProjectMinimizedBinding.bind(view)
                TaskComposedViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (taskList[position].getNature() == Nature.PROJECT.name || taskList[position].getNature() == Nature.INTERMEDIATE_PROJECT.name) ViewType.PROJECT.ordinal else ViewType.TASK.ordinal
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    // for drag and drop operation
    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        //Collections.swap(taskList, fromPosition, toPosition)
        notifyItemChanged(toPosition)
    }

    fun getTaskList(): List<ThingToDo> {
        return taskList
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : ViewHolder<ThingToDo>(binding.root) {

        init {
            binding.apply {
                checkBoxCompleted.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val thingToDo = taskList[position]
                        listener.onTaskChecked(thingToDo, checkBoxCompleted.isChecked, position)
                    }
                }
                root.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = taskList[position].taskRelations.mainTask
                        listener.onTaskClick(task, itemView)
                    }
                }
            }
        }

        override fun bind(data: ThingToDo) {
            binding.apply {
                ViewCompat.setTransitionName(binding.root, "shared_element_${data.taskRelations.mainTask.id}")
                tvTaskName.text = data.taskRelations.mainTask.title
                checkBoxCompleted.isChecked = data.lastCompletionStatus ?: false
                checkBoxCompleted.isVisible = !data.taskRelations.mainTask.isDraft
                tvTaskName.paint.isStrikeThruText = data.lastCompletionStatus ?: false
                tvCategory.text = data.taskRelations.category?.title
                tvCategory.isVisible = data.taskRelations.category != null
                val status = data.getStatus()
                tvStatus.text = status.apply {
                    first().uppercase()
                    replace('_',' ')
                }
                tvNumberPriority.text =
                    this@TaskViewHolder.itemView.context.getString(
                        R.string.importance_thing_to_do,
                        data.taskRelations.mainTask.priority
                    )
                tvNumberPriority.isVisible = data.taskRelations.mainTask.priority != null
                tvDeadline.isVisible = data.taskRelations.mainTask.deadline != null
                divider.isVisible = data.taskRelations.mainTask.dueDate != null || data.taskRelations.mainTask.priority != null
                /*if (data.isLate()) {
                    tvTaskName.setTextColor(
                        ContextCompat.getColor(
                            this@TaskViewHolder.itemView.context,
                            R.color.design_default_color_error
                        )
                    )
                }*/
                tvDeadline.text = Task.getDateFormatted(data.taskRelations.mainTask.deadline)
                tvStartDate.text = Task.getDateFormatted(data.taskRelations.mainTask.startDate)
                tvStartDate.isVisible = data.taskRelations.mainTask.startDate != null
            }
        }
    }

    inner class TaskComposedViewHolder(
        private val binding: ItemProjectMinimizedBinding
    ) : ViewHolder<ThingToDo>(binding.root) {

        private var expanded = false

        init {
            binding.apply {
                mainCardView.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val project = taskList[position]
                        listener.onProjectClick(project)
                    }
                }
                btnAddSubTask.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val project = taskList[position]
                        listener.onProjectAddClick(project)
                    }
                }
            }
        }

        override fun bind(
            data: ThingToDo
        ) {
            binding.apply {
                tvProjectName.text = data.taskRelations.mainTask.title
                tvProjectName.paint.isStrikeThruText = data.lastCompletionStatus ?: false
                tvCategory.text = data.taskRelations.category?.title
                tvCategory.isVisible = data.taskRelations.category != null
                tvStatus.text = data.getStatus()
                tvNature.text = data.getNature()
                tvDeadline.text = Task.getDateFormatted(data.taskRelations.mainTask.dueDate)
                tvDeadline.isVisible =
                    Task.getDateFormatted(data.taskRelations.mainTask.deadline) != null
                tvStartDate.text = Task.getDateFormatted(data.taskRelations.mainTask.startDate)
                tvStartDate.isVisible =
                    Task.getDateFormatted(data.taskRelations.mainTask.startDate) != null
                tvNumberPriority.text = this@TaskComposedViewHolder.itemView.context.getString(
                    R.string.importance_thing_to_do,
                    data.taskRelations.mainTask.priority
                )
                progressText.text = this@TaskComposedViewHolder.itemView.context.getString(
                    R.string.nb_sub_tasks_project,
                     data.nbSubTasksCompleted,
                    data.nbSubTasks
                )
                progressHorizontal.progress = if (data.nbSubTasksCompleted != null && data.nbSubTasks != null && data.nbSubTasks != 0) data.nbSubTasksCompleted /
                    data.nbSubTasks * 100 else 0
            }
        }
    }
}

