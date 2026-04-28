package com.example.vaultmind.data

import android.content.Context

class WidgetStore(private val context: Context) {
    private val prefs by lazy { context.getSharedPreferences("vaultmind_widget", Context.MODE_PRIVATE) }

    fun setSticky(appWidgetId: Int, noteId: Long, title: String, snippet: String) {
        prefs.edit()
            .putLong(keyNoteId(appWidgetId), noteId)
            .putString(keyTitle(appWidgetId), title)
            .putString(keySnippet(appWidgetId), snippet)
            .putLong(keyUpdatedAt(appWidgetId), System.currentTimeMillis())
            .apply()
    }

    fun clearSticky(appWidgetId: Int) {
        prefs.edit()
            .remove(keyNoteId(appWidgetId))
            .remove(keyTitle(appWidgetId))
            .remove(keySnippet(appWidgetId))
            .remove(keyUpdatedAt(appWidgetId))
            .apply()
    }

    fun getSticky(appWidgetId: Int): Sticky? {
        val noteId = prefs.getLong(keyNoteId(appWidgetId), -1L)
        if (noteId < 0) return null
        val title = prefs.getString(keyTitle(appWidgetId), null) ?: return null
        val snippet = prefs.getString(keySnippet(appWidgetId), null) ?: ""
        return Sticky(noteId, title, snippet, prefs.getLong(keyUpdatedAt(appWidgetId), 0L))
    }

    fun syncNoteChange(noteId: Long, title: String, snippet: String, isPublic: Boolean) {
        val keys = prefs.all.keys
        val widgetIds = keys.mapNotNull { key ->
            if (key.startsWith(KEY_NOTE_ID_PREFIX)) key.removePrefix(KEY_NOTE_ID_PREFIX).toIntOrNull() else null
        }.toSet()

        val editor = prefs.edit()
        for (widgetId in widgetIds) {
            val selectedNoteId = prefs.getLong(keyNoteId(widgetId), -1L)
            if (selectedNoteId != noteId) continue

            if (isPublic) {
                editor.putString(keyTitle(widgetId), title)
                editor.putString(keySnippet(widgetId), snippet)
                editor.putLong(keyUpdatedAt(widgetId), System.currentTimeMillis())
            } else {
                editor.remove(keyNoteId(widgetId))
                editor.remove(keyTitle(widgetId))
                editor.remove(keySnippet(widgetId))
                editor.remove(keyUpdatedAt(widgetId))
            }
        }
        editor.apply()
    }

    data class Sticky(val id: Long, val title: String, val snippet: String, val updatedAt: Long)

    companion object {
        private const val KEY_NOTE_ID_PREFIX = "sticky_note_id_"
        private const val KEY_TITLE_PREFIX = "sticky_title_"
        private const val KEY_SNIPPET_PREFIX = "sticky_snippet_"
        private const val KEY_UPDATED_AT_PREFIX = "sticky_updated_at_"

        private fun keyNoteId(appWidgetId: Int) = "$KEY_NOTE_ID_PREFIX$appWidgetId"
        private fun keyTitle(appWidgetId: Int) = "$KEY_TITLE_PREFIX$appWidgetId"
        private fun keySnippet(appWidgetId: Int) = "$KEY_SNIPPET_PREFIX$appWidgetId"
        private fun keyUpdatedAt(appWidgetId: Int) = "$KEY_UPDATED_AT_PREFIX$appWidgetId"
    }
}
