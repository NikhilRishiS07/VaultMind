package com.example.vaultmind.expenses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.R

class ExpensesFragment : Fragment() {
    private val transactions = listOf(
        Transaction("Apple Store Online", "Yesterday, 4:20 PM • Electronics", "-$1,299.00", "Secured"),
        Transaction("The Daily Grind", "Today, 9:15 AM • Food & Drink", "-$18.40", "Secured"),
        Transaction("Nimbus Subscription", "Today, 8:10 AM • Software", "-$39.99", "Secured")
    )

    private val adapter by lazy { TransactionAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_expenses, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val totalSpent = 3200 - 2450
        val budgetProgress = view.findViewById<android.widget.ProgressBar>(R.id.budgetProgressBar)
        budgetProgress.progress = 75
        view.findViewById<TextView>(R.id.budgetRemainingValue).text = "$2,450 remaining"
        view.findViewById<TextView>(R.id.budgetSpentValue).text = "$totalSpent spent of $3,200"

        view.findViewById<RecyclerView>(R.id.transactionsRecycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExpensesFragment.adapter
        }

        adapter.submitList(transactions)

        view.findViewById<View>(R.id.addExpenseFab).setOnClickListener {
            Toast.makeText(requireContext(), "Add expense flow comes next", Toast.LENGTH_SHORT).show()
        }
    }

    private data class Transaction(
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
            notifyDataSetChanged()
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
            }
        }
    }
}