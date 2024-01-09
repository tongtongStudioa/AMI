package com.tongtongstudio.ami.adapter

import com.tongtongstudio.ami.data.datatables.TaskWithSubTasks
import com.tongtongstudio.ami.data.datatables.Ttd

interface InteractionListener {
    fun onTaskChecked(thingToDo: Ttd, isChecked: Boolean, position: Int)
    fun onComposedTaskClick(thingToDo: TaskWithSubTasks)
    fun onTaskClick(thingToDo: Ttd)
    fun onAddClick(composedTask: TaskWithSubTasks)
    fun onSubTaskRightSwipe(thingToDo: Ttd)
    fun onSubTaskLeftSwipe(thingToDo: Ttd)
}