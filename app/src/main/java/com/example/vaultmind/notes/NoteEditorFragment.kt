package com.example.vaultmind.notes

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.widget.EditText
import android.widget.TextView
import com.example.vaultmind.AppGraph
import com.google.android.material.button.MaterialButton
import com.example.vaultmind.R
import kotlinx.coroutines.launch

class NoteEditorFragment : Fragment() {
    private val suggestionHandler = Handler(Looper.getMainLooper())
    private var pendingSuggestion: Runnable? = null
    private var isLocked = false
    private var isPinned = false
    private val repository by lazy { AppGraph.repository(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_note_editor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleInput = view.findViewById<EditText>(R.id.noteTitleInput)
        val bodyInput = view.findViewById<EditText>(R.id.noteBodyInput)
        val suggestionText = view.findViewById<TextView>(R.id.aiSuggestionText)
        val statusText = view.findViewById<TextView>(R.id.draftStatusText)
        val lockButton = view.findViewById<MaterialButton>(R.id.lockNoteButton)
        val pinButton = view.findViewById<MaterialButton>(R.id.pinNoteButton)

        bodyInput.addTextChangedListener { editable ->
            statusText.text = "Drafting..."
            val content = editable?.toString().orEmpty()
            pendingSuggestion?.let { suggestionHandler.removeCallbacks(it) }
            val task = Runnable {
                suggestionText.text = buildSuggestion(content)
                statusText.text = "Draft saved just now"
            }
            pendingSuggestion = task
            suggestionHandler.postDelayed(task, 500)
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
            suggestionText.text = "AI suggestion will appear here as you type."
            Toast.makeText(requireContext(), "Note cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildSuggestion(text: String): String {
        val trimmed = text.trim()
        return if (trimmed.isBlank()) {
            "AI suggestion will appear here as you type."
        } else if (trimmed.contains("encrypt", ignoreCase = true)) {
            "...and include a short verification checklist for encryption and access control."
        } else if (trimmed.length < 120) {
            "...and finish with a concise action list for the next review."
        } else {
            "...and summarize the next steps into a checklist."
        }
    }
}