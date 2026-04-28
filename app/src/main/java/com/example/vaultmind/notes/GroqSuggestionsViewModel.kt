package com.example.vaultmind.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroqSuggestionsViewModel(
    private val repository: GroqSuggestionsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<GroqSuggestionsUiState>(GroqSuggestionsUiState.Idle)
    val uiState: StateFlow<GroqSuggestionsUiState> = _uiState.asStateFlow()
    private var requestVersion = 0
    private val recentSuggestions = ArrayDeque<String>()

    fun generateSuggestions(
        noteTitle: String,
        noteBody: String,
        cursorBeforeText: String,
        cursorAfterText: String
    ) {
        val trimmedTitle = noteTitle.trim()
        val trimmedBody = noteBody.trim()
        val trimmedBefore = cursorBeforeText.trim()
        val trimmedAfter = cursorAfterText.trim()

        if (trimmedTitle.isBlank() && trimmedBody.isBlank()) {
            _uiState.value = GroqSuggestionsUiState.Error("Type a title or note first")
            return
        }

        val activeVersion = ++requestVersion

        viewModelScope.launch {
            _uiState.value = GroqSuggestionsUiState.Loading
            runCatching {
                repository.generateSuggestions(
                    noteTitle = trimmedTitle,
                    noteBody = trimmedBody,
                    cursorBeforeText = trimmedBefore,
                    cursorAfterText = trimmedAfter,
                    recentSuggestions = recentSuggestions.toList()
                )
            }
                .onSuccess { suggestions ->
                    if (activeVersion != requestVersion) return@onSuccess
                    if (suggestions.isEmpty()) {
                        _uiState.value = GroqSuggestionsUiState.Error("Groq returned no suggestions")
                    } else {
                        rememberSuggestions(suggestions)
                        _uiState.value = GroqSuggestionsUiState.Success(suggestions)
                    }
                }
                .onFailure { error ->
                    if (activeVersion != requestVersion) return@onFailure
                    _uiState.value = GroqSuggestionsUiState.Error(error.message ?: "Groq request failed")
                }
        }
    }

    fun reset() {
        requestVersion++
        recentSuggestions.clear()
        _uiState.value = GroqSuggestionsUiState.Idle
    }

    private fun rememberSuggestions(suggestions: List<String>) {
        for (suggestion in suggestions) {
            val normalized = suggestion.trim().lowercase()
            if (normalized.isBlank()) continue
            recentSuggestions.removeAll { it.equals(normalized, ignoreCase = true) }
            recentSuggestions.addLast(normalized)
            while (recentSuggestions.size > 12) {
                recentSuggestions.removeFirst()
            }
        }
    }
}