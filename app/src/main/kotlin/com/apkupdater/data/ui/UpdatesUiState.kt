package com.apkupdater.data.ui


sealed class UpdatesUiState {
	data class Loading(val stage: UpdateStage, val progress: Float): UpdatesUiState()
	data class Error(val message: String? = null) : UpdatesUiState()
	data class Success(val updates: List<AppUpdate>): UpdatesUiState()

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

enum class UpdateStage {
	CONNECTING,
	FETCHING,
	CHECKING,
	READY
}
