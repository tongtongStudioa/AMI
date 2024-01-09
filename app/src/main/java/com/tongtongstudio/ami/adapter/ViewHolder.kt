package com.tongtongstudio.ami.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

// Base view holder for all type of view in recycler view
abstract class ViewHolder<in T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(data: T)
}