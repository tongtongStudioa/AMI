package com.tongtongstudio.ami.adapter.project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.ItemTouchHelperAdapter
import com.tongtongstudio.ami.adapter.ViewHolder
import com.tongtongstudio.ami.adapter.thingToDo.InteractionListener
import com.tongtongstudio.ami.data.datatables.Nature
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo
import com.tongtongstudio.ami.databinding.ItemProjectWithSubtasksBinding


class ProjectAdapter(private val listener: InteractionListener) :
    RecyclerView.Adapter<ProjectAdapter.TaskComposedViewHolder>(), ItemTouchHelperAdapter {

    private val taskList: MutableList<ThingToDo> = mutableListOf()

    enum class ViewType {TASK, PROJECT}

    fun submitList(tasks: List<ThingToDo>) {
        taskList.clear()
        taskList.addAll(tasks)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: TaskComposedViewHolder, position: Int) {
        val element = taskList[position]
        holder.bind(element)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskComposedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_with_subtasks, parent, false)
        val binding = ItemProjectWithSubtasksBinding.bind(view)
        return TaskComposedViewHolder(binding)
    }

    override fun getItemViewType(position: Int): Int {
        return if (taskList[position].taskRelations.mainTask.nature == Nature.PROJECT.name) ViewType.PROJECT.ordinal else ViewType.TASK.ordinal
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    // for drag and drop operation
    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        //Collections.swap(taskList, fromPosition, toPosition)
        notifyItemChanged(toPosition)
    }

    fun getProjectList(): List<ThingToDo> {
        return taskList
    }

    inner class TaskComposedViewHolder(
        private val binding: ItemProjectWithSubtasksBinding
    ) : ViewHolder<ThingToDo>(binding.root) {

        private var expanded = false

        init {
            binding.apply {
                mainCardView.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val project = taskList[position]
                        listener.onProjectClick(project,itemView, position)
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
                tvNature.text = data.getNature(itemView.context)
                tvDeadline.text = Task.getDateFormatted(data.taskRelations.mainTask.dueDate)
                tvDeadline.isVisible =
                    Task.getDateFormatted(data.taskRelations.mainTask.dueDate) != null
                tvStartDate.text = Task.getDateFormatted(data.taskRelations.mainTask.startDate)
                tvStartDate.isVisible =
                    Task.getDateFormatted(data.taskRelations.mainTask.startDate) != null
                tvNumberPriority.text = itemView.context.getString(
                    R.string.thing_to_do_priority,
                    data.taskRelations.mainTask.priority
                )
                tvNbSubTasks.text = itemView.context.getString(
                    R.string.nb_sub_tasks_project,
                    data.nbSubTasksCompleted,
                    data.nbSubTasks
                )
                progressHorizontal.progress = (data.completionRate ?: 0).toInt()
                /*val subTaskAdapter = SubTaskAdapter(listener)
                subTaskAdapter.swapData(data.subTasks)
                rvSubTasks.apply {
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                    adapter = subTaskAdapter
                }
                tvCategory.text = data.category
                tvCategory.isVisible = data.category != null

                // TODO: resolve sub item touch behavior
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

                        val subTask: Task =
                            taskList[bindingAdapterPosition].subTasks[viewHolder.bindingAdapterPosition]
                        if (direction == ItemTouchHelper.RIGHT) {
                            listener.onSubTaskRightSwipe(subTask)
                        } else if (direction == ItemTouchHelper.LEFT) {
                            listener.onSubTaskLeftSwipe(subTask)
                        }
                    }

                }).attachToRecyclerView(rvSubTasks)*/
            }
        }
    }
}

