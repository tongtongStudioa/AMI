package com.tongtongstudio.ami.adapter

import com.tongtongstudio.ami.data.datatables.ProjectWithSubTasks
import com.tongtongstudio.ami.data.datatables.Task
import com.tongtongstudio.ami.data.datatables.ThingToDo

interface ThingToDoListener {
    fun onItemThingToDoClicked(thingToDo: ThingToDo)

    // TODO: initialize function member : shared view model and sound when check
    fun onCheckBoxClick(task: Task, isChecked: Boolean, position: Int)
    fun onItemTaskSwiped(subTask: Task, dir: Int)

    //fun onItemEventSwiped(event: Event, dir: Int)
    //fun onItemEventClicked(event: Event)

    //fun onItemProjectSwiped(project: ProjectWithSubTasks, dir: Int)
    //fun onItemProjectClicked(projectWithSubTasks: ProjectWithSubTasks)
    fun onProjectBtnAddSubTaskClicked(projectData: ProjectWithSubTasks)
}