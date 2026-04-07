package com.tongtongstudio.ami.adapter.thingToDo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.BaseAdapter
import com.tongtongstudio.ami.adapter.ItemTouchHelperAdapter
import com.tongtongstudio.ami.adapter.ViewHolder
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.data.datatables.Type
import com.tongtongstudio.ami.databinding.ItemProjectMinimizedBinding
import com.tongtongstudio.ami.databinding.ItemTaskBinding


class ThingToDoAdapter(private val listener: InteractionListener, private val showDueDate: Boolean = true ) :
    BaseAdapter<ThingToDo>(), ItemTouchHelperAdapter {

    enum class FragmentViewType {LATER, TODAY, COMPLETED, OTHERS}
    enum class ViewType {TASK, PROJECT}

    /*fun addTask(newTask: ThingToDo) {
        val position: Int = findInsertionPosition(newTask)
        taskList.add(position, newTask)
        notifyItemInserted(position)
    }*/

    override fun onBindViewHolder(holder: ViewHolder<ThingToDo>, position: Int) {
        val element = elementsList[position]
        when (holder) {
            is TaskViewHolder -> holder.bind(element)
            is TaskComposedViewHolder -> holder.bind(element)
            else -> throw IllegalArgumentException()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<ThingToDo> {
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
        return if (elementsList[position].taskRelations.mainTask.nature == Nature.PROJECT.name || elementsList[position].taskRelations.mainTask.nature == Nature.INTERMEDIATE_PROJECT.name) ViewType.PROJECT.ordinal else ViewType.TASK.ordinal
    }

    // for drag and drop operation
    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        //Collections.swap(taskList, fromPosition, toPosition)
        notifyItemChanged(toPosition)
    }

    fun getTaskList(): List<ThingToDo> {
        return elementsList
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : ViewHolder<ThingToDo>(binding.root) {

        init {
            binding.apply {
                checkBoxCompleted.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val thingToDo = elementsList[position]
                        listener.onTaskChecked(thingToDo, checkBoxCompleted.isChecked, position)
                    }
                }
                root.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = elementsList[position].taskRelations.mainTask
                        listener.onTaskClick(task, itemView)
                    }
                }
            }
        }

        override fun bind(data: ThingToDo) {
            val thingToDo = data
            binding.apply {
                ViewCompat.setTransitionName(binding.root, "shared_element_${data.taskRelations.mainTask.id}")
                tvTaskName.text = thingToDo.taskRelations.mainTask.title
                val isCompleted = thingToDo.lastCompletionStatus == true && thingToDo.getType() != Type.RECURRING.name
                checkBoxCompleted.isChecked = isCompleted
                checkBoxCompleted.isVisible = !thingToDo.taskRelations.mainTask.isDraft
                tvTaskName.paint.isStrikeThruText = isCompleted
                tvCategory.text = thingToDo.taskRelations.category?.title
                tvCategory.isVisible = thingToDo.taskRelations.category != null
                tvStatus.text = thingToDo.getStatus(itemView.context)
                tvNature.text = thingToDo.getNature(itemView.context)
                tvRecurrentInfos.text = if (thingToDo.getType() != Type.UNIQUE.name) thingToDo.getType() else ""
                tvRecurrentInfos.isVisible = thingToDo.getType() != Type.UNIQUE.name
                tvNumberPriority.text =
                    this@TaskViewHolder.itemView.context.getString(
                        R.string.thing_to_do_priority,
                        thingToDo.taskRelations.mainTask.priority
                    )
                tvNumberPriority.isVisible = thingToDo.taskRelations.mainTask.priority != null
                //tvDeadline.isVisible = thingToDo.taskRelations.mainTask.deadline != null
                divider.isVisible = thingToDo.taskRelations.mainTask.dueDate != null || thingToDo.taskRelations.mainTask.priority != null
                /*if (thingToDo.isLate()) {
                    tvTaskName.setTextColor(
                        ContextCompat.getColor(
                            this@TaskViewHolder.itemView.context,
                            R.color.design_default_color_error
                        )
                    )
                }*/
                //tvDeadline.text = Task.getDateFormatted(thingToDo.taskRelations.mainTask.deadline)
                //tvStartDate.text = Task.getDateFormatted(thingToDo.taskRelations.mainTask.startDate)
                //tvStartDate.isVisible = thingToDo.taskRelations.mainTask.startDate != null
                tvDueDate.isVisible = showDueDate || thingToDo.isLate()
                tvDueDate.text = Task.getDateFormatted(thingToDo.taskRelations.mainTask.dueDate)
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
                        val project = elementsList[position]
                        listener.onProjectClick(project,itemView)
                    }
                }
                btnAddSubTask.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val project = elementsList[position]
                        listener.onProjectAddClick(project)
                    }
                }
            }
        }

        override fun bind(
            data: ThingToDo
        ) {
            binding.apply {
                ViewCompat.setTransitionName(binding.root, "shared_element_${data.taskRelations.mainTask.id}")
                tvProjectName.text = data.taskRelations.mainTask.title
                tvProjectName.paint.isStrikeThruText = data.lastCompletionStatus ?: false
                tvCategory.text = data.taskRelations.category?.title
                tvCategory.isVisible = data.taskRelations.category != null
                tvStatus.text = data.getStatus(itemView.context)
                tvNature.text = data.getNature(itemView.context)
                tvDeadline.text = Task.getDateFormatted(data.taskRelations.mainTask.dueDate)
                tvDeadline.isVisible =
                    Task.getDateFormatted(data.taskRelations.mainTask.deadline) != null
                tvStartDate.text = Task.getDateFormatted(data.taskRelations.mainTask.startDate)
                tvStartDate.isVisible =
                    Task.getDateFormatted(data.taskRelations.mainTask.startDate) != null
                tvNumberPriority.text = itemView.context.getString(
                    R.string.thing_to_do_priority,
                    data.taskRelations.mainTask.priority
                )
                progressText.text = itemView.context.getString(
                    R.string.nb_sub_tasks_project,
                     data.nbSubTasksCompleted,
                    data.nbSubTasks
                )
                progressHorizontal.progress = (data.completionRate ?: 0).toInt()
            }
        }
    }
}

