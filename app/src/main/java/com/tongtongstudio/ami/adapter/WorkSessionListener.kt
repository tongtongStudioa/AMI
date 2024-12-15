package com.tongtongstudio.ami.adapter

import com.tongtongstudio.ami.data.datatables.WorkSession

interface WorkSessionListener {
    fun onClick(workSession: WorkSession)
    fun onRemoveClick(workSession: WorkSession)
}
