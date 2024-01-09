package com.tongtongstudio.ami.adapter

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.data.datatables.*
import com.tongtongstudio.ami.databinding.ItemEventBinding
import com.tongtongstudio.ami.databinding.ItemProjectBinding
import com.tongtongstudio.ami.databinding.ItemTaskBinding
import java.util.*

class MainAdapter(private val listener: ThingToDoListener, val fragmentContext: Context) :
    RecyclerView.Adapter<MainAdapter.BaseViewHolder<*>>() {

    private val pool = RecyclerView.RecycledViewPool()
    val data: MutableList<ThingToDo>

    companion object {
        private const val TYPE_TASK = 0
        private const val TYPE_PROJECT_WITH_SUB_TASKS = 1
        private const val TYPE_EVENT = 2
    }

    init {
        data = ArrayList()
    }

    fun swapData(newData: List<ThingToDo>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    // Base view holder for all type of view in recycler view
    abstract class BaseViewHolder<in T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(thingToDo: T, uniquePool: RecyclerView.RecycledViewPool)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<*> {
        val context = parent.context
        return when (viewType) {
            TYPE_TASK -> {
                LayoutInflater.from(context).inflate(R.layout.item_task, parent, false)
                val binding = ItemTaskBinding.inflate(LayoutInflater.from(context), parent, false)
                TaskViewHolder(binding, listener, data, context)
            }
            TYPE_PROJECT_WITH_SUB_TASKS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_project, parent, false)
                val binding = ItemProjectBinding.bind(view)
                ProjectViewHolder(binding, listener, data)
            }
            TYPE_EVENT -> {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
                val binding = ItemEventBinding.bind(view)
                EventViewHolder(binding, listener, data)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder<*>, position: Int) {
        val element = data[position]
        when (holder) {
            is TaskViewHolder -> holder.bind(element as Task, pool)
            is ProjectViewHolder -> holder.bind(element as ProjectWithSubTasks, pool)
            is EventViewHolder -> holder.bind(element as Event, pool)
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is Task -> TYPE_TASK
            is ProjectWithSubTasks -> TYPE_PROJECT_WITH_SUB_TASKS
            is Event -> TYPE_EVENT
            else -> throw IllegalArgumentException("Invalid type of data $position")
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class TaskViewHolder(
        private val binding: ItemTaskBinding,
        private val listener: ThingToDoListener,
        private val data: List<ThingToDo>,
        private val context: Context
    ) : BaseViewHolder<Task>(binding.root) {

        init {
            binding.apply {
                checkBoxCompleted.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = data[position] as Task
                        listener.onCheckBoxClick(task, checkBoxCompleted.isChecked, position)
                    }
                }
                root.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val task = data[position]
                        listener.onItemThingToDoClicked(task)
                    }
                }
            }
        }

        override fun bind(thingToDo: Task, uniquePool: RecyclerView.RecycledViewPool) {
            binding.apply {
                tvTaskName.text = thingToDo.taskName
                tvNature.text = Nature.TASK.name
                checkBoxCompleted.isChecked = thingToDo.isTaskCompleted
                tvTaskName.paint.isStrikeThruText = thingToDo.isTaskCompleted
                tvNumberPriority.text =
                    context.getString(R.string.importance_thing_to_do, thingToDo.taskPriority)
                if (thingToDo.taskDeadline != null) {
                    val todayDate = Calendar.getInstance().run {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        timeInMillis
                    }
                    if (thingToDo.taskDeadline < todayDate && !thingToDo.isTaskCompleted) {
                        tvTaskName.setTextColor(
                            context.resources.getColor(
                                resolveThemeAttribute(R.attr.colorError)
                            )
                        )
                    }
                    tvDeadline.text = thingToDo.getDeadlineFormatted()
                } else tvDeadline.isVisible = false
                if (thingToDo.taskStartDate != null) {
                    tvStartDate.text = thingToDo.getStartDateFormatted()
                } else tvStartDate.isVisible = false
            }
        }

        private fun resolveThemeAttribute(attr: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.resourceId
        }
    }

    inner class ProjectViewHolder(
        private val binding: ItemProjectBinding,
        listener: ThingToDoListener,
        data: List<ThingToDo>
    ) : BaseViewHolder<ProjectWithSubTasks>(binding.root) {

        private var expanded = false

        init {
            binding.apply {
                mainCardView.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val project = data[position]
                        listener.onItemThingToDoClicked(project)
                    }
                }
                btnAddSubTask.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val project = data[position] as ProjectWithSubTasks
                        listener.onProjectBtnAddSubTaskClicked(project)
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
            thingToDo: ProjectWithSubTasks,
            uniquePool: RecyclerView.RecycledViewPool
        ) {
            binding.apply {
                tvProjectName.text = thingToDo.project.pjtName
                tvProjectName.paint.isStrikeThruText = thingToDo.isCompleted()
                tvNature.text = Nature.PROJECT.name
                tvDeadline.text = thingToDo.project.getDeadlineFormatted()
                tvDeadline.isVisible = thingToDo.project.getDeadlineFormatted() != null
                tvStartDate.text = thingToDo.project.getStartDateFormatted()
                tvStartDate.isVisible = thingToDo.project.getStartDateFormatted() != null
                tvNumberPriority.text = fragmentContext.getString(
                    R.string.importance_thing_to_do,
                    thingToDo.project.pjtPriority
                )
                tvNbSubTasks.text = fragmentContext.getString(
                    R.string.nb_sub_tasks_project,
                    thingToDo.project.nb_sub_tasks_completed.toString(),
                    thingToDo.project.nb_sub_task.toString()
                )

                rvSubTasks.apply {
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                    adapter = SubAdapter(listener, thingToDo.subTasks, context)
                }
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

                        val subTask =
                            (data[bindingAdapterPosition] as ProjectWithSubTasks).subTasks[viewHolder.bindingAdapterPosition]
                        if (direction == ItemTouchHelper.RIGHT) {
                            listener.onItemTaskSwiped(subTask, ItemTouchHelper.RIGHT)
                        } else if (direction == ItemTouchHelper.LEFT) {
                            listener.onItemTaskSwiped(subTask, ItemTouchHelper.LEFT)
                        }
                    }

                }).attachToRecyclerView(rvSubTasks)
            }
        }
    }

    inner class EventViewHolder(
        private val binding: ItemEventBinding,
        listener: ThingToDoListener,
        data: List<ThingToDo>
    ) : BaseViewHolder<Event>(binding.root) {

        init {
            binding.apply {
                root.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val event = data[position]
                        listener.onItemThingToDoClicked(event)
                    }
                }
            }
        }

        override fun bind(thingToDo: Event, uniquePool: RecyclerView.RecycledViewPool) {
            binding.apply {
                tvEventName.text = thingToDo.eventName
                tvNature.text = Nature.EVENT.name
                icCalendar.isVisible = thingToDo.eventDeadline != null
                tvNumberPriority.text = fragmentContext.getString(
                    R.string.importance_thing_to_do,
                    thingToDo.eventPriority
                )
                if (thingToDo.isSpread) {
                    //tvDeadline.text = thingToDo.startDate + " - " + thingToDo.deadline
                } else tvDeadline.text = thingToDo.getDeadlineFormatted()
            }
        }
    }
}