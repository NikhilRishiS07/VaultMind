package com.example.vaultmind.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.vaultmind.AppGraph
import com.example.vaultmind.R
import com.example.vaultmind.data.WidgetStore
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfigActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val listView = findViewById<ListView>(R.id.publicNotesList)

        // Only public notes are selectable for widget
        val repository = AppGraph.repository(this)
        CoroutineScope(Dispatchers.Main).launch {
            val notes = repository.getPublicNotes()
            val titles = notes.map { note -> note.title }
            val adapter = ArrayAdapter(this@WidgetConfigActivity, android.R.layout.simple_list_item_1, titles)
            listView.adapter = adapter

            listView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>, _: View, position: Int, _: Long ->
                val note = notes[position]
                val widgetStore = WidgetStore(this@WidgetConfigActivity)
                widgetStore.setSticky(appWidgetId, note.id, note.title, note.preview.take(160))

                val resultValue = Intent()
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(Activity.RESULT_OK, resultValue)
                StickyNoteWidgetProvider.refreshAll(this@WidgetConfigActivity)
                finish()
            }

            if (notes.isEmpty()) {
                AlertDialog.Builder(this@WidgetConfigActivity)
                    .setTitle("No public notes")
                    .setMessage("Mark at least one note as Public to show it on the widget.")
                    .setPositiveButton("OK") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            }
        }
    }
}
