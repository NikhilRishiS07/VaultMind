package com.example.vaultmind.notes

sealed interface GroqSuggestionsUiState {
    data object Idle : GroqSuggestionsUiState
    data object Loading : GroqSuggestionsUiState
    data class Success(val suggestions: List<String>) : GroqSuggestionsUiState
    data class Error(val message: String) : GroqSuggestionsUiState
}