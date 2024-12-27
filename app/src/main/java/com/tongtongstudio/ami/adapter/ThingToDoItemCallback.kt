package com.tongtongstudio.ami.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.tongtongstudio.ami.R
import com.tongtongstudio.ami.adapter.task.ThingToDoAdapter
import com.tongtongstudio.ami.data.datatables.ThingToDo
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

abstract class ThingToDoItemCallback(
    private val adapter: ThingToDoAdapter,
    private val swipeFlags: Int = ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
    private val context: Context
) :
    ItemTouchHelper.Callback() {


    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
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
            // delete thingToDo
            adapter.notifyItemRemoved(viewHolder.absoluteAdapterPosition)
            actionOnRightSwiped(thingToDo)
        } else if (direction == ItemTouchHelper.LEFT) {
            // edit thingToDo
            adapter.notifyItemChanged(viewHolder.absoluteAdapterPosition)
            actionLeftSwiped(thingToDo)
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

    open fun actionOnTaskMove(thingToDo: ThingToDo, parentId: Long) {}
    open fun actionOnRightSwiped(thingToDo: ThingToDo) {}
    open fun actionLeftSwiped(thingToDo: ThingToDo) {}
}