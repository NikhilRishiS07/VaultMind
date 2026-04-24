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

    fun generateSuggestions(noteTitle: String, noteBody: String) {
        val trimmedTitle = noteTitle.trim()
        val trimmedBody = noteBody.trim()

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
                    noteBody = trimmedBody
                )
            }
                .onSuccess { suggestions ->
                    if (activeVersion != requestVersion) return@onSuccess
                    if (suggestions.isEmpty()) {
                        _uiState.value = GroqSuggestionsUiState.Error("Groq returned no suggestions")
                    } else {
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
        _uiState.value = GroqSuggestionsUiState.Idle
    }
}