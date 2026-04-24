package com.example.vaultmind.notes

class GroqSuggestionsRepository(
    private val client: GroqSuggestionClient
) {
    suspend fun generateSuggestions(noteTitle: String, noteBody: String): List<String> {
        return client.generateSuggestions(noteTitle = noteTitle, noteBody = noteBody)
    }

    suspend fun testConnection(): String {
        return client.testConnection()
    }
}