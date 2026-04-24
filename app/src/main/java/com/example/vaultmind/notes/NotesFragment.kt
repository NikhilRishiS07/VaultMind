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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.AppGraph
import com.example.vaultmind.R
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    private val repository by lazy { AppGraph.repository(requireContext()) }
    private var allNotes = emptyList<VaultNote>()

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

        loadNotes()
    }

    override fun onResume() {
        super.onResume()
        loadNotes()
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

    private fun loadNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            allNotes = repository.getNotes().map {
                VaultNote(
                    title = it.title,
                    preview = it.preview,
                    category = it.category,
                    locked = it.locked,
                    pinned = it.pinned,
                    lastEdited = it.lastEdited,
                    createdAt = it.createdAt
                )
            }
            applyFilters()
        }
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