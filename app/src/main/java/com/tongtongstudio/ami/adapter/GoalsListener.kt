package com.tongtongstudio.ami.adapter

import android.view.View
import com.tongtongstudio.ami.data.datatables.Assessment

interface GoalsListener {
    fun onGoalClick(goal: Assessment, itemView: View)
    fun onGoalRightSwipe(goal: Assessment)
    fun onGoalLeftSwipe(goal: Assessment)
}
