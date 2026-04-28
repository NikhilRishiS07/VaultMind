package com.example.vaultmind.notes

class GroqSuggestionsRepository(
    private val client: GroqSuggestionClient
) {
    suspend fun generateSuggestions(
        noteTitle: String,
        noteBody: String,
        cursorBeforeText: String,
        cursorAfterText: String,
        recentSuggestions: List<String>
    ): List<String> {
        return client.generateSuggestions(
            noteTitle = noteTitle,
            noteBody = noteBody,
            cursorBeforeText = cursorBeforeText,
            cursorAfterText = cursorAfterText,
            recentSuggestions = recentSuggestions
        )
    }

    suspend fun testConnection(): String {
        return client.testConnection()
    }
}