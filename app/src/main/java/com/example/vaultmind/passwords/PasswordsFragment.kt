package com.example.vaultmind.passwords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.R

class PasswordsFragment : Fragment() {
    private val allEntries = listOf(
        PasswordEntry("Google Workspace", "alex.design@gmail.com", "Aex$2481#Vault", "Strong"),
        PasswordEntry("Netflix Premium", "home_vault_2024", "Ntfx@5520!", "Good"),
        PasswordEntry("Dropbox Pro", "alexander_vault", "drop-2026", "Weak"),
        PasswordEntry("Chase Private", "wm_user_091", "Chase#9090!", "Strong")
    )

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

        view.findViewById<View>(R.id.addPasswordFab).setOnClickListener {
            Toast.makeText(requireContext(), "Add password form comes next", Toast.LENGTH_SHORT).show()
        }

        applyFilter()
    }

    private fun applyFilter() {
        val filtered = allEntries.filter {
            currentQuery.isBlank() || it.service.contains(currentQuery, ignoreCase = true) || it.username.contains(currentQuery, ignoreCase = true)
        }
        adapter.submitList(filtered)
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