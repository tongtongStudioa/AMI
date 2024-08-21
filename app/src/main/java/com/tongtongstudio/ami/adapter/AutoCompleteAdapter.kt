package com.tongtongstudio.ami.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.tongtongstudio.ami.data.datatables.Category

class AutoCompleteAdapter(
    private val context: Context,

    ) : BaseAdapter(), Filterable {

    var data = mutableListOf<Category>()
    private var filteredCategories: List<String> = data.map { it.title }

    fun submitList(categories: List<Category>) {
        data.clear()
        data.addAll(categories)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): String {
        return data[position].title
    }

    fun getCategorySelected(position: Int): Category {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView
            ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_dropdown_item_1line, parent, false)

        val categoryTextView: TextView = view.findViewById(android.R.id.text1)
        categoryTextView.text = data[position].title

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                filteredCategories = if (constraint.isNullOrBlank()) {
                    data.map { it.title }
                } else {
                    val filteredList = data.map { it.title }.filter {
                        it.contains(constraint.toString(), ignoreCase = true)
                    }
                    filteredList
                }

                results.values = filteredCategories
                results.count = filteredCategories.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredCategories = results?.values as? List<String> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}