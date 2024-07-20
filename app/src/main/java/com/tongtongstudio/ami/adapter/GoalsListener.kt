package com.tongtongstudio.ami.adapter

import com.tongtongstudio.ami.data.datatables.Assessment

interface GoalsListener {
    fun onGoalClick(goal: Assessment)
    fun onGoalRightSwipe(goal: Assessment)
    fun onGoalLeftSwipe(goal: Assessment)
}
