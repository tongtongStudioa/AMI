package com.tongtongstudio.ami.adapter.thingToDo

import android.view.View
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo

interface InteractionListener {
    fun onTaskChecked(thingToDo: ThingToDo, isChecked: Boolean, position: Int)
    fun onProjectClick(thingToDo: ThingToDo, itemView: View, position: Int)
    fun onTaskClick(thingToDo: Task, itemView: View, position: Int)
    fun onProjectAddClick(thingToDo: ThingToDo)
}