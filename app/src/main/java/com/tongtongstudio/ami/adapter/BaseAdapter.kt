package com.tongtongstudio.ami.adapter

import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<T>() :
    RecyclerView.Adapter<ViewHolder<T>>() {

    open val elementsList = mutableListOf<T>()

    override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) {
        val element = elementsList[position]
        holder.bind(element)
    }

    override fun getItemCount(): Int {
        return elementsList.size
    }

    fun getElements(): List<T> {
        return elementsList.toList()
    }

    fun submitList(newGoalsList: List<T>) {
        elementsList.clear()
        elementsList.addAll(newGoalsList)
        notifyDataSetChanged()
    }
}