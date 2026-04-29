package com.example.vaultmind.expenses

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.AppGraph
import com.example.vaultmind.R
import kotlinx.coroutines.launch
import java.util.Locale

class ExpensesFragment : Fragment() {
    private val repository by lazy { AppGraph.repository(requireContext()) }
    private var transactions = emptyList<Transaction>()
    private val adapter by lazy { TransactionAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_expenses, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.transactionsRecycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExpensesFragment.adapter
        }

        loadTransactions()
        setupBudgetSection(view)

        view.findViewById<View>(R.id.addExpenseFab).setOnClickListener {
            showAddExpenseDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun setupBudgetSection(view: View) {
        val editBudgetBtn = view.findViewById<View>(R.id.editBudgetBtn)
        val budgetText = view.findViewById<TextView>(R.id.budgetValueText)
        
        val prefs = requireContext().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
        val currentBudget = prefs.getLong("monthly_budget", 5000)
        budgetText.text = String.format(Locale.US, "₹%,d", currentBudget)
        
        editBudgetBtn?.setOnClickListener {
            val budgetInput = EditText(requireContext()).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(currentBudget.toString())
            }
            
            AlertDialog.Builder(requireContext())
                .setTitle("Set Monthly Budget")
                .setView(budgetInput)
                .setPositiveButton("Save") { _, _ ->
                    val newBudget = budgetInput.text.toString().toLongOrNull() ?: currentBudget
                    prefs.edit().putLong("monthly_budget", newBudget).apply()
                    budgetText.text = String.format(Locale.US, "₹%,d", newBudget)
                    updateBudgetUi()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            transactions = repository.getExpenses().map {
                Transaction(
                    id = it.id,
                    title = it.title,
                    subtitle = it.subtitle,
                    amount = it.amountText,
                    tag = it.tag
                )
            }
            adapter.submitList(transactions)
            view?.findViewById<TextView>(R.id.transactionsEmptyState)?.visibility =
                if (transactions.isEmpty()) View.VISIBLE else View.GONE
            updateBudgetUi()
        }
    }

    private fun updateBudgetUi() {
        val prefs = requireContext().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
        val budgetTotal = prefs.getLong("monthly_budget", 5000).toDouble()
        val spent = transactions.sumOf { parseAmount(it.amount) }
        val spentPositive = kotlin.math.abs(spent)
        val remaining = (budgetTotal - spentPositive).coerceAtLeast(0.0)
        val progress = ((spentPositive / budgetTotal) * 100).toInt().coerceIn(0, 100)

        view?.findViewById<android.widget.ProgressBar>(R.id.budgetProgressBar)?.progress = progress
        view?.findViewById<TextView>(R.id.budgetRemainingValue)?.text =
            String.format(Locale.US, "₹%,.0f remaining", remaining)
        view?.findViewById<TextView>(R.id.budgetSpentValue)?.text =
            String.format(Locale.US, "₹%,.0f spent of ₹%,d", spentPositive, budgetTotal.toLong())
    }

    private fun showAddExpenseDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }

        val titleInput = EditText(requireContext()).apply { hint = "Title" }
        val amountInput = EditText(requireContext()).apply {
            hint = "Amount (e.g. 45.90)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        container.addView(titleInput)
        container.addView(amountInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Expense")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val title = titleInput.text?.toString().orEmpty().trim()
                val amountValue = amountInput.text?.toString().orEmpty().toDoubleOrNull()

                if (title.isBlank() || amountValue == null) {
                    Toast.makeText(requireContext(), "Valid title and amount required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amountText = String.format(Locale.US, "-₹%,.2f", amountValue)
                val subtitle = "Today • Manual Entry"

                viewLifecycleOwner.lifecycleScope.launch {
                    repository.saveExpense(title, subtitle, amountText, -amountValue, "Secured")
                    loadTransactions()
                    Toast.makeText(requireContext(), "Expense saved", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun parseAmount(amountText: String): Double {
        val cleaned = amountText.replace("₹", "").replace(",", "").replace("-", "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private data class Transaction(
        val id: Long = 0,
        val title: String,
        val subtitle: String,
        val amount: String,
        val tag: String
    )

    private inner class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
        private val items = mutableListOf<Transaction>()

        fun submitList(newItems: List<Transaction>) {
            items.clear()
            items.addAll(newItems)
            notifyItemRangeChanged(0, items.size)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            val itemView = layoutInflater.inflate(R.layout.item_transaction, parent, false)
            return TransactionViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val titleText: TextView = itemView.findViewById(R.id.transactionTitleText)
            private val subtitleText: TextView = itemView.findViewById(R.id.transactionSubtitleText)
            private val amountText: TextView = itemView.findViewById(R.id.transactionAmountText)
            private val tagText: TextView = itemView.findViewById(R.id.transactionTagText)

            fun bind(item: Transaction) {
                titleText.text = item.title
                subtitleText.text = item.subtitle
                amountText.text = item.amount
                tagText.text = item.tag
                
                itemView.setOnLongClickListener {
                    showTransactionOptionsDialog(item)
                    true
                }
            }
        }
    }

    private fun showTransactionOptionsDialog(transaction: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle(transaction.title)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> showEditTransactionDialog(transaction)
                    1 -> deleteTransaction(transaction.id)
                }
            }
            .show()
    }

    private fun showEditTransactionDialog(transaction: Transaction) {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 0)
        }

        val titleInput = EditText(requireContext()).apply {
            hint = "Title"
            setText(transaction.title)
        }
        val amountInput = EditText(requireContext()).apply {
            hint = "Amount"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(parseAmount(transaction.amount).toString())
        }

        container.addView(titleInput)
        container.addView(amountInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Expense")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val newTitle = titleInput.text?.toString().orEmpty().trim()
                val newAmount = amountInput.text?.toString().orEmpty().toDoubleOrNull()

                if (newTitle.isBlank() || newAmount == null) {
                    Toast.makeText(requireContext(), "Valid title and amount required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val amountText = String.format(Locale.US, "-₹%,.2f", newAmount)
                    repository.updateExpense(
                        transaction.id,
                        newTitle,
                        transaction.subtitle,
                        amountText,
                        -newAmount,
                        transaction.tag
                    )
                    loadTransactions()
                    Toast.makeText(requireContext(), "Expense updated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transactionId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense?")
            .setMessage("This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteExpense(transactionId)
                    loadTransactions()
                    Toast.makeText(requireContext(), "Expense deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
