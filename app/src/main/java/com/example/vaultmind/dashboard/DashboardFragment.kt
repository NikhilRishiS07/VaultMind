package com.example.vaultmind.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.AppGraph
import com.example.vaultmind.R
import com.example.vaultmind.data.auth.AppLockManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

class DashboardFragment : Fragment() {
    private val repository by lazy { AppGraph.repository(requireContext()) }
    private val lockManager by lazy { AppLockManager(requireContext()) }
    private val activities = mutableListOf<RecentActivity>()
    private val activityAdapter by lazy { ActivityAdapter() }
    private var dashboardRenderedOnce = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dashboardRenderedOnce = false
        
        val username = lockManager.tempUsername()
        view.findViewById<TextView>(R.id.greetingText).text = "Greetings $username, your vault is ready."

        view.findViewById<View>(R.id.dashboardContent).alpha = 0f

        view.findViewById<RecyclerView>(R.id.recentActivityRecycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activityAdapter
        }

        refreshDashboard(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { 
            refreshDashboard(it)
        }
    }

    private fun refreshDashboard(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            val summary = repository.dashboardSummary()
            val recentItems = loadRecentActivityItems()

            view.findViewById<TextView>(R.id.notesCountText).text = summary.notesCount.toString()
            view.findViewById<TextView>(R.id.passwordsCountText).text = summary.passwordsCount.toString()

            val budgetSpent = kotlin.math.abs(summary.spend)
            val prefs = requireContext().getSharedPreferences("dashboard_prefs", Context.MODE_PRIVATE)
            val currentBudget = prefs.getLong("monthly_budget", 5000).toDouble()
            val remaining = (currentBudget - budgetSpent).coerceAtLeast(0.0)
            val progress = ((budgetSpent / currentBudget) * 100).toInt().coerceIn(0, 100)

            view.findViewById<TextView>(R.id.budgetSpentLabel)?.text = String.format(Locale.US, "₹%,.0f", budgetSpent)
            view.findViewById<TextView>(R.id.budgetLimitLabel)?.text = String.format(Locale.US, "₹%,d limit", currentBudget.toLong())
            view.findViewById<TextView>(R.id.budgetRemainingLabel)?.text = String.format(Locale.US, "₹%,.0f remaining", remaining)
            view.findViewById<android.widget.ProgressBar>(R.id.budgetCircleProgress)?.progress = progress

            activities.clear()
            activities.addAll(recentItems)
            activityAdapter.submitList(activities)

            val dashboardContent = view.findViewById<View>(R.id.dashboardContent)
            if (!dashboardRenderedOnce) {
                dashboardRenderedOnce = true
                dashboardContent.animate()
                    .alpha(1f)
                    .setDuration(240)
                    .start()
            }
        }
    }

    private suspend fun loadRecentActivityItems(): List<RecentActivity> = coroutineScope {
        val notesDeferred = async { repository.getNotes().take(3) }
        val expensesDeferred = async { repository.getExpenses().take(3) }

        val notes = notesDeferred.await()
        val expenses = expensesDeferred.await()

        val noteActivities = notes.map { note ->
            RecentActivity(
                iconLetter = "N",
                title = note.title,
                subtitle = note.category.ifBlank { note.lastEdited },
                status = note.lastEdited
            )
        }

        val expenseActivities = expenses.map { expense ->
            RecentActivity(
                iconLetter = "E",
                title = expense.title,
                subtitle = expense.subtitle,
                status = expense.tag.ifBlank { "Spent" }
            )
        }

        (noteActivities + expenseActivities).take(6)
    }

    private data class RecentActivity(
        val iconLetter: String,
        val title: String,
        val subtitle: String,
        val status: String
    )

    private inner class ActivityAdapter : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {
        private val items = mutableListOf<RecentActivity>()

        fun submitList(newItems: List<RecentActivity>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

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