package com.example.vaultmind.passwords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.AppGraph
import com.example.vaultmind.R
import kotlinx.coroutines.launch

class PasswordsFragment : Fragment() {
    private val repository by lazy { AppGraph.repository(requireContext()) }
    private var allEntries = emptyList<PasswordEntry>()
    private val adapter by lazy { PasswordsAdapter() }
    private var currentQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_passwords, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.passwordsRecycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PasswordsFragment.adapter
        }

        view.findViewById<EditText>(R.id.passwordsSearchInput).addTextChangedListener { text ->
            currentQuery = text?.toString().orEmpty()
            applyFilter()
        }

        view.findViewById<View>(R.id.addPasswordFab).setOnClickListener { showAddPasswordDialog() }

        loadPasswords()
    }

    override fun onResume() {
        super.onResume()
        loadPasswords()
    }

    private fun applyFilter() {
        val filtered = allEntries.filter {
            currentQuery.isBlank() || it.service.contains(currentQuery, ignoreCase = true) || it.username.contains(currentQuery, ignoreCase = true)
        }
        adapter.submitList(filtered)
    }

    private fun loadPasswords() {
        viewLifecycleOwner.lifecycleScope.launch {
            allEntries = repository.getPasswords().map {
                PasswordEntry(
                    service = it.service,
                    username = it.username,
                    password = it.password,
                    strength = it.strength
                )
            }
            applyFilter()
        }
    }

    private fun showAddPasswordDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }

        val serviceInput = EditText(requireContext()).apply { hint = "Service" }
        val usernameInput = EditText(requireContext()).apply { hint = "Username" }
        val passwordInput = EditText(requireContext()).apply { hint = "Password" }

        container.addView(serviceInput)
        container.addView(usernameInput)
        container.addView(passwordInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Password")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val service = serviceInput.text?.toString().orEmpty().trim()
                val username = usernameInput.text?.toString().orEmpty().trim()
                val password = passwordInput.text?.toString().orEmpty().trim()

                if (service.isBlank() || username.isBlank() || password.isBlank()) {
                    Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    repository.savePassword(service, username, password, estimateStrength(password))
                    loadPasswords()
                    Toast.makeText(requireContext(), "Encrypted password saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun estimateStrength(password: String): String {
        val score = listOf(
            password.length >= 12,
            password.any { it.isUpperCase() },
            password.any { it.isLowerCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() }
        ).count { it }

        return when {
            score >= 5 -> "Strong"
            score >= 3 -> "Good"
            else -> "Weak"
        }
    }

    private data class PasswordEntry(
        val service: String,
        val username: String,
        val password: String,
        val strength: String
    )

    private inner class PasswordsAdapter : RecyclerView.Adapter<PasswordsAdapter.PasswordViewHolder>() {
        private val items = mutableListOf<PasswordEntry>()

        fun submitList(newItems: List<PasswordEntry>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
            val itemView = layoutInflater.inflate(R.layout.item_password_entry, parent, false)
            return PasswordViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class PasswordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val icon: TextView = itemView.findViewById(R.id.passwordServiceIcon)
            private val serviceName: TextView = itemView.findViewById(R.id.passwordServiceName)
            private val usernameText: TextView = itemView.findViewById(R.id.passwordUserText)
            private val dotsText: TextView = itemView.findViewById(R.id.passwordDotsText)
            private val strengthText: TextView = itemView.findViewById(R.id.passwordStrengthText)
            private val revealButton: View = itemView.findViewById(R.id.passwordRevealButton)
            private val copyButton: View = itemView.findViewById(R.id.passwordCopyButton)

            private var revealed = false
            private var boundPassword = ""

            fun bind(item: PasswordEntry) {
                icon.text = item.service.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
                serviceName.text = item.service
                usernameText.text = item.username
                strengthText.text = item.strength
                boundPassword = item.password
                revealed = false
                dotsText.text = "••••••••"

                revealButton.setOnClickListener {
                    revealed = !revealed
                    dotsText.text = if (revealed) boundPassword else "••••••••"
                    (revealButton as? TextView)?.text = if (revealed) "Hide" else "Reveal"
                }

                copyButton.setOnClickListener {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("password", boundPassword))
                    Toast.makeText(requireContext(), "Password copied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}