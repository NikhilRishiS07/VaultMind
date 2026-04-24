package com.example.vaultmind.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.vaultmind.AppGraph
import com.example.vaultmind.R
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {
    private val repository by lazy { AppGraph.repository(requireContext()) }

    private val activities = listOf(
        RecentActivity("D", "Modified Note • #8412", "Encrypted metadata sync", "OK"),
        RecentActivity("K", "Access Key Generated", "Security module refresh", "OK"),
        RecentActivity("S", "Cloud Handshake", "Node verification complete", "OK")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSummary(view)

        view.findViewById<RecyclerView>(R.id.recentActivityRecycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ActivityAdapter(activities)
        }

        view.findViewById<View>(R.id.unlockVaultButton).setOnClickListener {
            Toast.makeText(requireContext(), "Vault unlocked for demo mode", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadSummary(it) }
    }

    private fun loadSummary(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            val summary = repository.dashboardSummary()
            view.findViewById<TextView>(R.id.notesCountText).text = summary.notesCount.toString()
            view.findViewById<TextView>(R.id.passwordsCountText).text = summary.passwordsCount.toString()
            view.findViewById<TextView>(R.id.spendValueText).text = summary.spendText
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