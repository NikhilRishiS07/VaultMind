package com.example.vaultmind.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.R

class NotesFragment : Fragment() {
    private val allNotes = listOf(
        VaultNote("Q3 Project Quantum", "The preliminary assessment reveals clean encryption posture and structured recovery paths.", "Work", locked = false, pinned = true, "Last edited 2h ago", "Created May 12"),
        VaultNote("Grocery List Sanctuary", "Milk, oats, spinach, coffee beans, and a new filter replacement.", "Personal", locked = false, pinned = false, "Last edited 5h ago", "Created Apr 28"),
        VaultNote("Vault Credentials", "Production keys and backup recovery steps. Keep locked.", "Locked", locked = true, pinned = false, "Last edited 1d ago", "Created Jan 15"),
        VaultNote("Investment Thesis", "Thesis summary, risk controls, and review checklist for the next sprint.", "Work", locked = false, pinned = true, "Last edited 3h ago", "Created May 10"),
        VaultNote("Journal Thoughts", "A short reflection on momentum, clarity, and implementation priorities.", "Personal", locked = false, pinned = false, "Last edited 6h ago", "Created May 05"),
        VaultNote("Release Checklist", "QA, signing, export validation, and device smoke tests.", "Work", locked = false, pinned = false, "Last edited 45m ago", "Created Today")
    )

    private var activeFilter = "All"
    private var currentQuery = ""

    private val adapter by lazy { NotesAdapter(::openEditor) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_notes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val notesRecycler = view.findViewById<RecyclerView>(R.id.notesRecycler)
        notesRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        notesRecycler.adapter = adapter

        view.findViewById<Button>(R.id.allFilterButton).setOnClickListener { setFilter("All") }
        view.findViewById<Button>(R.id.workFilterButton).setOnClickListener { setFilter("Work") }
        view.findViewById<Button>(R.id.personalFilterButton).setOnClickListener { setFilter("Personal") }
        view.findViewById<Button>(R.id.lockedFilterButton).setOnClickListener { setFilter("Locked") }

        view.findViewById<View>(R.id.addNoteFab).setOnClickListener { openEditor() }

        view.findViewById<EditText>(R.id.notesSearchInput).addTextChangedListener { text ->
            currentQuery = text?.toString().orEmpty()
            applyFilters()
        }

        applyFilters()
    }

    private fun setFilter(filter: String) {
        activeFilter = filter
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = allNotes.filter { note ->
            val matchesFilter = activeFilter == "All" || note.category == activeFilter
            val matchesQuery = currentQuery.isBlank() || note.title.contains(currentQuery, ignoreCase = true) || note.preview.contains(currentQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
        adapter.submitList(filtered)
    }

    private fun openEditor() {
        findNavController().navigate(R.id.noteEditorFragment)
        Toast.makeText(requireContext(), "Opening note editor", Toast.LENGTH_SHORT).show()
    }

    private data class VaultNote(
        val title: String,
        val preview: String,
        val category: String,
        val locked: Boolean,
        val pinned: Boolean,
        val lastEdited: String,
        val createdAt: String
    )

    private inner class NotesAdapter(
        private val onClick: () -> Unit
    ) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {
        private val items = mutableListOf<VaultNote>()

        fun submitList(newItems: List<VaultNote>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val itemView = layoutInflater.inflate(R.layout.item_note_card, parent, false)
            return NoteViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.noteTitleText)
            private val previewText: TextView = itemView.findViewById(R.id.notePreviewText)
            private val metaText: TextView = itemView.findViewById(R.id.noteMetaText)
            private val badgeText: TextView = itemView.findViewById(R.id.noteBadgeText)
            private val lockedText: TextView = itemView.findViewById(R.id.noteLockedText)
            private val pinnedText: TextView = itemView.findViewById(R.id.notePinnedText)

            fun bind(item: VaultNote) {
                titleText.text = item.title
                previewText.text = item.preview
                metaText.text = "${item.lastEdited} • ${item.createdAt}"
                badgeText.text = item.category.take(1)
                lockedText.visibility = if (item.locked) View.VISIBLE else View.GONE
                pinnedText.visibility = if (item.pinned) View.VISIBLE else View.GONE
                itemView.setOnClickListener { onClick() }
            }
        }
    }
}