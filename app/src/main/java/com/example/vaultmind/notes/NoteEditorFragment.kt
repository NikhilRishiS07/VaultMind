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
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.example.vaultmind.R

class NoteEditorFragment : Fragment() {
    private val suggestionHandler = Handler(Looper.getMainLooper())
    private var pendingSuggestion: Runnable? = null

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
            Toast.makeText(requireContext(), "Note saved locally", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.lockNoteButton).setOnClickListener {
            Toast.makeText(requireContext(), "Note locked", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<MaterialButton>(R.id.pinNoteButton).setOnClickListener {
            Toast.makeText(requireContext(), "Note pinned", Toast.LENGTH_SHORT).show()
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