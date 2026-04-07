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

class AutoCompleteAdapter<T>(
    private val context: Context,

    ) : BaseAdapter(), Filterable {

    var data = mutableListOf<T>()
    private var suggestions: List<String> = data.map { it.anyToString() }

    fun submitList(categories: List<T>) {
        data.clear()
        data.addAll(categories)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): String {
        return data[position].anyToString()
    }

    fun getItemSelected(position: Int): T {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView
            ?: LayoutInflater.from(context)
                .inflate(android.R.layout.simple_dropdown_item_1line, parent, false)

        val textView: TextView = view.findViewById(android.R.id.text1)
        textView.text = data[position].anyToString()

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                suggestions = if (constraint.isNullOrBlank()) {
                    data.map { it.anyToString() }
                } else {
                    val filteredList = data.map { it.anyToString() }.filter {
                        it.contains(constraint.toString(), ignoreCase = true)
                    }
                    filteredList
                }

                results.values = suggestions
                results.count = suggestions.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                suggestions = results?.values as? List<String> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    fun T.anyToString() = when (this) {
        is Category -> (this as Category).title
        else -> this.toString()
    }
}