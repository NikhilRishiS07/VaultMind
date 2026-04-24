package com.example.vaultmind.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.AppGraph
import com.example.vaultmind.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class NoteEditorFragment : Fragment() {
    private val repository by lazy { AppGraph.repository(requireContext()) }
    private lateinit var groqSuggestionsViewModel: GroqSuggestionsViewModel
    private lateinit var groqSuggestionAdapter: GroqSuggestionAdapter

    private var isLocked = false
    private var isPinned = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_note_editor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleInput = view.findViewById<EditText>(R.id.noteTitleInput)
        val bodyInput = view.findViewById<EditText>(R.id.noteBodyInput)
        val statusText = view.findViewById<TextView>(R.id.draftStatusText)
        val generateSuggestionsButton = view.findViewById<MaterialButton>(R.id.generateSuggestionsButton)
        val suggestionsStateText = view.findViewById<TextView>(R.id.groqSuggestionsStateText)
        val suggestionsRecycler = view.findViewById<RecyclerView>(R.id.groqSuggestionsRecycler)
        val lockButton = view.findViewById<MaterialButton>(R.id.lockNoteButton)
        val pinButton = view.findViewById<MaterialButton>(R.id.pinNoteButton)

        groqSuggestionAdapter = GroqSuggestionAdapter { suggestion ->
            val currentText = bodyInput.text?.toString().orEmpty()
            val needsLeadingSpace = currentText.isNotBlank() && !currentText.last().isWhitespace()
            val prefix = if (needsLeadingSpace) " " else ""
            val suffix = if (suggestion.endsWith(" ")) "" else " "
            bodyInput.append(prefix + suggestion + suffix)
        }

        suggestionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        suggestionsRecycler.adapter = groqSuggestionAdapter

        groqSuggestionsViewModel = ViewModelProvider(
            this,
            GroqSuggestionsViewModelFactory(AppGraph.groqSuggestionsRepository(requireContext()))
        )[GroqSuggestionsViewModel::class.java]

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                groqSuggestionsViewModel.uiState.collect { state ->
                    renderSuggestionState(
                        state = state,
                        button = generateSuggestionsButton,
                        stateText = suggestionsStateText,
                        recyclerView = suggestionsRecycler
                    )
                }
            }
        }

        val initialTitle = arguments?.getString(NotesFragment.ARG_NOTE_TITLE).orEmpty()
        val initialBody = arguments?.getString(NotesFragment.ARG_NOTE_BODY).orEmpty()
        val initialCategory = arguments?.getString(NotesFragment.ARG_NOTE_CATEGORY).orEmpty()
        val initialLocked = arguments?.getBoolean(NotesFragment.ARG_NOTE_LOCKED, false) ?: false
        val initialPinned = arguments?.getBoolean(NotesFragment.ARG_NOTE_PINNED, false) ?: false

        if (initialTitle.isNotBlank()) {
            titleInput.setText(initialTitle)
        }
        if (initialBody.isNotBlank()) {
            bodyInput.setText(initialBody)
        }

        isLocked = initialLocked || initialCategory.equals("Locked", ignoreCase = true)
        isPinned = initialPinned
        lockButton.text = if (isLocked) "Locked" else "Lock"
        pinButton.text = if (isPinned) "Pinned" else "Pin"

        titleInput.addTextChangedListener { _ ->
            groqSuggestionsViewModel.reset()
        }

        bodyInput.addTextChangedListener { _ ->
            groqSuggestionsViewModel.reset()
        }

        generateSuggestionsButton.setOnClickListener {
            val noteTitle = titleInput.text?.toString().orEmpty().trim()
            val noteBody = bodyInput.text?.toString().orEmpty().trim()

            if (noteTitle.isBlank() && noteBody.isBlank()) {
                groqSuggestionsViewModel.reset()
                suggestionsStateText.text = "Type a note first, then tap Generate Suggestions."
                groqSuggestionAdapter.submitList(emptyList())
                return@setOnClickListener
            }

            groqSuggestionsViewModel.generateSuggestions(
                noteTitle = noteTitle,
                noteBody = noteBody
            )
        }

        view.findViewById<MaterialButton>(R.id.saveNoteButton).setOnClickListener {
            val title = titleInput.text?.toString().orEmpty().trim()
            val body = bodyInput.text?.toString().orEmpty().trim()

            if (title.isBlank() || body.isBlank()) {
                Toast.makeText(requireContext(), "Title and body are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                repository.saveNote(
                    title = title,
                    body = body,
                    category = if (isLocked) "Locked" else "Personal",
                    locked = isLocked,
                    pinned = isPinned
                )
                statusText.text = "Draft saved just now"
                Toast.makeText(requireContext(), "Encrypted note saved", Toast.LENGTH_SHORT).show()
            }
        }

        lockButton.setOnClickListener {
            isLocked = !isLocked
            lockButton.text = if (isLocked) "Locked" else "Lock"
            Toast.makeText(requireContext(), if (isLocked) "Note will be locked" else "Note unlocked", Toast.LENGTH_SHORT).show()
        }

        pinButton.setOnClickListener {
            isPinned = !isPinned
            pinButton.text = if (isPinned) "Pinned" else "Pin"
            Toast.makeText(requireContext(), if (isPinned) "Note pinned" else "Pin removed", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.shareNoteButton).setOnClickListener {
            Toast.makeText(requireContext(), "Share action ready", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.deleteNoteButton).setOnClickListener {
            titleInput.setText("")
            bodyInput.setText("")
            groqSuggestionsViewModel.reset()
            groqSuggestionAdapter.submitList(emptyList())
            Toast.makeText(requireContext(), "Note cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderSuggestionState(
        state: GroqSuggestionsUiState,
        button: MaterialButton,
        stateText: TextView,
        recyclerView: RecyclerView
    ) {
        when (state) {
            GroqSuggestionsUiState.Idle -> {
                button.isEnabled = true
                stateText.text = "Tap Generate Suggestions to get 3 short productivity tips."
                groqSuggestionAdapter.submitList(emptyList())
                recyclerView.visibility = View.GONE
            }
            GroqSuggestionsUiState.Loading -> {
                button.isEnabled = false
                stateText.text = "Generating Groq suggestions..."
                groqSuggestionAdapter.submitList(emptyList())
                recyclerView.visibility = View.VISIBLE
            }
            is GroqSuggestionsUiState.Success -> {
                button.isEnabled = true
                stateText.text = "Groq suggestions ready"
                groqSuggestionAdapter.submitList(state.suggestions)
                recyclerView.visibility = View.VISIBLE
            }
            is GroqSuggestionsUiState.Error -> {
                button.isEnabled = true
                stateText.text = state.message
                groqSuggestionAdapter.submitList(emptyList())
                recyclerView.visibility = View.GONE
            }
        }
    }
}

class GroqSuggestionsViewModelFactory(
    private val repository: GroqSuggestionsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroqSuggestionsViewModel::class.java)) {
            return GroqSuggestionsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}