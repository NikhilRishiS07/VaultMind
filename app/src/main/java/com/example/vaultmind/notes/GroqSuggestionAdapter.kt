package com.example.vaultmind.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.R

class GroqSuggestionAdapter(
    private val onSuggestionClicked: (String) -> Unit
) : RecyclerView.Adapter<GroqSuggestionAdapter.GroqSuggestionViewHolder>() {
    private val items = mutableListOf<String>()

    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroqSuggestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_groq_suggestion, parent, false)
        return GroqSuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroqSuggestionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class GroqSuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val suggestionText: TextView = itemView.findViewById(R.id.groqSuggestionText)

        fun bind(item: String) {
            suggestionText.text = item
            itemView.setOnClickListener { onSuggestionClicked(item) }
        }
    }
}