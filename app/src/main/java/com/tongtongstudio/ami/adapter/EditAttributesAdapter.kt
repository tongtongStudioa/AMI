package com.tongtongstudio.ami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tongtongstudio.ami.data.datatables.Assessment
import com.tongtongstudio.ami.data.datatables.Reminder
import com.tongtongstudio.ami.data.datatables.Ttd
import com.tongtongstudio.ami.databinding.ItemOverviewDataEditBinding

class EditAttributesAdapter<T>(val listener: AttributeListener<T>) :
    RecyclerView.Adapter<EditAttributesAdapter<T>.OverviewViewHolder>() {

    private val dataList: MutableList<T> = mutableListOf()

    companion object {
        private const val TYPE_ASSESSMENT = 0
        private const val TYPE_REMINDER = 1
    }

    fun submitList(data: List<T>) {
        dataList.clear()
        dataList.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverviewViewHolder {
        val binding =
            ItemOverviewDataEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OverviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OverviewViewHolder, position: Int) {
        val currentData = dataList[position]
        holder.bind(currentData)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class OverviewViewHolder(
        private val binding: ItemOverviewDataEditBinding
    ) : ViewHolder<T>(binding.root) {

        init {
            binding.apply {
                removeItemButton.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val data = dataList[position]
                        listener.onRemoveCrossClick(data)
                    }
                }
                root.setOnClickListener {
                    val position = absoluteAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val data = dataList[position]
                        listener.onItemClicked(data)
                    }
                }
            }
        }

        override fun bind(data: T) {
            binding.apply {
                titleOverview.text = when (data) {
                    is Assessment -> data.title
                    is Reminder -> data.getReminderDueDateFormatted()
                    is Ttd -> data.title
                    else -> ""
                }
            }
        }
    }
}
