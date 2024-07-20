package com.tongtongstudio.ami.adapter.task

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ItemTouchHelperAdapter
import com.tongtongstudio.ami.adapter.ViewHolder
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.ItemProjectBinding
import com.tongtongstudio.ami.databinding.ItemTaskBinding


class TaskAdapter(private val listener: InteractionListener) :
    RecyclerView.Adapter<ViewHolder<*>>(), ItemTouchHelperAdapter {

    private val taskList: MutableList<TaskWithSubTasks> = mutableListOf()

    companion object {
        private const val TYPE_TASK = 0
        private const val TYPE_TASK_COMPOSED = 1
    }

    fun submitList(tasks: List<TaskWithSubTasks>) {
        taskList.clear()
        taskList.addAll(tasks)
        notifyDataSetChanged()
    }

    /*fun addTask(newTask: TaskWithSubTasks) {
        val position: Int = findInsertionPosition(newTask)
        taskList.add(position, newTask)
        notifyItemInserted(position)
    }*/

    override fun onBindViewHolder(holder: ViewHolder<*>, position: Int) {
        val element = taskList[position]
        when (holder) {
            is TaskViewHolder -> holder.bind(element.mainTask)
            is TaskComposedViewHolder -> holder.bind(element)
            else -> throw IllegalArgumentException()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<*> {
        val context = parent.context
        return when (viewType) {
            TYPE_TASK -> {
                LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
                val binding = ItemTaskBinding.inflate(LayoutInflater.from(context), parent, false)
                TaskViewHolder(binding)
            }
            TYPE_TASK_COMPOSED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_project, parent, false)
                val binding = ItemProjectBinding.bind(view)
                TaskComposedViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (taskList[position].mainTask.type == Nature.PROJECT.name || taskList[position].subTasks.isNotEmpty()) TYPE_TASK_COMPOSED else TYPE_TASK
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    // for drag and drop operation
    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        //Collections.swap(taskList, fromPosition, toPosition)
        notifyItemChanged(toPosition)
    }

    fun getTaskList(): List<TaskWithSubTasks> {
        return taskList
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : ViewHolder<Ttd>(binding.root) {

        init {
            binding.apply {
                checkBoxCompleted.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = taskList[position].mainTask
                        listener.onTaskChecked(task, checkBoxCompleted.isChecked, position)
                    }
                }
                root.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = taskList[position].mainTask
                        listener.onTaskClick(task)
                    }
                }
            }
        }

        override fun bind(data: Ttd) {
            binding.apply {
                tvTaskName.text = data.title
                //tvNature.text = thingToDo.getCategoryTitle()
                checkBoxCompleted.isChecked = data.isCompleted
                tvTaskName.paint.isStrikeThruText = data.isCompleted
                // TODO: show the task's category
                tvNumberPriority.text =
                    this@TaskViewHolder.itemView.context.getString(
                        R.string.importance_thing_to_do,
                        data.priority
                    )

                /*if (data.dueDate < todayDate && !data.isCompleted) {
                    tvTaskName.setTextColor(
                        ContextCompat.getColor(
                            this@TaskViewHolder.itemView.context,
                            R.color.design_default_color_error
                        )
                    )
                }*/
                tvDeadline.text = Ttd.getDateFormatted(data.dueDate)
                if (data.startDate != null) {
                    tvStartDate.text = Ttd.getDateFormatted(data.startDate)
                } else tvStartDate.isVisible = false
            }
        }
    }

    inner class TaskComposedViewHolder(
        private val binding: ItemProjectBinding
    ) : ViewHolder<TaskWithSubTasks>(binding.root) {

        private var expanded = false

        init {
            binding.apply {
                mainCardView.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val composedTask = taskList[position]
                        listener.onComposedTaskClick(composedTask)
                    }
                }
                btnAddSubTask.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val composedTask = taskList[position]
                        listener.onProjectAddClick(composedTask)
                    }
                }
                subCardView.setOnClickListener {
                    if (expanded) {
                        collapseView()
                    } else {
                        expandView()
                    }
                }
                btnExpandCollapse.setOnClickListener {
                    if (expanded) {
                        collapseView()
                    } else {
                        expandView()
                    }
                }
            }
        }

        private fun collapseView() {
            expanded = false
            binding.btnExpandCollapse.setImageResource(R.drawable.ic_baseline_expand_more_24)
            binding.rvSubTasks.isVisible = expanded
        }

        private fun expandView() {
            expanded = true
            binding.btnExpandCollapse.setImageResource(R.drawable.ic_baseline_expand_less_24)
            binding.rvSubTasks.isVisible = expanded
        }

        override fun bind(
            data: TaskWithSubTasks
        ) {
            binding.apply {
                tvProjectName.text = data.mainTask.title
                tvProjectName.paint.isStrikeThruText = data.mainTask.isCompleted
                tvDeadline.text = Ttd.getDateFormatted(data.mainTask.dueDate)
                tvDeadline.isVisible =
                    Ttd.getDateFormatted(data.mainTask.dueDate) != null
                tvStartDate.text = Ttd.getDateFormatted(data.mainTask.startDate)
                tvStartDate.isVisible =
                    Ttd.getDateFormatted(data.mainTask.startDate) != null
                tvNumberPriority.text = this@TaskComposedViewHolder.itemView.context.getString(
                    R.string.importance_thing_to_do,
                    data.mainTask.priority
                )
                tvNbSubTasks.text = this@TaskComposedViewHolder.itemView.context.getString(
                    R.string.nb_sub_tasks_project,
                    data.getNbSubTasksCompleted(),
                    data.getNbSubTasks()
                )
                // TODO: is it here we make the sub task adapter alive ?
                rvSubTasks.apply {
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                    adapter = SubTaskAdapter(listener, data.subTasks)
                }

                // TODO: search for help on this helper
                ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                    0,
                    ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
                ) {
                    override fun onMove(
                        recyclerView: RecyclerView,
                        viewHolder: RecyclerView.ViewHolder,
                        target: RecyclerView.ViewHolder
                    ): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                        val subTask: Ttd =
                            taskList[bindingAdapterPosition].subTasks[viewHolder.bindingAdapterPosition]
                        if (direction == ItemTouchHelper.RIGHT) {
                            listener.onSubTaskRightSwipe(subTask)
                        } else if (direction == ItemTouchHelper.LEFT) {
                            listener.onSubTaskLeftSwipe(subTask)
                        }
                    }

                }).attachToRecyclerView(rvSubTasks)
            }
        }
    }
}

