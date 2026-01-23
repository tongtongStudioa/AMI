package com.tongtongstudio.ami.adapter.thingToDo

import android.view.View
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo

interface InteractionListener {
    fun onTaskChecked(thingToDo: ThingToDo, isChecked: Boolean, position: Int)
    fun onProjectClick(thingToDo: ThingToDo, itemView: View)
    fun onTaskClick(thingToDo: Task, itemView: View)
    fun onProjectAddClick(thingToDo: ThingToDo)
}