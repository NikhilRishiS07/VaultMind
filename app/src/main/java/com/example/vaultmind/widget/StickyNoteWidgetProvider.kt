package com.example.vaultmind.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.vaultmind.R
import com.example.vaultmind.data.WidgetStore

class StickyNoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val widgetStore = WidgetStore(context)
        appWidgetIds.forEach { widgetStore.clearSticky(it) }
    }

    private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val widgetStore = WidgetStore(context)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_sticky)
            val sticky = widgetStore.getSticky(appWidgetId)

            if (sticky == null) {
                views.setTextViewText(R.id.widget_title, "Note NA")
                views.setTextViewText(R.id.widget_snippet, "No public sticky note selected")
            } else {
                views.setTextViewText(R.id.widget_title, sticky.title)
                views.setTextViewText(R.id.widget_snippet, sticky.snippet)
            }

            // Open app when tapping widget body
            val intent = Intent(context, com.example.vaultmind.MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            // Open widget configuration from settings icon
            val configIntent = Intent(context, WidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val configPending = PendingIntent.getActivity(
                context,
                appWidgetId + 10_000,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_settings, configPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, StickyNoteWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(provider)
            if (ids.isNotEmpty()) {
                StickyNoteWidgetProvider().updateWidgets(context, mgr, ids)
            }
        }
    }
}
