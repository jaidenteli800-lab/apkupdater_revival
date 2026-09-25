package com.apkupdater.data.ui


sealed class SearchUiState {
    data object Loading: SearchUiState()
    data class Error(val message: String? = null) : SearchUiState()
    data class Success(val updates: List<AppUpdate>): SearchUiState()

    fun mutableUpdates(): MutableList<AppUpdate> {
        if (this is Success) {
            return updates.toMutableList()
        }
        return mutableListOf()
    }

    fun updates(): List<AppUpdate> {
        if (this is Success) {
            return updates
        }
        return emptyList()
    }
}
