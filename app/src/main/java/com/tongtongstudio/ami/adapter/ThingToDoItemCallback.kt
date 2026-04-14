package com.tongtongstudio.ami.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.project.ProjectAdapter
import com.tongtongstudio.ami.adapter.thingToDo.ThingToDoAdapter
import com.tongtongstudio.ami.data.datatables.ThingToDo
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

abstract class ThingToDoItemCallback<T>(
    private val adapter: T,
    private val swipeFlags: Int = ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
    private val context: Context
) :
    ItemTouchHelper.Callback() {


    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        ItemTouchHelper.UP or ItemTouchHelper.DOWN
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
        val newSubTask = adapter.getProjectList()[fromPosition].mainTask
        val parentId = adapter.getProjectList()[toPosition].mainTask.id
        actionOnTaskMove(newSubTask,parentId)
        */
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val thingToDo: ThingToDo = when (adapter) {
            is ThingToDoAdapter -> adapter.getTaskList()[viewHolder.absoluteAdapterPosition]
            is ProjectAdapter -> adapter.getProjectList()[viewHolder.absoluteAdapterPosition]
            else -> throw NullPointerException()
        }

        if (direction == ItemTouchHelper.RIGHT) {
            // delete task
            actionOnRightSwiped(thingToDo, viewHolder.absoluteAdapterPosition)
        } else if (direction == ItemTouchHelper.LEFT) {
            // edit task
            actionLeftSwiped(thingToDo, viewHolder.absoluteAdapterPosition)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {

        RecyclerViewSwipeDecorator.Builder(
            context,
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
            .addSwipeLeftActionIcon(R.drawable.ic_baseline_edit_24)
            .addSwipeRightActionIcon(R.drawable.ic_baseline_delete_24)
            .setSwipeLeftActionIconTint(
                MaterialColors.getColor(
                    context,
                    R.attr.colorTertiary,
                    Color.GREEN
                )
            )
            .setSwipeRightActionIconTint(
                MaterialColors.getColor(
                    context,
                    R.attr.colorError,
                    Color.RED
                )
            )
            .create()
            .decorate()
        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return true
    }
    open fun actionOnRightSwiped(thingToDo: ThingToDo, position: Int) {}
    open fun actionLeftSwiped(thingToDo: ThingToDo, position: Int) {}
}