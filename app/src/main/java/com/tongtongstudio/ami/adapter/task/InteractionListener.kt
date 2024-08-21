package com.tongtongstudio.ami.adapter.task

import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo

interface InteractionListener {
    fun onTaskChecked(thingToDo: Task, isChecked: Boolean, position: Int)
    fun onComposedTaskClick(thingToDo: ThingToDo)
    fun onTaskClick(thingToDo: Task)
    fun onProjectAddClick(composedTask: ThingToDo)
    fun onSubTaskRightSwipe(thingToDo: Task)
    fun onSubTaskLeftSwipe(thingToDo: Task)
}