package com.example.vaultmind.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.AppGraph
import com.example.vaultmind.R
import com.example.vaultmind.data.auth.AppLockManager
import kotlinx.coroutines.launch
import java.util.Locale

class DashboardFragment : Fragment() {
    private val repository by lazy { AppGraph.repository(requireContext()) }
    private val lockManager by lazy { AppLockManager(requireContext()) }
    private val activities = mutableListOf<RecentActivity>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val username = lockManager.tempUsername()
        view.findViewById<TextView>(R.id.greetingText).text = "Greetings $username, you are currently logged in."
        
        loadSummary(view)
        loadRecentActivity(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { 
            loadSummary(it)
            loadRecentActivity(it)
        }
    }

    private fun loadSummary(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            val summary = repository.dashboardSummary()
            view.findViewById<TextView>(R.id.notesCountText).text = summary.notesCount.toString()
            view.findViewById<TextView>(R.id.passwordsCountText).text = summary.passwordsCount.toString()
            setupBudgetEditing(view)
        }
    }

    private fun loadRecentActivity(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Load notes and expenses
            val notes = repository.getNotes().take(3)
            val expenses = repository.getExpenses().take(2)
            
            // Create activity items from notes and expenses
            val noteActivities = notes.map { note ->
                RecentActivity("N", "Note created", note.title, "")
            }
            
            val expenseActivities = expenses.map { expense ->
                RecentActivity("E", "Expense added", expense.title, "")
            }
            
            // Combine all activities
            activities.clear()
            activities.addAll(noteActivities + expenseActivities)
            
            val recycler = view.findViewById<RecyclerView>(R.id.recentActivityRecycler)
            recycler.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = ActivityAdapter(activities)
            }
        }
    }

    private fun setupBudgetEditing(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            val prefs = requireContext().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
            val currentBudget = prefs.getLong("monthly_budget", 5000)
            val spend = repository.dashboardSummary().spend
            
            view.findViewById<TextView>(R.id.budgetSpentLabel)?.text = String.format(Locale.US, "₹%,.0f", kotlin.math.abs(spend))
            view.findViewById<TextView>(R.id.budgetLimitLabel)?.text = String.format(Locale.US, "₹%,d limit", currentBudget)
            view.findViewById<TextView>(R.id.budgetRemainingLabel)?.text = String.format(Locale.US, "₹%,d remaining", (currentBudget - kotlin.math.abs(spend).toLong()).coerceAtLeast(0L))
            
            val progress = ((kotlin.math.abs(spend) / currentBudget.toDouble()) * 100).toInt().coerceIn(0, 100)
            view.findViewById<android.widget.ProgressBar>(R.id.budgetCircleProgress)?.progress = progress
        }
    }

    private data class RecentActivity(
        val iconLetter: String,
        val title: String,
        val subtitle: String,
        val status: String
    )

    private inner class ActivityAdapter(
        private val items: List<RecentActivity>
    ) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
            val itemView = layoutInflater.inflate(R.layout.item_activity_log, parent, false)
            return ActivityViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val iconText: TextView = itemView.findViewById(R.id.activityIconText)
            private val titleText: TextView = itemView.findViewById(R.id.activityTitleText)
            private val subtitleText: TextView = itemView.findViewById(R.id.activitySubtitleText)
            private val statusText: TextView = itemView.findViewById(R.id.activityStatusText)

            fun bind(item: RecentActivity) {
                iconText.text = item.iconLetter
                titleText.text = item.title
                subtitleText.text = item.subtitle
                statusText.text = item.status
            }
        }
    }
}