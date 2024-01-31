package com.tongtongstudio.ami.adapter

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd

class TaskTouchHelperCallback(
    private val adapter: TaskAdapter,
    private val actionOnTaskMove: (Ttd, Long) -> Unit,
    private val actionOnRightSwiped: (TaskWithSubTasks) -> Unit,
    private val actionLeftSwiped: (TaskWithSubTasks) -> Unit
) :
    ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
        return makeMovementFlags(0, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        /*val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition
        adapter.onItemMove(fromPosition, toPosition)
        val newSubTask = adapter.getTaskList()[fromPosition].mainTask
        val parentId = adapter.getTaskList()[toPosition].mainTask.id
        actionOnTaskMove(newSubTask,parentId)
        */
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val thingToDo = adapter.getTaskList()[viewHolder.absoluteAdapterPosition]
        if (direction == ItemTouchHelper.RIGHT) {
            // delete task
            adapter.notifyItemRemoved(viewHolder.absoluteAdapterPosition)
            actionOnRightSwiped(thingToDo)
        } else if (direction == ItemTouchHelper.LEFT) {
            // edit task
            adapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)
            actionLeftSwiped(thingToDo)
        }
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }
}